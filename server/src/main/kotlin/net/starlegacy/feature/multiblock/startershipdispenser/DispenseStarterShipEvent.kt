package net.starlegacy.feature.multiblock.startershipdispenser

import net.starlegacy.feature.multiblock.PlayerMultiblockEvent
import net.starlegacy.feature.starship.event.StarshipUnpilotEvent
import org.bukkit.block.Sign
import org.bukkit.entity.Player
import org.bukkit.event.HandlerList

class DispenseStarterShipEvent(
	val sign: Sign,
	override val player: Player
) : PlayerMultiblockEvent<StarterShipDispenserMultiblock>(StarterShipDispenserMultiblock, player) {
	override fun getHandlers(): HandlerList {
		return StarshipUnpilotEvent.handlerList
	}

	companion object {
		@JvmStatic
		val handlerList = HandlerList()
	}
}
