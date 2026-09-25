package com.volmit.adapt.content.adaptation.pickaxe;


import com.volmit.adapt.api.adaptation.SimpleAdaptation;
import com.volmit.adapt.util.Element;
import com.volmit.adapt.util.Localizer;
import com.volmit.adapt.util.NaturalBlockDrop;
import lombok.NoArgsConstructor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

public class PickaxeSilkSpawner extends SimpleAdaptation<PickaxeSilkSpawner.Config> {
    public PickaxeSilkSpawner() {
        super("pickaxe-silk-spawner");
        registerConfiguration(PickaxeSilkSpawner.Config.class);
        setDescription(Localizer.component("pickaxe", "silkspawner", "description"));
        setDisplayName(Localizer.component("pickaxe", "silkspawner", "name"));
        setIcon(Material.SPAWNER);
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
        if (!event.isDropItems() || !hasAdaptation(player) || block.getType() != Material.SPAWNER
                || !canBlockBreak(player, event.getBlock())) {
            return;
        }
        var level = getLevel(player);
        boolean silk = player.getInventory().getItemInMainHand().containsEnchantment(Enchantment.SILK_TOUCH);
        if (level == 1 && !silk) {
            return;
        }
        if (level > 1 && !player.isSneaking() && !silk) {
            return;
        }

        event.setDropItems(false);
        var spawner = new ItemStack(Material.SPAWNER);
        var state = block.getState();

        NaturalBlockDrop.drop(block, state, player, spawner);
    }

    @Override
    public boolean isEnabled() {
        return getConfig().enabled;
    }

    @Override
    public void addStats(int level, Element v) {
        v.addLore(Localizer.components("pickaxe", "silkspawner", "lore").get(level < 2 ? 0 : 1));
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
        int maxLevel = 2;
        int initialCost = 4;
        double costFactor = 2.325;
    }
}
