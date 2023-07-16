package net.starlegacy.feature.multiblock.tractorbeam

import com.destroystokyo.paper.event.player.PlayerJumpEvent
import io.papermc.paper.entity.TeleportFlag
import net.starlegacy.feature.multiblock.LegacyMultiblockShape
import net.starlegacy.feature.multiblock.Multiblock
import net.starlegacy.feature.multiblock.InteractableMultiblock
import net.starlegacy.feature.multiblock.Multiblocks
import net.starlegacy.feature.starship.active.ActiveStarships
import net.starlegacy.util.LegacyBlockUtils
import net.starlegacy.util.isStainedGlass
import net.starlegacy.util.isWallSign
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.BlockFace
import org.bukkit.block.Sign
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause
import org.bukkit.event.player.PlayerToggleSneakEvent

object TractorBeamMultiblock : Multiblock(), InteractableMultiblock, Listener {
	override val name = "tractorbeam"

	override val signText = createSignText(
		line1 = "&7Tractor",
		line2 = "&7Beam",
		line3 = "[-?::]",
		line4 = "[:->:]"
	)

	override fun LegacyMultiblockShape.buildStructure() {
		at(+0, +0, +0).anySlab()
		at(-1, +0, +1).anySlab()
		at(+1, +0, +1).anySlab()
		at(+0, +0, +2).anySlab()

		at(+0, +0, +1).anyGlass()
	}

	override fun onSignInteract(sign: Sign, player: Player, event: PlayerInteractEvent) {
		if (event.item?.type != Material.CLOCK) return
		if (event.action != Action.RIGHT_CLICK_BLOCK) return
		if (event.clickedBlock?.type?.isWallSign != true) return

		tryDescend(player)
	}

	fun tryDescend(player: Player) {
		val below = player.location.block.getRelative(BlockFace.DOWN)

		if (below.type != Material.GLASS && !below.type.isStainedGlass) return

		var distance = 1
		val maxDistance = below.y - 1

		while (distance < maxDistance) {
			val relative = below.getRelative(BlockFace.DOWN, distance)

			if (relative.type != Material.AIR) {
				break
			}

			distance++
		}

		if (distance < 3) return

		val relative = below.getRelative(BlockFace.DOWN, distance)
		val relativeLoc = relative.location.toCenterLocation().add(0.0, 1.0, 0.0)

		if (relative.type != Material.AIR) {
			doTeleport(player, relativeLoc)
		}
	}

	fun doTeleport(player: Player, newLoc: Location) {
		val event = PlayerUseTractorBeamEvent(
			player,
			player.location,
			newLoc
		)

		event.callEvent()

		println("calling event")
		if (event.isCancelled) return

		player.teleport(
			newLoc,
			TeleportCause.PLUGIN,
			*TeleportFlag.Relative.values()
		)
	}

	fun tryAscend(player: Player) {
		val blockStandingIn = player.location.block

		for (i in player.world.minHeight..(player.world.maxHeight - blockStandingIn.y)) {
			val block = blockStandingIn.getRelative(BlockFace.UP, i)
			if (block.type == Material.AIR) continue

			val newLoc = block.location.toCenterLocation().add(0.0, 1.0, 0.0)

			if (block.type == Material.GLASS || block.type.isStainedGlass) {
				for (face in LegacyBlockUtils.PIPE_DIRECTIONS) {
					val sign = block.getRelative(face, 2)
					if (!sign.type.isWallSign) continue

					if (Multiblocks[sign.getState(false) as Sign] !is TractorBeamMultiblock) continue

					doTeleport(player, newLoc)
				}

				continue
			}

			return
		}
	}

	// Bring the player up if they right click while facing up with a clock
	// and there's a tractor beam above them
	@EventHandler(priority = EventPriority.MONITOR)
	fun onPlayerInteractEventC(event: PlayerInteractEvent) {
		if (event.item?.type != Material.CLOCK) return
		if (event.action != Action.RIGHT_CLICK_AIR && event.action != Action.RIGHT_CLICK_BLOCK) return
		if (event.player.location.pitch > -60) return

		tryAscend(event.player)
	}

	@EventHandler(priority = EventPriority.MONITOR)
	fun onPlayerJumpEvent(event: PlayerJumpEvent) {
		if (event.player.inventory.itemInMainHand.type != Material.CLOCK) return

		tryAscend(event.player)
	}

	@EventHandler(priority = EventPriority.MONITOR)
	fun onPlayerToggleSneakEvent(event: PlayerToggleSneakEvent) {
		if (!event.isSneaking) return
		if (ActiveStarships.findByPilot(event.player) != null) return
		if (event.player.inventory.itemInMainHand.type != Material.CLOCK) return

		tryDescend(event.player)
	}
}
