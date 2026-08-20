package com.volmit.adapt.api.version;

public class Version {
    private static final Bindings bindings = new Bindings();

    public static Bindings get() {
        return bindings;
    }
}
