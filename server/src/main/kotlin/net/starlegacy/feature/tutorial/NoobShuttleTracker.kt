package net.starlegacy.feature.tutorial

import it.unimi.dsi.fastutil.longs.LongOpenHashSet
import net.horizonsend.ion.common.database.schema.starships.PlayerStarshipData
import net.horizonsend.ion.server.IonServerComponent
import net.horizonsend.ion.server.legacy.NewPlayerProtection.hasProtection
import net.horizonsend.ion.server.miscellaneous.bukkitWorld
import net.starlegacy.feature.starship.DeactivatedPlayerStarships
import net.starlegacy.feature.starship.PlayerStarshipState
import net.starlegacy.feature.starship.StarshipDestruction
import net.starlegacy.feature.starship.event.StarshipDetectedEvent
import net.starlegacy.feature.starship.event.StarshipPilotedEvent
import net.starlegacy.feature.starship.event.StarshipUnpilotedEvent
import org.bukkit.Statistic
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.player.PlayerQuitEvent
import java.util.UUID

/**
 * This tracks the ships of new players who have played for less than 30 minutes.
 * If they quit for longer than an hour, it deletes their ship.
 **/
object NoobShuttleTracker : IonServerComponent() {
	val trackedShips = mutableListOf<AbandonedNoobShuttle>()

	/** If the noob has logged out,  **/
	val toRemove = mutableMapOf<AbandonedNoobShuttle, Long>()

	data class AbandonedNoobShuttle(
		val player: UUID,
		val state: PlayerStarshipState,
		val data: PlayerStarshipData,
	) {
		fun destroy() {
			StarshipDestruction.vanishShipBlocks(LongOpenHashSet(state.blockMap.keys), data.bukkitWorld())

			DeactivatedPlayerStarships.destroyAsync(data)
		}
	}

	@EventHandler
	fun onShipPilot(event: StarshipPilotedEvent) {
		trackedShips.removeAll { it.data._id == event.starship.dataId }
	}

	@EventHandler
	fun onStarshipUnpilot(event: StarshipUnpilotedEvent) {
		if (!isNoob(event.player)) return


	}

	@EventHandler
	fun onPlayerLogout(event: PlayerQuitEvent) {

	}

	@EventHandler
	fun onRedetect(event: StarshipDetectedEvent) { // If they've modified the ship, don't remove it
		val ship = trackedShips.firstOrNull { it.data._id == event.dataId } ?: return

		trackedShips.remove(ship)
	}

	/** The players most likely to immediately quit **/
	fun isNoob(player: Player): Boolean {
		if (!player.hasProtection()) return false
		return player.getStatistic(Statistic.PLAY_ONE_MINUTE) < 20L * 60L * 30L // 30 minutes
	}

	override fun onDisable() {
		for (ship in trackedShips) {
			if (ship.data.lastUsed >= System.currentTimeMillis() - 1000L * 60L * 60L) continue // 1000 millis, 60 seconds, 60 minutes

			ship.destroy()
		}
	}
}
