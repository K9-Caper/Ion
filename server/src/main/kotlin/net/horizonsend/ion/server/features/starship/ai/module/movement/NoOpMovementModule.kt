package net.horizonsend.ion.server.features.starship.ai.module.movement

import net.horizonsend.ion.server.features.starship.ai.module.pathfinding.AStarPathfindingModule
import net.horizonsend.ion.server.features.starship.control.controllers.ai.AIController

class NoOpMovementModule(controller: AIController, pathfindingModule: AStarPathfindingModule) : MovementModule(controller, pathfindingModule) {
	override fun tick() {}
}
