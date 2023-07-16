package net.starlegacy.feature.multiblock

import org.bukkit.event.Event

abstract class MultiblockEvent<T: Multiblock>(
	open val multiblock: T
) : Event()

