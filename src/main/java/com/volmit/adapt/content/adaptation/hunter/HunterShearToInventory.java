package com.volmit.adapt.content.adaptation.hunter;

import com.volmit.adapt.api.adaptation.SimpleAdaptation;
import com.volmit.adapt.util.Element;
import com.volmit.adapt.util.Localizer;
import com.volmit.adapt.util.Components;
import lombok.NoArgsConstructor;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerShearEntityEvent;

import java.util.List;

public class HunterShearToInventory extends SimpleAdaptation<HunterShearToInventory.Config> {
    private static final String TAG = "adapt_shear_tag";

    public HunterShearToInventory() {
        super("hunter-shear-to-inventory");
        registerConfiguration(HunterShearToInventory.Config.class);
        setDescription(Localizer.component("hunter", "sheartoinventory", "description"));
        setDisplayName(Localizer.component("hunter", "sheartoinventory", "name"));
        setIcon(Material.SHEARS);
        setBaseCost(getConfig().baseCost);
        setMaxLevel(getConfig().maxLevel);
        setInitialCost(getConfig().initialCost);
        setCostFactor(getConfig().costFactor);
        setInterval(18440);
    }

    @Override
    public boolean isEnabled() {
        return getConfig().enabled;
    }

    public void addStats(int level, Element v) {
        v.addLore(Components.mini("<gray><lore>", Placeholder.component("lore",
                Localizer.component("hunter", "sheartoinventory", "lore1"))));
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onShear(PlayerShearEntityEvent e) {
        if (e.isCancelled()) {
            return;
        }
        Player p = e.getPlayer();
        if (!hasAdaptation(p)) {
            return;
        }
        if (p.getGameMode() != GameMode.SURVIVAL) {
            return;
        }

        p.give(e.getDrops());
        e.setDrops(List.of());
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
        int baseCost = 1;
        int maxLevel = 1;
        int initialCost = 4;
        double costFactor = 1;
    }
}
