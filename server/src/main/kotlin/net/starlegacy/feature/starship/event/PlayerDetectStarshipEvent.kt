package net.starlegacy.feature.starship.event

import org.bukkit.entity.Player
import org.bukkit.event.Cancellable
import org.bukkit.event.HandlerList
import org.bukkit.event.player.PlayerEvent

class PlayerDetectStarshipEvent(player: Player) : PlayerEvent(player, true), Cancellable {
	private var cancelled: Boolean = false

	override fun getHandlers(): HandlerList = handlerList

	override fun isCancelled() = cancelled

	override fun setCancelled(cancelled: Boolean) {
		this.cancelled = cancelled
	}

	companion object {
		@JvmStatic
		val handlerList = HandlerList()
	}
}
