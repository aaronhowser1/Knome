package dev.aaronhowser.apps.knome.crosspost

import dev.aaronhowser.apps.knome.discord.AaronServer
import dev.aaronhowser.apps.knome.discord.await
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.components.label.Label
import net.dv8tion.jda.api.components.selections.StringSelectMenu
import net.dv8tion.jda.api.components.textinput.TextInput
import net.dv8tion.jda.api.components.textinput.TextInputStyle
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.Interaction
import net.dv8tion.jda.api.interactions.InteractionHook
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.CommandData
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData
import net.dv8tion.jda.api.modals.Modal

object CrosspostCommand {

	const val COMMAND_NAME = "crosspost"
	const val MESSAGE_COMMAND_NAME = "Crosspost"
	private const val START_ARGUMENT = "start"
	private const val END_ARGUMENT = "end"
	private const val DESTINATION_ARGUMENT = "destination"
	private const val PARENT_ARGUMENT = "parent"
	private const val MODAL_PREFIX = "crosspost-range:"
	private const val MODAL_START_ID = "start-message"
	private const val MODAL_END_ID = "end-message"
	private const val DESTINATION_ID = "destination"
	private const val PARENT_MESSAGE_ID = "parent-crosspost"

	fun getCommand(): SlashCommandData {
		val destinationOption = OptionData(OptionType.STRING, DESTINATION_ARGUMENT, "Where to publish", true)
			.addChoice("Publish both", "both")
			.addChoice("Tumblr only", "tumblr")
			.addChoice("Bluesky only", "bluesky")

		return Commands.slash(COMMAND_NAME, "Publish messages from #philosophy")
			.addOption(OptionType.STRING, START_ARGUMENT, "First message link or ID", true)
			.addOptions(destinationOption)
			.addOption(OptionType.STRING, END_ARGUMENT, "Last message link or ID", false)
			.addOption(OptionType.STRING, PARENT_ARGUMENT, "Prior Knome crosspost message link to reply to", false)
			.setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR))
	}

	fun getMessageCommand(): CommandData {
		return Commands.message(MESSAGE_COMMAND_NAME)
			.setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR))
	}

	suspend fun handleCrosspost(event: SlashCommandInteractionEvent) {
		if (event.user.idLong != AaronServer.AARON_MEMBER_ID) {
			event.reply("Only Aaron can use crosspost commands.").setEphemeral(true).await()
			return
		}

		event.deferReply(true).await()

		try {
			val startId = parseMessageId(event.getOption(START_ARGUMENT)?.asString)
			val endId = event.getOption(END_ARGUMENT)?.asString?.let { value -> parseMessageId(value) } ?: startId
			val destination = parseDestination(event.getOption(DESTINATION_ARGUMENT)?.asString)
			val parent = resolveParent(event.getOption(PARENT_ARGUMENT)?.asString, event)
			val draft = prepareDraft(event.user.idLong, startId, endId, event.channel)
			publish(draft, destination, parent, event.hook)
		} catch (exception: Exception) {
			event.hook.editOriginal("Could not publish the crosspost: ${exception.message}").await()
		}
	}

	suspend fun handleMessageCrosspost(event: MessageContextInteractionEvent) {
		if (event.user.idLong != AaronServer.AARON_MEMBER_ID) {
			event.reply("Only Aaron can use crosspost commands.").setEphemeral(true).await()
			return
		}

		val startInput = TextInput.create(MODAL_START_ID, TextInputStyle.SHORT)
			.setValue(event.target.id)
			.setRequiredRange(1, 100)
			.build()
		val endInput = TextInput.create(MODAL_END_ID, TextInputStyle.SHORT)
			.setPlaceholder("Leave blank to crosspost only the selected message")
			.setRequired(false)
			.setMaxLength(100)
			.build()
		val destinationMenu = StringSelectMenu.create(DESTINATION_ID)
			.addOption("Publish both", "both")
			.addOption("Tumblr only", "tumblr")
			.addOption("Bluesky only", "bluesky")
			.setDefaultValues("both")
			.build()
		val parentInput = TextInput.create(PARENT_MESSAGE_ID, TextInputStyle.SHORT)
			.setPlaceholder("Optional Knome crosspost audit message link")
			.setRequired(false)
			.setMaxLength(200)
			.build()
		val modal = Modal.create("$MODAL_PREFIX${event.channelId}:${event.target.id}", "Publish crosspost")
			.addComponents(
				Label.of("Start message ID or link", startInput),
				Label.of("End message ID or link", endInput),
				Label.of("Destination", destinationMenu),
				Label.of("Reply to prior crosspost", parentInput)
			)
			.build()

		event.replyModal(modal).await()
	}

	suspend fun handleRangeModal(event: ModalInteractionEvent) {
		if (!event.modalId.startsWith(MODAL_PREFIX)) {
			return
		}

		if (event.user.idLong != AaronServer.AARON_MEMBER_ID) {
			event.reply("Only Aaron can use crosspost commands.").setEphemeral(true).await()
			return
		}

		event.deferReply(true).await()

		try {
			val modalParts = event.modalId.removePrefix(MODAL_PREFIX).split(":", limit = 2)
			require(modalParts.size == 2 && modalParts[0] == event.channelId) {
				"This crosspost dialog is invalid or was opened in another channel."
			}

			val startId = parseMessageId(event.getValue(MODAL_START_ID)?.asString)
			val endValue = event.getValue(MODAL_END_ID)?.asString
			val endId = if (endValue.isNullOrBlank()) startId else parseMessageId(endValue)
			val destination = parseDestination(event.getValue(DESTINATION_ID)?.asStringList?.singleOrNull())
			val parent = resolveParent(event.getValue(PARENT_MESSAGE_ID)?.asString, event)
			val draft = prepareDraft(event.user.idLong, startId, endId, event.channel)
			publish(draft, destination, parent, event.hook)
		} catch (exception: Exception) {
			event.hook.editOriginal("Could not publish the crosspost: ${exception.message}").await()
		}
	}

	private fun parseDestination(value: String?): CrosspostDestination {
		return when (value) {
			"tumblr" -> CrosspostDestination.TUMBLR
			"bluesky" -> CrosspostDestination.BLUESKY
			"both" -> CrosspostDestination.BOTH
			else -> throw IllegalArgumentException("Choose a valid crosspost destination.")
		}
	}

	private suspend fun prepareDraft(
		ownerId: Long,
		startMessageId: Long,
		endMessageId: Long,
		channel: MessageChannelUnion
	): CrosspostDraft {
		CrosspostConfiguration.requireConfigured()
		val draft = CrosspostService.prepare(ownerId, startMessageId, endMessageId, channel)
		CrosspostService.discardDraft(draft.id, ownerId)
		return draft
	}

	private suspend fun publish(
		draft: CrosspostDraft,
		destination: CrosspostDestination,
		parent: CrosspostParent?,
		hook: InteractionHook
	) {
		hook.editOriginal("Publishing…").setEmbeds(emptyList()).setComponents(emptyList()).await()
		val results = CrosspostService.publish(draft, destination, parent)
		val description = results.joinToString("\n") { result ->
			if (result.succeeded) {
				"✅ ${result.destination}: ${result.url}"
			} else {
				"❌ ${result.destination}: ${result.error}"
			}
		}

		hook.editOriginal(description).await()
		val trackingError = try {
			CrosspostRepository.recordSuccessfulPublications(draft, results)
			null
		} catch (exception: Exception) {
			" The posts succeeded, but their Discord messages could not be marked as crossposted: ${exception.message}"
		}
		if (trackingError != null) {
			hook.editOriginal(description + "\n\n⚠️" + trackingError).await()
		}
		CrosspostAuditLog.publish(hook.jda, draft, results)
	}

	private fun parseMessageId(value: String?): Long {
		require(!value.isNullOrBlank()) { "A start message is required." }
		val id = value.trim().substringAfterLast('/')
		return id.toLongOrNull() ?: throw IllegalArgumentException("Message IDs and links must end in a numeric message ID.")
	}

	private suspend fun resolveParent(
		value: String?,
		event: Interaction
	): CrosspostParent? {
		if (value.isNullOrBlank()) {
			return null
		}
		return CrosspostParentResolver.resolve(value, event.jda)
	}

}