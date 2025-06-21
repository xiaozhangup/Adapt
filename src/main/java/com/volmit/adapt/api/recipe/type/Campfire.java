package com.volmit.adapt.api.recipe.type;

import com.volmit.adapt.Adapt;
import com.volmit.adapt.api.recipe.AdaptRecipe;
import lombok.Builder;
import lombok.Data;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.CampfireRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;

@Builder
@Data
public class Campfire implements AdaptRecipe {
    private String key;
    private ItemStack result;
    private Material ingredient;
    private float experience;
    private int cookTime;

    @Override
    public ItemStack getResult() {
        return this.result;
    }

    public void register() {
        CampfireRecipe s = new CampfireRecipe(new NamespacedKey(Adapt.instance, getKey()), result, ingredient, experience, cookTime);
        Bukkit.getServer().addRecipe(s);
        Adapt.verbose("Registered Campfire Recipe " + s.getKey());
    }

    @Override
    public boolean is(Recipe recipe) {
        return recipe instanceof CampfireRecipe s && s.getKey().equals(getNSKey());
    }

    @Override
    public void unregister() {
        Bukkit.getServer().removeRecipe(getNSKey());
        Adapt.verbose("Unregistered Campfire Recipe " + getKey());
    }
}