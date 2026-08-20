package com.volmit.adapt.api.potion;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.volmit.adapt.Adapt;
import com.volmit.adapt.api.world.AdaptPlayer;
import com.volmit.adapt.util.J;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BrewingStand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.inventory.BrewerInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;

import java.util.List;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class BrewingManager implements Listener {

    private static final Map<BrewingRecipe, List<String>> recipes = Maps.newHashMap();
    private static final Map<Location, BrewingTask> activeTasks = Maps.newHashMap();

    public BrewingManager() {
        Adapt.instance.getServer().getScheduler().runTaskTimer(Adapt.instance, BrewingManager::tickActiveTasks, 1L, 1L);
    }

    private static void tickActiveTasks() {
        Iterator<Map.Entry<Location, BrewingTask>> iterator = activeTasks.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Location, BrewingTask> entry = iterator.next();
            try {
                if (!entry.getValue().tick()) {
                    iterator.remove();
                }
            } catch (Throwable e) {
                entry.getValue().cancel();
                iterator.remove();
                Adapt.warn("Cancelled broken custom brewing task at " + entry.getKey());
                e.printStackTrace();
            }
        }
    }

    public static void registerRecipe(String adaptation, BrewingRecipe recipe) {
        recipes.putIfAbsent(recipe, Lists.newArrayList(adaptation));
        recipes.computeIfPresent(recipe, (k, v) -> {
            if (!v.contains(adaptation))
                v.add(adaptation);
            return v;
        });
    }

    public static void clear() {
        activeTasks.values().forEach(BrewingTask::cancel);
        activeTasks.clear();
        recipes.clear();
    }

    @EventHandler
    public void on(WorldUnloadEvent event) {
        activeTasks.entrySet().removeIf(entry -> {
            if (!entry.getKey().getWorld().equals(event.getWorld())) {
                return false;
            }
            entry.getValue().cancel();
            return true;
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent e) {
        if (e.getView().getTopInventory().getType() != InventoryType.BREWING
                || e.getView().getTopInventory().getHolder() == null) {
            return;
        }
        Adapt.verbose("Brewing click: " + e.getRawSlot());
        BrewerInventory inv = (BrewerInventory) e.getInventory();
        boolean doTheThing = inv.getIngredient() == null && e.getCursor() != null && e.getRawSlot() == 3
                && e.getClickedInventory() != null && e.getClickedInventory().getType().equals(InventoryType.BREWING)
                && (e.getClick() == ClickType.LEFT);
        if (doTheThing) {
            Adapt.verbose("Brewing Stand Ingredient Clicked");
            e.setCancelled(true);
            inv.setIngredient(e.getCursor().clone());
            e.setCursor(null);
        }
        UUID playerId = e.getWhoClicked().getUniqueId();
        if (!Adapt.instance.getAdaptServer().isPlayerLoaded(playerId)) {
            return;
        }
        AdaptPlayer expectedPlayer = Adapt.instance.getAdaptServer().getPlayer((Player) e.getWhoClicked());
        Location standLocation = inv.getHolder().getLocation();
        UUID worldId = standLocation.getWorld().getUID();
        int x = standLocation.getBlockX();
        int y = standLocation.getBlockY();
        int z = standLocation.getBlockZ();
        J.s(() -> refreshTask(worldId, x, y, z, playerId, expectedPlayer));
    }

    private void refreshTask(UUID worldId, int x, int y, int z, UUID playerId, AdaptPlayer expectedPlayer) {
        Player player = Bukkit.getPlayer(playerId);
        World world = Bukkit.getWorld(worldId);
        if (player == null || !player.isOnline() || world == null
                || !Adapt.instance.getAdaptServer().isPlayerLoaded(playerId)
                || !Adapt.instance.getAdaptServer().isCurrentPlayer(playerId, expectedPlayer)) {
            return;
        }

        Location location = new Location(world, x, y, z);
        if (!(location.getBlock().getState() instanceof BrewingStand)) {
            BrewingTask removed = activeTasks.remove(location);
            if (removed != null) {
                removed.cancel();
            }
            return;
        }

        Optional<BrewingRecipe> recipe = recipes.keySet().stream()
                .filter(r -> BrewingTask.isValid(r, location)).findFirst();
        recipe.ifPresent(r -> {
            BrewingTask current = activeTasks.get(location);
            if (current != null && current.getRecipe().getId().equals(r.getId())) {
                return;
            }
            if (recipes.get(r).stream().noneMatch(expectedPlayer::hasAdaptation)) {
                if (current != null) {
                    activeTasks.remove(location);
                    current.cancel();
                }
                return;
            }
            if (current != null) {
                current.cancel();
            }
            activeTasks.put(location, new BrewingTask(r, location));
        });
        if (recipe.isEmpty()) {
            BrewingTask removed = activeTasks.remove(location);
            if (removed != null) {
                removed.cancel();
            }
        }
    }

    @EventHandler
    public void onBrew(BrewEvent e) {
        Material m = e.getContents().getIngredient().getType();
        if (m != Material.GUNPOWDER && m != Material.DRAGON_BREATH) {
            return;
        }
        for (int i = 0; i < 3; i++) {
            ItemStack s = e.getContents().getItem(i);
            if (s == null)
                continue;
            PotionMeta meta = (PotionMeta) s.getItemMeta();
            if (meta.getBasePotionType() == null) {
                continue;
            }
            ItemStack newStack = s.clone();
            if (m == Material.GUNPOWDER) {
                newStack.setType(Material.SPLASH_POTION);
            } else {
                newStack.setType(Material.LINGERING_POTION);
                /*
                 * PotionMeta meta = (PotionMeta)newStack.getItemMeta(); List<PotionEffect>
                 * newEffects = Lists.newArrayList(); meta.getCustomEffects().forEach(effect ->
                 * newEffects.add(new PotionEffect(effect.getType(), effect.getDuration() / 4,
                 * effect.getAmplifier()))); meta.clearCustomEffects();
                 * newEffects.forEach(effect -> meta.addCustomEffect(effect, true));
                 * newStack.setItemMeta(meta);
                 */
            }
            e.getResults().set(i, newStack);
        }
    }
}
