@file:Suppress("ArrayInDataClass", "unused")

package dev.pandier.kpresence.activity

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * Represents a user's activity on Discord.
 *
 * @property type The type of activity.
 * @property statusDisplayType Controls which field is displayed in the user's status text.
 * @property timestamps Unix timestamps for start and/or end of the activity.
 * @property details What the player is currently doing (2 to 128 characters).
 * @property detailsUrl URL that is linked when clicking on the details text (1 to 256 characters).
 * @property state User's current party status, or text used for a custom status (2 to 128 characters).
 * @property stateUrl URL that is linked when clicking on the state text (1 to 256 characters).
 * @property party Information for the current party of the player.
 * @property assets Images for the presence and their hover texts.
 * @property secrets Secrets for Rich Presence joining and spectating.
 * @property instance Whether the activity is an instanced game session.
 * @property buttons Custom buttons shown in the Rich Presence (max 2).
 */
@Serializable
public data class Activity(
    val type: ActivityType = ActivityType.PLAYING,
    @SerialName("status_display_type") val statusDisplayType: StatusDisplayType = StatusDisplayType.NAME,
    val timestamps: ActivityTimestamps = ActivityTimestamps(),
    val details: String? = null,
    @SerialName("details_url") val detailsUrl: String? = null,
    val state: String? = null,
    @SerialName("state_url") val stateUrl: String? = null,
    val party: ActivityParty = ActivityParty(),
    val assets: ActivityAssets = ActivityAssets(),
    val secrets: ActivitySecrets = ActivitySecrets(),
    val instance: Boolean? = null,
    val buttons: List<ActivityButton> = listOf(),
) {
    init {
        require(details == null || details.length in 2..128) { "details must be between 2 and 128 characters" }
        require(detailsUrl == null || detailsUrl.length in 1..256) { "detailsUrl must be between 2 and 256 characters" }
        require(state == null || state.length in 2..128) { "state must be between 2 and 128 characters" }
        require(stateUrl == null || stateUrl.length in 1..256) { "stateUrl must be between 2 and 256 characters" }
        require(buttons.size <= 2) { "buttons must contain less than or equal to 2 items" }
    }
}

/**
 * Represents the type of [Activity].
 */
@Serializable(ActivityTypeSerializer::class)
public enum class ActivityType(public val value: Short) {
    PLAYING(0),
    LISTENING(2),
    WATCHING(3),
    COMPETING(5),
}

/**
 * Controls which field is displayed in the user's status text.
 */
@Serializable(StatusDisplayTypeSerializer::class)
public enum class StatusDisplayType(public val value: Short) {
    NAME(0),
    STATE(1),
    DETAILS(2),
}

/**
 * Represents the timestamps for an [Activity].
 *
 * @property start Unix time in milliseconds of when the activity started.
 * @property end Unix time in milliseconds of when the activity ends.
 */
@Serializable
public data class ActivityTimestamps(
    val start: Long? = null,
    val end: Long? = null,
)

/**
 * Represents the party for an [Activity].
 *
 * @property id ID of the party (2 to 128 characters).
 * @property size An array of 2 integers representing the party's current size (1st item) and maximum size (2nd item).
 */
@Serializable
public data class ActivityParty(
    val id: String? = null,
    val size: IntArray? = null,
) {
    init {
        require(id == null || id.length in 2..128) { "id must be between 2 and 128 characters" }
        require(size == null || size.size == 2) { "size must be an array of 2 integers" }
    }
}

/**
 * Represents the assets for an activity.
 *
 * @property largeImage ID of the large image or a URL (1 to 300 characters).
 * @property largeText Text displayed when hovering over the large image of the activity (2 to 128 characters).
 * @property largeUrl URL that is opened when clicking the large image (1 to 256 characters).
 * @property smallImage ID of the small image or a URL (1 to 300 characters).
 * @property smallText Text displayed when hovering over the small image of the activity (2 to 128 characters).
 * @property smallUrl URL that is opened when clicking the small image (1 to 256 characters).
 */
@Serializable
public data class ActivityAssets(
    @SerialName("large_image") val largeImage: String? = null,
    @SerialName("large_text") val largeText: String? = null,
    @SerialName("large_url") val largeUrl: String? = null,
    @SerialName("small_image") val smallImage: String? = null,
    @SerialName("small_text") val smallText: String? = null,
    @SerialName("small_url") val smallUrl: String? = null
) {
    init {
        require(largeImage == null || largeImage.length in 1..300) { "largeImage must be between 1 and 300 characters" }
        require(largeText == null || largeText.length in 2..128) { "largeText must be between 2 and 128 characters" }
        require(largeUrl == null || largeUrl.length in 1..256) { "largeUrl must be between 1 and 256 characters" }
        require(smallImage == null || smallImage.length in 1..300) { "smallImage must be between 1 and 300 characters" }
        require(smallText == null || smallText.length in 2..128) { "smallText must be between 2 and 128 characters" }
        require(smallUrl == null || smallUrl.length in 1..256) { "smallUrl must be between 1 and 256 characters" }
    }
}

/**
 * Represents the secrets for an activity.
 *
 * @property join Secret for joining a party (2 to 128 characters).
 * @property spectate Secret for spectating a game (2 to 128 characters).
 * @property match Secret for a specific instanced match (2 to 128 characters).
 */
@Serializable
public data class ActivitySecrets(
    val join: String? = null,
    val spectate: String? = null,
    val match: String? = null
) {
    init {
        require(join == null || join.length in 2..128) { "join secret must be between 2 and 128 characters" }
        require(spectate == null || spectate.length in 2..128) { "spectate secret must be between 2 and 128 characters" }
        require(match == null || match.length in 2..128) { "match secret must be between 2 and 128 characters" }
    }
}

/**
 * Represents a button for an activity.
 *
 * @property label Text shown on the button (1 to 32 characters).
 * @property url URL opened when clicking the button (1 to 512 characters).
 */
@Serializable
public data class ActivityButton(val label: String, val url: String) {
    init {
        require(label.length in 1..32) { "label must be between 2 and 32 characters" }
        require(url.length in 1..512) { "url must be between 2 and 512 characters" }
    }
}

private object ActivityTypeSerializer : KSerializer<ActivityType> {
    override val descriptor = PrimitiveSerialDescriptor("ActivityType", PrimitiveKind.SHORT)

    override fun serialize(encoder: Encoder, value: ActivityType) {
        encoder.encodeShort(value.value)
    }

    override fun deserialize(decoder: Decoder): ActivityType {
        return ActivityType.entries.first { it.value == decoder.decodeShort() }
    }
}

private object StatusDisplayTypeSerializer : KSerializer<StatusDisplayType> {
    override val descriptor = PrimitiveSerialDescriptor("StatusDisplayType", PrimitiveKind.SHORT)

    override fun serialize(encoder: Encoder, value: StatusDisplayType) {
        encoder.encodeShort(value.value)
    }

    override fun deserialize(decoder: Decoder): StatusDisplayType {
        return StatusDisplayType.entries.first { it.value == decoder.decodeShort() }
    }
}
