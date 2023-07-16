package net.starlegacy.feature.tutorial.message

import io.papermc.paper.util.Tick
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.Component.text
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.title.Title
import org.bukkit.entity.Player
import java.time.Duration

open class PopupMessage(private val title: Component = text(""), private val subtitle: Component = text("")) :
	TutorialMessage("$title $subtitle".split(" ").count().toDouble() * 0.25 + 0.25) {
	override fun show(player: Player) {
		player.showTitle(
			Title.title(
				title,
				subtitle,
				Title.Times.times(
					Duration.of(10, Tick.tick()),
					Duration.of(Int.MAX_VALUE - 20L, Tick.tick()),
					Duration.of(0,  Tick.tick()))
			)
		)

		player.sendMessage(
			text()
				.append(title)
				.append(text(" >> ", NamedTextColor.GRAY))
				.append(subtitle)
		)
	}
}
