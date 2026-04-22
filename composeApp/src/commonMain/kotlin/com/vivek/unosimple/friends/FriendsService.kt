package com.vivek.unosimple.friends

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable

/**
 * Friends feature (F2 on the roadmap).
 *
 * Three node types backed by Firebase RTDB:
 * ```
 *   users/{uid}/{displayName, avatarId}          — public profile mirror
 *   friendRequests/{targetUid}/{senderUid}/…    — inbox of pending invites
 *   friends/{uid}/{friendUid}/…                  — accepted friendships (two-way mirror)
 * ```
 *
 * Platform implementations talk to the DB through either the wasm JS glue
 * or the dev.gitlive Kotlin SDK. The default no-op [SilentFriendsService]
 * is used on targets that haven't wired it yet (or in tests).
 */
@Serializable
data class FriendProfile(
    val uid: String,
    val displayName: String,
    val avatarId: String? = null,
)

interface FriendsService {
    /** Accepted friends, newest-first as they're added locally. */
    val friends: StateFlow<List<FriendProfile>>

    /** Pending incoming friend requests. */
    val pendingRequests: StateFlow<List<FriendProfile>>

    /** Send a request to [targetUid] carrying the current user's profile. */
    suspend fun sendRequest(targetUid: String)

    /** Accept an incoming request from [senderUid]; mirrors to both sides. */
    suspend fun acceptRequest(senderUid: String)

    /** Dismiss / reject an incoming request without adding the friend. */
    suspend fun rejectRequest(senderUid: String)

    /** Remove an existing friendship from both sides. */
    suspend fun removeFriend(friendUid: String)

    /** Detach listeners. */
    suspend fun close() {}
}

/** No-op fallback for targets that don't support the friends feature yet. */
class SilentFriendsService : FriendsService {
    override val friends: StateFlow<List<FriendProfile>> =
        MutableStateFlow<List<FriendProfile>>(emptyList()).asStateFlow()
    override val pendingRequests: StateFlow<List<FriendProfile>> =
        MutableStateFlow<List<FriendProfile>>(emptyList()).asStateFlow()
    override suspend fun sendRequest(targetUid: String) = Unit
    override suspend fun acceptRequest(senderUid: String) = Unit
    override suspend fun rejectRequest(senderUid: String) = Unit
    override suspend fun removeFriend(friendUid: String) = Unit
}

/** Platform factory — Wasm wires FirebaseFriendsService; others silent for now. */
expect fun createFriendsService(myUid: String, myDisplayName: String, myAvatarId: String?): FriendsService
