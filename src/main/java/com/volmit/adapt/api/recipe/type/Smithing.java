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
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.SmithingRecipe;

@Builder
@Data
public class Smithing implements AdaptRecipe {
    private String key;
    private ItemStack result;
    private Material base;
    private Material addition;

    @Override
    public ItemStack getResult() {
        return this.result;
    }

    public void register() {
        SmithingRecipe s = new SmithingRecipe(new NamespacedKey(Adapt.instance, getKey()), result,
                new RecipeChoice.ExactChoice(new ItemStack(base)),
                new RecipeChoice.ExactChoice(new ItemStack(addition)));
        Bukkit.getServer().addRecipe(s);
        Adapt.verbose("Registered Smithing Table Recipe " + s.getKey());
    }

    @Override
    public boolean is(Recipe recipe) {
        return recipe instanceof SmithingRecipe s && s.getKey().equals(getNSKey());
    }

    @Override
    public void unregister() {
        Bukkit.getServer().removeRecipe(getNSKey());
        Adapt.verbose("Unregistered Smithing Table Recipe " + getKey());
    }
}