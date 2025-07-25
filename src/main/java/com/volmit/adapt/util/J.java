/*------------------------------------------------------------------------------
-   Adapt is a Skill/Integration plugin  for Minecraft Bukkit Servers
-   Copyright (c) 2022 Arcane Arts (Volmit Software)
-
-   This program is free software: you can redistribute it and/or modify
-   it under the terms of the GNU General Public License as published by
-   the Free Software Foundation, either version 3 of the License, or
-   (at your option) any later version.
-
-   This program is distributed in the hope that it will be useful,
-   but WITHOUT ANY WARRANTY; without even the implied warranty of
-   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
-   GNU General Public License for more details.
-
-   You should have received a copy of the GNU General Public License
-   along with this program.  If not, see <https://www.gnu.org/licenses/>.
-----------------------------------------------------------------------------*/

package com.volmit.adapt.util;

import com.volmit.adapt.Adapt;
import org.bukkit.Bukkit;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class J {
    private static final int tid = 0;
    private static List<Runnable> afterStartup = new ArrayList<>();
    private static List<Runnable> afterStartupAsync = new ArrayList<>();
    private static boolean started = false;

    public static void dofor(int a, Function<Integer, Boolean> c, int ch, Consumer<Integer> d) {
        for (int i = a; c.apply(i); i += ch) {
            c.apply(i);
        }
    }

    public static boolean doif(Supplier<Boolean> c, Runnable g) {
        if (c.get()) {
            g.run();
            return true;
        }

        return false;
    }

    public static void a(Runnable a) {
        MultiBurst.burst.lazy(a);
    }

    public static <T> Future<T> a(Callable<T> a) {
        return MultiBurst.burst.getService().submit(a);
    }

    public static void sleep(long ms) {
        J.attempt(() -> {
            try {
                Thread.sleep(ms);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public static boolean attempt(Runnable r) {
        return attemptCatch(r) == null;
    }

    public static Throwable attemptCatch(Runnable r) {
        try {
            r.run();
        } catch (Throwable e) {
            return e;
        }

        return null;
    }

    public static <T> T attempt(Supplier<T> t, T i) {
        try {
            return t.get();
        } catch (Throwable e) {
            return i;
        }
    }

    /**
     * Dont call this unless you know what you are doing!
     */
    public static void executeAfterStartupQueue() {
        if (started) {
            return;
        }

        started = true;

        for (Runnable r : afterStartup) {
            s(r);
        }

        for (Runnable r : afterStartupAsync) {
            a(r);
        }

        afterStartup = null;
        afterStartupAsync = null;
    }

    /**
     * Schedule a sync task to be run right after startup. If the server has already
     * started ticking, it will simply run it in a sync task.
     * <p>
     * If you dont know if you should queue this or not, do so, it's pretty
     * forgiving.
     *
     * @param r
     *            the runnable
     */
    public static void ass(Runnable r) {
        if (started) {
            s(r);
        } else {
            afterStartup.add(r);
        }
    }

    /**
     * Schedule an async task to be run right after startup. If the server has
     * already started ticking, it will simply run it in an async task.
     * <p>
     * If you dont know if you should queue this or not, do so, it's pretty
     * forgiving.
     *
     * @param r
     *            the runnable
     */
    public static void asa(Runnable r) {
        if (started) {
            a(r);
        } else {
            afterStartupAsync.add(r);
        }
    }

    /**
     * Queue a sync task
     *
     * @param r
     *            the runnable
     */
    public static void s(Runnable r) {
        Bukkit.getScheduler().scheduleSyncDelayedTask(Adapt.instance, r);
    }

    /**
     * Queue a sync task
     *
     * @param r
     *            the runnable
     * @param delay
     *            the delay to wait in ticks before running
     */
    public static void s(Runnable r, int delay) {
        Bukkit.getScheduler().scheduleSyncDelayedTask(Adapt.instance, r, delay);
    }

    /**
     * Cancel a sync repeating task
     *
     * @param id
     *            the task id
     */
    public static void csr(int id) {
        Bukkit.getScheduler().cancelTask(id);
    }

    /**
     * Start a sync repeating task
     *
     * @param r
     *            the runnable
     * @param interval
     *            the interval
     * @return the task id
     */
    public static int sr(Runnable r, int interval) {
        return Bukkit.getScheduler().scheduleSyncRepeatingTask(Adapt.instance, r, 0, interval);
    }

    /**
     * Call an async task dealyed
     *
     * @param r
     *            the runnable
     * @param delay
     *            the delay to wait before running
     */
    @SuppressWarnings("deprecation")
    public static void a(Runnable r, int delay) {
        Bukkit.getScheduler().scheduleAsyncDelayedTask(Adapt.instance, r, delay);
    }

    /**
     * Cancel an async repeat task
     *
     * @param id
     *            the id
     */
    public static void car(int id) {
        Bukkit.getScheduler().cancelTask(id);
    }

    /**
     * Start an async repeat task
     *
     * @param r
     *            the runnable
     * @param interval
     *            the interval in ticks
     * @return the task id
     */
    @SuppressWarnings("deprecation")
    public static int ar(Runnable r, int interval) {
        return Bukkit.getScheduler().scheduleAsyncRepeatingTask(Adapt.instance, r, 0, interval);
    }
}
