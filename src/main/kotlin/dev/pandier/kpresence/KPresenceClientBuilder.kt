package dev.pandier.kpresence

import dev.pandier.kpresence.logger.KPresenceLogger
import dev.pandier.kpresence.util.defaultUnixPaths
import kotlinx.coroutines.CoroutineScope
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * A builder for constructing a [KPresenceClient] instance.
 */
public class KPresenceClientBuilder(
    public val clientId: Long,
) {
    /**
     * The parent [CoroutineScope] whose [Job][kotlinx.coroutines.Job] will be used
     * as the parent for the client's coroutine scope.
     */
    public var parentScope: CoroutineScope? = null

    /**
     * The [KPresenceLogger] used for logging.
     */
    public var logger: KPresenceLogger = KPresenceLogger.Dummy

    /**
     * Toggles automatic reconnecting on disconnection.
     */
    public var autoReconnect: Boolean = true

    /**
     * The delay as a [Duration] used between automatic reconnections.
     */
    public var autoReconnectDelay: Duration = 15.seconds

    /**
     * Indicates whether the runtime operating system is Unix-like.
     *
     * The value is lazily evaluated using the `os.name` JVM system property.
     */
    public val isUnix: Boolean
        get() = dev.pandier.kpresence.util.isUnix

    private val unixPaths: MutableList<String> = mutableListOf()

    /**
     * Executes the given [block] on the unix path list. The [block] is executed only
     * if the runtime operating system is Unix-like.
     */
    public fun unixPaths(block: MutableList<String>.() -> Unit) {
        if (isUnix) {
            unixPaths.block()
        }
    }

    init {
        unixPaths {
            addAll(defaultUnixPaths)
        }
    }

    internal fun build(): KPresenceClient {
        return KPresenceClient(clientId, parentScope, logger, autoReconnect, autoReconnectDelay, unixPaths)
    }
}
