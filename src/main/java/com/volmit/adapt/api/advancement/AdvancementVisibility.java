package com.volmit.adapt.api.advancement;

import com.volmit.adapt.api.world.PlayerData;

public enum AdvancementVisibility {
    ALWAYS,
    PARENT_GRANTED,
    HIDDEN,
    VANILLA;

    public boolean isVisible(PlayerData data, String key, String parent, String grandparent) {
        return switch (this) {
            case ALWAYS -> true;
            case HIDDEN -> data.isGranted(key);
            case PARENT_GRANTED -> data.isGranted(key) || parent != null && data.isGranted(parent);
            case VANILLA -> data.isGranted(key) || parent != null && data.isGranted(parent)
                    || grandparent != null && data.isGranted(grandparent);
        };
    }
}
