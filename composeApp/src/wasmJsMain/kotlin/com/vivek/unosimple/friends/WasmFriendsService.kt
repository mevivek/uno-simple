package com.vivek.unosimple.friends

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

// ---------------------------------------------------------------------------
// JS glue externals — the same helpers used by WasmFirebaseSyncService.
// ---------------------------------------------------------------------------

@JsFun("(path, value) => window.unoDbSet(path, value)")
private external fun unoDbSet(path: String, value: String)

@JsFun("(path, value) => window.unoDbSetObject(path, value)")
private external fun unoDbSetObject(path: String, jsonValue: String)

@JsFun("(path, cb) => window.unoDbSubscribe(path, cb)")
private external fun unoDbSubscribe(path: String, callback: (String) -> Unit): Int

@JsFun("(handle) => window.unoDbUnsubscribe(handle)")
private external fun unoDbUnsubscribe(handle: Int)

@JsFun("(path) => window.unoDbDelete ? window.unoDbDelete(path) : window.unoDbSet(path, null)")
private external fun unoDbDelete(path: String)

/**
 * Wasm-native friends service. Talks to the Firebase Web SDK via the
 * uno-db.js glue for three RTDB sub-trees:
 *
 *   - `users/{uid}/…`            — current user's public profile mirror
 *   - `friendRequests/{target}/…` — pending invite inbox per user
 *   - `friends/{uid}/{friend}`    — accepted friendships (two-way mirrored)
 *
 * Both `friendRequests/{myUid}` and `friends/{myUid}` are subscribed on
 * construction; emissions map the nested keys back into FriendProfile
 * lists exposed as StateFlows.
 */
class WasmFriendsService(
    private val myUid: String,
    private val myDisplayName: String,
    private val myAvatarId: String?,
) : FriendsService {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private val _friends = MutableStateFlow<List<FriendProfile>>(emptyList())
    override val friends: StateFlow<List<FriendProfile>> = _friends.asStateFlow()

    private val _pending = MutableStateFlow<List<FriendProfile>>(emptyList())
    override val pendingRequests: StateFlow<List<FriendProfile>> = _pending.asStateFlow()

    private val friendsHandle: Int
    private val requestsHandle: Int

    init {
        // Keep our own profile mirror fresh on every service construction —
        // makes sure display-name / avatar edits propagate to anyone
        // already in a user's friends list.
        unoDbSet("users/$myUid/displayName", myDisplayName)
        myAvatarId?.let { unoDbSet("users/$myUid/avatarId", it) }

        friendsHandle = unoDbSubscribe("friends/$myUid") { raw ->
            _friends.value = parseProfileMap(raw)
        }
        requestsHandle = unoDbSubscribe("friendRequests/$myUid") { raw ->
            _pending.value = parseProfileMap(raw)
        }
    }

    override suspend fun sendRequest(targetUid: String) {
        if (targetUid == myUid) return // can't friend yourself
        unoDbSetObject(
            "friendRequests/$targetUid/$myUid",
            json.encodeToString(
                FriendProfile.serializer(),
                FriendProfile(uid = myUid, displayName = myDisplayName, avatarId = myAvatarId),
            ),
        )
    }

    override suspend fun acceptRequest(senderUid: String) {
        val sender = _pending.value.firstOrNull { it.uid == senderUid } ?: return
        // Two-way mirror: each side writes the other's profile under
        // friends/{me}/{other}.
        unoDbSetObject(
            "friends/$myUid/$senderUid",
            json.encodeToString(FriendProfile.serializer(), sender),
        )
        unoDbSetObject(
            "friends/$senderUid/$myUid",
            json.encodeToString(
                FriendProfile.serializer(),
                FriendProfile(uid = myUid, displayName = myDisplayName, avatarId = myAvatarId),
            ),
        )
        // Clear the request from our inbox.
        unoDbDelete("friendRequests/$myUid/$senderUid")
    }

    override suspend fun rejectRequest(senderUid: String) {
        unoDbDelete("friendRequests/$myUid/$senderUid")
    }

    override suspend fun removeFriend(friendUid: String) {
        unoDbDelete("friends/$myUid/$friendUid")
        unoDbDelete("friends/$friendUid/$myUid")
    }

    override suspend fun close() {
        unoDbUnsubscribe(friendsHandle)
        unoDbUnsubscribe(requestsHandle)
    }

    /**
     * Parse a `{ "<uid>": {displayName, avatarId}, ... }` map blob into a
     * flat profile list. Empty string = empty map.
     */
    private fun parseProfileMap(raw: String): List<FriendProfile> {
        if (raw.isEmpty()) return emptyList()
        val map = runCatching {
            json.decodeFromString<Map<String, FriendProfile>>(raw)
        }.getOrNull() ?: return emptyList()
        return map.entries.map { (uid, p) ->
            // Some writes might not include the uid in the nested object
            // (e.g. legacy blobs) — fall back to the map key.
            p.copy(uid = p.uid.ifBlank { uid })
        }
    }
}
