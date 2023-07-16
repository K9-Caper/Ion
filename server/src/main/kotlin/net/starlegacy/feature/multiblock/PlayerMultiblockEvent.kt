package net.starlegacy.feature.multiblock

import org.bukkit.entity.Player

abstract class PlayerMultiblockEvent<T: Multiblock>(
	override val multiblock: T,
	open val player: Player
) : MultiblockEvent<T>(multiblock) {
}
