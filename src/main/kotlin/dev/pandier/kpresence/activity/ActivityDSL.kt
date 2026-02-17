@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package dev.pandier.kpresence.activity

@DslMarker
public annotation class ActivityDSL

/**
 * Creates an Activity instance using the provided builder block.
 *
 * @param block The builder block used to configure the Activity.
 * @return The constructed Activity instance.
 */
public fun activity(block: ActivityBuilder.() -> Unit): Activity = ActivityBuilder().apply(block).build()

/** Builder class for constructing Activity instances. */
@ActivityDSL
public class ActivityBuilder {
    /** Activity type. Defaults to [ActivityType.PLAYING]. */
    public var type: ActivityType = ActivityType.PLAYING

    /** Controls which field is displayed in the user's status text. */
    public var statusDisplayType: StatusDisplayType = StatusDisplayType.NAME

    /** Unix timestamps for start/end of the game. */
    public var timestamps: ActivityTimestamps? = null

    /** What the player is currently doing (First line). */
    public var details: String? = null

    /** URL of the details field. */
    public var detailsUrl: String? = null

    /** User's current party status (Second line). */
    public var state: String? = null

    /** URL of the state field. */
    public var stateUrl: String? = null

    /** Information for the current party of the player. */
    public var party: ActivityParty? = null

    /** Images for the presence and their hover texts. */
    public var assets: ActivityAssets? = null

    /** Secrets for Rich Presence joining and spectating. */
    public var secrets: ActivitySecrets? = null

    /** Whether the activity is an instanced game session. */
    public var instance: Boolean? = null

    private val buttons: MutableList<ActivityButton> = mutableListOf()

    /**
     * Configures the timestamps for the Activity.
     *
     * @param block The builder block used to configure the ActivityTimestamps.
     */
    public fun timestamps(block: ActivityTimestampsBuilder.() -> Unit) {
        timestamps = ActivityTimestampsBuilder().apply(block).build()
    }

    /**
     * Configures the party information for the Activity.
     *
     * @param block The builder block used to configure the ActivityParty.
     */
    public fun party(block: ActivityPartyBuilder.() -> Unit) {
        party = ActivityPartyBuilder().apply(block).build()
    }

    /**
     * Configures the assets for the Activity.
     *
     * @param block The builder block used to configure the ActivityAssets.
     */
    public fun assets(block: ActivityAssetsBuilder.() -> Unit) {
        assets = ActivityAssetsBuilder().apply(block).build()
    }

    /**
     * Configures the secrets for the Activity.
     *
     * @param block The builder block used to configure the ActivitySecrets.
     */
    public fun secrets(block: ActivitySecretsBuilder.() -> Unit) {
        secrets = ActivitySecretsBuilder().apply(block).build()
    }

    /**
     * Adds a button to the Activity.
     *
     * @param label The text shown on the button.
     * @param url The URL opened when clicking the button.
     */
    public fun button(label: String, url: String) {
        buttons.add(ActivityButton(label, url))
    }

    /**
     * Builds the configured Activity instance.
     *
     * @return The constructed Activity instance.
     */
    public fun build(): Activity =
        Activity(
            type,
            statusDisplayType,
            timestamps,
            details,
            detailsUrl,
            state,
            stateUrl,
            party,
            assets,
            secrets,
            instance,
            buttons.takeIf { it.isNotEmpty() }?.toTypedArray()
        )
}

/** Builder class for constructing ActivityTimestamps instances. */
@ActivityDSL
public class ActivityTimestampsBuilder {
    /** Unix time (in milliseconds) of when the activity started. */
    public var start: Long? = null

    /** Unix time (in milliseconds) of when the activity ends. */
    public var end: Long? = null

    /** Returns the current time in milliseconds since the Unix epoch. */
    public fun now(): Long = System.currentTimeMillis()

    /**
     * Builds the configured ActivityTimestamps instance.
     *
     * @return The constructed ActivityTimestamps instance.
     */
    public fun build(): ActivityTimestamps = ActivityTimestamps(start, end)
}

/** Builder class for constructing ActivityParty instances. */
@ActivityDSL
public class ActivityPartyBuilder {
    /** ID of the party. */
    public var id: String? = null
    public var currentSize: Int? = null
    public var maxSize: Int? = null

    /**
     * Configures the size of the party.
     *
     * @param current The current size of the party.
     * @param max The maximum size of the party.
     */
    public fun size(current: Int, max: Int) {
        currentSize = current
        maxSize = max
    }

    /**
     * Builds the configured ActivityParty instance.
     *
     * @return The constructed ActivityParty instance.
     */
    public fun build(): ActivityParty {
        val sizeArray =
            if (currentSize != null && maxSize != null) intArrayOf(currentSize!!, maxSize!!)
            else null
        return ActivityParty(id, sizeArray)
    }
}

/** Builder class for constructing ActivityAssets instances. */
@ActivityDSL
public class ActivityAssetsBuilder {
    /** ID of the large image, or a URL. */
    public var largeImage: String? = null

    /** Text displayed when hovering over the large image of the activity. */
    public var largeText: String? = null

    /** URL of the large image. */
    public var largeUrl: String? = null

    /** ID of the small image, or a URL. */
    public var smallImage: String? = null

    /** Text displayed when hovering over the small image of the activity. */
    public var smallText: String? = null

    /** URL of the small image. */
    public var smallUrl: String? = null

    /**
     * Builds the configured ActivityAssets instance.
     *
     * @return The constructed ActivityAssets instance.
     */
    public fun build(): ActivityAssets = ActivityAssets(largeImage, largeText, largeUrl, smallImage, smallText, smallUrl)
}

/** Builder class for constructing ActivitySecrets instances. */
@ActivityDSL
public class ActivitySecretsBuilder {
    /** Secret for joining a party. */
    public var join: String? = null

    /** Secret for spectating a game. */
    public var spectate: String? = null

    /** Secret for a specific instanced match. */
    public var match: String? = null

    /**
     * Builds the configured ActivitySecrets instance.
     *
     * @return The constructed ActivitySecrets instance.
     */
    public fun build(): ActivitySecrets = ActivitySecrets(join, spectate, match)
}
