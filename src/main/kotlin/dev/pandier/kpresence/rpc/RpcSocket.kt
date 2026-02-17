package dev.pandier.kpresence.rpc

import dev.pandier.kpresence.activity.Activity
import dev.pandier.kpresence.logger.KPresenceLogger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.channels.ClosedSendChannelException
import kotlinx.coroutines.isActive
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.serializer
import java.io.Closeable
import java.nio.channels.ClosedChannelException

private val json = Json {
    ignoreUnknownKeys = true
}

internal class RpcSocket(
    clientId: Long,
    scope: CoroutineScope,
    private val logger: KPresenceLogger,
    private val channel: RpcPacketChannel,
    private val onReady: suspend (RpcSocket) -> Unit,
    private val onClose: suspend (RpcSocket) -> Unit,
) : Closeable {
    private val outgoing = Channel<RpcPacket>(Channel.UNLIMITED)
    private val job: Job = scope.launch(Dispatchers.IO) {
        try {
            // Reading
            val read = launch {
                try {
                    while (isActive) {
                        val packet = channel.read()
                        when (packet.opcode) {
                            1 -> { // Message
                                val message = json.decodeFromString<RpcIncomingMessage>(packet.data)
                                logger.debug("Received message: $message")

                                if (message.event == "ERROR") {
                                    val error = json.decodeFromJsonElement<RpcError>(message.data)
                                    logger.error("Received error ${error.code}: ${error.message}")
                                } else if (message.command == "DISPATCH" && message.event == "READY") {
                                    onReady.invoke(this@RpcSocket)
                                }
                            }
                            2 -> { // Close
                                val error = json.decodeFromString<RpcError>(packet.data)
                                logger.error("Closing with error ${error.code}: ${error.message}")
                                break
                            }
                            3 -> { // Ping
                                logger.debug("Responding to PING request")
                                outgoing.send(RpcPacket(4, packet.data))
                            }
                            4 -> {
                                logger.debug("Received PONG response")
                            }
                            else -> error("Unexpected opcode: ${packet.opcode}")
                        }
                    }
                } catch (_: CancellationException) {
                } catch (_: ClosedChannelException) {
                } catch (e: Exception) {
                    logger.error("An exception happened while reading, closing the connection", e)
                } finally {
                    close()
                }
            }

            // Writing
            val write = launch(Dispatchers.IO) {
                try {
                    // Handshake
                    logger.debug("Sending handshake (client id: ${clientId})")
                    channel.write(RpcPacket(0, "{\"v\": 1,\"client_id\":\"${clientId}\"}"))

                    while (isActive) {
                        channel.write(outgoing.receive())
                    }
                } catch (_: CancellationException) {
                } catch (_: ClosedChannelException) {
                } catch (_: ClosedReceiveChannelException) {
                } catch (e: Exception) {
                    logger.error("An exception happened while writing, closing the connection", e)
                } finally {
                    close()
                }
            }

            joinAll(read, write)
        } finally {
            // fully dispose of everything
            outgoing.close()
            channel.close()
            onClose(this@RpcSocket)
            logger.debug("Closed connection")
        }
    }

    suspend fun sendActivity(activity: Activity?): Boolean {
        return send(RpcSetActivityArgs(ProcessHandle.current().pid(), activity))
    }

    suspend inline fun <reified T : RpcArgs> send(args: T): Boolean {
        return send(args, json.serializersModule.serializer<T>())
    }

    suspend fun <T : RpcArgs> send(args: T, serializer: KSerializer<T>): Boolean {
        // TODO: Serialize inside writing coroutine?
        val message = RpcOutgoingMessage(command = args.command, args = args, nonce = JsonPrimitive(1))
        try {
            logger.debug("Sending message: $message")
            val data = json.encodeToString(RpcOutgoingMessage.serializer(serializer), message)
            outgoing.send(RpcPacket(1, data))
            return true
        } catch (_: ClosedSendChannelException) {
            return false
        }
    }

    override fun close() {
        job.cancel()
        // rest is handled by writing coroutine
    }
}
