package org.example.dev.aaronhowser.apps.knome.command

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData
import org.example.dev.aaronhowser.apps.knome.feature.CrosspostFeature

object CrosspostCommand {

	const val COMMAND_NAME = "crosspost"
	const val START_ARGUMENT = "start"
	const val END_ARGUMENT = "end"

	fun getCommand(): SlashCommandData {
		return Commands.slash(COMMAND_NAME, "Crosspost messages")
			.addOption(OptionType.STRING, "start", "First message ID", true)
			.addOption(OptionType.STRING, "end", "Last message ID", false)
	}

	fun handleCrosspost(event: SlashCommandInteractionEvent) {
		event.deferReply(true).complete()

		val startId = event.getOption(START_ARGUMENT)?.asLong

		if (startId == null) {
			event.hook.sendMessage("Start id is required.").queue()
			return
		}

		val endId = event.getOption(END_ARGUMENT)?.asLong ?: startId
		val channel = event.channel

		CrosspostFeature.crosspost(
			jda = event.jda,
			startId = startId,
			endId = endId,
			channel = channel
		)

		event.hook.sendMessage("Done!").queue()
	}

	data class MessageReference(val guildId: Long, val channelId: Long, val messageId: Long) {
		companion object {
			fun fromLink(link: String): MessageReference {
				val parts = link
					.removePrefix("https://discord.com/channels/")
					.split("/")

				return MessageReference(
					guildId = parts[0].toLong(),
					channelId = parts[1].toLong(),
					messageId = parts[2].toLong()
				)
			}
		}
	}

}