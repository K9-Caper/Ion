package net.horizonsend.ion.server.features.multiblock.generator

import org.bukkit.Material

object GeneratorMultiblockTier1 : GeneratorMultiblock("&8Tier 1", Material.IRON_BLOCK) {
	override val maxStoredValue = 100_000
	override val speed = 1.0
}
