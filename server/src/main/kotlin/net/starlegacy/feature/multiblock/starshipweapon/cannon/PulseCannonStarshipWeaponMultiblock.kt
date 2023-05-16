package net.starlegacy.feature.multiblock.starshipweapon.cannon

import net.starlegacy.feature.multiblock.MultiblockShape
import net.starlegacy.feature.multiblock.starshipweapon.SignlessStarshipWeaponMultiblock
import net.starlegacy.feature.starship.active.ActiveStarship
import net.starlegacy.feature.starship.subsystem.weapon.primary.PulseCannonWeaponSubsystem
import net.horizonsend.ion.server.miscellaneous.Vec3i
import org.bukkit.block.BlockFace

object PulseCannonStarshipWeaponMultiblock : SignlessStarshipWeaponMultiblock<PulseCannonWeaponSubsystem>() {
	override fun createSubsystem(starship: ActiveStarship, pos: Vec3i, face: BlockFace): PulseCannonWeaponSubsystem {
		return PulseCannonWeaponSubsystem(starship, pos, face)
	}

	override fun MultiblockShape.buildStructure() {
		at(+0, +0, +0).sponge()
		at(+0, +0, +1).stainedGlass()
	}
}
