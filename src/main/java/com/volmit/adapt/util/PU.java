package com.volmit.adapt.util;

import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;

import java.util.function.Predicate;

public class PU {
    public static int dropTo(Player player, Location location, double range) {
        var drops = location.getWorld().getNearbyEntities(location, range, range, range);
        drops.forEach( entity -> {
            if (entity instanceof Item item) {
                player.playSound(location, Sound.BLOCK_CALCITE_HIT, 0.05f, 0.01f);

                if (player.getInventory().addItem(item.getItemStack()).isEmpty()) {
                    item.remove();
                } else {
                    item.teleport(player);
                }
            }
        });
        return drops.size();
    }

    public static int dropTo(Player player, Location location) {
        return dropTo(player, location, 1);
    }

    public static int dropTo(Player player, Block block) {
        return dropTo(player, block.getLocation().add(0.5, 0.0, 0.5), 1);
    }

    public static int dropTo(Player player, Entity entity) {
        return dropTo(player, entity.getLocation(), 1);
    }

}
