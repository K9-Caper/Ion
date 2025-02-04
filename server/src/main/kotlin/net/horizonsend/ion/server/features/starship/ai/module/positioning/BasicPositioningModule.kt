package net.horizonsend.ion.server.features.starship.ai.module.positioning

import net.horizonsend.ion.server.features.starship.control.controllers.ai.AIController
import net.horizonsend.ion.server.miscellaneous.utils.Vec3i
import org.bukkit.Location

class BasicPositioningModule(controller: AIController, val destination: Location) : PositioningModule(controller) {
	val vec3i = Vec3i(destination)

	override fun findPosition(): Location = destination
	override fun findPositionVec3i(): Vec3i = vec3i

	override fun getDestination(): Vec3i = findPositionVec3i()
}
