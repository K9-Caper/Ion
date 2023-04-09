package net.horizonsend.ion.server.explosions

import kotlin.DeprecationLevel.ERROR
import net.horizonsend.ion.server.IonWorld
import net.horizonsend.ion.server.miscellaneous.minecraft
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState
import net.starlegacy.feature.starship.active.ActiveStarships
import org.bukkit.World
import org.bukkit.block.Block
import org.bukkit.craftbukkit.v1_19_R2.block.CraftBlock
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority.HIGHEST
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockExplodeEvent
import org.bukkit.event.entity.EntityExplodeEvent

object ExplosionReversal : Listener {
	@Deprecated("Event Listener", level = ERROR)
	@EventHandler(priority = HIGHEST)
	fun onBlockExplodeEvent(event: BlockExplodeEvent) {
		handleBlockExplosions(event.block.world, event.blockList())
	}

	@Deprecated("Event Listener", level = ERROR)
	@EventHandler(priority = HIGHEST)
	fun onEntityExplodeEvent(event: EntityExplodeEvent) {
		handleBlockExplosions(event.location.world, event.blockList())
	}

	private fun handleBlockExplosions(world: World, blocks: List<Block>) {
		val serverLevel = world.minecraft

		for (block in blocks) {
			block as CraftBlock // We use NMS :)

			val blockPosition = block.position
			val ionWorld = IonWorld[serverLevel]

			val blockState = block.nms
			val blockEntity = serverLevel.getBlockEntity(block.position)?.saveWithFullMetadata()

			val explosion = Explosion(blockPosition, blockState, blockEntity)
			val levelChunk = block.chunk.minecraft

			val ionChunk = ionWorld[block.chunk.minecraft]
			ionChunk.lastExplodeTick = MinecraftServer.currentTick
			ionChunk.explosions.add(explosion)

			// Remove the block cleanly to prevent items from being dropped
			levelChunk.level.setBlock(blockPosition, Blocks.AIR.defaultBlockState(), 0)
			levelChunk.level.getChunk(blockPosition).removeBlockEntity(blockPosition)
		}
	}

	data class Explosion(val pos: BlockPos, val blockState: BlockState, val tag: CompoundTag?) {
		fun canRegenerate(serverLevel: ServerLevel): Boolean {
			return ActiveStarships.findByBlock(serverLevel.world, pos.x, pos.y, pos.z) == null
		}
	}
}