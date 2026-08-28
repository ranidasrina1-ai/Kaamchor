package com.kaamchor.data.model

import com.google.gson.annotations.SerializedName

data class User(
    val id: Int,
    val username: String,
    val email: String? = null,
    @SerializedName("display_name")
    val displayName: String? = null,
    val bio: String? = null,
    @SerializedName("avatar_url")
    val avatarUrl: String? = null,
    @SerializedName("created_at")
    val createdAt: String? = null
)

data class Post(
    val id: Int,
    @SerializedName("user_id")
    val userId: Int,
    val caption: String? = null,
    @SerializedName("photo_url")
    val photoUrl: String,
    @SerializedName("created_at")
    val createdAt: String? = null,
    val author: User? = null,
    @SerializedName("like_count")
    val likeCount: Int = 0,
    @SerializedName("comment_count")
    val commentCount: Int = 0,
    @SerializedName("liked_by_me")
    val likedByMe: Boolean = false
)

data class Comment(
    val id: Int,
    @SerializedName("post_id")
    val postId: Int,
    @SerializedName("user_id")
    val userId: Int,
    val text: String,
    @SerializedName("created_at")
    val createdAt: String? = null,
    val author: User? = null
)

data class ChatGroup(
    val id: Int,
    val name: String? = null,
    @SerializedName("is_group")
    val isGroup: Boolean = false,
    val members: List<User> = emptyList(),
    @SerializedName("created_at")
    val createdAt: String? = null,
    @SerializedName("last_message")
    val lastMessage: Message? = null,
    @SerializedName("unread_count")
    val unreadCount: Int = 0
)

data class Message(
    val id: Int,
    @SerializedName("group_id")
    val groupId: Int,
    @SerializedName("sender_id")
    val senderId: Int,
    val text: String? = null,
    @SerializedName("media_url")
    val mediaUrl: String? = null,
    @SerializedName("media_type")
    val mediaType: String = "text",
    val duration: Float? = null,
    @SerializedName("created_at")
    val createdAt: String? = null,
    val sender: User? = null
)

data class CallRecord(
    val id: Int,
    @SerializedName("group_id")
    val groupId: Int? = null,
    @SerializedName("caller_id")
    val callerId: Int,
    @SerializedName("call_type")
    val callType: String = "audio",
    val status: String = "initiated",
    @SerializedName("started_at")
    val startedAt: String? = null,
    @SerializedName("ended_at")
    val endedAt: String? = null
)

// ── Response wrappers ───────────────────────────────────────────────────────
data class AuthResponse(
    val token: String,
    val user: User
)

data class FeedResponse(
    val posts: List<Post>,
    @SerializedName("next_cursor")
    val nextCursor: Int? = null,
    @SerializedName("has_more")
    val hasMore: Boolean = false
)

data class MessagesResponse(
    val messages: List<Message>,
    @SerializedName("next_cursor")
    val nextCursor: Int? = null,
    @SerializedName("has_more")
    val hasMore: Boolean = false
)

data class LikeResponse(
    val liked: Boolean,
    @SerializedName("like_count")
    val likeCount: Int
)

data class ErrorResponse(
    val error: String
)
