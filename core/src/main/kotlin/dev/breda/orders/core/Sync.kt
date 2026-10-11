package dev.breda.orders.core

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class RemoteOrder(val id: String, val title: String, val cents: Long, val version: Long)

data class Order(
    val id: String,
    val title: String,
    val cents: Long,
    val revision: Long = 1,
    val syncedRevision: Long = 0,
    val serverVersion: Long = 0,
    val conflict: RemoteOrder? = null,
) {
    init {
        require(title.isNotBlank())
        require(cents > 0)
        require(revision > 0)
    }

    val pending: Boolean
        get() = revision > syncedRevision && conflict == null

    val requestKey: String
        get() = "$id:$revision"
}

sealed interface PushResult {
    data class Accepted(val remote: RemoteOrder) : PushResult

    data class Conflict(val remote: RemoteOrder) : PushResult

    data object Retry : PushResult
}

interface OrderStore {
    suspend fun pending(): List<Order>

    /** Must atomically reduce against the current row, never overwrite a later local edit. */
    suspend fun apply(sent: Order, result: PushResult)
}

fun interface OrderRemote {
    suspend fun push(order: Order): PushResult
}

data class SyncReport(val accepted: Int, val conflicts: Int, val retryRequired: Boolean)

class SyncEngine(private val store: OrderStore, private val remote: OrderRemote) {
    private val mutex = Mutex()

    suspend fun sync(): SyncReport =
        mutex.withLock {
            var accepted = 0
            var conflicts = 0
            var retry = false
            for (sent in store.pending()) {
                val result =
                    try {
                        remote.push(sent)
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: Exception) {
                        PushResult.Retry
                    }
                when (result) {
                    is PushResult.Accepted -> accepted++
                    is PushResult.Conflict -> conflicts++
                    PushResult.Retry -> retry = true
                }
                store.apply(sent, result)
            }
            SyncReport(accepted, conflicts, retry)
        }
}

/** Pure conflict/acknowledgment rules, shared by Room and JVM tests. */
fun reduce(current: Order, sent: Order, result: PushResult): Order {
    require(current.id == sent.id)
    return when (result) {
        is PushResult.Accepted -> {
            require(result.remote.id == current.id)
            if (result.remote.version < current.serverVersion) current
            else
                current.copy(
                    serverVersion = result.remote.version,
                    syncedRevision = maxOf(current.syncedRevision, sent.revision),
                )
        }
        is PushResult.Conflict -> {
            require(result.remote.id == current.id)
            if (current.revision == sent.revision) current.copy(conflict = result.remote)
            else current
        }
        PushResult.Retry -> current
    }
}

fun keepLocal(order: Order): Order {
    val remote = requireNotNull(order.conflict)
    return order.copy(
        revision = order.revision + 1,
        serverVersion = remote.version,
        conflict = null,
    )
}

fun acceptRemote(order: Order): Order {
    val remote = requireNotNull(order.conflict)
    val next = order.revision + 1
    return order.copy(
        title = remote.title,
        cents = remote.cents,
        revision = next,
        syncedRevision = next,
        serverVersion = remote.version,
        conflict = null,
    )
}
