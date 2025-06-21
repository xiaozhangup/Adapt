package com.volmit.adapt.api.recipe.type;

import com.volmit.adapt.Adapt;
import com.volmit.adapt.api.recipe.AdaptRecipe;
import lombok.Builder;
import lombok.Data;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.StonecuttingRecipe;

@Builder
@Data
public class Stonecutter implements AdaptRecipe {
    private String key;
    private ItemStack result;
    private Material ingredient;

    @Override
    public ItemStack getResult() {
        return this.result;
    }

    public void register() {
        StonecuttingRecipe s = new StonecuttingRecipe(new NamespacedKey(Adapt.instance, getKey()), result, ingredient);
        Bukkit.getServer().addRecipe(s);
        Adapt.verbose("Registered Stone Cutter Recipe " + s.getKey());
    }

    @Override
    public boolean is(Recipe recipe) {
        return recipe instanceof StonecuttingRecipe s && s.getKey().equals(getNSKey());
    }


    @Override
    public void unregister() {
        Bukkit.getServer().removeRecipe(getNSKey());
        Adapt.verbose("Unregistered Stone Cutter Recipe " + getKey());
    }
}