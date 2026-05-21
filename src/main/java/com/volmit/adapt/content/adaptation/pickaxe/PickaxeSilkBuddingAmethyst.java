package com.volmit.adapt.content.adaptation.pickaxe;

import com.volmit.adapt.api.adaptation.SimpleAdaptation;
import com.volmit.adapt.util.C;
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

public class PickaxeSilkBuddingAmethyst extends SimpleAdaptation<PickaxeSilkBuddingAmethyst.Config> {
    public PickaxeSilkBuddingAmethyst() {
        super("pickaxe-silk-budding-amethyst");
        registerConfiguration(PickaxeSilkBuddingAmethyst.Config.class);
        setDescription(Localizer.dLocalize("pickaxe", "silkbuddingamethyst", "description"));
        setDisplayName(Localizer.dLocalize("pickaxe", "silkbuddingamethyst", "name"));
        setIcon(Material.BUDDING_AMETHYST);
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
        if (!event.isDropItems() || !hasAdaptation(player) || block.getType() != Material.BUDDING_AMETHYST
                || !canBlockBreak(player, event.getBlock())) {
            return;
        }
        var level = getLevel(player);
        boolean silk = player.getInventory().getItemInMainHand().containsEnchantment(Enchantment.SILK_TOUCH);
        if (level == 1 && !silk) {
            return;
        }

        event.setDropItems(false);
        var buddingAmethyst = new ItemStack(Material.BUDDING_AMETHYST);
        var state = block.getState();

        NaturalBlockDrop.drop(block, state, player, buddingAmethyst);
    }

    @Override
    public boolean isEnabled() {
        return getConfig().enabled;
    }

    @Override
    public void addStats(int level, Element v) {
        v.addLore(C.GREEN + Localizer.dLocalize("pickaxe", "silkbuddingamethyst", "lore" + (level < 2 ? 1 : 2)));
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
