package net.starlegacy.feature.multiblock.misc

import net.horizonsend.ion.common.extensions.information
import net.horizonsend.ion.common.extensions.success
import net.horizonsend.ion.common.extensions.userError
import net.horizonsend.ion.server.IonServer
import net.horizonsend.ion.server.miscellaneous.minecraft
import net.starlegacy.feature.misc.DecomposeTask
import net.starlegacy.feature.multiblock.InteractableMultiblock
import net.starlegacy.feature.multiblock.LegacyMultiblockShape
import net.starlegacy.feature.multiblock.Multiblock
import net.starlegacy.feature.multiblock.Multiblocks
import net.starlegacy.feature.multiblock.PowerStoringMultiblock
import net.starlegacy.util.CHISELED_TYPES
import net.starlegacy.util.add
import net.starlegacy.util.getFacing
import net.starlegacy.util.getRelativeIfLoaded
import net.starlegacy.util.isAir
import net.starlegacy.util.rightFace
import net.starlegacy.util.toBlockPos
import org.bukkit.Location
import org.bukkit.block.BlockFace
import org.bukkit.block.Sign
import org.bukkit.entity.Player
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder
import kotlin.math.max

object DecomposerMultiblock : Multiblock(), PowerStoringMultiblock, InteractableMultiblock {
	override val maxPower: Int = 75_000
	override val name: String = "decomposer"
	override val signText = createSignText(
		"&cDecomposer",
		null,
		null,
		null
	)

	val busySigns = mutableMapOf<Location, DecomposeTask>()

	private const val MAX_LENGTH = 100
	const val BLOCKS_PER_SECOND = 1000
	private val FRAME_MATERIAL = CHISELED_TYPES

	override fun LegacyMultiblockShape.buildStructure() {
		at(0, 0, 0).ironBlock()
		at(0, -1, -1).anyPipedInventory()
	}

	fun getStorage(sign: Sign): Inventory {
		return (sign.block.getRelative(BlockFace.DOWN).state as InventoryHolder).inventory
	}

	override fun onSignInteract(sign: Sign, player: Player, event: PlayerInteractEvent) {
		val multiblock = Multiblocks[sign] as? DecomposerMultiblock ?: return
		val signLoc = sign.location

		if (event.action == Action.RIGHT_CLICK_BLOCK) {
			val forward = sign.getFacing().oppositeFace
			val up = BlockFace.UP
			val right = forward.rightFace

			val origin: Location = signLoc.clone()
				.add(forward.direction.multiply(2))
				.add(up.direction)
				.add(right.direction)

			val frameOrigin: Location = signLoc.clone().add(forward.direction)

			val width = getDimension(frameOrigin, right)
			val height = getDimension(frameOrigin, up)
			val length = getDimension(frameOrigin, forward)

			val area = height * length
			val delay = max(10L, area / BLOCKS_PER_SECOND * 20L)

			val offset = calculateOffset(origin, width, height, length, right, up, forward)

			if (offset > width) return event.player.userError("Decomposer empty!")

			val task = DecomposeTask(
				signLoc,
				width,
				offset,
				height,
				length,
				origin,
				right,
				up,
				forward,
				event.player.uniqueId,
				multiblock
			)

			if (busySigns.containsKey(signLoc)) {
				event.player.userError("Decomposer in use")
				return
			}

			task.runTaskTimer(IonServer, delay, delay)

			event.player.success("Started Decomposer")

			busySigns[signLoc] = task
		} else {
			(busySigns[signLoc] ?: return event.player.information("Decomposer not in use")).cancel()

			event.player.information("Cancelled running decomposer")
		}
	}

	private fun getDimension(origin: Location, direction: BlockFace): Int {
		var dimension = 0
		var tempBlock = origin.block

		while (dimension < MAX_LENGTH) {
			tempBlock = tempBlock.getRelativeIfLoaded(direction)
				?: return dimension

			if (!FRAME_MATERIAL.contains(tempBlock.type)) {
				return dimension
			}

			dimension++
		}

		return dimension
	}

	private fun calculateOffset(
		origin: Location,
		width: Int,
		height: Int,
		length: Int,
		right: BlockFace,
		up: BlockFace,
		forward: BlockFace
	): Int {
		val serverLevel = origin.world.minecraft

		for (offsetWidth: Int in 0 until height) {
			for (offsetForward: Int in 0 until length) {
				for (offsetUp: Int in 0 until width) {
					val originBlockPos = origin.toBlockPos().mutable()
					originBlockPos.add(up.direction.multiply(offsetUp).toBlockPos())
					originBlockPos.add(forward.direction.multiply(offsetForward).toBlockPos())
					originBlockPos.add(right.direction.multiply(offsetWidth).toBlockPos())

					val block = serverLevel.getBlockIfLoaded(originBlockPos)
					if (block?.isAir() == false) return offsetWidth
				}
			}
		}

		return width + 1
	}
}
