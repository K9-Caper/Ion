package net.horizonsend.ion.proxy.managers

import net.horizonsend.ion.common.IonComponent
import net.horizonsend.ion.proxy.PLUGIN
import java.util.concurrent.TimeUnit
import org.bukkit.Bukkit
import org.bukkit.*
import java.util.*


class GalacticBroadcastPlugin : IonComponent() {

    private val messages = listOf(
        "Low on chetherite? Get your votes in, today! /vote",
        "Be sure to visit Prometheus Station to be able to buy or sell up to 30,000 C worth of crops per day.",
        "Bed not working? Just got blasted out of oblivion? Use /kit starter to build a cryopod.",
	"Commissions and cargo trading are great ways for new players to make money. Feeling confident? Try your hand at /bounty.",
	"Need an item? Use /bazaar browse, although be aware: the prices are quadrupled if you're buying remotely!",
        "24 siegeable stations exist in space. Only nations can siege stations, and owning between 1-6 stations reaps benefits for all nation members.",
        "Just starting out? Try visiting the planets of Luxiterna or Aret. Both have resources valuable for new players.",
	"Level 60 is currently the highest level required to unlock the biggest avaialable ship class: Destroyers / Heavy Friegthers (12,000 blocks).",
	"Want to pick a fight with NPCs? It's recommended you take a gunship, at least.",
	"Looking for a settlement or nation to join? Be sure to ask in global chat using /g. There are always players recruiting.",
	"Setting up machinery? Solar power and Gas generators are clean and reliable. Do /wiki to learn more.",
	"Don't forget to enable your ship lock when parking in territory that's not your own. The lock is toggleable in the ship computer (jukebox).",
	"Use /starship when piloting a ship to get a diagnostic overview of your vessel. This is quite useful for shipbuilding and repairs.",
	"Unsure who someone is? Use /pinfo [player]."

        // expand list here
    )

    private var currentIndex = 0

    override fun onEnable() {
        scheduleGalacticBroadcast()
    }

    private fun scheduleGalacticBroadcast() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, {
            broadcastNextMessage()
        }, 0, 20 * 60 * 30) // 20 ticks per second, 60 seconds per minute, 30 minutes per broadcast
    }

    private fun broadcastNextMessage() {
	  val nextMessage = messages[currentIndex]
          val prefix = Component.text("[Galactic Broadcast]", TextColor.fromHexString("#752082"))
          PLUGIN.proxy.sendMessage(ofChildren(prefix, text(nextMessage, TextColor.fromHexString("#DBBCE0"))))
	  
// old wrong code below	
//
//        PLUGIN.proxy.sendMessage(Component.text("<#752082> [Galactic Broadcast] <#DBBCE0> nextMessage"))

        currentIndex = (currentIndex + 1) % messages.size
	    
// broadcastnextmessage might need fine tuning
	    
    }
}


// below is glutin code
//
//object ReminderManager : IonComponent() {
//	private val scheduledMessages = listOf<Runnable>(
//		Runnable { voteReminder() }
//	)
//
//	private const val delay: Long = 900 // Might need to be increased if more messages are added (Currently 15 minutes)
//
//	override fun onEnable() {
//		scheduleReminders()
//	}
//
//	private fun scheduleReminders() {
//		 for (message in scheduledMessages) {
//		 	PLUGIN.proxy.scheduler.repeat(
//				delay,
//		 		(delay / scheduledMessages.size.toLong()) * scheduledMessages.indexOf(message),
//		 		TimeUnit.SECONDS,
//				message
//		 	)
//		 }
//	}
// Might bring this back eventually
//	private fun voteReminder() = transaction {
//		for (player in PLUGIN.proxy.players) {
//			val playerData = PlayerData[player.uniqueId] ?: continue
//			val shouldPrompt: Boolean = playerData.voteTimes.find { it.dateTime.isBefore(LocalDateTime.now().minusDays(1)) } != null
//			if (shouldPrompt) player.special("Please vote for the server to help grow the community! <green><click:run_command:/vote>Run /vote to see where!")
//		}
//	}
}
