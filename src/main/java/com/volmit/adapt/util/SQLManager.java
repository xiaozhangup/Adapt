package com.volmit.adapt.util;

import com.volmit.adapt.AdaptConfig;
import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Owns Adapt's single JDBC connection. Every JDBC operation is serialized on
 * the dedicated executor; callers never use the connection from a Bukkit
 * thread.
 */
public class SQLManager {
    private static final Logger LOGGER = Logger.getLogger("Adapt-SQL");
    private static final String TABLE_NAME = "ADAPT_DATA";
    private static final long LEASE_MILLIS = TimeUnit.MINUTES.toMillis(2);

    private static final String CREATE_TABLE_QUERY = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " ("
            + "UUID CHAR(36) NOT NULL PRIMARY KEY, "
            + "DATA MEDIUMTEXT NOT NULL, "
            + "TIME BIGINT NOT NULL DEFAULT 0, "
            + "SESSION_ID VARCHAR(36) NULL, "
            + "LEASE_UNTIL BIGINT NOT NULL DEFAULT 0)";
    private static final String ACQUIRE_SELECT_QUERY = "SELECT DATA, SESSION_ID, LEASE_UNTIL, "
            + "CAST(UNIX_TIMESTAMP(CURRENT_TIMESTAMP(3)) * 1000 AS SIGNED) AS DB_NOW "
            + "FROM " + TABLE_NAME + " WHERE UUID=? FOR UPDATE";
    private static final String ACQUIRE_UPDATE_QUERY = "UPDATE " + TABLE_NAME
            + " SET SESSION_ID=?, LEASE_UNTIL=?, TIME=? WHERE UUID=?";
    private static final String ACQUIRE_INSERT_QUERY = "INSERT INTO " + TABLE_NAME
            + " (UUID, DATA, TIME, SESSION_ID, LEASE_UNTIL) VALUES (?, ?, ?, ?, ?)";
    private static final String SAVE_QUERY = "UPDATE " + TABLE_NAME
            + " SET DATA=?, TIME=?, LEASE_UNTIL=? WHERE UUID=? AND SESSION_ID=?";
    private static final String RELEASE_QUERY = "UPDATE " + TABLE_NAME
            + " SET DATA=?, TIME=0, SESSION_ID=NULL, LEASE_UNTIL=0 "
            + "WHERE UUID=? AND SESSION_ID=?";
    private static final String RELEASE_UNCHANGED_QUERY = "UPDATE " + TABLE_NAME
            + " SET TIME=0, SESSION_ID=NULL, LEASE_UNTIL=0 WHERE UUID=? AND SESSION_ID=?";
    private static final String DELETE_QUERY = "DELETE FROM " + TABLE_NAME + " WHERE UUID=? AND SESSION_ID=?";
    private static final String FETCH_QUERY = "SELECT DATA FROM " + TABLE_NAME + " WHERE UUID=?";

