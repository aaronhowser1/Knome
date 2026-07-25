package dev.aaronhowser.apps.knome.discord

import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel

object AaronServer {

	const val MODLOG_CHANNEL_ID = 1263962403238973532
	const val PHILOSOPHY_CROSSPOSTS_CHANNEL_ID = 1530588468449906789
	const val PHILOSOPHY_CHANNEL_ID = 1406029327421804655
	const val OFF_TOPIC_CHANNEL_ID = 1264017551806169130
	const val BOTS_CHANNEL_ID = 1405759176222965780

	fun getModlog(jda: JDA): TextChannel = jda.getTextChannelById(MODLOG_CHANNEL_ID) ?: error("Modlog channel not found")
	fun getPhilosophyCrossposts(jda: JDA): TextChannel = jda.getTextChannelById(PHILOSOPHY_CROSSPOSTS_CHANNEL_ID)
		?: error("Philosophy crossposts channel not found")

	const val MOD_UPDATES_GROUP_ID = 1323641112220532778
	const val SERVER_GROUP_ID = 1263959668997357571

	fun channelIsInGroup(guild: Guild, channelId: Long, groupId: Long): Boolean {
		return guild.getTextChannelById(channelId)?.parentCategoryIdLong == groupId
	}

	const val ARIEL_MEMBER_ID = 1396780485581209684
	const val AARON_MEMBER_ID = 116353741162151939

}