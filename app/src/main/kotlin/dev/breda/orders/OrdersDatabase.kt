package dev.breda.orders

import androidx.room.*
import dev.breda.orders.core.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "orders")
data class OrderRow(
    @PrimaryKey val id: String,
    val title: String,
    val cents: Long,
    val revision: Long,
    val syncedRevision: Long,
    val serverVersion: Long,
    val conflictTitle: String? = null,
    val conflictCents: Long? = null,
    val conflictVersion: Long? = null,
) {
    fun domain() =
        Order(
            id,
            title,
            cents,
            revision,
            syncedRevision,
            serverVersion,
            conflictVersion?.let {
                RemoteOrder(id, requireNotNull(conflictTitle), requireNotNull(conflictCents), it)
            },
        )
}

fun Order.row() =
    OrderRow(
        id,
        title,
        cents,
        revision,
        syncedRevision,
        serverVersion,
        conflict?.title,
        conflict?.cents,
        conflict?.version,
    )

@Entity(tableName = "remote_orders")
data class RemoteRow(
    @PrimaryKey val id: String,
    val title: String,
    val cents: Long,
    val version: Long,
) {
    fun domain() = RemoteOrder(id, title, cents, version)
}

@Entity(tableName = "remote_receipts")
data class RemoteReceipt(
    @PrimaryKey val key: String,
    val id: String,
    val title: String,
    val cents: Long,
    val version: Long,
)

@Dao
interface OrdersDao {
    @Query("SELECT * FROM orders ORDER BY id") fun observe(): Flow<List<OrderRow>>

    @Query("SELECT * FROM orders WHERE id = :id") suspend fun get(id: String): OrderRow?

    @Query("SELECT * FROM orders WHERE revision > syncedRevision AND conflictVersion IS NULL")
    suspend fun pending(): List<OrderRow>

    @Upsert suspend fun put(order: OrderRow)

    @Query("SELECT * FROM remote_orders WHERE id = :id") suspend fun remote(id: String): RemoteRow?

    @Upsert suspend fun putRemote(order: RemoteRow)

    @Query("SELECT * FROM remote_receipts WHERE `key` = :key")
    suspend fun receipt(key: String): RemoteReceipt?

    @Insert suspend fun putReceipt(receipt: RemoteReceipt)
}

@Database(
    entities = [OrderRow::class, RemoteRow::class, RemoteReceipt::class],
    version = 1,
    exportSchema = true,
)
abstract class OrdersDatabase : RoomDatabase() {
    abstract fun orders(): OrdersDao
}
