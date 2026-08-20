package com.volmit.adapt.util;

final class SessionLease {
    private SessionLease() {
    }

    static boolean canAcquire(String currentSession, long leaseUntil, String requestedSession, long now) {
        return currentSession == null || currentSession.isBlank() || currentSession.equals(requestedSession)
                || leaseUntil <= now;
    }
}
