package com.volmit.adapt.content.skill;

import com.volmit.adapt.api.advancement.AdaptAdvancement;
import com.volmit.adapt.api.skill.SimpleSkill;
import com.volmit.adapt.api.world.AdaptPlayer;
import com.volmit.adapt.content.adaptation.UnarmedPower;
import com.volmit.adapt.content.adaptation.UnarmedSuckerPunch;
import com.volmit.adapt.util.C;
import com.volmit.adapt.util.KList;
import org.bukkit.Material;
import org.bukkit.boss.BarColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;

public class SkillUnarmed extends SimpleSkill {
    public SkillUnarmed() {
        super("unarmed", "\u269C");
        setColor(C.YELLOW);
        setDescription("Without a weapon is not without strength");
        setInterval(2570);
        registerAdaptation(new UnarmedSuckerPunch());
        registerAdaptation(new UnarmedPower());
        setIcon(Material.FIRE_CHARGE);
    }

    @EventHandler
    public void on(EntityDamageByEntityEvent e) {
        if(e.getDamager() instanceof Player) {
            AdaptPlayer a = getPlayer((Player) e.getDamager());
            ItemStack hand = a.getPlayer().getInventory().getItemInMainHand();

            if(!isMelee(hand)) {
                xp(a.getPlayer(), e.getEntity().getLocation(), 13.26 * e.getDamage());
            }
        }
    }

    @Override
    public void onTick() {

    }

    @Override
    public void onRegisterAdvancements(KList<AdaptAdvancement> advancements) {

    }
}
