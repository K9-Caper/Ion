package net.horizonsend.ion.server.features.multiblock.powerfurnace

import org.bukkit.Material

object PowerFurnaceMultiblockTier3 : PowerFurnaceMultiblock("&bTier 3") {
	override val tierMaterial = Material.DIAMOND_BLOCK
	override val maxStored = 75_000
	override val burnTime = 400
}
