package com.volmit.adapt.content.skill;

import com.volmit.adapt.api.skill.SimpleSkill;
import com.volmit.adapt.util.C;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerMoveEvent;

public class SkillStealth extends SimpleSkill {
    public SkillStealth() {
        super("stealth");
        setColor(C.BLACK);
        setBarColor(BarColor.WHITE);
        setBarStyle(BarStyle.SEGMENTED_20);
        setInterval(1000);
    }

    @EventHandler
    public void on(PlayerMoveEvent e)
    {
        if(e.getPlayer().isSneaking() && e.getTo().getWorld().equals(e.getFrom().getWorld()) && e.getTo().distanceSquared(e.getFrom()) > 0)
        {
            xp(e.getPlayer(), 1.64);
        }
    }

    @Override
    public void onTick() {
        for(Player i : Bukkit.getOnlinePlayers())
        {
            if(i.isSneaking())
            {
                xp(i, 11.28);
            }
        }
    }
}
