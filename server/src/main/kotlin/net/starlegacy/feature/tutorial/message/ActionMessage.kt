package net.starlegacy.feature.tutorial.message

import net.kyori.adventure.text.Component
import org.bukkit.entity.Player

class ActionMessage(title: Component, subtitle: Component, private val action: (Player) -> Unit) :
	PopupMessage(title, subtitle) {
	override fun show(player: Player) {
		super.show(player)
		action(player)
	}
}
