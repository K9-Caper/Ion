package net.horizonsend.ion.server.features.transport.type

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import net.horizonsend.ion.server.IonServer
import net.horizonsend.ion.server.features.multiblock.Multiblocks
import net.horizonsend.ion.server.features.multiblock.PowerStoringMultiblock
import net.horizonsend.ion.server.features.multiblock.StoringMultiblock
import net.horizonsend.ion.server.features.multiblock.areashield.AreaShield
import net.horizonsend.ion.server.features.transport.IonMetricsCollection
import net.horizonsend.ion.server.features.transport.Transports
import net.horizonsend.ion.server.features.transport.transportConfig
import net.horizonsend.ion.server.miscellaneous.registrations.NamespacedKeys
import net.horizonsend.ion.server.miscellaneous.utils.Tasks
import net.horizonsend.ion.server.miscellaneous.utils.Vec3i
import net.horizonsend.ion.server.miscellaneous.utils.debugHighlightBlock
import net.horizonsend.ion.server.miscellaneous.utils.getBlockDataSafe
import net.horizonsend.ion.server.miscellaneous.utils.getBlockTypeSafe
import net.horizonsend.ion.server.miscellaneous.utils.getStateIfLoaded
import net.horizonsend.ion.server.miscellaneous.utils.orNull
import net.horizonsend.ion.server.miscellaneous.utils.randomEntry
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextColor
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.World
import org.bukkit.block.BlockFace
import org.bukkit.block.Sign
import org.bukkit.block.data.BlockData
import org.bukkit.persistence.PersistentDataType
import java.util.Optional
import java.util.concurrent.ThreadLocalRandom
import java.util.concurrent.TimeUnit
import kotlin.math.min
import kotlin.system.measureNanoTime

abstract class TransportType<T : StoringMultiblock> {
	abstract val namespacedKey: NamespacedKey
	abstract val transportBlocks: Set<Material>
	abstract val inputBlock: Material
	abstract val extractionBlock: Material

	/** The prefix of the stored value on the sign */
	abstract val prefixComponent: Component

	/** The color of the stored value on the sign */
	abstract val textColor: TextColor

	/** Controls which line of the sign to put the display text */
	abstract val storedLine: Int

	/** Controls if the value on the destination should be set to zero if none is present */
	abstract val setIfEmpty: Boolean

	/** Offsets of signs relative to import computers */
	abstract val offsets: Set<Vec3i>

	private val storageSignUpdateCache = CacheBuilder.newBuilder()
		.build<Sign, Int>(
			CacheLoader.from { sign ->
				checkNotNull(sign)
				getStoredValue(sign, fast = true)
			}
		)

	data class CachedMultiblockStore<T : StoringMultiblock>(val multiblock: T, val sign: Sign)

	private val multiblockCache = CacheBuilder.newBuilder()
		.expireAfterWrite(5, TimeUnit.SECONDS)
		.build<Location, Optional<CachedMultiblockStore<T>>>(
			CacheLoader.from { loc ->
				checkNotNull(loc)
				for ((x, y, z) in offsets) {
					val state = getStateIfLoaded(loc.world, loc.blockX + x, loc.blockY + y, loc.blockZ + z)
					val sign = state as? Sign ?: continue

					@Suppress("UNCHECKED_CAST") // No clue why its warning this
					val multiblock = Multiblocks[sign, true, false] as? T ?: continue

					return@from Optional.of(CachedMultiblockStore(multiblock, sign))
				}
				return@from Optional.empty()
			}
		)

	private fun setCachedStorageValue(sign: Sign, value: Int) = storageSignUpdateCache.put(sign, value)

	abstract fun canTransfer(originType: Material, isDirectional: Boolean, face: BlockFace, data: BlockData): Boolean

	open fun pickDirection(isDirectional: Boolean, adjacentWires: Set<BlockFace>, direction: BlockFace): BlockFace {
		return when {
			isDirectional && adjacentWires.contains(direction) -> direction
			else -> adjacentWires.randomEntry()
		}
	}

