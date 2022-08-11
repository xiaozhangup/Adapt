package com.volmit.adapt.content.adaptation.hunter;

import com.volmit.adapt.api.adaptation.SimpleAdaptation;
import com.volmit.adapt.util.C;
import com.volmit.adapt.util.Element;
import lombok.NoArgsConstructor;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.potion.PotionEffectType;

public class HunterSpeed extends SimpleAdaptation<HunterSpeed.Config> {
    public HunterSpeed() {
        super("hunter-speed");
        registerConfiguration(Config.class);
        setDescription("When you are struck you gain speed, at the cost of hunger");
        setDisplayName("Hunter's Swiftness");
        setIcon(Material.LAVA_BUCKET);
        setBaseCost(getConfig().baseCost);
        setMaxLevel(getConfig().maxLevel);
        setInitialCost(getConfig().initialCost);
        setCostFactor(getConfig().costFactor);
    }

    @Override
    public void addStats(int level, Element v) {
        v.addLore(C.GRAY + "Gain passive swiftness when struck");
        v.addLore(C.GREEN + "+ " + level + C.GRAY + "x Swiftness stacks for a 3 seconds on hit");
        v.addLore(C.RED + "- " + 5+level + C.GRAY + "x Stacking hunger");
        v.addLore(C.GRAY + "* " + level + C.GRAY + " Hunger stacks duration and multiplier.");
        v.addLore(C.GRAY + "* " + level + C.GRAY + " Swiftness stacks multiplier, not duration.");
    }


    @EventHandler
    public void on(EntityDamageEvent e) {
        if(e.getEntity() instanceof org.bukkit.entity.Player p && !e.getCause().equals(EntityDamageEvent.DamageCause.STARVATION) && hasAdaptation(p)) {
            addPotionStacks(p, PotionEffectType.HUNGER, 5 + getLevel(p), 100, true);
            addPotionStacks(p, PotionEffectType.SPEED, getLevel(p), 50, false);
        }
    }

    @Override
    public void onTick() {

    }

    @Override
    public boolean isEnabled() {
        return getConfig().enabled;
    }

    @NoArgsConstructor
    protected static class Config {
        boolean enabled = true;
        int baseCost = 4;
        int maxLevel = 5;
        int initialCost = 8;
        double costFactor = 0.4;
    }
}