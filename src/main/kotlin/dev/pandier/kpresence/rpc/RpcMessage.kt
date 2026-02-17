package dev.pandier.kpresence.rpc

import dev.pandier.kpresence.activity.Activity
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

internal interface RpcArgs {
    val command: String
}

@Serializable
internal data class RpcSetActivityArgs(
    val pid: Long,
    val activity: Activity?,
) : RpcArgs {
    override val command: String
        get() = "SET_ACTIVITY"
}

@Serializable
internal data class RpcOutgoingMessage<T : RpcArgs>(
    @SerialName("cmd")
    val command: String,
    @SerialName("evt")
    val event: String? = null,
    val args: T,
    val nonce: JsonElement,
)

@Serializable
internal data class RpcIncomingMessage(
    @SerialName("cmd")
    val command: String,
    @SerialName("evt")
    val event: String?,
    val data: JsonElement,
    val nonce: JsonElement,
)

@Serializable
internal data class RpcError(
    val code: Int,
    val message: String,
)
