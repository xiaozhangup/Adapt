/*------------------------------------------------------------------------------
 -   Adapt is a Skill/Integration plugin  for Minecraft Bukkit Servers
 -   Copyright (c) 2022 Arcane Arts (Volmit Software)
 -
 -   This program is free software: you can redistribute it and/or modify
 -   it under the terms of the GNU General Public License as published by
 -   the Free Software Foundation, either version 3 of the License, or
 -   (at your option) any later version.
 -
 -   This program is distributed in the hope that it will be useful,
 -   but WITHOUT ANY WARRANTY; without even the implied warranty of
 -   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 -   GNU General Public License for more details.
 -
 -   You should have received a copy of the GNU General Public License
 -   along with this program.  If not, see <https://www.gnu.org/licenses/>.
 -----------------------------------------------------------------------------*/

package com.volmit.adapt.content.item;

import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;

import java.util.*;

import static org.bukkit.Material.*;
import static org.bukkit.Material.MANGROVE_ROOTS;
import static org.bukkit.Material.MUDDY_MANGROVE_ROOTS;

public class ItemListings {
    public static final List<EntityType> additionalInvalid = List.of(
            EntityType.ARMOR_STAND,
            EntityType.ITEM_FRAME,
            EntityType.PAINTING,
            EntityType.LEASH_KNOT,
            EntityType.EVOKER_FANGS,
            EntityType.MARKER
    );

    public static boolean isFlowers(Material material) {
        return flowers.contains(material);
    }

    public static boolean isLeaves(Material material) {
        return material.name().endsWith("_LEAVES");
    }

    public static boolean isLog(Material material) {
        return List.of(MUSHROOM_STEM, BROWN_MUSHROOM_BLOCK, RED_MUSHROOM_BLOCK, MANGROVE_ROOTS, MUDDY_MANGROVE_ROOTS).contains(material)
                    || material.name().endsWith("_LOG")
                    || material.name().endsWith("_WOOD");

    }

    public static List<Material> flowers = List.of(
            Material.DANDELION,
            Material.POPPY,
            Material.BLUE_ORCHID,
            Material.ALLIUM,
            Material.AZURE_BLUET,
            Material.RED_TULIP,
            Material.ORANGE_TULIP,
            Material.WHITE_TULIP,
            Material.PINK_TULIP,
            Material.OXEYE_DAISY,
            Material.CORNFLOWER,
            Material.LILY_OF_THE_VALLEY,
            Material.LILAC,
            Material.ROSE_BUSH,
            Material.PEONY,
            Material.WITHER_ROSE
    );

    public static boolean isFood(Material material) {
        return food.contains(material);
    }

    public static List<Material> food = List.of(
            Material.APPLE,
            Material.BAKED_POTATO,
            Material.BEETROOT,
            Material.BEETROOT_SOUP,
            Material.BREAD,
            Material.CARROT,
            Material.CHORUS_FRUIT,
            Material.COOKED_CHICKEN,
            Material.COOKED_COD,
            Material.COOKED_MUTTON,
            Material.COOKED_PORKCHOP,
            Material.COOKED_RABBIT,
            Material.COOKED_SALMON,
            Material.COOKIE,
            Material.DRIED_KELP,
            Material.GOLDEN_APPLE,
            Material.GLOW_BERRIES,
            Material.GOLDEN_CARROT,
            Material.HONEY_BLOCK,
            Material.MELON_SLICE,
            Material.MUSHROOM_STEW,
            Material.POISONOUS_POTATO,
            Material.POTATO,
            Material.PUFFERFISH,
            Material.PUMPKIN_PIE,
            Material.RABBIT_STEW,
            Material.BEEF,
            Material.CHICKEN,
            Material.COD,
            Material.MUTTON,
            Material.PORKCHOP,
            Material.SALMON,
            Material.ROTTEN_FLESH,
            Material.SPIDER_EYE,
            Material.COOKED_BEEF,
            Material.SUSPICIOUS_STEW,
            Material.SWEET_BERRIES,
            Material.TROPICAL_FISH

    );

