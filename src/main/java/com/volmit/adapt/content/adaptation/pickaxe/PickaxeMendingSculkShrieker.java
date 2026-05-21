package com.volmit.adapt.content.adaptation.pickaxe;

import com.volmit.adapt.api.adaptation.SimpleAdaptation;
import com.volmit.adapt.util.C;
import com.volmit.adapt.util.Element;
import com.volmit.adapt.util.Localizer;
import com.volmit.adapt.util.NaturalBlockDrop;
import lombok.NoArgsConstructor;
import org.bukkit.Material;
import org.bukkit.block.data.type.SculkShrieker;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockDataMeta;

public class PickaxeMendingSculkShrieker extends SimpleAdaptation<PickaxeMendingSculkShrieker.Config> {
    public PickaxeMendingSculkShrieker() {
        super("pickaxe-mending-sculk-shrieker");
        registerConfiguration(PickaxeMendingSculkShrieker.Config.class);
        setDescription(Localizer.dLocalize("pickaxe", "mendingsculkshrieker", "description"));
        setDisplayName(Localizer.dLocalize("pickaxe", "mendingsculkshrieker", "name"));
        setIcon(Material.SCULK_SHRIEKER);
        setBaseCost(getConfig().baseCost);
        setMaxLevel(getConfig().maxLevel);
        setInitialCost(getConfig().initialCost);
        setCostFactor(getConfig().costFactor);
        setInterval(8444);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        var player = event.getPlayer();
        var block = event.getBlock();
        if (!event.isDropItems() || !hasAdaptation(player) || block.getType() != Material.SCULK_SHRIEKER
                || !canBlockBreak(player, event.getBlock())) {
            return;
        }
        if (!player.getInventory().getItemInMainHand().containsEnchantment(Enchantment.SILK_TOUCH)) {
            return;
        }

        event.setDropItems(false);
        var item = new ItemStack(Material.SCULK_SHRIEKER);
        var data = block.getBlockData();
        var state = block.getState();
        if (item.getItemMeta() instanceof BlockDataMeta meta) {
            meta.setBlockData(data);
            item.setItemMeta(meta);
        }

        NaturalBlockDrop.drop(block, state, player, item);
    }

    @Override
    public boolean isEnabled() {
        return getConfig().enabled;
    }

    @Override
    public void addStats(int level, Element v) {
        v.addLore(C.GREEN + Localizer.dLocalize("pickaxe", "mendingsculkshrieker", "lore1"));
    }

    @Override
    public void onTick() {
    }

    @Override
    public boolean isPermanent() {
        return getConfig().permanent;
    }

    @NoArgsConstructor
    protected static class Config {
        boolean permanent = false;
        boolean enabled = true;
        int baseCost = 6;
        int maxLevel = 1;
        int initialCost = 8;
        double costFactor = 2.325;
    }
}
