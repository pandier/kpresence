package io.github.pandier.kpresence

import kotlinx.coroutines.CoroutineScope
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * A builder for constructing a [io.github.pandier.kpresence.KPresenceClient] instance.
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
     * The [io.github.pandier.kpresence.logger.KPresenceLogger] used for logging.
     */
    public var logger: io.github.pandier.kpresence.logger.KPresenceLogger = _root_ide_package_.io.github.pandier.kpresence.logger.KPresenceLogger.Dummy

    /**
     * Toggles automatic reconnecting on disconnection.
     */
    public var autoReconnect: Boolean = true

    /**
     * The period as a [Duration] between automatic reconnections.
     */
    public var autoReconnectPeriod: Duration = 20.seconds

    /**
     * Indicates whether the runtime operating system is Unix-like.
     *
     * The value is lazily evaluated using the `os.name` JVM system property.
     */
    public val isUnix: Boolean
        get() = _root_ide_package_.io.github.pandier.kpresence.util.isUnix

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
            addAll(_root_ide_package_.io.github.pandier.kpresence.util.defaultUnixPaths)
        }
    }

    internal fun build(): io.github.pandier.kpresence.KPresenceClient {
        return _root_ide_package_.io.github.pandier.kpresence.KPresenceClient(
            clientId,
            parentScope,
            logger,
            autoReconnect,
            autoReconnectPeriod,
            unixPaths
        )
    }
}
