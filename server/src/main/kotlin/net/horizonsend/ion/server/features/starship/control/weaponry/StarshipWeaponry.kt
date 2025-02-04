package net.horizonsend.ion.server.features.starship.control.weaponry

import net.horizonsend.ion.server.IonServerComponent
import net.horizonsend.ion.server.command.admin.debug
import net.horizonsend.ion.server.command.admin.debugBanner
import net.horizonsend.ion.server.features.starship.active.ActiveStarship
import net.horizonsend.ion.server.features.starship.damager.Damager
import net.horizonsend.ion.server.features.starship.subsystem.weapon.StarshipWeapons
import net.horizonsend.ion.server.features.starship.subsystem.weapon.TurretWeaponSubsystem
import net.horizonsend.ion.server.features.starship.subsystem.weapon.WeaponSubsystem
import net.horizonsend.ion.server.features.starship.subsystem.weapon.interfaces.HeavyWeaponSubsystem
import net.horizonsend.ion.server.features.starship.subsystem.weapon.interfaces.ManualWeaponSubsystem
import net.horizonsend.ion.server.miscellaneous.utils.PerDamagerCooldown
import net.horizonsend.ion.server.miscellaneous.utils.isLava
import net.horizonsend.ion.server.miscellaneous.utils.isWater
import org.bukkit.Location
import org.bukkit.block.BlockFace
import org.bukkit.util.Vector
import java.util.LinkedList
import java.util.concurrent.ThreadLocalRandom
import java.util.concurrent.TimeUnit

object StarshipWeaponry : IonServerComponent() {
	val cooldown = PerDamagerCooldown(250L, TimeUnit.MILLISECONDS)
	val rightClickTimes = mutableMapOf<Damager, Long>()

	fun manualFire(
        shooter: Damager,
        starship: ActiveStarship,
        leftClick: Boolean,
        facing: BlockFace,
        dir: Vector,
        target: Vector,
        weaponSet: String?
	) {
		starship.debug("Common manual firing")

		val weapons = (if (weaponSet == null) starship.weapons else starship.weaponSets[weaponSet]).shuffled(ThreadLocalRandom.current())

		starship.debug("Weapons: ${weapons.joinToString { it.name }}")

		val fireTask = {
			val queuedShots = queueShots(shooter, weapons, leftClick, facing, dir, target)
			starship.debug("Queued shots: ${queuedShots.joinToString { it.weapon.name }}")
			StarshipWeapons.fireQueuedShots(queuedShots, starship)
		}

		if (!leftClick) cooldown.tryExec(shooter, fireTask) else fireTask()
	}

	fun getTarget(loc: Location, dir: Vector, starship: ActiveStarship): Vector {
		val world = loc.world
		var target: Vector = loc.toVector()
		val x = loc.blockX
		val y = loc.blockY
		val z = loc.blockZ
		for (i in 0 until 500) {
			val bx = (x + dir.x * i).toInt()
			val by = (y + dir.y * i).toInt()
			val bz = (z + dir.z * i).toInt()
			if (starship.contains(bx, by, bz)) {
				continue
			}
			if (!world.isChunkLoaded(bx shr 4, bz shr 4)) {
				continue
			}
			val type = world.getBlockAt(bx, by, bz).type
			target = Vector(bx + 0.5, by + 0.5, bz + 0.5)
			if (!type.isAir && !type.isWater && !type.isLava) {
				break
			}
			if (world.getNearbyLivingEntities(target.toLocation(world), 0.5).any { !starship.isWithinHitbox(it) }) {
				break
			}
		}
		return target
	}

	fun queueShots(
        shooter: Damager,
        weapons: List<WeaponSubsystem>,
        leftClick: Boolean,
        facing: BlockFace,
        dir: Vector,
        target: Vector
	): LinkedList<StarshipWeapons.ManualQueuedShot> {
		val queuedShots = LinkedList<StarshipWeapons.ManualQueuedShot>()

		shooter.debugBanner("Queuing shots")

		for (weapon: WeaponSubsystem in weapons) {
			shooter.debug("Weapon: ${weapon.name}")

			if (weapon !is ManualWeaponSubsystem) {
				shooter.debug("Continue, weapon cannot be manually fired.")
				continue
			}

			if (!weapon.isAcceptableDirection(facing)) {
				shooter.debug("Continue, weapon cannot fire in this direction.")
				continue
			}

			if (weapon is HeavyWeaponSubsystem != !leftClick) {
				shooter.debug("Continue, not correct click")
				continue
			}

			if (!weapon.isCooledDown()) {
				shooter.debug("Continue, weapon not cooled down")
				continue
			}

			if (!weapon.isIntact()) {
				shooter.debug("Continue, weapon not intact")
				continue
			}

			val targetedDir: Vector = weapon.getAdjustedDir(dir, target)

			if (weapon is TurretWeaponSubsystem && !weapon.ensureOriented(targetedDir)) {
				shooter.debug("Continue, turret not oriented properly")
				continue
			}

			if (!weapon.canFire(targetedDir, target)) {
				shooter.debug("Continue, weapon cannot fire.")
				continue
			}

			queuedShots.add(StarshipWeapons.ManualQueuedShot(weapon, shooter, targetedDir, target))
		}

		shooter.debugBanner("Queuing shots end")

		return queuedShots
	}
}
