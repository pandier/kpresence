@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package dev.pandier.kpresence

import dev.pandier.kpresence.rpc.RpcSocket
import dev.pandier.kpresence.rpc.openRpcPacketChannel
import dev.pandier.kpresence.logger.KPresenceLogger
import dev.pandier.kpresence.activity.Activity
import dev.pandier.kpresence.activity.ActivityBuilder
import dev.pandier.kpresence.exception.DiscordNotFoundException
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.Closeable
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.time.Duration

public fun KPresenceClient(
    clientId: Long,
    block: KPresenceClientBuilder.() -> Unit = {}
): KPresenceClient {
    return KPresenceClientBuilder(clientId).apply(block).build()
}

public class KPresenceClient internal constructor(
    clientId: Long,
    parentScope: CoroutineScope?,
    public val logger: KPresenceLogger,
    private val autoReconnect: Boolean,
    private val autoReconnectDelay: Duration,
    private val unixPaths: List<String>,
) : Closeable {
    public enum class State {
        DISCONNECTED,
        CONNECTING,
        CONNECTED,
        READY,
    }

    private val scope: CoroutineScope = CoroutineScope((parentScope?.coroutineContext ?: EmptyCoroutineContext) + SupervisorJob(parentScope?.coroutineContext?.get(Job)))
    private val mutex: Mutex = Mutex()
    private var activity: Activity? = null
    private var socket: RpcSocket? = null
    private var connectJob: Deferred<ConnectResult>? = null
    private var autoReconnectJob: Job? = null
    private val _state = MutableStateFlow(State.DISCONNECTED)

    public var clientId: Long = clientId
        private set

    /**
     * A [StateFlow] of the client's [State].
     */
    public val state: StateFlow<State> = _state.asStateFlow()

    public sealed interface ConnectResult {
        public object Success : ConnectResult
        public object AlreadyConnected : ConnectResult
        public object Cancelled : ConnectResult
        public class Failed(exception: Throwable) : ConnectResult
    }

    public fun connect(): Deferred<ConnectResult> = async {
        mutex.withLock {
            connectLocked()
        }.await()
    }

    public fun disconnect(): Deferred<Boolean> = async {
        mutex.withLock {
            disconnectLocked()
        }
    }

    public fun reconnect(): Deferred<ConnectResult> = async {
        mutex.withLock {
            disconnectLocked()
            connectLocked()
        }.await()
    }

    public fun update(block: ActivityBuilder.() -> Unit): Deferred<Unit> {
        return update(Activity(block))
    }

    public fun update(newActivity: Activity?): Deferred<Unit> = async {
        mutex.withLock {
            if (activity == newActivity)
                return@async
            activity = newActivity
            if (_state.value == State.READY) {
                socket?.sendActivity(newActivity)
            }
        }
    }

    public suspend fun changeClientId(clientId: Long) {
        mutex.withLock {
            this.clientId = clientId
        }
    }

    override fun close() {
        scope.cancel()
    }

    private fun connectLocked(userTriggered: Boolean = true): Deferred<ConnectResult> {
        if (_state.value == State.CONNECTING) {
            return connectJob ?: CompletableDeferred(ConnectResult.Success)
        } else if (_state.value != State.DISCONNECTED) {
            return CompletableDeferred(ConnectResult.AlreadyConnected)
        }

        _state.value = State.CONNECTING

        if (userTriggered) {
            autoReconnectJob?.cancel()
            autoReconnectJob = null
        }
        connectJob?.cancel() // shouldn't happen but just to be sure

        val clientId = clientId

        return async {
            logger.debug("Connecting")

            val result = runCatching {
                RpcSocket(
                    clientId = clientId,
                    scope = scope,
                    logger = logger,
                    channel = openRpcPacketChannel(unixPaths),
                    onReady = this@KPresenceClient::onSocketReady,
                    onClose = this@KPresenceClient::onSocketClose,
                )
            }

            try {
                result.fold({ newSocket ->
                    mutex.withLock {
                        ensureActive()
                        _state.value = State.CONNECTED
                        connectJob = null
                        socket = newSocket
                        logger.info("Connected to Discord")
                    }

                    ConnectResult.Success
                }, { exception ->
                    mutex.withLock {
                        ensureActive()
                        _state.value = State.DISCONNECTED
                        connectJob = null
                        autoReconnectLocked()
                    }

                    if (exception is DiscordNotFoundException) {
                        if (userTriggered) {
                            logger.info("Could not find a running Discord instance")
                        }
                    } else {
                        logger.error("Failed to connect", exception)
                    }

                    ConnectResult.Failed(exception)
                })
            } catch (ex: CancellationException) {
                logger.debug("Connection process has been canceled")
                result.getOrNull()?.close()
                ConnectResult.Cancelled
            }
        }.also {
            connectJob = it
        }
    }

    private suspend fun onSocketReady(eventSocket: RpcSocket): Unit = mutex.withLock {
        if (eventSocket !== socket) return
        eventSocket.sendActivity(activity)
        _state.value = State.READY
    }

    private suspend fun onSocketClose(eventSocket: RpcSocket): Unit = mutex.withLock {
        if (eventSocket !== socket) return
        socket = null
        _state.value = State.DISCONNECTED
        logger.info("Connection with Discord has been closed")
        autoReconnectLocked()
    }

    private fun autoReconnectLocked() {
        if (!autoReconnect) return
        autoReconnectJob?.cancel()
        autoReconnectJob = scope.launch(Dispatchers.IO) {
            try {
                logger.debug("Reconnecting in $autoReconnectDelay")

                delay(autoReconnectDelay)

                @Suppress("DeferredResultUnused")
                mutex.withLock {
                    connectLocked(false)
                    autoReconnectJob = null
                }
            } catch (_: CancellationException) {
                logger.debug("Reconnect cancelled")
            }
        }
    }

    private fun disconnectLocked(): Boolean {
        logger.debug("Disconnect called in state ${_state.value}")

        autoReconnectJob?.cancel()
        autoReconnectJob = null
        connectJob?.cancel()
        connectJob = null

        if (_state.value == State.DISCONNECTED) {
            return false
        } else if (_state.value == State.CONNECTING) {
            _state.value = State.DISCONNECTED
            return true
        }

        try {
            socket?.close()
            socket = null
            _state.value = State.DISCONNECTED
            logger.info("Disconnected from Discord")
        } catch (ex: Exception) {
            logger.error("Failed to disconnect", ex)
        }
        return true
    }

    private fun <T> async(block: suspend CoroutineScope.() -> T): Deferred<T> {
        return scope.async(Dispatchers.IO) { block() }
    }
}
