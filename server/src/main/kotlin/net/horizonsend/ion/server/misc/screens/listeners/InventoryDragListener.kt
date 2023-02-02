package net.horizonsend.ion.server.misc.screens.listeners

import net.horizonsend.ion.server.misc.screens.ScreenManager.isInScreen
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryDragEvent

class InventoryDragListener : Listener {
	@EventHandler
	@Suppress("Unused")
	fun onInventoryDragEvent(event: InventoryDragEvent) {
		if ((event.whoClicked as? Player)?.isInScreen == true) event.isCancelled = true
	}
}
