package dev.breda.orders
import androidx.room.withTransaction
import dev.breda.orders.core.*
import java.util.UUID
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.map

class OrdersRepository(private val db: OrdersDatabase) : OrderStore {
 private val dao = db.orders()
 val orders = dao.observe().map { rows -> rows.map { it.domain() } }
 suspend fun create(title: String, cents: Long) { dao.put(Order(UUID.randomUUID().toString(),title,cents).row()) }
 suspend fun edit(id: String, title: String, cents: Long) = db.withTransaction {
  val order = requireNotNull(dao.get(id)).domain()
  check(order.conflict == null) { "Resolve the conflict before editing" }
  dao.put(order.copy(title = title,cents = cents,revision = order.revision + 1).row())
 }
 suspend fun resolve(id: String, useRemote: Boolean) = db.withTransaction {
  val order = requireNotNull(dao.get(id)).domain()
  dao.put((if(useRemote) acceptRemote(order) else keepLocal(order)).row())
 }
 override suspend fun pending() = dao.pending().map { it.domain() }
 override suspend fun apply(sent: Order, result: PushResult) {
  db.withTransaction { dao.get(sent.id)?.let { current -> dao.put(reduce(current.domain(),sent,result).row()) } }
 }
}
/** Durable local stand-in for a server, deliberately sharing a device database. */
class RoomSimulatedRemote(private val db: OrdersDatabase, private val online: () -> Boolean) : OrderRemote {
 override suspend fun push(order: Order): PushResult {
  if (!online()) return PushResult.Retry
  delay(400)
  return db.withTransaction {
   val dao = db.orders()
   dao.receipt(order.requestKey)?.let { receipt ->
    return@withTransaction PushResult.Accepted(RemoteOrder(receipt.id,receipt.title,receipt.cents,receipt.version))
   }
   val remote = dao.remote(order.id)
   if (remote != null && remote.version != order.serverVersion) return@withTransaction PushResult.Conflict(remote.domain())
   if (remote == null && order.serverVersion != 0L) return@withTransaction PushResult.Retry
   val updated = RemoteRow(order.id,order.title,order.cents,(remote?.version ?: 0) + 1)
   dao.putRemote(updated)
   dao.putReceipt(RemoteReceipt(order.requestKey,updated.id,updated.title,updated.cents,updated.version))
   PushResult.Accepted(updated.domain())
  }
 }
 suspend fun simulateRemoteEdit(id: String): Boolean = db.withTransaction {
  val dao = db.orders(); val remote = dao.remote(id) ?: return@withTransaction false
  dao.putRemote(remote.copy(title = "Alterado no servidor", version = remote.version + 1)); true
 }
}
