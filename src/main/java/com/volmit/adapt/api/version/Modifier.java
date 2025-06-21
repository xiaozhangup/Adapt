package com.volmit.adapt.api.version;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.AttributeModifier;

import java.util.Optional;
import java.util.UUID;

@ToString
@EqualsAndHashCode
@AllArgsConstructor
public class Modifier {
    private final UUID uuid;
    private final NamespacedKey key;
    @Getter
    private final double amount;
    @Getter
    private final AttributeModifier.Operation operation;

    public Optional<UUID> getUUID() {
        return Optional.ofNullable(uuid);
    }

    public Optional<NamespacedKey> getKey() {
        return Optional.ofNullable(key);
    }
}