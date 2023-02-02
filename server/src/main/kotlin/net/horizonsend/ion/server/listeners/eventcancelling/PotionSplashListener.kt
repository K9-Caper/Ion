package net.horizonsend.ion.server.listeners.eventcancelling

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.PotionSplashEvent

class PotionSplashListener : Listener {
	@EventHandler
	@Suppress("Unused")
	fun onPotionSplashEvent(event: PotionSplashEvent) {
		event.isCancelled = true
	}
}
