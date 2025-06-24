package com.volmit.adapt.api.version;

import com.volmit.adapt.util.collection.KList;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.inventory.EquipmentSlotGroup;

import java.util.UUID;
import java.util.stream.Collectors;

public record Attribute(AttributeInstance instance) {

    private static Modifier wrap(AttributeModifier modifier) {
        return new Modifier(null, modifier.getKey(), modifier.getAmount(), modifier.getOperation());
    }

    public double getValue() {
        return instance.getValue();
    }

    public double getDefaultValue() {
        return instance.getDefaultValue();
    }

    public double getBaseValue() {
        return instance.getBaseValue();
    }

    public void setBaseValue(double baseValue) {
        instance.setBaseValue(baseValue);
    }

    @SuppressWarnings("all")
    public void addModifier(UUID uuid, NamespacedKey key, double amount, AttributeModifier.Operation operation) {
        instance.addModifier(new AttributeModifier(key, amount, operation, EquipmentSlotGroup.ANY));
    }

    public boolean hasModifier(UUID uuid, NamespacedKey key) {
        return instance.getModifiers().stream().anyMatch(m -> m.getKey().equals(key));
    }

    public void removeModifier(UUID uuid, NamespacedKey key) {
        instance.getModifiers().stream().filter(m -> m.getKey().equals(key)).forEach(instance::removeModifier);
    }

    public KList<Modifier> getModifier(UUID uuid, NamespacedKey key) {
        return instance.getModifiers().stream().filter(m -> m.getKey().equals(key)).map(Attribute::wrap)
                .collect(Collectors.toCollection(KList::new));
    }

    public void setModifier(UUID uuid, NamespacedKey key, double amount, AttributeModifier.Operation operation) {
        removeModifier(uuid, key);
        addModifier(uuid, key, amount, operation);
    }
}
