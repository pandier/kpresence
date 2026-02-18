package dev.pandier.kpresence

import dev.pandier.kpresence.logger.KPresenceLogger
import kotlinx.coroutines.CoroutineScope
import java.lang.System.getenv

// TODO: Only do this if on Linux
private fun defaultUnixPaths(): MutableList<String> =
    listOf("XDG_RUNTIME_DIR", "TMPDIR", "TMP", "TEMP")
        .mapNotNull { getenv(it) }
        .plus("/tmp")
        .flatMap { base -> listOf(base, "$base/app/com.discordapp.Discord", "$base/snap.discord") }
        .toMutableList()

public class KPresenceClientBuilder(
    public val clientId: Long,
) {
    public var parentScope: CoroutineScope? = null
    public var logger: KPresenceLogger = KPresenceLogger.Dummy
    public var autoReconnect: Boolean = true
    private val unixPaths: MutableList<String> = defaultUnixPaths()

    public fun addUnixPath(path: String) {
        unixPaths += path
    }

    public fun addUnixPaths(vararg paths: String) {
        unixPaths += paths
    }

    public fun unixPaths(paths: List<String>) {
        unixPaths.clear()
        unixPaths += paths
    }

    internal fun build(): KPresenceClient {
        return KPresenceClient(clientId, parentScope, logger, autoReconnect, unixPaths)
    }
}
