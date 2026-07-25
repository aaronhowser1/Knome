package dev.aaronhowser.apps.knome.crosspost

import dev.aaronhowser.apps.knome.discord.AaronServer
import dev.aaronhowser.apps.knome.discord.await
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.components.actionrow.ActionRow
import net.dv8tion.jda.api.components.buttons.Button
import net.dv8tion.jda.api.components.label.Label
import net.dv8tion.jda.api.components.textinput.TextInput
import net.dv8tion.jda.api.components.textinput.TextInputStyle
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.interactions.InteractionHook
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.CommandData
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData
import net.dv8tion.jda.api.modals.Modal
import net.dv8tion.jda.api.utils.FileUpload

object CrosspostCommand {

	const val COMMAND_NAME = "crosspost"
	const val MESSAGE_COMMAND_NAME = "Crosspost"
	private const val START_ARGUMENT = "start"
	private const val END_ARGUMENT = "end"
	private const val BUTTON_PREFIX = "crosspost:"
	private const val MODAL_PREFIX = "crosspost-range:"
	private const val MODAL_START_ID = "start-message"
	private const val MODAL_END_ID = "end-message"

	fun getCommand(): SlashCommandData {
		return Commands.slash(COMMAND_NAME, "Preview and publish messages from #philosophy")
			.addOption(OptionType.STRING, START_ARGUMENT, "First message link or ID", true)
			.addOption(OptionType.STRING, END_ARGUMENT, "Last message link or ID", false)
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
			createPreview(event.user.idLong, startId, endId, event.channel, event.hook)
		} catch (exception: Exception) {
			event.hook.editOriginal("Could not create the preview: ${exception.message}").await()
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
		val modal = Modal.create("$MODAL_PREFIX${event.channelId}:${event.target.id}", "Preview crosspost")
			.addComponents(
				Label.of("Start message ID or link", startInput),
				Label.of("End message ID or link", endInput)
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
			createPreview(event.user.idLong, startId, endId, event.channel, event.hook)
		} catch (exception: Exception) {
			event.hook.editOriginal("Could not create the preview: ${exception.message}").await()
		}
	}

	suspend fun handleButton(event: ButtonInteractionEvent) {
		if (!event.componentId.startsWith(BUTTON_PREFIX)) {
			return
		}

		if (event.user.idLong != AaronServer.AARON_MEMBER_ID) {
			event.reply("Only Aaron can use crosspost commands.").setEphemeral(true).await()
			return
		}

		event.deferEdit().await()

		val parts = event.componentId.split(":", limit = 3)
		if (parts.size != 3) {
			event.hook.editOriginal("This crosspost action is invalid.").setComponents(emptyList()).await()
			return
		}

		val action = parts[1]
		val draftId = parts[2]
		if (action == "cancel") {
			CrosspostService.discardDraft(draftId, event.user.idLong)
			event.hook.editOriginal("Crosspost cancelled.").setEmbeds(emptyList()).setComponents(emptyList()).await()
			return
		}

		val draft = CrosspostService.claimDraft(draftId, event.user.idLong)
		if (draft == null) {
			event.hook.editOriginal("This crosspost preview expired or belongs to someone else.")
				.setComponents(emptyList())
				.await()
			return
		}

		val destination = when (action) {
			"tumblr" -> CrosspostDestination.TUMBLR
			"bluesky" -> CrosspostDestination.BLUESKY
			"both" -> CrosspostDestination.BOTH
			else -> {
				event.hook.editOriginal("Unknown crosspost destination.").setComponents(emptyList()).await()
				return
			}
		}

		event.hook.editOriginal("Publishing…").setEmbeds(emptyList()).setComponents(emptyList()).await()
		val results = CrosspostService.publish(draft, destination)
		val description = results.joinToString("\n") { result ->
			if (result.succeeded) {
				"✅ ${result.destination}: ${result.url}"
			} else {
				"❌ ${result.destination}: ${result.error}"
			}
		}

		event.hook.editOriginal(description).await()
		CrosspostAuditLog.publish(event.jda, draft.content, results)
	}

	private fun parseMessageId(value: String?): Long {
		require(!value.isNullOrBlank()) { "A start message is required." }
		val id = value.trim().substringAfterLast('/')
		return id.toLongOrNull() ?: throw IllegalArgumentException("Message IDs and links must end in a numeric message ID.")
	}

	private suspend fun createPreview(
		ownerId: Long,
		startMessageId: Long,
		endMessageId: Long,
		channel: MessageChannelUnion,
		hook: InteractionHook
	) {
		CrosspostConfiguration.requireConfigured()
		val draft = CrosspostService.prepare(ownerId, startMessageId, endMessageId, channel)

		hook.editOriginalEmbeds(createPreview(draft))
			.setComponents(createButtons(draft.id))
			.setFiles(draft.images.map { image -> FileUpload.fromData(image.data, image.fileName) })
			.await()
	}

	private fun createPreview(draft: CrosspostDraft): List<net.dv8tion.jda.api.entities.MessageEmbed> {
		val tumblrPreview = EmbedBuilder()
			.setTitle("Tumblr preview")
			.setColor(0x34526F)
			.setDescription(truncate(draft.content.ifBlank { "(images only)" }, 4000))
			.addField("Images", imageSummary(draft), false)
			.build()

		val blueskyParts = BlueskyPublisher.previewParts(draft)
		val blueskyPreview = EmbedBuilder()
			.setTitle(if (blueskyParts.size == 1) "Bluesky preview" else "Bluesky thread preview")
			.setColor(0x1185FE)
			.setDescription("${blueskyParts.size} post${if (blueskyParts.size == 1) "" else "s"} · ${draft.images.size} image(s)")

		for (index in blueskyParts.indices.take(20)) {
			val imagesForPart = draft.images.drop(index * 4).take(4).size
			val value = buildString {
				append(blueskyParts[index].ifBlank { "(images only)" })
				if (imagesForPart > 0) {
					append("\n\n🖼️ $imagesForPart image(s)")
				}
			}

			blueskyPreview.addField("Post ${index + 1}", truncate(value, 1000), false)
		}

		if (blueskyParts.size > 20) {
			blueskyPreview.addField("Additional posts", "${blueskyParts.size - 20} more", false)
		}

		return listOf(tumblrPreview, blueskyPreview.build())
	}

	private fun imageSummary(draft: CrosspostDraft): String {
		if (draft.images.isEmpty()) {
			return "None"
		}

		return truncate(draft.images.joinToString("\n") { image -> "• ${image.fileName}" }, 1000)
	}

	private fun createButtons(draftId: String): List<ActionRow> {
		return listOf(
			ActionRow.of(
				Button.success("${BUTTON_PREFIX}both:$draftId", "Publish both"),
				Button.primary("${BUTTON_PREFIX}tumblr:$draftId", "Tumblr only"),
				Button.primary("${BUTTON_PREFIX}bluesky:$draftId", "Bluesky only"),
				Button.danger("${BUTTON_PREFIX}cancel:$draftId", "Cancel")
			)
		)
	}

	private fun truncate(value: String, maximum: Int): String {
		return if (value.length <= maximum) value else value.take(maximum - 1) + "…"
	}
}