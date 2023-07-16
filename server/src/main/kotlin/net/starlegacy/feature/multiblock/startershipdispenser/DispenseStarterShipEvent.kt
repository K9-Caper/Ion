package net.starlegacy.feature.multiblock.startershipdispenser

import net.starlegacy.feature.multiblock.PlayerMultiblockEvent
import org.bukkit.block.Sign
import org.bukkit.entity.Player
import org.bukkit.event.HandlerList

class DispenseStarterShipEvent(
	val sign: Sign,
	override val player: Player
) : PlayerMultiblockEvent<StarterShipDispenserMultiblock>(StarterShipDispenserMultiblock, player) {
	override fun getHandlers(): HandlerList {
		return handlerList
	}

	companion object {
		@JvmStatic
		val handlerList = HandlerList()
	}
}
