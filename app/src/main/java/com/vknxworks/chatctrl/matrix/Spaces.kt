package com.vknxworks.chatctrl.matrix

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.matrix.rustcomponents.sdk.Membership
import org.matrix.rustcomponents.sdk.RoomType
import org.matrix.rustcomponents.sdk.SpaceService
import uniffi.matrix_sdk_ui.SpaceRoomListPaginationState

/** A space you have joined, with the chats that belong to it. [level] 0 is a top-level space, 1 a space inside one. */
data class SpaceEntry(
    val id: String,
    val name: String,
    val avatarUrl: String?,
    val level: Int,
    /** Every room under this space, at any depth for a top-level space. */
    val roomIds: Set<String>,
)

/** A room or space listed in a space, as the homeserver describes it; you may or may not be in it. */
data class SpaceChild(
    val roomId: String,
    val name: String,
    val topic: String?,
    val avatarUrl: String?,
    val members: Long,
    val isSpace: Boolean,
    val joined: Boolean,
    val via: List<String>,
)

/** Reads the spaces you are in and what they contain, through the SDK's space service. */
class SpaceBrowser(private val session: MatrixSession) {
    private val mutex = Mutex()
    private var service: SpaceService? = null

    private suspend fun service(): SpaceService = mutex.withLock {
        service ?: session.client.spaceService().also { service = it }
    }

    /** Your spaces: each top-level space followed by the spaces directly inside it. */
    suspend fun spaces(): List<SpaceEntry> = withContext(Dispatchers.IO) {
        val s = service()
        // Rebuilds the SDK's picture of which room is in which space; the filters below are read from it.
        runCatching { s.topLevelJoinedSpaces() }
        val filters = s.spaceFilters()
        val top = filters.filter { it.level.toInt() == 0 }
        val inner = filters.filter { it.level.toInt() == 1 }
        val out = ArrayList<SpaceEntry>()
        for (t in top) {
            val direct = t.descendants.toSet()
            val below = inner.filter { it.spaceRoom.roomId in direct }
            val all = HashSet(direct)
            below.forEach { all.addAll(it.descendants) }
            out += SpaceEntry(t.spaceRoom.roomId, t.spaceRoom.displayName, t.spaceRoom.avatarUrl, 0, all)
            below.forEach { b ->
                out += SpaceEntry(b.spaceRoom.roomId, b.spaceRoom.displayName, b.spaceRoom.avatarUrl, 1, b.descendants.toSet())
            }
        }
        out
    }

    /** The rooms and spaces directly inside [spaceId], asked from the homeserver (also ones you haven't joined). */
    suspend fun children(spaceId: String): List<SpaceChild> = withContext(Dispatchers.IO) {
        service().spaceRoomList(spaceId).use { list ->
            var pages = 0
            do {
                list.paginate()
                pages++
            } while (pages < MAX_PAGES && (list.paginationState() as? SpaceRoomListPaginationState.Idle)?.endReached != true)
            list.rooms().map { r ->
                SpaceChild(
                    roomId = r.roomId,
                    name = r.displayName.ifBlank { r.canonicalAlias ?: r.roomId },
                    topic = r.topic?.takeIf { it.isNotBlank() },
                    avatarUrl = r.avatarUrl,
                    members = r.numJoinedMembers.toLong(),
                    isSpace = r.roomType is RoomType.Space,
                    joined = r.state == Membership.JOINED,
                    via = r.via,
                )
            }
        }
    }

    /** Joins a room listed in a space, through the servers the space names for it. Returns its room ID. */
    suspend fun join(child: SpaceChild): String = withContext(Dispatchers.IO) {
        session.client.joinRoomByIdOrAlias(child.roomId, child.via).use { it.id() }
    }

    private companion object {
        const val MAX_PAGES = 10
    }
}
