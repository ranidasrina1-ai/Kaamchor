package com.kaamchor.data

import android.util.Log
import com.kaamchor.data.model.Message
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.emitter.Emitter
import org.json.JSONObject
import java.net.URISyntaxException

/**
 * Singleton managing the Socket.IO connection for realtime chat + WebRTC signaling.
 */
object SocketClient {

    private const val TAG = "SocketClient"

    private var socket: Socket? = null
    private var isConnected = false

    // Listener references so we can remove them
    private val listeners = mutableMapOf<String, Emitter.Listener>()

    fun connect(token: String, serverUrl: String) {
        if (socket != null && isConnected) {
            Log.d(TAG, "Socket already connected")
            return
        }

        try {
            val opts = IO.Options()
            opts.query = "token=$token"
            opts.reconnection = true
            opts.reconnectionAttempts = Int.MAX_VALUE
            opts.reconnectionDelay = 1000
            opts.reconnectionDelayMax = 5000
            opts.timeout = 10000

            socket = IO.socket(serverUrl, opts)
        } catch (e: URISyntaxException) {
            Log.e(TAG, "Invalid server URL: $serverUrl", e)
            return
        }

        socket?.apply {
            on(Socket.EVENT_CONNECT) {
                isConnected = true
                Log.d(TAG, "Socket connected")
            }
            on(Socket.EVENT_DISCONNECT) {
                isConnected = false
                Log.d(TAG, "Socket disconnected")
            }
            on(Socket.EVENT_CONNECT_ERROR) { args ->
                Log.e(TAG, "Socket connect error: ${args?.joinToString()}")
            }
            connect()
        }
    }

    fun disconnect() {
        socket?.disconnect()
        socket = null
        isConnected = false
        listeners.clear()
    }

    fun is_connected(): Boolean = isConnected

    // ── Event listeners ─────────────────────────────────────────────────

