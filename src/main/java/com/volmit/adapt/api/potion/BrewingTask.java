package com.volmit.adapt.api.potion;

import com.volmit.adapt.util.SoundPlayer;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.BrewingStand;
import org.bukkit.entity.Player;
import org.bukkit.inventory.BrewerInventory;
import org.bukkit.inventory.ItemStack;

public class BrewingTask {

    private static final int DEFAULT_BREW_TIME = 400;
    private static final int DISPLAY_UPDATE_INTERVAL = 10;

    @Getter
    private final BrewingRecipe recipe;

    private final Location location;
    private int brewTime;
    private boolean cancelled;
    private boolean completed;

    public BrewingTask(BrewingRecipe recipe, Location loc) {
        this.recipe = recipe;
        this.location = loc;
        this.brewTime = recipe.getBrewingTime();

        BrewingStand block = (BrewingStand) loc.getBlock().getState();
        block.setBrewingTime(DEFAULT_BREW_TIME);
        block.update(true);
    }

    public static ItemStack decrease(ItemStack source, int amount) {
        if (source == null || source.getType().isAir()) {
            return new ItemStack(Material.AIR);
        }
        if (source.getAmount() > amount) {
            source.setAmount(source.getAmount() - amount);
            return source;
        } else {
            return new ItemStack(Material.AIR);
        }
    }

    public static boolean isValid(BrewingRecipe recipe, Location loc) {
        BrewingStand block = (BrewingStand) loc.getBlock().getState();
        BrewerInventory inv = block.getInventory();
        if (inv.getIngredient() == null || recipe.getIngredient() != inv.getIngredient().getType()) {
            return false;
        }

        int totalFuel = (inv.getFuel() != null && inv.getFuel().getType() != Material.AIR
                ? inv.getFuel().getAmount() * 20
                : 0) + block.getFuelLevel();
        if (totalFuel < recipe.getFuelCost()) {
            return false;
        }

        for (int i = 0; i < 3; i++) {
            if (recipe.getBasePotion().isSimilar(inv.getItem(i))) {
                return true;
            }
        }

        return false;
    }

    public boolean tick() {
        if (cancelled || !(location.getBlock().getState() instanceof BrewingStand block)) {
            return false;
        }

        BrewerInventory inventory = block.getInventory();
        if (brewTime <= 0) {
            if (!isValid(recipe, location)) {
                cancel();
                return false;
            }

            consumeFuel(block, recipe.getFuelCost());
            inventory.setIngredient(decrease(inventory.getIngredient(), 1));

            for (int i = 0; i < 3; i++) {
                if (recipe.getBasePotion().equals(inventory.getItem(i))) {
                    inventory.setItem(i, recipe.getResult());
                }
            }

            inventory.getViewers().forEach(e -> {
                if (e instanceof Player p) {
                    SoundPlayer sp = SoundPlayer.of(p);
                    sp.play(block.getLocation(), Sound.BLOCK_BREWING_STAND_BREW, 1, 1);
                }
            });
            completed = true;
            block.setBrewingTime(0);
            block.update(true);
            cancelled = true;
            return false;
        }

        brewTime--;
        if (brewTime == 0 || brewTime % DISPLAY_UPDATE_INTERVAL == 0) {
            block.setBrewingTime(getRemainingTime());
            block.update(true);
        }
        return true;
    }

    public void cancel() {
        if (cancelled || completed) {
            return;
        }
        cancelled = true;
        if (location.getBlock().getState() instanceof BrewingStand block) {
            block.setBrewingTime(0);
            block.update(true);
        }
    }

    private static void consumeFuel(BrewingStand block, int fuelCost) {
        if (block.getFuelLevel() >= fuelCost) {
            block.setFuelLevel(block.getFuelLevel() - fuelCost);
            return;
        }

        int needed = fuelCost - block.getFuelLevel();
        int fuelItems = (needed + 19) / 20;
        block.getInventory().setFuel(decrease(block.getInventory().getFuel(), fuelItems));
        block.setFuelLevel(fuelItems * 20 - needed);
    }

    private int getRemainingTime() {
        return (int) (DEFAULT_BREW_TIME * (brewTime / (float) recipe.getBrewingTime()));
    }
}
