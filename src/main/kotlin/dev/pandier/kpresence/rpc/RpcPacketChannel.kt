package dev.pandier.kpresence.rpc

import dev.pandier.kpresence.exception.DiscordNotFoundException
import kotlinx.coroutines.suspendCancellableCoroutine
import java.net.UnixDomainSocketAddress
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.AsynchronousFileChannel
import java.nio.channels.Channel
import java.nio.channels.ClosedChannelException
import java.nio.channels.CompletionHandler
import java.nio.channels.SocketChannel
import java.nio.file.NoSuchFileException
import java.nio.file.StandardOpenOption
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.io.path.Path
import kotlin.io.path.exists

internal fun openRpcPacketChannel(unixPaths: List<String>): RpcPacketChannel {
    if (System.getProperty("os.name").lowercase().startsWith("windows")) {
        for (i in 0..9) {
            val path = Path("\\\\.\\pipe\\discord-ipc-$i")
            try {
                val fileChannel = AsynchronousFileChannel.open(path, StandardOpenOption.READ, StandardOpenOption.WRITE)
                return FileRpcPacketChannel(fileChannel)
            } catch (_: NoSuchFileException) {
            }
        }
    } else {
        for (dirPath in unixPaths) {
            for (i in 0..9) {
                val path = Path(dirPath).resolve("discord-ipc-$i")
                if (!path.exists()) continue // there is no good atomic way to check this
                val socketChannel = SocketChannel.open(UnixDomainSocketAddress.of(path))
                return SocketRpcPacketChannel(socketChannel)
            }
        }
    }
    throw DiscordNotFoundException()
}

internal class RpcPacket(val opcode: Int, val data: String)

internal interface RpcPacketChannel : Channel {

    suspend fun read(): RpcPacket {
        val header = ByteBuffer.allocate(8)
        header.order(ByteOrder.LITTLE_ENDIAN)

        if (read(header) < 8)
            throw ClosedChannelException()

        header.flip()
        val opcode = header.getInt()
        val length = header.getInt()

        val data = ByteBuffer.allocate(length)
        if (length > 0 && read(data) <= 0)
            throw ClosedChannelException()

        return RpcPacket(opcode, data.array().decodeToString())
    }

    suspend fun read(buffer: ByteBuffer): Int

    suspend fun write(packet: RpcPacket) {
        val data = packet.data.encodeToByteArray()
        val buffer = ByteBuffer.allocate(data.size + 8)
        buffer.order(ByteOrder.LITTLE_ENDIAN)

        buffer.putInt(packet.opcode)
        buffer.putInt(data.size)
        buffer.put(data)
        buffer.flip()

        if (write(buffer) <= 0)
            throw ClosedChannelException()
    }

    suspend fun write(buffer: ByteBuffer): Int
}

internal class FileRpcPacketChannel(
    private val channel: AsynchronousFileChannel
) : RpcPacketChannel {
    override suspend fun read(buffer: ByteBuffer): Int {
        return suspendCancellableCoroutine { continuation ->
            channel.read(buffer, 0L, Unit, object : CompletionHandler<Int, Unit> {
                override fun completed(result: Int, attachment: Unit?) {
                    continuation.resume(result)
                }

                override fun failed(exc: Throwable, attachment: Unit?) {
                    continuation.resumeWithException(exc)
                }
            })
        }
    }

    override suspend fun write(buffer: ByteBuffer): Int {
        return suspendCancellableCoroutine { continuation ->
            channel.write(buffer, 0L, Unit, object : CompletionHandler<Int, Unit> {
                override fun completed(result: Int, attachment: Unit?) {
                    continuation.resume(result)
                }

                override fun failed(exc: Throwable, attachment: Unit?) {
                    continuation.resumeWithException(exc)
                }
            })
        }
    }

    override fun close() {
        channel.close()
    }

    override fun isOpen(): Boolean =
        channel.isOpen
}

@Suppress("BlockingMethodInNonBlockingContext")
internal class SocketRpcPacketChannel(
    private val channel: SocketChannel
) : RpcPacketChannel {
    override suspend fun read(buffer: ByteBuffer): Int {
        return channel.read(buffer)
    }

    override suspend fun write(buffer: ByteBuffer): Int {
        return channel.write(buffer)
    }

    override fun close() {
        channel.close()
    }

    override fun isOpen(): Boolean =
        channel.isOpen
}
