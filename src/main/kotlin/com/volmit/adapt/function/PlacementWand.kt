package com.volmit.adapt.function

import com.volmit.adapt.content.adaptation.architect.ArchitectPlacement
import me.xiaozhangup.slimecargo.command.takeItems
import me.xiaozhangup.whale.util.PlayerBaffle
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Color
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.block.Container
import org.bukkit.block.TileState
import org.bukkit.block.data.type.Slab
import org.bukkit.entity.BlockDisplay
import org.bukkit.entity.Display
import org.bukkit.entity.Player
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import java.util.*
import java.util.concurrent.TimeUnit

class PlacementWand(
    val adapt: ArchitectPlacement,
    val maxBlocks: Int
) {
    private val playerDisplayEntities = mutableMapOf<Player, MutableList<BlockDisplay>>()
    private val playerLastTargetBlock = mutableMapOf<Player, String>()
    private val placeBaffle = PlayerBaffle(150, TimeUnit.MILLISECONDS)
    private val wandDistance = 8
    private val glowColor = Color.fromRGB(242, 224, 255)

    fun renderBlockEntity(player: Player) {
        val hasAdaptation = adapt.hasAdaptation(player)
        if (!isWand(player.inventory.itemInMainHand) || !hasAdaptation) {
            clearPlayerEntities(player)
            return
        }

        val block = player.getTargetBlockExact(wandDistance)
        if (block == null) {
            clearPlayerEntities(player)
            return
        }

        val face = player.getTargetBlockFace(wandDistance) ?: return
        val relative = getFlatFace(face)

        // 查重
        val targetBlock = "${face}_${block.world}_${block.x}_${block.y}_${block.z}"
        val lastTargetBlock = playerLastTargetBlock[player]
        if (lastTargetBlock == targetBlock) return

        clearPlayerEntities(player)
        playerLastTargetBlock[player] = targetBlock

        val connectedBlocks = findConnectedBlocks(block, face, relative, maxBlocks) { adapt.canBlockPlace(player, it) }
        if (connectedBlocks.isEmpty()) return

        val entities = mutableListOf<BlockDisplay>()
        for (connectedBlock in connectedBlocks) {
            val b = connectedBlock.getRelative(face)

            val loc = b.location
            val entity = loc.world.spawn(loc, BlockDisplay::class.java) {
                it.isPersistent = false
                it.block = connectedBlock.blockData
                it.glowColorOverride = glowColor
                it.isGlowing = true
                it.brightness = Display.Brightness(15, 15)
            }
            entities.add(entity)
        }

        playerDisplayEntities[player] = entities
    }

    fun onPlayerInteract(event: PlayerInteractEvent) {
        val player = event.player

        if (event.action != Action.RIGHT_CLICK_BLOCK && event.action != Action.RIGHT_CLICK_AIR) return
        if (event.hand != EquipmentSlot.HAND) return

        val item = event.item ?: return
        if (!isWand(item) || !adapt.hasAdaptation(player)) return
        if (!placeBaffle.hasNext(player)) return

        event.isCancelled = true

        val block = player.getTargetBlockExact(wandDistance) ?: return
        if (block.state is TileState) {
            player.sendActionBar(
                MiniMessage.miniMessage().deserialize("<color:#85ced1>◆ 不支持放置<lang:${block.type.translationKey()}>")
            )
            return
        }

        val face = player.getTargetBlockFace(wandDistance) ?: return
        val relative = getFlatFace(face)
        val connectedBlocks = findConnectedBlocks(block, face, relative, maxBlocks) { adapt.canBlockPlace(player, it) }
        if (connectedBlocks.isEmpty()) return

        val type = block.type
        val typeName = type.data.simpleName
        var take = 0
        for (it in connectedBlocks) {
            take += when (typeName) {
                "MaterialData", "Stairs", "Snowable", "Directional", "Wall", "Orientable" -> 1
                "Slab" -> {
                    val s = it.blockData as Slab
                    when (s.type) {
                        Slab.Type.DOUBLE -> 2
                        Slab.Type.BOTTOM -> 1
                        Slab.Type.TOP -> 1
                    }
                }
                else -> {
                    player.sendActionBar(
                        MiniMessage.miniMessage().deserialize("<color:#85ced1>◆ 不支持放置<lang:${type.translationKey()}>")
                    )
                    return
                }
            }
        }

        if (!player.takeItems(ItemStack(type), take)) {
            player.sendActionBar(
                MiniMessage.miniMessage().deserialize("<color:#85ced1>◆ 不足 $take 个<lang:${type.translationKey()}>")
            )
            return
        }

        for (connectedBlock in connectedBlocks) {
            val b = connectedBlock.getRelative(face)
            b.type = connectedBlock.type
            b.blockData = connectedBlock.blockData
        }
        player.sendActionBar(
            MiniMessage.miniMessage().deserialize("<color:#85ced1>◆ 放置了 $take 个<lang:${type.translationKey()}>")
        )
    }

    fun clearPlayerEntities(player: Player) {
        playerDisplayEntities[player]?.forEach { entity ->
            if (!entity.isDead) {
                entity.remove()
            }
        }
        playerDisplayEntities.remove(player)
        playerLastTargetBlock.remove(player)
    }

    private fun isWand(item: ItemStack) : Boolean {
        return item.type == Material.BREEZE_ROD && !item.hasItemMeta()
    }

    private fun findConnectedBlocks(startBlock: Block, face: BlockFace, searchDirections: Set<BlockFace>, maxBlocks: Int, filter: (Block) -> Boolean): Set<Block> {
        val block = startBlock.getRelative(face)
        if (!block.type.isAir || !filter.invoke(block)) return setOf()

        val visited = mutableSetOf<Block>()
        val queue: Queue<Block> = LinkedList()
        val result = mutableSetOf<Block>()

        queue.offer(startBlock)
        visited.add(startBlock)
        result.add(startBlock)

        while (queue.isNotEmpty() && result.size < maxBlocks) {
            val currentBlock = queue.poll()

            for (direction in searchDirections) {
                val nextBlock = currentBlock.getRelative(direction)

                // 检查是否已访问、是否为同类方块、结果集是否已满
                val relative = nextBlock.getRelative(face)
                if (nextBlock !in visited &&
                    nextBlock.type == startBlock.type &&
                    result.size < maxBlocks &&
                    relative.type.isAir &&
                    filter.invoke(relative)
                ) {
                    visited.add(nextBlock)
                    queue.offer(nextBlock)
                    result.add(nextBlock)
                }
            }
        }

        return result
    }

    private fun getFlatFace(face: BlockFace) : Set<BlockFace> {
        val faces = mutableSetOf(
            BlockFace.NORTH,
            BlockFace.EAST,
            BlockFace.SOUTH,
            BlockFace.WEST,
            BlockFace.UP,
            BlockFace.DOWN
        )

        faces.remove(face)
        faces.remove(face.oppositeFace)
        return faces
    }
}