	/**
	 * Should only be called in the update queue up top.
	 *
	 * @param world The world of the wire chain
	 * @param x The X-coordinate of the current spot in the wire chain
	 * @param y The Y-coordinate of the current spot in the wire chain
	 * @param z The Z-coordinate of the current spot in the wire chain
	 * @param isDirectional Whether the current wire block type is a directional connector
	 */
	private fun checkComputers(
		world: World,
		x: Int,
		y: Int,
		z: Int,
		isDirectional: Boolean,
		direction: BlockFace,
		computers: Set<BlockFace>,
		wires: Set<BlockFace>,
		originComputer: Vec3i?,
		distance: Int
	) {
		val validComputers = computers.asSequence()
			.mapNotNull { getStateIfLoaded(world, x + it.modX, y + it.modY, z + it.modZ) }
			.filter { it.type == inputBlock }
			.toList().shuffled(ThreadLocalRandom.current())

		if (validComputers.isNotEmpty()) {
			val originSign: Sign? = when {
				// if there's an origin computer, find its power machine, if it's not findable, end the chain
				originComputer != null -> multiblockCache[originComputer.toLocation(world)]
					.orNull()?.sign
					?: return

				else -> null
			}

			// It does not have any of this transport's stored value
			if (originSign?.persistentDataContainer?.keys?.contains(this.namespacedKey) == false) return

			var originPower = when {
				originSign != null -> storageSignUpdateCache[originSign]
				else -> transportConfig.wires.solarPanelPower /
					if (world.environment == World.Environment.NORMAL) 1 else 2
			}

			// if it has no power then there is nothing to extract from it anymore
			if (originPower <= 0) {
				return
			}

			computerLoop@
			for (destination in validComputers) {
				val (destinationMultiblock, destinationSign) = multiblockCache[destination.location]
					.orNull() ?: continue@computerLoop

				// ensure we're not returning power to the same computer
				if (destinationSign.location == originSign?.location) {
					continue@computerLoop
				}

				// Check the destination multiblock's free space, or null if it can't be transferred to this multiblock
				val freeSpace = destinationMultiblock.canTake(this, storageSignUpdateCache, destinationSign) ?: continue@computerLoop
				val destinationPower = storageSignUpdateCache[destinationSign]

				val transferLimit = when (destinationMultiblock) {
					is AreaShield -> transportConfig.wires.maxShieldInput
					else -> transportConfig.wires.maxPowerInput
				}

				// Amount that can be transferred is capped by the free space, the transfer limit, and the available amount
				val amount: Int = min(transferLimit, min(originPower, freeSpace))

				// Update destination power
				setCachedStorageValue(destinationSign, destinationPower + amount)

				originPower -= amount

				// Remove from origin
				if (originSign != null) {
					setCachedStorageValue(originSign, originPower)
				}

				// Try the other computers if theres still power to go around
				if (originPower <= 0) {
					return // no more power to extract
				}
			}
		}

		// double check the wires to make sure they can still be pushed into
		val validWires = wires.filter {
			val data = getBlockDataSafe(world, x + it.modX, y + it.modY, z + it.modZ)
				?: return@filter false

			return@filter canTransfer(data.material, isDirectional, direction, data)
		}.toSet()

		if (validWires.isEmpty()) return // end the chain if there's no more valid wires

		val newDirection = pickDirection(isDirectional, validWires, direction)

		// Iterate again if theres more pipes in the chain
		Transports.thread.submit {
			step(world, x, y, z, newDirection, originComputer, distance + 1)
		}
	}

	/**
	 * Power updates can be slow. In fact, they're the only sync part of wires, and by far the most CPU-intensive.
	 * So, they're batched in a queue down below. It's easier to explain how it works with code,
	 * so just look at this file to get a feel for it.
	 *
	 * `powerUpdateRate` is the rate in ticks of the updates being batched.
	 * Higher values mean more updates being batched together and potentially better usage of caching.
	 * However, it also means larger delay, and could affect user usage.
	 */
	fun scheduleUpdates() {
		val interval = transportConfig.wires.powerUpdateRate
		Tasks.syncRepeat(delay = interval, interval = interval) {
			val start = System.nanoTime()

			val maxTime = TimeUnit.MILLISECONDS.toNanos(transportConfig.wires.powerUpdateMaxTime)

			while (!Transports.computerCheckQueue.isEmpty() && System.nanoTime() - start < maxTime) {
				val time = measureNanoTime { Transports.computerCheckQueue.poll().invoke() }

				IonMetricsCollection.timeSpent += time
			}

			if (System.nanoTime() - start > maxTime) {
				IonServer.slF4JLogger.warn("Power update took too long!")
			}

			for ((sign, power) in storageSignUpdateCache.asMap()) {
				setStoredValue(sign, power, fast = true)
			}

			storageSignUpdateCache.invalidateAll()
		}
	}

