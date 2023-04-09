@file:Suppress("DeprecatedCallableAddReplaceWith")
package net.horizonsend.ion.server

import com.destroystokyo.paper.event.server.ServerTickStartEvent
import kotlin.DeprecationLevel.ERROR
import net.horizonsend.ion.server.explosions.ExplosionReversal
import net.horizonsend.ion.server.features.starship.Starship
import net.horizonsend.ion.server.miscellaneous.mainThreadCheck
import net.horizonsend.ion.server.miscellaneous.minecraft
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.chunk.LevelChunk
import net.starlegacy.feature.machine.AreaShields
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.world.ChunkLoadEvent
import org.bukkit.event.world.ChunkUnloadEvent
import org.bukkit.event.world.WorldInitEvent
import org.bukkit.event.world.WorldUnloadEvent

class IonWorld private constructor(val serverLevel: ServerLevel) {
	val starships = mutableListOf<Starship>()

	fun tick() = mainThreadCheck {
		for (starship in starships) {
			val result = runCatching(starship::tick).exceptionOrNull() ?: continue
			IonServer.slF4JLogger.error("Exception while ticking Starship!", result)
		}

		for (chunk in ionChunks.values) {
			val result = runCatching(chunk::tick).exceptionOrNull() ?: continue
			IonServer.slF4JLogger.error("Exception while ticking IonChunk", result)
		}
	}

	fun destroy() = mainThreadCheck {
		ionChunks.values.forEach(IonChunk::destroy)
	}

	private val ionChunks = mutableMapOf<LevelChunk, IonChunk>()

	operator fun get(levelChunk: LevelChunk) = ionChunks[levelChunk]!!

	fun register(levelChunk: LevelChunk) = mainThreadCheck {
		ionChunks[levelChunk] = IonChunk(levelChunk)
	}

	fun unregister(levelChunk: LevelChunk) = mainThreadCheck {
		ionChunks.remove(levelChunk)?.destroy()
	}

	inner class IonChunk(
		val levelChunk: LevelChunk
	) {
		var lastExplodeTick: Int? = null
		val explosions = mutableListOf<ExplosionReversal.Explosion>()

		fun tick() = mainThreadCheck {
			if (lastExplodeTick == null) return@mainThreadCheck

			// TODO: Make this code check neighbouring chunks to see if they're not ready to regenerate before regenerating itself - Sciath
			if (MinecraftServer.currentTick-lastExplodeTick!! > IonServer.configuration.explosionReversalTicks) return@mainThreadCheck

			// Check if we can regenerate these blocks, if we cant regenerate one of them, dont regenerate any
			for (explosion in explosions) if (!explosion.canRegenerate(levelChunk.level)) return@mainThreadCheck

			for (explosion in explosions) {
				serverLevel.setBlock(explosion.pos, explosion.blockState, 0)
				serverLevel.setBlockEntity(BlockEntity.loadStatic(explosion.pos, explosion.blockState, explosion.tag ?: continue) ?: continue)
			}

			explosions.clear()
			lastExplodeTick = null
		}

		fun destroy() = mainThreadCheck {}
	}

	companion object : Listener {
		private val ionWorlds = mutableMapOf<ServerLevel, IonWorld>()

		operator fun get(serverLevel: ServerLevel): IonWorld = ionWorlds[serverLevel]!!

		fun register(serverLevel: ServerLevel) = mainThreadCheck {
			mainThreadCheck()
			ionWorlds[serverLevel] = IonWorld(serverLevel)

			AreaShields.loadData() // TODO: Remove this mess - Aury @ Astralchroma
		}

		fun unregister(serverLevel: ServerLevel) = mainThreadCheck {
			ionWorlds.remove(serverLevel)?.destroy()
		}

		fun unregisterAll() = mainThreadCheck {
			while (ionWorlds.isNotEmpty()) unregister(ionWorlds.keys.first())
		}

		@Deprecated("Event Listener", level = ERROR)
		@EventHandler
		fun onWorldInitEvent(event: WorldInitEvent) = mainThreadCheck {
			register(event.world.minecraft)
		}

		@Deprecated("Event Listener", level = ERROR)
		@EventHandler
		fun onWorldUnloadEvent(event: WorldUnloadEvent) = mainThreadCheck {
			unregister(event.world.minecraft)
		}

		@Deprecated("Event Listener", level = ERROR)
		@EventHandler
		fun onChunkLoadEvent(event: ChunkLoadEvent) = mainThreadCheck {
			IonWorld[event.world.minecraft].register(event.chunk.minecraft)
		}

		@Deprecated("Event Listener", level = ERROR)
		@EventHandler
		fun onChunkUnloadEvent(event: ChunkUnloadEvent) = mainThreadCheck {
			IonWorld[event.world.minecraft].unregister(event.chunk.minecraft)
		}

		@Deprecated("Event Listener", level = ERROR)
		@EventHandler
		fun onServerTickStartEvent(@Suppress("UNUSED_PARAMETER") event: ServerTickStartEvent) = mainThreadCheck {
			for (ionWorld in ionWorlds.values) {
				val result = runCatching(ionWorld::tick).exceptionOrNull() ?: continue
				IonServer.slF4JLogger.error("Exception while ticking IonWorld!", result)
			}
		}
	}
}
