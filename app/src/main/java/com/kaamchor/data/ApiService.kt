package com.kaamchor.data

import com.kaamchor.data.model.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // ── AUTH ────────────────────────────────────────────────────────────
    @POST("auth/signup")
    suspend fun signup(@Body body: SignupRequest): Response<AuthResponse>

    @POST("auth/login")
    suspend fun login(@Body body: LoginRequest): Response<AuthResponse>

    @GET("auth/me")
    suspend fun getMe(@Header("Authorization") token: String): Response<User>

    @GET("auth/users")
    suspend fun getUsers(@Header("Authorization") token: String): Response<List<User>>

    @POST("auth/update-ntfy-token")
    suspend fun updateNtfyToken(
        @Header("Authorization") token: String,
        @Body body: NtfyTokenRequest
    ): Response<Unit>

    // ── POSTS ───────────────────────────────────────────────────────────
    @Multipart
    @POST("posts")
    suspend fun createPost(
        @Header("Authorization") token: String,
        @Part photo: MultipartBody.Part,
        @Part("caption") caption: RequestBody?
    ): Response<Post>

    @GET("posts")
    suspend fun getFeed(
        @Header("Authorization") token: String,
        @Query("cursor") cursor: Int? = null,
        @Query("limit") limit: Int = 20
    ): Response<FeedResponse>

    @GET("posts/{postId}")
    suspend fun getPost(
        @Header("Authorization") token: String,
        @Path("postId") postId: Int
    ): Response<Post>

    @GET("posts/user/{userId}")
    suspend fun getUserPosts(
        @Header("Authorization") token: String,
        @Path("userId") userId: Int,
        @Query("cursor") cursor: Int? = null,
        @Query("limit") limit: Int = 20
    ): Response<FeedResponse>

    // ── LIKES & COMMENTS ────────────────────────────────────────────────
    @POST("posts/{postId}/like")
    suspend fun toggleLike(
        @Header("Authorization") token: String,
        @Path("postId") postId: Int
    ): Response<LikeResponse>

    @GET("posts/{postId}/comments")
    suspend fun getComments(
        @Header("Authorization") token: String,
        @Path("postId") postId: Int
    ): Response<List<Comment>>

    @POST("posts/{postId}/comments")
    suspend fun addComment(
        @Header("Authorization") token: String,
        @Path("postId") postId: Int,
        @Body body: CommentRequest
    ): Response<Comment>

    // ── PROFILE ─────────────────────────────────────────────────────────
    @GET("profile/{userId}")
    suspend fun getProfile(
        @Header("Authorization") token: String,
        @Path("userId") userId: Int
    ): Response<User>

    @GET("profile/me")
    suspend fun getMyProfile(@Header("Authorization") token: String): Response<User>

    @Multipart
    @PUT("profile/edit")
    suspend fun editProfile(
        @Header("Authorization") token: String,
        @Part avatar: MultipartBody.Part?,
        @Part("display_name") displayName: RequestBody?,
        @Part("bio") bio: RequestBody?
    ): Response<User>

    @PUT("profile/edit")
    suspend fun editProfileJson(
        @Header("Authorization") token: String,
        @Body body: EditProfileJsonRequest
    ): Response<User>

    // ── CHAT ────────────────────────────────────────────────────────────
    @GET("chat/groups")
    suspend fun getGroups(@Header("Authorization") token: String): Response<List<ChatGroup>>

    @POST("chat/groups")
    suspend fun createGroup(
        @Header("Authorization") token: String,
        @Body body: CreateGroupRequest
    ): Response<ChatGroup>

    @GET("chat/groups/{groupId}/messages")
    suspend fun getMessages(
        @Header("Authorization") token: String,
        @Path("groupId") groupId: Int,
        @Query("cursor") cursor: Int? = null,
        @Query("limit") limit: Int = 30,
        @Query("mark_read") markRead: String = "true"
    ): Response<MessagesResponse>

    @Multipart
    @POST("chat/groups/{groupId}/messages")
    suspend fun sendMessageMedia(
        @Header("Authorization") token: String,
        @Path("groupId") groupId: Int,
        @Part file: MultipartBody.Part,
        @Part("text") text: RequestBody?,
        @Part("media_type") mediaType: RequestBody,
        @Part("duration") duration: RequestBody?
    ): Response<Message>

    @POST("chat/groups/{groupId}/messages")
    suspend fun sendMessageText(
        @Header("Authorization") token: String,
        @Path("groupId") groupId: Int,
        @Body body: TextMessageRequest
    ): Response<Message>

    @POST("chat/groups/{groupId}/read-all")
    suspend fun markAllRead(
        @Header("Authorization") token: String,
        @Path("groupId") groupId: Int
    ): Response<Unit>

    // ── CALLS ───────────────────────────────────────────────────────────
    @POST("calls/initiate")
    suspend fun initiateCall(
        @Header("Authorization") token: String,
        @Body body: InitiateCallRequest
    ): Response<CallRecord>

    @POST("calls/{callId}/end")
    suspend fun endCall(
        @Header("Authorization") token: String,
        @Path("callId") callId: Int
    ): Response<CallRecord>

    @POST("calls/{callId}/reject")
    suspend fun rejectCall(
        @Header("Authorization") token: String,
        @Path("callId") callId: Int
    ): Response<CallRecord>

    // ── ICE SERVERS ─────────────────────────────────────────────────────
    @GET("ice-servers")
    suspend fun getIceServers(@Header("Authorization") token: String): Response<IceServersResponse>
}

data class SignupRequest(
    val username: String,
    val email: String,
    val password: String,
    val display_name: String? = null,
    val ntfy_token: String? = null
)

data class LoginRequest(
    val identifier: String,
    val password: String
)

data class NtfyTokenRequest(
    val ntfy_token: String
)

data class CommentRequest(
    val text: String
)

data class CreateGroupRequest(
    val name: String? = null,
    val member_ids: List<Int>
)

data class TextMessageRequest(
    val text: String
)

data class InitiateCallRequest(
    val group_id: Int
)

data class IceServersResponse(
    val iceServers: List<IceServer>
)

data class IceServer(
    val urls: String,
    val username: String? = null,
    val credential: String? = null
)

data class EditProfileJsonRequest(
    val display_name: String,
    val bio: String
)
