package com.volmit.adapt.api.recipe.type;

import com.volmit.adapt.Adapt;
import com.volmit.adapt.api.recipe.AdaptRecipe;
import lombok.Builder;
import lombok.Data;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.FurnaceRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;

@Builder
@Data
public class Furnace implements AdaptRecipe {
    private String key;
    private ItemStack result;
    private Material ingredient;
    //        private float experience = 1;
//        private int cookTime = 20;
    private float experience;
    private int cookTime;

    @Override
    public ItemStack getResult() {
        return this.result;
    }

    public void register() {
        FurnaceRecipe s = new FurnaceRecipe(new NamespacedKey(Adapt.instance, getKey()), result, ingredient, experience, cookTime);
        Bukkit.getServer().addRecipe(s);
        Adapt.verbose("Registered Furnace Recipe " + s.getKey());
    }

    @Override
    public boolean is(Recipe recipe) {
        return recipe instanceof FurnaceRecipe s && s.getKey().equals(getNSKey());
    }


    @Override
    public void unregister() {
        Bukkit.getServer().removeRecipe(getNSKey());
        Adapt.verbose("Unregistered Furnace Recipe " + getKey());
    }
}