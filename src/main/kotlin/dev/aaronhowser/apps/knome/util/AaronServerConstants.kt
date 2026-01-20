package dev.aaronhowser.apps.knome.util

import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel

object AaronServerConstants {

	const val MODLOG_CHANNEL_ID = 1263962403238973532
	fun getModlog(jda: JDA): TextChannel = jda.getTextChannelById(MODLOG_CHANNEL_ID) ?: error("Modlog channel not found")

	const val ARIEL_MEMBER_ID = 1396780485581209684

}