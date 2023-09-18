package net.horizonsend.ion.server.features.multiblock.charger

import org.bukkit.ChatColor
import org.bukkit.Material

object ChargerMultiblockTier3 : ChargerMultiblock(ChatColor.AQUA.toString() + "Tier 3") {
	override val tierMaterial = Material.DIAMOND_BLOCK
	override val maxStoredValue = 300_000
	override val powerPerSecond = 3000
}