    private final ExecutorService databaseExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "Adapt-SQL");
        thread.setDaemon(true);
        return thread;
    });
    private final ConcurrentLinkedHashMap<UUID, String> dataCache =
            new ConcurrentLinkedHashMap.Builder<UUID, String>()
                    .maximumWeightedCapacity(4096)
                    .concurrencyLevel(4)
                    .build();
    private final AtomicBoolean accepting = new AtomicBoolean(true);
    private final AtomicBoolean closing = new AtomicBoolean(false);
    private final AtomicReference<Throwable> terminalReleaseFailure = new AtomicReference<>();
    private final Object submissionGate = new Object();
    private final ConcurrentHashMap<UUID, CompletableFuture<FetchResult>> pendingFetches = new ConcurrentHashMap<>();
    private volatile DatabaseSettings settings;
    private Connection connection;

    public CompletableFuture<Void> establishConnection() {
        DatabaseSettings captured = DatabaseSettings.capture(AdaptConfig.get());
        settings = captured;
        CompletableFuture<Void> connectionAttempt = submit(() -> {
            ensureConnection(captured);
            return null;
        });
        connectionAttempt.whenComplete((ignored, error) -> {
            if (error != null) {
                LOGGER.log(Level.SEVERE, "Failed to establish the initial SQL connection", error);
            }
        });
        return connectionAttempt;
    }

    /**
     * Waits for all previously queued writes, closes the JDBC connection on its
     * owner thread, and then terminates that thread.
     */
    public void closeConnection() {
        if (!closing.compareAndSet(false, true)) {
            return;
        }

        long timeout = settings == null ? 10_000L : Math.max(10_000L, settings.connectionTimeoutMillis() * 2L);
        RuntimeException failure = null;
        CompletableFuture<Void> barrier;
        synchronized (submissionGate) {
            barrier = submitClosing(() -> null);
        }
        try {
            // Pending acquire completions run before this barrier and may enqueue a
            // compensating release for a player who disconnected while loading.
            barrier.get(timeout, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            failure = new IllegalStateException("Interrupted while reaching the SQL shutdown barrier", e);
        } catch (ExecutionException | TimeoutException e) {
            failure = new IllegalStateException("Failed to reach the SQL shutdown barrier", e);
        }

        CompletableFuture<Void> close;
        synchronized (submissionGate) {
            accepting.set(false);
            // No submitter can pass its accepting check between this transition
            // and the final close entering the FIFO executor queue.
            close = submitClosing(() -> {
                closeDirect();
                return null;
            });
        }
        try {
            close.get(timeout, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            RuntimeException closeFailure = new IllegalStateException("Interrupted while closing SQL", e);
            if (failure == null) {
                failure = closeFailure;
            } else {
                failure.addSuppressed(closeFailure);
            }
        } catch (ExecutionException | TimeoutException e) {
            RuntimeException closeFailure = new IllegalStateException("Failed to flush and close SQL", e);
            if (failure == null) {
                failure = closeFailure;
            } else {
                failure.addSuppressed(closeFailure);
            }
        } finally {
            databaseExecutor.shutdown();
            try {
                if (!databaseExecutor.awaitTermination(timeout, TimeUnit.MILLISECONDS)) {
                    databaseExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                databaseExecutor.shutdownNow();
            }
        }
        Throwable releaseError = terminalReleaseFailure.get();
        if (releaseError != null) {
            RuntimeException releaseFailure = new IllegalStateException(
                    "One or more final SQL session releases failed", releaseError);
            if (failure == null) {
                failure = releaseFailure;
            } else {
                failure.addSuppressed(releaseFailure);
            }
        }
        if (failure != null) {
            LOGGER.log(Level.SEVERE, failure.getMessage(), failure);
            throw failure;
        }
    }

    /**
     * Atomically claims this player's SQL row for one server session. The local
     * fallback is evaluated only after a locked SELECT proved that no SQL row
     * exists; an SQL error can therefore never be mistaken for NOT_FOUND.
     */
    public CompletableFuture<AcquireResult> acquireSession(UUID uuid, String sessionId,
            Supplier<String> initialData) {
        if (!accepting.get()) {
            return CompletableFuture.completedFuture(AcquireResult.closed());
        }

        return submitHandled(() -> acquireSessionDirect(uuid, sessionId, initialData), AcquireResult::error);
    }

    public CompletableFuture<WriteResult> saveSession(UUID uuid, String sessionId, String data) {
        validateData(data);
        if (!accepting.get()) {
            return CompletableFuture.completedFuture(WriteResult.CLOSED);
        }

        return submitHandled(() -> {
            ensureConnection(currentSettings());
            long now = databaseNow();
            try (PreparedStatement statement = prepare(SAVE_QUERY)) {
                statement.setString(1, data);
                statement.setLong(2, now);
                statement.setLong(3, now + LEASE_MILLIS);
                statement.setString(4, uuid.toString());
                statement.setString(5, sessionId);
                if (statement.executeUpdate() == 0) {
                    return WriteResult.LOST_LEASE;
                }
            }
            dataCache.put(uuid, data);
            return WriteResult.STORED;
        }, throwable -> WriteResult.ERROR);
    }

    public CompletableFuture<WriteResult> releaseSession(UUID uuid, String sessionId, String data) {
        validateData(data);
        if (!accepting.get()) {
            return CompletableFuture.completedFuture(WriteResult.CLOSED);
        }

        return submitHandled(() -> {
            ensureConnection(currentSettings());
            try (PreparedStatement statement = prepare(RELEASE_QUERY)) {
                statement.setString(1, data);
                statement.setString(2, uuid.toString());
                statement.setString(3, sessionId);
                if (statement.executeUpdate() == 0) {
                    return WriteResult.LOST_LEASE;
                }
            }
            dataCache.put(uuid, data);
            return WriteResult.STORED;
        }, throwable -> {
            terminalReleaseFailure.compareAndSet(null, throwable);
            return WriteResult.ERROR;
        });
    }

    /** Releases an acquired claim without rewriting a row that could not be decoded. */
    public CompletableFuture<WriteResult> releaseSessionUnchanged(UUID uuid, String sessionId) {
        if (!accepting.get()) {
            return CompletableFuture.completedFuture(WriteResult.CLOSED);
        }

        return submitHandled(() -> {
            ensureConnection(currentSettings());
            try (PreparedStatement statement = prepare(RELEASE_UNCHANGED_QUERY)) {
                statement.setString(1, uuid.toString());
                statement.setString(2, sessionId);
                if (statement.executeUpdate() == 0) {
                    return WriteResult.LOST_LEASE;
                }
            }
            return WriteResult.STORED;
        }, throwable -> {
            terminalReleaseFailure.compareAndSet(null, throwable);
            return WriteResult.ERROR;
        });
    }

    public CompletableFuture<WriteResult> deleteSession(UUID uuid, String sessionId) {
        if (!accepting.get()) {
            return CompletableFuture.completedFuture(WriteResult.CLOSED);
        }

        return submitHandled(() -> {
            ensureConnection(currentSettings());
            try (PreparedStatement statement = prepare(DELETE_QUERY)) {
                statement.setString(1, uuid.toString());
                statement.setString(2, sessionId);
                if (statement.executeUpdate() == 0) {
                    return WriteResult.LOST_LEASE;
                }
            }
            dataCache.remove(uuid);
            return WriteResult.STORED;
        }, throwable -> WriteResult.ERROR);
    }

    public Optional<String> getCachedData(UUID uuid) {
        return Optional.ofNullable(dataCache.get(uuid));
    }

    /** Starts a non-blocking cache fill for callers such as peekData. */
    public CompletableFuture<FetchResult> fetchDataAsync(UUID uuid) {
        if (!accepting.get()) {
            return CompletableFuture.completedFuture(FetchResult.closed());
        }

        CompletableFuture<FetchResult> fetch = pendingFetches.computeIfAbsent(uuid, ignored -> submitHandled(() -> {
            ensureConnection(currentSettings());
            try (PreparedStatement statement = prepare(FETCH_QUERY)) {
                statement.setString(1, uuid.toString());
                try (ResultSet result = statement.executeQuery()) {
                    if (!result.next()) {
                        dataCache.remove(uuid);
                        return FetchResult.notFound();
                    }
                    String data = result.getString("DATA");
                    dataCache.put(uuid, data);
                    return FetchResult.found(data);
                }
            }
        }, FetchResult::error));
        fetch.whenComplete((result, error) -> pendingFetches.remove(uuid, fetch));
        return fetch;
    }

    private AcquireResult acquireSessionDirect(UUID uuid, String sessionId, Supplier<String> initialData)
            throws Exception {
        ensureConnection(currentSettings());
        boolean oldAutoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);
        try {
            try (PreparedStatement select = prepare(ACQUIRE_SELECT_QUERY)) {
                select.setString(1, uuid.toString());
                try (ResultSet result = select.executeQuery()) {
                    if (result.next()) {
                        String currentSession = result.getString("SESSION_ID");
                        long leaseUntil = result.getLong("LEASE_UNTIL");
                        long now = result.getLong("DB_NOW");
                        if (!SessionLease.canAcquire(currentSession, leaseUntil, sessionId, now)) {
                            connection.rollback();
                            return AcquireResult.busy(Math.max(1L, leaseUntil - now));
                        }

                        String data = result.getString("DATA");
                        try (PreparedStatement update = prepare(ACQUIRE_UPDATE_QUERY)) {
                            update.setString(1, sessionId);
                            update.setLong(2, now + LEASE_MILLIS);
                            update.setLong(3, now);
                            update.setString(4, uuid.toString());
                            update.executeUpdate();
                        }
                        connection.commit();
                        if (closing.get()) {
                            releaseClaimDuringShutdown(uuid, sessionId, data);
                            return AcquireResult.closed();
                        }
                        dataCache.put(uuid, data);
                        return AcquireResult.acquired(data, false);
                    }
                }
            }

            String data = initialData.get();
            validateData(data);
            long now = databaseNow();
            try (PreparedStatement insert = prepare(ACQUIRE_INSERT_QUERY)) {
                insert.setString(1, uuid.toString());
                insert.setString(2, data);
                insert.setLong(3, now);
                insert.setString(4, sessionId);
                insert.setLong(5, now + LEASE_MILLIS);
                insert.executeUpdate();
            }
            connection.commit();
            if (closing.get()) {
                releaseClaimDuringShutdown(uuid, sessionId, data);
                return AcquireResult.closed();
            }
            dataCache.put(uuid, data);
            return AcquireResult.acquired(data, true);
        } catch (Throwable e) {
            try {
                connection.rollback();
            } catch (SQLException rollbackError) {
                e.addSuppressed(rollbackError);
            }
            throw e;
        } finally {
            connection.setAutoCommit(oldAutoCommit);
        }
    }

    /** Runs on the connection owner while auto-commit is disabled. */
    private void releaseClaimDuringShutdown(UUID uuid, String sessionId, String data) throws SQLException {
        try {
            try (PreparedStatement statement = prepare(RELEASE_UNCHANGED_QUERY)) {
                statement.setString(1, uuid.toString());
                statement.setString(2, sessionId);
                if (statement.executeUpdate() != 1) {
                    throw new SQLException("New SQL claim could not be released during shutdown");
                }
            }
            connection.commit();
            dataCache.put(uuid, data);
        } catch (SQLException e) {
            terminalReleaseFailure.compareAndSet(null, e);
            throw e;
        }
    }

    private void setupDatabase() throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.setQueryTimeout(queryTimeoutSeconds());
            statement.executeUpdate(CREATE_TABLE_QUERY);
        }
        addColumnIfMissing("TIME", "BIGINT NOT NULL DEFAULT 0");
        addColumnIfMissing("SESSION_ID", "VARCHAR(36) NULL");
        addColumnIfMissing("LEASE_UNTIL", "BIGINT NOT NULL DEFAULT 0");
    }

    private void addColumnIfMissing(String column, String definition) throws SQLException {
        boolean exists;
        try (PreparedStatement statement = prepare("SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS "
                + "WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME=? AND COLUMN_NAME=?")) {
            statement.setString(1, TABLE_NAME);
            statement.setString(2, column);
            try (ResultSet columns = statement.executeQuery()) {
                exists = columns.next();
            }
        }
        if (!exists) {
            try (Statement statement = connection.createStatement()) {
                statement.setQueryTimeout(queryTimeoutSeconds());
                try {
                    statement.executeUpdate("ALTER TABLE " + TABLE_NAME + " ADD COLUMN " + column + " "
                            + definition);
                } catch (SQLException e) {
                    // Another server may have completed the same online migration.
                    if (e.getErrorCode() != 1060) {
                        throw e;
                    }
                }
            }
        }
    }

    private PreparedStatement prepare(String query) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(query);
        statement.setQueryTimeout(queryTimeoutSeconds());
        return statement;
    }

    private long databaseNow() throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.setQueryTimeout(queryTimeoutSeconds());
            try (ResultSet result = statement.executeQuery(
                    "SELECT CAST(UNIX_TIMESTAMP(CURRENT_TIMESTAMP(3)) * 1000 AS SIGNED)")) {
                if (!result.next()) {
                    throw new SQLException("SQL server did not return its current time");
                }
                return result.getLong(1);
            }
        }
    }

    private void ensureConnection(DatabaseSettings databaseSettings) throws SQLException {
        if (connection != null && !connection.isClosed()
                && connection.isValid(databaseSettings.validationTimeoutSeconds())) {
            return;
        }

        closeDirect();
        try {
            Class.forName("com.mysql.cj.jdbc.Driver", true, SQLManager.class.getClassLoader());
        } catch (ClassNotFoundException e) {
            throw new SQLException("MySQL Connector/J is not available to Adapt", e);
        }
        connection = DriverManager.getConnection(databaseSettings.url(), databaseSettings.username(),
                databaseSettings.password());
        if (!connection.isValid(databaseSettings.validationTimeoutSeconds())) {
            closeDirect();
            throw new SQLException("SQL connection validation timed out");
        }
        setupDatabase();
    }

    private void closeDirect() {
        if (connection == null) {
            return;
        }
        try {
            connection.close();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to close the SQL connection", e);
        } finally {
            connection = null;
        }
    }

    private int queryTimeoutSeconds() {
        return currentSettings().validationTimeoutSeconds();
    }

    private DatabaseSettings currentSettings() {
        DatabaseSettings current = settings;
        if (current == null) {
            throw new IllegalStateException("SQLManager has not been initialized");
        }
        return current;
    }

    private static void validateData(String data) {
        if (data == null || data.isBlank() || "null".equals(data)) {
            throw new IllegalArgumentException("Player data cannot be null or empty");
        }
    }

    private <T> CompletableFuture<T> submit(SqlCallable<T> work) {
        synchronized (submissionGate) {
            if (!accepting.get()) {
                return CompletableFuture.failedFuture(new IllegalStateException("SQLManager is closed"));
            }
            return submitClosing(work);
        }
    }

    private <T> CompletableFuture<T> submitClosing(SqlCallable<T> work) {
        CompletableFuture<T> result = new CompletableFuture<>();
        try {
            databaseExecutor.execute(() -> {
                try {
                    result.complete(work.call());
                } catch (Throwable e) {
                    result.completeExceptionally(e);
                }
            });
        } catch (RejectedExecutionException e) {
            result.completeExceptionally(e);
        }
        return result;
    }

    private <T> CompletableFuture<T> submitHandled(SqlCallable<T> work,
            java.util.function.Function<Throwable, T> error) {
        return submit(work).handle((result, throwable) -> {
            if (throwable == null) {
                return result;
            }
            Throwable cause = throwable instanceof java.util.concurrent.CompletionException && throwable.getCause() != null
                    ? throwable.getCause()
                    : throwable;
            LOGGER.log(Level.SEVERE, "SQL operation failed", cause);
            return error.apply(cause);
        });
    }

    @FunctionalInterface
    private interface SqlCallable<T> {
        T call() throws Exception;
    }

    private record DatabaseSettings(String url, String username, String password, long connectionTimeoutMillis,
            int validationTimeoutSeconds) {
        private static DatabaseSettings capture(AdaptConfig config) {
            long timeout = Math.max(1_000L, config.getSql().getConnectionTimeout());
            String url = String.format("jdbc:mysql://%s:%d/%s?connectTimeout=%d&socketTimeout=%d&tcpKeepAlive=true",
                    config.getSql().getHost(), config.getSql().getPort(), config.getSql().getDatabase(), timeout,
                    timeout);
            int validation = Math.max(1, Math.min(config.getSqlSecondsCheckverify(),
                    (int) Math.ceil(timeout / 1000D)));
            return new DatabaseSettings(url, config.getSql().getUsername(), config.getSql().getPassword(), timeout,
                    validation);
        }
    }

    public enum AcquireStatus {
        ACQUIRED,
        BUSY,
        ERROR,
        CLOSED
    }

    public record AcquireResult(AcquireStatus status, String data, boolean created, long retryAfterMillis,
            Throwable error) {
        private static AcquireResult acquired(String data, boolean created) {
            return new AcquireResult(AcquireStatus.ACQUIRED, data, created, 0L, null);
        }

        private static AcquireResult busy(long retryAfterMillis) {
            return new AcquireResult(AcquireStatus.BUSY, null, false, retryAfterMillis, null);
        }

        private static AcquireResult error(Throwable error) {
            return new AcquireResult(AcquireStatus.ERROR, null, false, 0L, error);
        }

        private static AcquireResult closed() {
            return new AcquireResult(AcquireStatus.CLOSED, null, false, 0L, null);
        }
    }

    public enum WriteResult {
        STORED,
        LOST_LEASE,
        ERROR,
        CLOSED
    }

    public enum FetchStatus {
        FOUND,
        NOT_FOUND,
        ERROR,
        CLOSED
    }

    public record FetchResult(FetchStatus status, String data, Throwable error) {
        private static FetchResult found(String data) {
            return new FetchResult(FetchStatus.FOUND, data, null);
        }

        private static FetchResult notFound() {
            return new FetchResult(FetchStatus.NOT_FOUND, null, null);
        }

        private static FetchResult error(Throwable error) {
            return new FetchResult(FetchStatus.ERROR, null, error);
        }

        private static FetchResult closed() {
            return new FetchResult(FetchStatus.CLOSED, null, null);
        }
    }
}
