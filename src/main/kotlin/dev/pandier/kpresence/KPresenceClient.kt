@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package dev.pandier.kpresence

import dev.pandier.kpresence.rpc.RpcSocket
import dev.pandier.kpresence.rpc.openRpcPacketChannel
import dev.pandier.kpresence.logger.KPresenceLogger
import dev.pandier.kpresence.activity.Activity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.Closeable
import java.lang.System.getenv

private fun getDefaultPaths(): List<String> {
    return listOf("XDG_RUNTIME_DIR", "TMPDIR", "TMP", "TEMP")
            .mapNotNull { getenv(it) }
            .plus("/tmp")
            .flatMap { base ->
                listOf(base, "$base/app/com.discordapp.Discord", "$base/snap.discord")
            }
}

public class KPresenceClient(
    public var clientId: Long,
    scope: CoroutineScope = CoroutineScope(Dispatchers.IO),
    public val logger: KPresenceLogger = KPresenceLogger.Dummy,
) : Closeable {
    public enum class State {
        DISCONNECTED,
        CONNECTING,
        CONNECTED,
        READY,
    }

    private val scope: CoroutineScope = scope + SupervisorJob(scope.coroutineContext[Job])
    private val mutex: Mutex = Mutex()
    private var activity: Activity? = null
    private var socket: RpcSocket? = null
    private val _state = MutableStateFlow(State.DISCONNECTED)

    /**
     * A [StateFlow] of the client's [State].
     */
    public val state: StateFlow<State> = _state.asStateFlow()

    public fun connect(): Deferred<Boolean> = asyncWithLock {
        if (_state.value != State.DISCONNECTED)
            return@asyncWithLock false
        _state.value = State.CONNECTING

        try {
            val channel = openRpcPacketChannel(getDefaultPaths())
            if (channel == null) {
                logger.info("Couldn't find a Discord instance")
                _state.value = State.DISCONNECTED
                return@asyncWithLock false
            }

            socket = RpcSocket(
                clientId = clientId,
                scope = this@KPresenceClient.scope,
                logger = logger,
                channel = channel,
                onReady = {
                    mutex.withLock {
                        if (socket == it) {
                            it.sendActivity(activity)
                            _state.value = State.READY
                        }
                    }
                },
                onClose = {
                    mutex.withLock {
                        if (socket == it) {
                            socket = null
                            _state.value = State.DISCONNECTED
                        }
                    }
                },
            )
            logger.info("Connected to Discord")
        } catch (ex: Exception) {
            logger.error("Failed to connect", ex)
            _state.value = State.DISCONNECTED
        }

        true
    }

    public fun disconnect(): Deferred<Boolean> = asyncWithLock {
        if (_state.value == State.DISCONNECTED)
            return@asyncWithLock false

        try {
            socket?.close()
            socket = null
            _state.value = State.DISCONNECTED
            logger.info("Disconnected from Discord")
        } catch (ex: Exception) {
            logger.error("Failed to disconnect", ex)
        }

        true
    }

    public fun update(newActivity: Activity?): Deferred<Unit> = asyncWithLock {
        if (activity == newActivity)
            return@asyncWithLock
        activity = newActivity
        if (_state.value == State.READY) {
            socket?.sendActivity(newActivity)
        }
    }

    private fun <T> asyncWithLock(block: suspend CoroutineScope.() -> T): Deferred<T> {
        return this@KPresenceClient.scope.async(Dispatchers.IO) {
            mutex.withLock { block() }
        }
    }

    override fun close() {
        this@KPresenceClient.scope.cancel()
    }
}
