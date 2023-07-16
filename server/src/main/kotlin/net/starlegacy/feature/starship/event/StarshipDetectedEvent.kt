package net.starlegacy.feature.starship.event

import net.horizonsend.ion.common.database.Oid
import net.horizonsend.ion.common.database.schema.starships.PlayerStarshipData
import org.bukkit.World
import org.bukkit.entity.Player
import org.bukkit.event.Cancellable
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

class StarshipDetectedEvent(
	val world: World,
	val dataId: Oid<PlayerStarshipData>
) : Event(), Cancellable {
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
