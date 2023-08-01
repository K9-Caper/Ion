package net.horizonsend.ion.server.features.multiblock.event

import net.horizonsend.ion.server.features.multiblock.misc.TractorBeamMultiblock
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.event.Cancellable
import org.bukkit.event.HandlerList

class PlayerUseTractorBeamEvent(
	override val player: Player,
	val oldLocation: Location,
	val newLocation: Location
) : PlayerMultiblockEvent<TractorBeamMultiblock>(TractorBeamMultiblock, player), Cancellable {
	private var cancelled: Boolean = false

	override fun getHandlers(): HandlerList {
		return handlerList
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