    public static boolean isFarmable(Material material) {
        return farmable.contains(material);
    }

    public static List<Material> farmable = List.of(
            Material.GRASS_BLOCK,
            Material.DIRT,
            Material.COARSE_DIRT,
            Material.ROOTED_DIRT,
            Material.WHEAT,
            Material.ATTACHED_MELON_STEM,
            Material.ATTACHED_PUMPKIN_STEM,
            Material.MELON_STEM,
            Material.PUMPKIN_STEM,
            Material.POTATOES,
            Material.SWEET_BERRY_BUSH,
            Material.CARROTS,
            Material.BEETROOTS,
            Material.DIRT_PATH
    );

    public static boolean isTool(Material material) {
        if (material == Material.SHEARS) return true;
        if (material == Material.MACE) return true;
        Set<String> invalidSuffixes = Set.of("_PICKAXE", "_AXE", "_SWORD", "_SHOVEL", "_HOE");
        return invalidSuffixes.stream().anyMatch(s -> material.name().endsWith(s));
    }

    @Getter
    public static List<Material> fishingDrops = List.of(
            Material.COD,
            Material.COD,
            Material.COD,
            Material.COD,
            Material.COD,
            Material.COD,
            Material.SALMON,
            Material.SALMON,
            Material.SALMON,
            Material.SALMON,
            Material.PUFFERFISH,
            Material.TROPICAL_FISH,
            Material.COD,
            Material.COD,
            Material.COD,
            Material.COD,
            Material.COD,
            Material.COD,
            Material.SALMON,
            Material.SALMON,
            Material.SALMON,
            Material.SALMON,
            Material.PUFFERFISH,
            Material.TROPICAL_FISH,
            Material.COD,
            Material.COD,
            Material.COD,
            Material.COD,
            Material.COD,
            Material.COD,
            Material.SALMON,
            Material.SALMON,
            Material.SALMON,
            Material.SALMON,
            Material.PUFFERFISH,
            Material.TROPICAL_FISH,
            Material.COD,
            Material.COD,
            Material.COD,
            Material.COD,
            Material.COD,
            Material.COD,
            Material.SALMON,
            Material.SALMON,
            Material.SALMON,
            Material.SALMON,
            Material.PUFFERFISH,
            Material.TROPICAL_FISH,
            Material.COD,
            Material.COD,
            Material.COD,
            Material.COD,
            Material.COD,
            Material.COD,
            Material.SALMON,
            Material.SALMON,
            Material.SALMON,
            Material.SALMON,
            Material.PUFFERFISH,
            Material.TROPICAL_FISH,

            Material.BOW,
            Material.FISHING_ROD,
            Material.NAME_TAG,
            Material.NAUTILUS_SHELL,
            Material.SADDLE,

            Material.LILY_PAD,
            Material.BOWL,
            Material.LEATHER,
            Material.STICK,
            Material.ROTTEN_FLESH,
            Material.STRING,
            Material.GLASS_BOTTLE,
            Material.BONE,
            Material.INK_SAC,
            Material.TRIPWIRE_HOOK
    );

    public static boolean isAxePreference(Block block) {
        return block.isPreferredTool(new ItemStack(Material.NETHERITE_AXE));
    }

    public static boolean isShovelPreference(Block block) {
        return block.isPreferredTool(new ItemStack(Material.NETHERITE_SHOVEL));
    }

    public static boolean isSwordPreference(Block block) {
        return block.isPreferredTool(new ItemStack(Material.NETHERITE_SWORD));
    }

    public static boolean isInvalidDamageableEntities(EntityType type) {
        Set<String> invalidSuffixes = Set.of("_BOAT", "_RAFT", "MINECART");
        if (invalidSuffixes.stream().anyMatch(s -> type.name().endsWith(s))) {
            return true;
        }

        return additionalInvalid.contains(type);
    }

    public static boolean isSmeltOre(Material material) {
        if (material == Material.ANCIENT_DEBRIS) return true;
        return material.name().endsWith("_ORE");
    }
}
