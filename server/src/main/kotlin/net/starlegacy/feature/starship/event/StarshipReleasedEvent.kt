package net.starlegacy.feature.starship.event

import net.horizonsend.ion.common.database.Oid
import net.horizonsend.ion.common.database.schema.starships.PlayerStarshipData
import net.horizonsend.ion.server.features.starship.controllers.Controller
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

class StarshipReleasedEvent(
	val controller: Controller?,
	val data: Oid<PlayerStarshipData>
) : Event() {
	override fun getHandlers(): HandlerList {
		return handlerList
	}

	companion object {
		@JvmStatic
		val handlerList = HandlerList()
	}
}
