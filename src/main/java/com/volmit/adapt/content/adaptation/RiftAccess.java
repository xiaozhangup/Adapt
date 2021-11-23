package com.volmit.adapt.content.adaptation;

import com.volmit.adapt.Adapt;
import com.volmit.adapt.api.adaptation.SimpleAdaptation;
import com.volmit.adapt.api.advancement.AdaptAdvancement;
import com.volmit.adapt.content.item.BoundEnderPearl;
import com.volmit.adapt.util.C;
import com.volmit.adapt.util.Element;
import com.volmit.adapt.util.J;
import com.volmit.adapt.util.KList;
import eu.endercentral.crazy_advancements.AdvancementDisplay;
import eu.endercentral.crazy_advancements.AdvancementVisibility;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;
import java.util.Objects;

public class RiftAccess extends SimpleAdaptation {
    private KList<InventoryView> activeViews = new KList<>();

    public RiftAccess() {
        super("ender-access");
        setDescription("Pull from the void");
        setIcon(Material.NETHER_STAR);
        setBaseCost(0);
        setCostFactor(0);
        setMaxLevel(1);
        setInitialCost(10);
        setInterval(50);
    }


    @Override
    public void onRegisterAdvancements(KList<AdaptAdvancement> advancements) {

    }

    private double getConsumePercent(int level) {
        return 0.15 + (0.15 * level);
    }


    @Override
    public void addStats(int level, Element v) {
        v.addLore(C.ITALIC + "1: Sneak Left-Click a container with an Enderpearl to link");
        v.addLore(C.ITALIC + "2: Click the Resonating pearl to access remote inventory");
        v.addLore(C.ITALIC + "3: Sneak Click the air to unbind the pearl");
        v.addLore(C.RED + "[ SNEAK-CLICKING A BLOCK WILL THROW IT ]");
    }

    @EventHandler
    public void on(PlayerInteractEvent e)
    {
        Player p = e.getPlayer();
        ItemStack hand = p.getInventory().getItemInMainHand();
        Block block = e.getClickedBlock();

        if(!hasAdaptation(p) || !hand.getType().equals(Material.ENDER_PEARL))
        {
            return;
        }

        switch(e.getAction())
        {
            case LEFT_CLICK_BLOCK,RIGHT_CLICK_BLOCK -> {
                if(p.isSneaking() && isStorage(block.getBlockData()))
                {
                    linkPearl(p, block);
                    e.setCancelled(true);
                }

                else if(isBound(hand) && !p.isSneaking())
                {
                    openPearl(p);
                    e.setCancelled(true);
                }
            }
            case RIGHT_CLICK_AIR -> {
                if(isBound(hand))
                {
                    openPearl(p);
                    e.setCancelled(true);
                }
            }
            case LEFT_CLICK_AIR -> {
                if(p.isSneaking() && isBound(hand))
                {
                    unlinkPearl(p);
                    e.setCancelled(true);
                }
            }
        }
    }

    private void linkPearl(Player p, Block block) {
        ItemStack hand = p.getInventory().getItemInMainHand();

        if(hand.getAmount() == 1)
        {
            BoundEnderPearl.setData(hand, block);
        }

        else
        {
            hand.setAmount(hand.getAmount() - 1);
            ItemStack pearl = BoundEnderPearl.withData(block);
            p.getInventory().addItem(pearl).values().forEach(i -> p.getWorld().dropItemNaturally(p.getLocation(), i));
        }
    }

    private void unlinkPearl(Player p) {
        ItemStack hand = p.getInventory().getItemInMainHand();

        if(hand.getAmount() > 1)
        {
            hand.setAmount(hand.getAmount() - 1);
        }

        else
        {
            p.getInventory().setItemInMainHand(null);
        }

        ItemStack pearl = new ItemStack(Material.ENDER_PEARL);
        p.getInventory().addItem(pearl).values().forEach(i -> p.getWorld().dropItemNaturally(p.getLocation(), i));
    }

    private void openPearl(Player p) {
        Block b = BoundEnderPearl.getBlock(p.getInventory().getItemInMainHand());

        if (b != null && b.getState() instanceof InventoryHolder holder) {
            activeViews.add(p.openInventory(holder.getInventory()));
            p.getLocation().getWorld().playSound(p.getLocation(), Sound.PARTICLE_SOUL_ESCAPE, 5.35f, 0.10f);
            p.getLocation().getWorld().playSound(p.getLocation(), Sound.BLOCK_BELL_RESONATE, 5.35f, 0.10f);
            p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 45, 1000));
        }
    }

    private boolean isBound(ItemStack stack)
    {
        return stack.getType().equals(Material.ENDER_PEARL) && BoundEnderPearl.getBlock(stack) != null;
    }

    @Override
    public void onTick() {
        J.s(() -> {
            for(int ii = activeViews.size() - 1; ii >= 0; ii--)
            {
                InventoryView i = activeViews.get(ii);

                if(i.getPlayer().getOpenInventory().equals(i))
                {
                    if(i.getTopInventory().getLocation() == null || !isStorage(i.getTopInventory().getLocation().getBlock().getBlockData()))
                    {
                        i.getPlayer().closeInventory();
                        activeViews.remove(ii);
                    }
                }

                else
                {
                    activeViews.remove(ii);
                }
            }
        });
    }
}