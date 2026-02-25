@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package dev.pandier.kpresence.activity

@DslMarker
public annotation class ActivityDsl

/**
 * Builds an [Activity] instance using the provided builder block.
 */
public fun Activity(block: ActivityBuilder.() -> Unit): Activity = ActivityBuilder().apply(block).build()

/**
 * Builder class for constructing [Activity] instances.
 */
@ActivityDsl
public class ActivityBuilder {
    /**
     * The name of the activity (1 to 128 characters).
     *
     * Uses the application name based on the client id if null.
     */
    public var name: String? = null

    /**
     * The type of the activity. Defaults to [ActivityType.PLAYING].
     */
    public var type: ActivityType = ActivityType.PLAYING

    /**
     * Controls which field is displayed in the user's status text.
     */
    public var statusDisplayType: StatusDisplayType = StatusDisplayType.NAME

    /**
     * Unix timestamps for start/end of the game.
     */
    public var timestamps: ActivityTimestampsBuilder = ActivityTimestampsBuilder()

    /**
     * What the player is currently doing (first line, 2 to 128 characters).
     */
    public var details: String? = null

    /**
     * URL of the details field (1 to 256 characters).
     */
    public var detailsUrl: String? = null

    /**
     * User's current party status (second line, 2 to 128 characters).
     */
    public var state: String? = null

    /**
     * URL of the state field (1 to 256 characters).
     */
    public var stateUrl: String? = null

    /**
     * Information for the current party of the player.
     */
    public var party: ActivityPartyBuilder = ActivityPartyBuilder()

    /**
     * Images for the presence and their hover texts.
     */
    public var assets: ActivityAssetsBuilder = ActivityAssetsBuilder()

    /**
     * Secrets for Rich Presence joining and spectating.
     */
    public var secrets: ActivitySecretsBuilder = ActivitySecretsBuilder()

    /**
     * Whether the activity is an instanced game session.
     */
    public var instance: Boolean? = null

    public val buttons: MutableList<ActivityButton> = mutableListOf()

    /**
     * Configures the timestamps for the [Activity].
     *
     * @param block The builder block used to configure the ActivityTimestamps.
     */
    public fun timestamps(block: ActivityTimestampsBuilder.() -> Unit) {
        timestamps.apply(block)
    }

    /**
     * Configures the party information for the [Activity].
     *
     * @param block The builder block used to configure the [ActivityParty].
     */
    public fun party(block: ActivityPartyBuilder.() -> Unit) {
        party.apply(block)
    }

    /**
     * Configures the assets for the [Activity].
     *
     * @param block The builder block used to configure the [ActivityAssets].
     */
    public fun assets(block: ActivityAssetsBuilder.() -> Unit) {
        assets.apply(block)
    }

    /**
     * Configures the secrets for the [Activity].
     *
     * @param block The builder block used to configure the [ActivitySecrets].
     */
    public fun secrets(block: ActivitySecretsBuilder.() -> Unit) {
        secrets.apply(block)
    }

    /**
     * Adds a button to the [Activity].
     *
     * @param label The text shown on the button.
     * @param url The URL opened when clicking the button.
     */
    public fun button(label: String, url: String) {
        buttons.add(ActivityButton(label, url))
    }

    /**
     * Builds the configured [Activity] instance.
     */
    public fun build(): Activity {
        return Activity(
            name,
            type,
            statusDisplayType,
            timestamps.build(),
            details,
            detailsUrl,
            state,
            stateUrl,
            party.build(),
            assets.build(),
            secrets.build(),
            instance,
            buttons.toList(),
        )
    }
}

/**
 * Builder class for constructing [ActivityTimestamps] instances.
 */
@ActivityDsl
public class ActivityTimestampsBuilder {
    /**
     * Unix time in milliseconds of when the activity started.
     */
    public var start: Long? = null

    /**
     * Unix time in milliseconds of when the activity ends.
     */
    public var end: Long? = null

    /**
     * Returns the current time in milliseconds since the Unix epoch.
     */
    public fun now(): Long = System.currentTimeMillis()

    /**
     * Builds the configured [ActivityTimestamps] instance.
     */
    public fun build(): ActivityTimestamps = ActivityTimestamps(start, end)
}

/**
 * Builder class for constructing [ActivityParty] instances.
 */
@ActivityDsl
public class ActivityPartyBuilder {
    /**
     * ID of the party.
     */
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
     * Builds the configured [ActivityParty] instance.
     */
    public fun build(): ActivityParty =
        ActivityParty(id, currentSize?.let { c -> maxSize?.let { m -> intArrayOf(c, m) } })
}

/**
 * Builder class for constructing [ActivityAssets] instances.
 */
@ActivityDsl
public class ActivityAssetsBuilder {
    /**
     * ID of the large image or a URL (1 to 300 characters).
     */
    public var largeImage: String? = null

    /**
     * Text displayed when hovering over the large image of the activity (2 to 128 characters).
     */
    public var largeText: String? = null

    /**
     * URL that is opened when clicking the large image (1 to 256 characters).
     */
    public var largeUrl: String? = null

    /**
     * ID of the small image or a URL (1 to 300 characters).
     */
    public var smallImage: String? = null

    /**
     * Text displayed when hovering over the small image of the activity (2 to 128 characters).
     */
    public var smallText: String? = null

    /**
     * URL that is opened when clicking the small image (1 to 256 characters).
     */
    public var smallUrl: String? = null

    /**
     * Builds the configured [ActivityAssets] instance.
     */
    public fun build(): ActivityAssets =
        ActivityAssets(largeImage, largeText, largeUrl, smallImage, smallText, smallUrl)
}

/**
 * Builder class for constructing [ActivitySecrets] instances.
 */
@ActivityDsl
public class ActivitySecretsBuilder {
    /**
     * Secret for joining a party (2 to 128 characters).
     */
    public var join: String? = null

    /**
     * Secret for spectating a game (2 to 128 characters).
     */
    public var spectate: String? = null

    /**
     * Secret for a specific instanced match (2 to 128 characters).
     */
    public var match: String? = null

    /**
     * Builds the configured [ActivitySecrets] instance.
     */
    public fun build(): ActivitySecrets = ActivitySecrets(join, spectate, match)
}
