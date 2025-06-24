package com.volmit.adapt.api.recipe.type;

import com.volmit.adapt.Adapt;
import com.volmit.adapt.api.recipe.AdaptRecipe;
import lombok.Builder;
import lombok.Data;
import lombok.Singular;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapelessRecipe;

import java.util.List;

@Builder
@Data
public class Shapeless implements AdaptRecipe {
    private String key;
    private ItemStack result;
    @Singular
    private List<Material> ingredients;

    @Override
    public ItemStack getResult() {
        return this.result;
    }

    public void register() {
        ShapelessRecipe s = new ShapelessRecipe(new NamespacedKey(Adapt.instance, getKey()), result);
        ingredients.forEach(s::addIngredient);
        Bukkit.getServer().addRecipe(s);
        Adapt.verbose("Registered Shapeless Crafting Recipe " + s.getKey());
    }

    @Override
    public boolean is(Recipe recipe) {
        return recipe instanceof ShapelessRecipe s && s.getKey().equals(getNSKey());
    }

    @Override
    public void unregister() {
        Bukkit.getServer().removeRecipe(getNSKey());
        Adapt.verbose("Unregistered Shapeless Crafting Recipe " + getKey());
    }
}