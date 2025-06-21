package com.volmit.adapt.api.version;

import org.bukkit.inventory.InventoryView;
import org.jetbrains.annotations.NotNull;

public class Version {
    public static final boolean SET_TITLE;
    private static final Bindings bindings = new Bindings();

    static {
        boolean titleMethod = false;
        try {
            InventoryView.class.getDeclaredMethod("setTitle", String.class);
            titleMethod = true;
        } catch (Throwable ignored) {
        }
        SET_TITLE = titleMethod;
    }

    public static Bindings get() {
        return bindings;
    }

    private static Entry of(String version) {
        var s = version.split("\\.");
        return new Entry(Integer.parseInt(s[0]), Integer.parseInt(s[1]), s.length == 3 ? Integer.parseInt(s[2]) : 0);
    }

    private record Entry(int major, int minor, int patch) implements Comparable<Entry> {
        public Class<?> getClass(String className) throws ClassNotFoundException {
            return Class.forName("com.volmit.adapt.api.version.v%s_%s_%s.%s".formatted(major, minor, patch, className));
        }

        @Override
        public int compareTo(@NotNull Version.Entry o) {
            int result = Integer.compare(major, o.major);
            if (result == 0) result = Integer.compare(minor, o.minor);
            if (result == 0) result = Integer.compare(patch, o.patch);
            return result;
        }

        @Override
        public String toString() {
            return major + "." + minor + "." + patch;
        }
    }
}
