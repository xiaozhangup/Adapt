package com.volmit.adapt.api.recipe.type;

import com.volmit.adapt.Adapt;
import com.volmit.adapt.api.recipe.AdaptRecipe;
import com.volmit.adapt.api.recipe.MaterialChar;
import lombok.Builder;
import lombok.Data;
import lombok.Singular;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;

import java.util.List;

@Builder
@Data
public class Shaped implements AdaptRecipe {
    private String key;
    private ItemStack result;
    @Singular
    private List<MaterialChar> ingredients;
    @Singular
    private List<String> shapes;

    @Override
    public ItemStack getResult() {
        return this.result;
    }

    public void register() {
        ShapedRecipe s = new ShapedRecipe(new NamespacedKey(Adapt.instance, getKey()), result);
        s.shape(shapes.toArray(new String[0]));
        ingredients.forEach(i -> s.setIngredient(i.getCharacter(), i.getChoice()));
        Bukkit.getServer().addRecipe(s);
        Adapt.verbose("Registered Shaped Crafting Recipe " + s.getKey());
    }

    @Override
    public boolean is(Recipe recipe) {
        return recipe instanceof ShapedRecipe s && s.getKey().equals(getNSKey());
    }

    @Override
    public void unregister() {
        Bukkit.getServer().removeRecipe(getNSKey());
        Adapt.verbose("Unregistered Shaped Crafting Recipe " + getKey());
    }
}