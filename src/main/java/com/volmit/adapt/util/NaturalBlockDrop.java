package com.volmit.adapt.util;

import com.volmit.adapt.util.collection.KList;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.inventory.ItemStack;

public class NaturalBlockDrop {

    public static void drop(Block block, BlockState state, Player player, ItemStack stack) {
        var item = block.getWorld().dropItemNaturally(block.getLocation(), stack);
        item.setOwner(player.getUniqueId());

        var dropEvent = new BlockDropItemEvent(block, state, player, new KList<Item>().qadd(item));
        Bukkit.getPluginManager().callEvent(dropEvent);
        if (dropEvent.isCancelled()) {
            remove(item);
            for (Item i : dropEvent.getItems()) {
                remove(i);
            }
        } else {
            if (!dropEvent.getItems().contains(item)) {
                remove(item);
            }

            for (Item i : dropEvent.getItems()) {
                if (!i.isValid()) {
                    block.getWorld().addEntity(i);
                }
            }
        }
    }

    private static void remove(Item item) {
        if (item.isValid()) {
            item.remove();
        }
    }
}
