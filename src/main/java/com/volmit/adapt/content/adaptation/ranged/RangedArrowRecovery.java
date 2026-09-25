package com.volmit.adapt.content.adaptation.ranged;

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;

import com.volmit.adapt.Adapt;
import com.volmit.adapt.api.adaptation.SimpleAdaptation;
import com.volmit.adapt.util.Element;
import com.volmit.adapt.util.Localizer;
import lombok.NoArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.UUID;

import static java.util.concurrent.ThreadLocalRandom.current;

public class RangedArrowRecovery extends SimpleAdaptation<RangedArrowRecovery.Config> {
    private final NamespacedKey shooterKey;

    public RangedArrowRecovery() {
        super("ranged-recovery");
        registerConfiguration(RangedArrowRecovery.Config.class);
        setDescription(Localizer.component("ranged", "arrowrecovery", "description"));
        setDisplayName(Localizer.component("ranged", "arrowrecovery", "name"));
        setIcon(Material.ARROW);
        setBaseCost(getConfig().baseCost);
        setMaxLevel(getConfig().maxLevel);
        setInitialCost(getConfig().initialCost);
        setCostFactor(getConfig().costFactor);
        shooterKey = new NamespacedKey(Adapt.instance, "ranged-recovery-shooter");
    }

    @EventHandler
    public void onEntityShootBow(EntityShootBowEvent event) {
        if (event.getEntity() instanceof Player player && hasAdaptation(player)) {
            if (!event.getBow().containsEnchantment(Enchantment.INFINITY)) {
                if (event.getProjectile() instanceof Arrow arrow) {
                    arrow.getPersistentDataContainer().set(shooterKey, PersistentDataType.STRING,
                            player.getUniqueId().toString());
                }
            }
        }
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if (event.getEntity() instanceof Arrow arrow) {
            String storedShooter = arrow.getPersistentDataContainer().get(shooterKey, PersistentDataType.STRING);
            arrow.getPersistentDataContainer().remove(shooterKey);
            Player shooter = getShooter(storedShooter);
            if (shooter != null && hasAdaptation(shooter)) {
                int level = getLevel(shooter);
                double chance = getConfig().hitChance[level - 1] / 100.0;
                if (current().nextDouble() < chance) {
                    ItemStack arrowStack = new ItemStack(Material.ARROW, 1);
                    shooter.getInventory().addItem(arrowStack);
                    Adapt.info("Arrow added to inventory.");
                }
            }
        }
    }

    private Player getShooter(String storedShooter) {
        if (storedShooter == null) {
            return null;
        }
        try {
            return Bukkit.getPlayer(UUID.fromString(storedShooter));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private double chancePerLevel(int level) {
        return (getConfig().hitChance[level - 1] / 100.0);
    }

    @Override
    public boolean isEnabled() {
        return getConfig().enabled;
    }

    @Override
    public void onTick() {
    }

    @Override
    public boolean needsTicking() {
        return false;
    }

    @Override
    public boolean isPermanent() {
        return getConfig().permanent;
    }

    @Override
    public void addStats(int level, Element v) {
        v.addLore(Localizer.components("ranged", "arrowrecovery", "lore",
                Placeholder.unparsed("chance", Double.toString(chancePerLevel(level)))));
    }

    @NoArgsConstructor
    protected static class Config {
        boolean permanent = false;
        boolean enabled = true;
        int baseCost = 5;
        int maxLevel = 8;
        int initialCost = 5;
        double costFactor = 1.10;
        double[] hitChance = {10, 20, 30, 40, 50, 60, 70, 80};
    }
}
