package net.starlegacy.feature.multiblock.startershipdispenser

import com.sk89q.worldedit.extent.clipboard.Clipboard
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap
import net.horizonsend.ion.common.extensions.success
import net.horizonsend.ion.server.IonServer
import net.horizonsend.ion.server.miscellaneous.Vec3i
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.Component.text
import net.minecraft.world.level.block.state.BlockState
import net.starlegacy.feature.multiblock.InteractableMultiblock
import net.starlegacy.feature.multiblock.LegacyMultiblockShape
import net.starlegacy.feature.multiblock.Multiblock
import net.starlegacy.util.blockKey
import net.starlegacy.util.blockplacement.BlockPlacement
import net.starlegacy.util.getFacing
import net.starlegacy.util.nms
import net.starlegacy.util.paste
import net.starlegacy.util.placeSchematicEfficiently
import net.starlegacy.util.readSchematic
import net.starlegacy.util.rightFace
import net.starlegacy.util.toBukkitBlockData
import org.bukkit.Material
import org.bukkit.block.BlockFace
import org.bukkit.block.Sign
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.util.BoundingBox
import kotlin.math.roundToInt

object StarterShipDispenserMultiblock : Multiblock(), InteractableMultiblock {
	override val name: String = "startership"
	override val signText: Array<Component?> = arrayOf(
		text("Retrieve Cargo"),
		text("Shuttle"),
		null,
		null
	)

	const val schemName: String = "StarterShuttle.schem"

	override fun LegacyMultiblockShape.buildStructure() {
		at(0, 0, 0).type(Material.BARRIER)
	}

	override fun onSignInteract(sign: Sign, player: Player, event: PlayerInteractEvent) {
		val isClear = checkClear(sign)

		if (isClear) clear(sign)

		spawnShuttle(sign)
		callEvent(sign, player)

		player.success("Retrieved cargo shuttle!")
	}

	private fun getSchematic(): Clipboard {
		val schemFile = IonServer.dataFolder.resolve(schemName)

		return readSchematic(schemFile) ?: error("Failed to read schematic $name")
	}

	fun getBoundingBox(sign: Sign): BoundingBox {
		val dir = sign.getFacing()

		val originBlock = sign.block.getRelative(dir.oppositeFace, 2)
		val origin = originBlock.location

		val boundingBox = getSchematic().region

		val length = boundingBox.length
		val width = boundingBox.width
		val height = boundingBox.height

		val opposite = origin.clone()
			.add(dir.rightFace.direction.multiply(length))
			.add(BlockFace.UP.direction.multiply(height))
			.add(dir.direction.multiply(width))
			.toBlockLocation()

		return BoundingBox.of(
			origin,
			opposite
		)
	}

	fun clear(sign: Sign) {
		val box = getBoundingBox(sign)

		val air = Material.AIR.createBlockData()

		for (x in box.minX.toInt()..box.maxX.toInt()) {
			for (y in box.minY.toInt()..box.maxY.toInt()) {
				for (z in box.minZ.toInt()..box.maxZ.toInt()) {
					sign.world.setBlockData(x, y, z, air)
				}
			}
		}
	}

	private fun checkClear(sign: Sign): Boolean {
		val box = getBoundingBox(sign)

		for (x in box.minX.toInt()..box.maxX.toInt()) {
			for (y in box.minY.toInt()..box.maxY.toInt()) {
				for (z in box.minZ.toInt()..box.maxZ.toInt()) {
					if (!sign.world.getBlockAt(x, y, z).type.isAir) return false
				}
			}
		}

		return true
	}

	fun spawnShuttle(sign: Sign) {
		val schematic = getSchematic()

		val direction = sign.getFacing().oppositeFace
		val sideDirection = direction.rightFace

		val negativeX = if (direction.modX == 0) sideDirection.modX < 0 else direction.modX < 0
		val negativeZ = if (direction.modZ == 0) sideDirection.modZ < 0 else direction.modZ < 0

		val x = if (negativeX) schematic.region.minimumPoint.x else schematic.region.maximumPoint.x
		val y = schematic.region.minimumPoint.y
		val z = if (negativeZ) schematic.region.minimumPoint.z else schematic.region.maximumPoint.z

		val offsetX = (x - schematic.region.center.x * 2).roundToInt()
		val offsetY = (-y.toDouble()).roundToInt()
		val offsetZ = (z - schematic.region.center.z * 2).roundToInt()

		val targetX = sign.x + direction.modX * 3 + sideDirection.modX
		val targetY = sign.y
		val targetZ = sign.z + direction.modZ * 3 + sideDirection.modZ

		println("pasting at ${targetX + offsetX}, ${targetY + offsetY}, ${targetZ + offsetZ}")

		schematic.paste(sign.world, targetX + offsetX, targetY + offsetY, targetZ + offsetZ, true) //TODO
	}

	fun callEvent(sign: Sign, player: Player) {
		DispenseStarterShipEvent(sign, player).callEvent()
	}
}
