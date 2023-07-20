package net.starlegacy.feature.tutorial.message

import org.bukkit.entity.Player

class Action(val delay: Double = 0.0, private val action: (Player) -> Unit) : TutorialMessage(delay) {
	override fun show(player: Player) {
		action(player)
	}
}
