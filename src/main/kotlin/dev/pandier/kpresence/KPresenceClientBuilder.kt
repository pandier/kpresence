package dev.pandier.kpresence

import dev.pandier.kpresence.logger.KPresenceLogger
import dev.pandier.kpresence.util.defaultUnixPaths
import kotlinx.coroutines.CoroutineScope
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

public class KPresenceClientBuilder(
    public val clientId: Long,
) {
    public var parentScope: CoroutineScope? = null
    public var logger: KPresenceLogger = KPresenceLogger.Dummy
    public var autoReconnect: Boolean = true
    public var autoReconnectDelay: Duration = 15.seconds
    private val unixPaths: MutableList<String> = mutableListOf()

    public val isUnix: Boolean
        get() = dev.pandier.kpresence.util.isUnix

    init {
        unixPaths {
            addAll(defaultUnixPaths)
        }
    }

    public fun unixPaths(block: MutableList<String>.() -> Unit) {
        if (isUnix) {
            unixPaths.block()
        }
    }

    internal fun build(): KPresenceClient {
        return KPresenceClient(clientId, parentScope, logger, autoReconnect, autoReconnectDelay, unixPaths)
    }
}