    fun onNewMessage(callback: (Message) -> Unit) {
        val listener = Emitter.Listener { args ->
            try {
                if (args != null && args.isNotEmpty()) {
                    val data = args[0] as JSONObject
                    val msg = parseMessage(data)
                    callback(msg)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing new_message", e)
            }
        }
        socket?.on("new_message", listener)
        listeners["new_message"] = listener
    }

    fun onTyping(callback: (groupId: Int, userId: Int, username: String, isTyping: Boolean) -> Unit) {
        val listener = Emitter.Listener { args ->
            try {
                if (args != null && args.isNotEmpty()) {
                    val data = args[0] as JSONObject
                    callback(
                        data.optInt("group_id", -1),
                        data.optInt("user_id", -1),
                        data.optString("username", "Someone"),
                        data.optBoolean("is_typing", false)
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing typing", e)
            }
        }
        socket?.on("user_typing", listener)
        listeners["user_typing"] = listener
    }

    fun onMessagesRead(callback: (groupId: Int, messageIds: List<Int>, readBy: Int) -> Unit) {
        val listener = Emitter.Listener { args ->
            try {
                if (args != null && args.isNotEmpty()) {
                    val data = args[0] as JSONObject
                    val ids = mutableListOf<Int>()
                    val arr = data.optJSONArray("message_ids")
                    if (arr != null) {
                        for (i in 0 until arr.length()) {
                            ids.add(arr.optInt(i))
                        }
                    }
                    callback(
                        data.optInt("group_id", -1),
                        ids,
                        data.optInt("read_by", -1)
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing messages_read", e)
            }
        }
        socket?.on("messages_read", listener)
        listeners["messages_read"] = listener
    }

    fun onMessagesDelivered(callback: (groupId: Int, messageIds: List<Int>) -> Unit) {
        val listener = Emitter.Listener { args ->
            try {
                if (args != null && args.isNotEmpty()) {
                    val data = args[0] as JSONObject
                    val ids = mutableListOf<Int>()
                    val arr = data.optJSONArray("message_ids")
                    if (arr != null) {
                        for (i in 0 until arr.length()) {
                            ids.add(arr.optInt(i))
                        }
                    }
                    callback(data.optInt("group_id", -1), ids)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing messages_delivered", e)
            }
        }
        socket?.on("messages_delivered", listener)
        listeners["messages_delivered"] = listener
    }

    // ── WebRTC signaling ────────────────────────────────────────────────

    fun onIncomingCall(callback: (fromUserId: Int, fromUsername: String, offer: Any?, callId: Int?, callType: String?) -> Unit) {
        val listener = Emitter.Listener { args ->
            try {
                if (args != null && args.isNotEmpty()) {
                    val data = args[0] as JSONObject
                    callback(
                        data.optInt("from_user_id", -1),
                        data.optString("from_username", "Unknown"),
                        data.opt("offer"),
                        if (data.has("call_id")) data.optInt("call_id") else null,
                        data.optString("call_type", "audio")
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing incoming_call", e)
            }
        }
        socket?.on("incoming_call", listener)
        listeners["incoming_call"] = listener
    }

    fun onCallAnswered(callback: (fromUserId: Int, answer: Any?, callId: Int?) -> Unit) {
        val listener = Emitter.Listener { args ->
            try {
                if (args != null && args.isNotEmpty()) {
                    val data = args[0] as JSONObject
                    callback(
                        data.optInt("from_user_id", -1),
                        data.opt("answer"),
                        if (data.has("call_id")) data.optInt("call_id") else null
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing call_answered", e)
            }
        }
        socket?.on("call_answered", listener)
        listeners["call_answered"] = listener
    }

    fun onCallRejected(callback: (fromUserId: Int, callId: Int?) -> Unit) {
        val listener = Emitter.Listener { args ->
            try {
                if (args != null && args.isNotEmpty()) {
                    val data = args[0] as JSONObject
                    callback(
                        data.optInt("from_user_id", -1),
                        if (data.has("call_id")) data.optInt("call_id") else null
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing call_rejected", e)
            }
        }
        socket?.on("call_rejected", listener)
        listeners["call_rejected"] = listener
    }

    fun onIceCandidate(callback: (fromUserId: Int, candidate: Any?) -> Unit) {
        val listener = Emitter.Listener { args ->
            try {
                if (args != null && args.isNotEmpty()) {
                    val data = args[0] as JSONObject
                    callback(data.optInt("from_user_id", -1), data.opt("candidate"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing ice_candidate", e)
            }
        }
        socket?.on("ice_candidate", listener)
        listeners["ice_candidate"] = listener
    }

    fun onCallEnded(callback: (fromUserId: Int, callId: Int?) -> Unit) {
        val listener = Emitter.Listener { args ->
            try {
                if (args != null && args.isNotEmpty()) {
                    val data = args[0] as JSONObject
                    callback(
                        data.optInt("from_user_id", -1),
                        if (data.has("call_id")) data.optInt("call_id") else null
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing call_ended", e)
            }
        }
        socket?.on("call_ended", listener)
        listeners["call_ended"] = listener
    }

    // ── Emit events ─────────────────────────────────────────────────────

    fun emitTyping(groupId: Int, isTyping: Boolean) {
        val data = JSONObject()
        data.put("group_id", groupId)
        data.put("is_typing", isTyping)
        socket?.emit("typing", data)
    }

    fun emitMarkRead(groupId: Int, messageIds: List<Int>) {
        val data = JSONObject()
        data.put("group_id", groupId)
        data.put("message_ids", messageIds)
        socket?.emit("mark_read", data)
    }

    fun emitMarkDelivered(groupId: Int) {
        val data = JSONObject()
        data.put("group_id", groupId)
        socket?.emit("mark_delivered", data)
    }

    fun emitCallOffer(toUserId: Int?, groupId: Int?, offer: Any, callId: Int) {
        val data = JSONObject()
        if (toUserId != null) data.put("to_user_id", toUserId)
        if (groupId != null) data.put("group_id", groupId)
        data.put("offer", offer)
        data.put("call_id", callId)
        data.put("call_type", "audio")
        socket?.emit("call_offer", data)
    }

    fun emitCallAnswer(toUserId: Int, answer: Any, callId: Int?) {
        val data = JSONObject()
        data.put("to_user_id", toUserId)
        data.put("answer", answer)
        if (callId != null) data.put("call_id", callId)
        socket?.emit("call_answer", data)
    }

    fun emitCallReject(toUserId: Int, callId: Int?) {
        val data = JSONObject()
        data.put("to_user_id", toUserId)
        if (callId != null) data.put("call_id", callId)
        socket?.emit("call_reject", data)
    }

    fun emitIceCandidate(toUserId: Int, candidate: Any) {
        val data = JSONObject()
        data.put("to_user_id", toUserId)
        data.put("candidate", candidate)
        socket?.emit("ice_candidate", data)
    }

    fun emitCallEnd(toUserId: Int?, groupId: Int?, callId: Int?) {
        val data = JSONObject()
        if (toUserId != null) data.put("to_user_id", toUserId)
        if (groupId != null) data.put("group_id", groupId)
        if (callId != null) data.put("call_id", callId)
        socket?.emit("call_end", data)
    }

    // ── Helpers ─────────────────────────────────────────────────────────

    private fun parseMessage(data: JSONObject): Message {
        return Message(
            id = data.optInt("id"),
            groupId = data.optInt("group_id"),
            senderId = data.optInt("sender_id"),
            text = data.optString("text", null),
            mediaUrl = if (data.has("media_url") && !data.isNull("media_url")) data.optString("media_url") else null,
            mediaType = data.optString("media_type", "text"),
            duration = if (data.has("duration") && !data.isNull("duration")) data.optDouble("duration").toFloat() else null,
            createdAt = data.optString("created_at", null),
            sender = if (data.has("sender") && !data.isNull("sender")) parseUser(data.optJSONObject("sender")) else null
        )
    }

    private fun parseUser(data: JSONObject?): com.kaamchor.data.model.User? {
        if (data == null) return null
        return com.kaamchor.data.model.User(
            id = data.optInt("id"),
            username = data.optString("username", ""),
            email = if (data.has("email") && !data.isNull("email")) data.optString("email") else null,
            displayName = if (data.has("display_name") && !data.isNull("display_name")) data.optString("display_name") else null,
            bio = if (data.has("bio") && !data.isNull("bio")) data.optString("bio") else null,
            avatarUrl = if (data.has("avatar_url") && !data.isNull("avatar_url")) data.optString("avatar_url") else null,
            createdAt = if (data.has("created_at") && !data.isNull("created_at")) data.optString("created_at") else null
        )
    }

    fun removeAllListeners() {
        for ((event, listener) in listeners) {
            socket?.off(event, listener)
        }
        listeners.clear()
    }
}
