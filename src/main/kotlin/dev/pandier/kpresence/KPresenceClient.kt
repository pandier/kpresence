@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package dev.pandier.kpresence

import dev.pandier.kpresence.rpc.RpcSocket
import dev.pandier.kpresence.rpc.openRpcPacketChannel
import dev.pandier.kpresence.logger.KPresenceLogger
import dev.pandier.kpresence.activity.Activity
import dev.pandier.kpresence.activity.ActivityBuilder
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.Closeable
import kotlin.coroutines.EmptyCoroutineContext

public fun KPresenceClient(clientId: Long, block: KPresenceClientBuilder.() -> Unit = {}): KPresenceClient =
    KPresenceClientBuilder(clientId).also(block).build()

public class KPresenceClient internal constructor(
    public var clientId: Long,
    parentScope: CoroutineScope?,
    public val logger: KPresenceLogger,
    private val autoReconnect: Boolean,
    private val unixPaths: List<String>,
) : Closeable {
    public enum class State {
        DISCONNECTED,
        CONNECTING,
        CONNECTED,
        READY,
    }

    public sealed interface ConnectResult {
        public object Success : ConnectResult
        public object AlreadyConnected : ConnectResult
        public object MissingInstance : ConnectResult
        public class Failed(exception: Exception) : ConnectResult
    }

    private val scope: CoroutineScope = CoroutineScope((parentScope?.coroutineContext ?: EmptyCoroutineContext) + SupervisorJob(parentScope?.coroutineContext?.get(Job)))
    private val mutex: Mutex = Mutex()
    private var activity: Activity? = null
    private var socket: RpcSocket? = null
    private var autoReconnectJob: Job? = null
    private val _state = MutableStateFlow(State.DISCONNECTED)

    /**
     * A [StateFlow] of the client's [State].
     */
    public val state: StateFlow<State> = _state.asStateFlow()

    public fun connect(): Deferred<ConnectResult> = async {
        connectInternal()
    }

    public fun disconnect(): Deferred<Boolean> = async {
        disconnectInternal()
    }

    public fun reconnect(): Deferred<ConnectResult> = async {
        disconnectInternal()
        connectInternal()
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

    override fun close() {
        scope.cancel()
    }

    private suspend fun connectInternal(userTriggered: Boolean = true): ConnectResult {
        mutex.withLock {
            if (_state.value != State.DISCONNECTED)
                return ConnectResult.AlreadyConnected
            _state.value = State.CONNECTING

            if (userTriggered) {
                autoReconnectJob?.cancel()
            }
        }

        val result = try {
            val channel = openRpcPacketChannel(unixPaths)
            if (channel != null) {
                val newSocket = RpcSocket(
                    clientId = clientId,
                    scope = scope,
                    logger = logger,
                    channel = channel,
                    onReady = this::onSocketReady,
                    onClose = this::onSocketClose,
                )

                mutex.withLock {
                    socket = newSocket
                    _state.value = State.CONNECTED
                }
                logger.info("Connected to Discord")
                ConnectResult.Success
            } else {
                if (userTriggered) {
                    logger.info("Couldn't find a Discord instance")
                }
                ConnectResult.MissingInstance
            }
        } catch (ex: Exception) {
            logger.error("Failed to connect", ex)
            ConnectResult.Failed(ex)
        }

        if (result != ConnectResult.Success) {
            mutex.withLock {
                _state.value = State.DISCONNECTED
                autoReconnect()
            }
        }

        return result
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
        autoReconnect()
    }

    private fun autoReconnect() {
        if (!autoReconnect) return
        autoReconnectJob?.cancel()
        autoReconnectJob = scope.launch(Dispatchers.IO) {
            logger.debug("Reconnecting in 5 seconds")
            try {
                delay(5000)
                connectInternal(false)
            } catch (_: CancellationException) {
                logger.debug("Reconnect cancelled")
            }
        }
    }

    private suspend fun disconnectInternal(): Boolean {
        mutex.withLock {
            if (_state.value == State.DISCONNECTED) {
                autoReconnectJob?.cancel()
                return false
            } else if (_state.value == State.CONNECTING) {
                return false
            }

            try {
                socket?.close()
                socket = null
                _state.value = State.DISCONNECTED
                logger.info("Disconnected from Discord")
            } catch (ex: Exception) {
                logger.error("Failed to disconnect", ex)
            }
        }
        return true
    }

    private fun <T> async(block: suspend CoroutineScope.() -> T): Deferred<T> {
        return scope.async(Dispatchers.IO) { block() }
    }
}