	fun startChain(world: World, x: Int, y: Int, z: Int, direction: BlockFace, computer: Vec3i?) {
		Transports.thread.submit {
			step(world, x, y, z, direction, computer, 0)
		}
	}

	abstract fun checkStep(direction: BlockFace, nextType: Material): Set<BlockFace>

	abstract fun isDirectional(isDirectional: Material): Boolean

	private fun step(world: World, x: Int, y: Int, z: Int, direction: BlockFace, computer: Vec3i?, distance: Int) {
		if (distance > transportConfig.wires.maxDistance) {
			return
		}

		val originMaterial = getBlockTypeSafe(world, x, y, z) ?: return

		val nextX = x + direction.modX
		val nextY = y + direction.modY
		val nextZ = z + direction.modZ

		debugHighlightBlock(x, y, z)
		debugHighlightBlock(nextX, nextY, nextZ)

		val nextType = getBlockTypeSafe(world, nextX, nextY, nextZ) ?: return

		val reverse = direction.oppositeFace // used for ensuring we're not going backwards when dealing w/ connectors

		val checkDirections = checkStep(direction, nextType)
		if (checkDirections.isEmpty()) return

		// directional wires go forward if possible, and don't go into sponges
		val isDirectional = isDirectional(nextType)

		val adjacentComputers = mutableSetOf<BlockFace>()
		val adjacentWires = mutableSetOf<BlockFace>()

		adjacentLoop@
		for (face: BlockFace in checkDirections) {
			if (face == reverse) continue

			val adjacentX = nextX + face.modX
			val adjacentY = nextY + face.modY
			val adjacentZ = nextZ + face.modZ

			debugHighlightBlock(adjacentX, adjacentY, adjacentZ)

			val data = getBlockDataSafe(world, adjacentX, adjacentY, adjacentZ) ?: continue

			if (data.material == inputBlock) {
				adjacentComputers.add(face)
			} else if (canTransfer(originMaterial, isDirectional, face, data)) {
				adjacentWires.add(face)
			}
		}

		// continue if there are no computers requiring main thread checks
		if (adjacentComputers.isEmpty()) {
			if (adjacentWires.isNotEmpty()) {
				val adjacentPipeDirection = pickDirection(isDirectional, adjacentWires, direction)
				step(world, nextX, nextY, nextZ, adjacentPipeDirection, computer, distance + 1)
			}
			return
		}

		// check computers on the main thread as their signs need to be accessed,
		// and tile entities aren't as easy to access in a thread-safe manner.
		// put it on the queue, see the top of the file for how that works.
		Transports.computerCheckQueue.offer {
			checkComputers(
				world = world,
				x = nextX,
				y = nextY,
				z = nextZ,
				isDirectional = isDirectional,
				direction = direction,
				computers = adjacentComputers,
				wires = adjacentWires,
				originComputer = computer,
				distance = distance + 1
			)
		}
	}

	@JvmOverloads
	fun getStoredValue(sign: Sign, fast: Boolean = true): Int {
		if (!fast && Multiblocks[sign] !is PowerStoringMultiblock) {
			return 0
		}

		return sign.persistentDataContainer.get(namespacedKey, PersistentDataType.INTEGER)
			?: if (setIfEmpty) return setStoredValue(sign, 0) else return 0
	}

	@JvmOverloads
	open fun setStoredValue(sign: Sign, value: Int, fast: Boolean = true): Int {
		val correctedValue: Int = if (!fast) {
			@Suppress("UNCHECKED_CAST") // No clue why its warning this
			val multiblock = (Multiblocks[sign] ?: return 0) as? T ?: return 0
			value.coerceIn(0, multiblock.maxStoredValue)
		} else {
			value.coerceAtLeast(0)
		}

		if (!sign.persistentDataContainer.has(NamespacedKeys.MULTIBLOCK)) return value

		if (value == 0) {
			sign.persistentDataContainer.remove(this.namespacedKey)
			sign.line(storedLine, Component.empty())
			sign.update()

			return 0
		}

		sign.persistentDataContainer.set(namespacedKey, PersistentDataType.INTEGER, correctedValue)

		sign.line(storedLine, Component.text().append(prefixComponent, Component.text(correctedValue, textColor)).build())
		sign.update(false, false)

		return value
	}

	fun addValue(sign: Sign, amount: Int) {
		setStoredValue(sign, getStoredValue(sign) + amount)
	}

	fun removeValue(sign: Sign, amount: Int) {
		setStoredValue(sign, getStoredValue(sign) - amount, true)
	}
}
