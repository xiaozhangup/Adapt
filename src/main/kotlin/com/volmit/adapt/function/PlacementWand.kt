package com.volmit.adapt.function

import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes
import com.volmit.adapt.content.adaptation.architect.ArchitectPlacement
import com.volmit.adapt.util.Components
import io.github.retrooper.packetevents.util.SpigotConversionUtil
import me.xiaozhangup.slimecargo.command.takeItems
import me.xiaozhangup.whale.lib.entitylib.meta.display.BlockDisplayMeta
import me.xiaozhangup.whale.util.PlayerBaffle
import me.xiaozhangup.whale.util.entity.VirtualEntity
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import org.bukkit.Color
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.block.TileState
import org.bukkit.block.data.Levelled
import org.bukkit.block.data.Waterlogged
import org.bukkit.block.data.type.Slab
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
    private val playerDisplayEntities = mutableMapOf<UUID, List<VirtualEntity>>()
    private val playerLastTargetBlock = mutableMapOf<UUID, String>()
    private val placeBaffle = PlayerBaffle(150, TimeUnit.MILLISECONDS)
    private val wandDistance = 8
    private val brightness = (15 shl 4) or (15 shl 20)
    private val glowColor = Color.fromRGB(242, 224, 255).asRGB()
    private val flatFaceCache = mutableMapOf<BlockFace, Set<BlockFace>>()

    fun renderBlockEntity(player: Player) {
        if (!isWand(player.inventory.itemInMainHand)) {
            clearPlayerEntities(player)
            return
        }

        val block = player.getTargetBlockExact(wandDistance) ?: run {
            clearPlayerEntities(player)
            return
        }

        val face = player.getTargetBlockFace(wandDistance) ?: return
        val targetBlockKey = buildTargetBlockKey(block, face)
        val uniqueId = player.uniqueId
        if (playerLastTargetBlock[uniqueId] == targetBlockKey) return

        val connectedBlocks = findConnectedBlocks(block, face, maxBlocks) {
            adapt.canBlockPlace(player, it)
        }

        if (connectedBlocks.isEmpty()) {
            clearPlayerEntities(player)
            return
        }

        val newEntities = connectedBlocks.map { connectedBlock ->
            val targetLocation = connectedBlock.getRelative(face).location
            VirtualEntity(EntityTypes.BLOCK_DISPLAY, targetLocation) {
                val meta = it.getEntityMeta() as BlockDisplayMeta
                meta.glowColorOverride = glowColor
                meta.isGlowing = true
                meta.brightnessOverride = brightness

                val blockData = connectedBlock.blockData
                if (blockData is Waterlogged) {
                    blockData.isWaterlogged = false
                }
                meta.blockState = SpigotConversionUtil.fromBukkitBlockData(blockData)
            }
        }

        clearPlayerEntities(player)
        playerLastTargetBlock[uniqueId] = targetBlockKey
        playerDisplayEntities[uniqueId] = newEntities
    }

    fun onPlayerInteract(event: PlayerInteractEvent) {
        if (event.action != Action.RIGHT_CLICK_BLOCK && event.action != Action.RIGHT_CLICK_AIR) return
        if (event.hand != EquipmentSlot.HAND) return

        val player = event.player
        val item = event.item ?: return
        if (!isWand(item) || !adapt.hasAdaptation(player)) return
        if (!placeBaffle.hasNext(player)) return

        event.isCancelled = true

        val block = player.getTargetBlockExact(wandDistance) ?: return
        val type = block.type

        if (block.state is TileState) {
            sendActionBarMessage(player, Components.mini(
                "<#85ced1>◆ 不支持放置<material>",
                Placeholder.component("material", Component.translatable(type.translationKey()))
            ))
            return
        }

        val face = player.getTargetBlockFace(wandDistance) ?: return
        val connectedBlocks = findConnectedBlocks(block, face, maxBlocks) {
            adapt.canBlockPlace(player, it)
        }
        if (connectedBlocks.isEmpty()) return

        val takeCount = calculateRequiredItems(connectedBlocks)

        if (!player.takeItems(ItemStack(type), takeCount)) {
            sendActionBarMessage(player, Components.mini(
                "<#85ced1>◆ 不足 <count> 个<material>",
                Placeholder.unparsed("count", takeCount.toString()),
                Placeholder.component("material", Component.translatable(type.translationKey()))
            ))
            return
        }

        for (connectedBlock in connectedBlocks) {
            val targetBlock = connectedBlock.getRelative(face)
            var waterlogged = false
            if (targetBlock.type == Material.WATER) {
                val targetLevelled = targetBlock.blockData as? Levelled
                if (targetLevelled != null && targetLevelled.level == targetLevelled.maximumLevel) {
                    waterlogged = true
                }
            }

            targetBlock.type = connectedBlock.type
            targetBlock.blockData = connectedBlock.blockData

            val targetData = targetBlock.blockData as? Waterlogged ?: continue
            targetData.isWaterlogged = waterlogged
        }

        sendActionBarMessage(player, Components.mini(
            "<#85ced1>◆ 放置了 <count> 个<material>",
            Placeholder.unparsed("count", takeCount.toString()),
            Placeholder.component("material", Component.translatable(type.translationKey()))
        ))
    }

    fun clearPlayerEntities(player: Player) {
        clearPlayerEntities(player.uniqueId)
    }

    fun clearPlayerEntities(playerId: UUID) {
        playerDisplayEntities[playerId]?.forEach { entity ->
            entity.despawn()
        }
        playerDisplayEntities.remove(playerId)
        playerLastTargetBlock.remove(playerId)
    }

    fun trackedPlayerIds(): Set<UUID> = (playerDisplayEntities.keys + playerLastTargetBlock.keys).toSet()

    private fun isWand(item: ItemStack) : Boolean {
        return item.type == Material.BREEZE_ROD && !item.hasItemMeta()
    }

    private fun findConnectedBlocks(startBlock: Block, face: BlockFace, maxBlocks: Int, filter: (Block) -> Boolean): Set<Block> {
        val targetBlock = startBlock.getRelative(face)
        if (!isReplaceable(targetBlock) || !filter(targetBlock)) return emptySet()

        val startType = startBlock.type
        val visited = mutableSetOf<Block>()
        val queue = ArrayDeque<Block>()
        val result = mutableSetOf<Block>()
        val searchDirections = getFlatFace(face)

        queue.add(startBlock)
        visited.add(startBlock)
        result.add(startBlock)

        while (queue.isNotEmpty() && result.size < maxBlocks) {
            val currentBlock = queue.removeFirst()

            for (direction in searchDirections) {
                if (result.size >= maxBlocks) break

                val nextBlock = currentBlock.getRelative(direction)
                if (visited.add(nextBlock) && nextBlock.type == startType) {
                    val nextTarget = nextBlock.getRelative(face)
                    if (isReplaceable(nextTarget) && filter(nextTarget)) {
                        queue.add(nextBlock)
                        result.add(nextBlock)
                    }
                }
            }
        }

        return result
    }

    private fun isReplaceable(block: Block): Boolean {
        return block.type.isAir || block.type == Material.WATER || block.type == Material.LAVA
    }

    private fun buildTargetBlockKey(block: Block, face: BlockFace): String {
        return "${face.ordinal}_${block.world.name}_${block.x}_${block.y}_${block.z}"
    }

    private fun sendActionBarMessage(player: Player, message: Component) {
        player.sendActionBar(message)
    }

    private fun calculateRequiredItems(connectedBlocks: Set<Block>): Int {
        return connectedBlocks.sumOf { block ->
            val blockData = block.blockData
            if (blockData is Slab && blockData.type == Slab.Type.DOUBLE) 2 else 1
        }
    }

    private fun getFlatFace(face: BlockFace): Set<BlockFace> {
        return flatFaceCache.getOrPut(face) {
            val allFaces = setOf(
                BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH,
                BlockFace.WEST, BlockFace.UP, BlockFace.DOWN
            )
            allFaces - face - face.oppositeFace
        }
    }
}
