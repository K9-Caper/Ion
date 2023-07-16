package net.starlegacy.feature.multiblock.tractorbeam

import net.starlegacy.feature.multiblock.PlayerMultiblockEvent
import net.starlegacy.feature.starship.event.StarshipUnpilotEvent
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.event.Cancellable
import org.bukkit.event.HandlerList

class PlayerUseTractorBeamEvent(
	override val location: Location,
	override val player: Player
) : PlayerMultiblockEvent<TractorBeamMultiblock>(TractorBeamMultiblock, player), Cancellable {
	private var cancelled: Boolean = false

	override fun getHandlers(): HandlerList {
		return StarshipUnpilotEvent.handlerList
	}

	override fun isCancelled() = cancelled

	override fun setCancelled(cancelled: Boolean) {
		this.cancelled = cancelled
	}

	companion object {
		@JvmStatic
		val handlerList = HandlerList()
	}
}
