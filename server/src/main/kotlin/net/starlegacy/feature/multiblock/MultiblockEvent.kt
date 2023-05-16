package net.starlegacy.feature.multiblock

import org.bukkit.Location
import org.bukkit.event.Event

abstract class MultiblockEvent<T: Multiblock>(
	open val multiblock: T
) : Event() {
	abstract val location: Location
}
