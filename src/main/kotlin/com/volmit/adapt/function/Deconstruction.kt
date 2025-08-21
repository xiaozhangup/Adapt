package com.volmit.adapt.function

import com.volmit.adapt.api.adaptation.SimpleAdaptation
import com.volmit.adapt.util.SoundPlayer
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Item
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.Recipe
import org.bukkit.inventory.RecipeChoice
import org.bukkit.inventory.ShapedRecipe
import org.bukkit.inventory.ShapelessRecipe
import org.bukkit.inventory.meta.Damageable
import java.util.*

object Deconstruction {

    @JvmStatic
    fun processItemInteraction(player: Player, mainHandItem: ItemStack, itemEntity: Item, adaptation: SimpleAdaptation<*>) {
        val forStuff = itemEntity.itemStack
        val offerings: List<ItemStack> = getDeconstructionOffering(forStuff, adaptation)

        val spw = SoundPlayer.of(player.world)
        if (offerings.isNotEmpty()) {
            // 设置第一个物品到原 itemEntity
            itemEntity.itemStack = offerings[0]

            // 如果有多个物品（堆叠溢出的情况），在同一位置创建新的 Item 实体
            for (i in 1 until offerings.size) {
                val newItem = player.world.dropItem(itemEntity.location, offerings[i])
                newItem.velocity = itemEntity.velocity // 保持相同的运动状态
            }

            spw.play(itemEntity.location, Sound.BLOCK_BASALT_BREAK, 1f, 0.2f)
            spw.play(itemEntity.location, Sound.BLOCK_BEEHIVE_SHEAR, 1f, 0.7f)

            // 计算总经验值
            val totalXp = offerings.sumOf { adaptation.getValue(it) }
            adaptation.skill.xp(player, totalXp)

            // 损坏剪刀
            val damageable = mainHandItem.itemMeta as Damageable
            val newDamage = damageable.damage + 8 * forStuff.amount
            if (newDamage >= mainHandItem.type.getMaxDurability()) {
                player.inventory.setItemInMainHand(null) // 破坏剪刀
            } else {
                damageable.damage = newDamage
                mainHandItem.setItemMeta(damageable)
            }
        } else {
            spw.play(itemEntity.location, Sound.BLOCK_REDSTONE_TORCH_BURNOUT, 1f, 1f) // 烧毁火把声音
        }
    }

    @JvmStatic
    private fun getDeconstructionOffering(forStuff: ItemStack, adaptation: SimpleAdaptation<*>): List<ItemStack> {
        val (selectedRecipe, items) = itemRecipes(forStuff)
        if (selectedRecipe == null || items.isEmpty()) return emptyList()

        // 从 itemRecipes 返回的 items 中找到数量最多的物品类型
        var bestItemStack: ItemStack? = null
        var bestAmountPerCraft = 0

        // 统计每种物品的总数量（考虑完整的ItemStack属性）
        val itemCounts: MutableMap<ItemStack, Int> = HashMap()
        for (item in items) {
            var found = false
            for (existingItem in itemCounts.keys) {
                if (existingItem.isSimilar(item)) {
                    itemCounts[existingItem] = itemCounts[existingItem]!! + item.amount
                    found = true
                    break
                }
            }
            if (!found) {
                itemCounts[item.clone()] = item.amount
            }
        }

        // 找到数量最多的物品类型
        for ((itemStack, amount) in itemCounts.entries) {
            if (amount > bestAmountPerCraft) {
                bestAmountPerCraft = amount
                bestItemStack = itemStack
            }
        }

        if (bestItemStack == null) return emptyList()
        val recipeOutputAmount = selectedRecipe.result.amount

        // 计算可以拆解多少次配方
        val timesCanDeconstruct = forStuff.amount / recipeOutputAmount
        val remainder = forStuff.amount % recipeOutputAmount

        // 计算应该给出的材料总数
        var totalMaterialAmount = 0

        if (timesCanDeconstruct > 0) {
            // 完整配方的拆解：给出需要材料数量的一半左右，至少1个
            val fullDeconstructAmount = (bestAmountPerCraft * timesCanDeconstruct / 2).coerceAtLeast(1)
            totalMaterialAmount += fullDeconstructAmount
        }

        if (remainder > 0) {
            // 不完整配方的按比例拆解
            val ratio = remainder.toDouble() / recipeOutputAmount.toDouble()
            val partialAmount = (bestAmountPerCraft * ratio / 2).toInt().coerceAtLeast(1)
            totalMaterialAmount += partialAmount
        }

        // 如果没有拆解出任何材料，至少给1个
        if (totalMaterialAmount == 0) {
            totalMaterialAmount = 1
        }

        // 创建用于价值检查的物品实例
        val resultItemForValueCheck = bestItemStack.clone()
        resultItemForValueCheck.amount = totalMaterialAmount
        if (adaptation.getValue(resultItemForValueCheck) >= adaptation.getValue(forStuff)) {
            return emptyList()
        }

        // 处理堆叠限制
        val maxStackSize = bestItemStack.maxStackSize
        val resultItems = mutableListOf<ItemStack>()

        var remainingAmount = totalMaterialAmount
        while (remainingAmount > 0) {
            val stackAmount = remainingAmount.coerceAtMost(maxStackSize)
            val resultItem = bestItemStack.clone()
            resultItem.amount = stackAmount
            resultItems.add(resultItem)
            remainingAmount -= stackAmount
        }

        return resultItems
    }

    @JvmStatic
    private fun itemRecipes(item: ItemStack): Pair<Recipe?, List<ItemStack>> {
        val recipes = Bukkit.getRecipesFor(item).filter { recipe ->
            (recipe is ShapedRecipe || recipe is ShapelessRecipe) &&
            item.isSimilar(recipe.result)
        } // 精确匹配配方

        var recipe: Recipe? = null
        var power = 0
        var list = listOf<ItemStack>()

        for (r in recipes) {
            val items = mutableListOf<ItemStack>()
            when(r) {
                is ShapedRecipe -> {
                    r.choiceMap.values.map { u ->
                        if (u is RecipeChoice.MaterialChoice) {
                            items += u.itemStack
                        }
                        if (u is RecipeChoice.ExactChoice) {
                            items += u.itemStack
                        }
                    }
                }
                is ShapelessRecipe -> {
                    r.choiceList.forEach { u ->
                        if (u is RecipeChoice.MaterialChoice) {
                            items += u.itemStack
                        }
                        if (u is RecipeChoice.ExactChoice) {
                            items += u.itemStack
                        }
                    }
                }
                else -> {}
            }

            if (items.isNotEmpty() && items.size > power) {
                power = items.size
                list = items
                recipe = r
            }
        }

        return recipe to list
    }
}