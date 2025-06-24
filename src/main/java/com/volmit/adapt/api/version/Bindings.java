package com.volmit.adapt.api.version;

import com.volmit.adapt.api.potion.PotionBuilder;
import com.volmit.adapt.util.CustomModel;
import com.volmit.adapt.util.reflect.Reflect;
import org.bukkit.attribute.Attributable;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionType;

import java.util.Optional;

public class Bindings {

    public void applyModel(CustomModel model, ItemMeta meta) {
        if (CustomModel.EMPTY_KEY.equals(model.modelKey()))
            meta.setCustomModelData(model.model());
        else
            meta.setItemModel(model.modelKey());
    }

    public Attribute getAttribute(Attributable attributable, org.bukkit.attribute.Attribute modifier) {
        return Optional.ofNullable(attributable.getAttribute(modifier)).map(Attribute::new).orElse(null);
    }

    public ItemStack buildPotion(PotionBuilder builder) {
        ItemStack stack = buildPotionStack(builder);
        PotionMeta meta = (PotionMeta) stack.getItemMeta();
        assert meta != null;

        PotionType type = builder.getBaseType();
        if (type == null) {
            meta.setBasePotionType(null);
        } else if (builder.isExtended()) {
            meta.setBasePotionType(Reflect.getEnum(PotionType.class, "LONG_" + type.name()).orElse(type));
        } else if (builder.isUpgraded()) {
            meta.setBasePotionType(Reflect.getEnum(PotionType.class, "STRONG_" + type.name()).orElse(type));
        } else {
            meta.setBasePotionType(type);
        }

        stack.setItemMeta(meta);
        return stack;
    }

    public ItemStack buildPotionStack(PotionBuilder builder) {
        ItemStack stack = new ItemStack(builder.getType().getMaterial());
        PotionMeta meta = (PotionMeta) stack.getItemMeta();
        assert meta != null;
        builder.getEffects().forEach(e -> meta.addCustomEffect(e, true));
        if (builder.getColor() != null)
            meta.setColor(builder.getColor());
        if (builder.getName() != null)
            meta.setDisplayName("§r" + builder.getName());
        stack.setItemMeta(meta);
        return stack;
    }
}
