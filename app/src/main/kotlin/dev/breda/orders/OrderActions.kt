package dev.breda.orders

import dev.breda.orders.core.Order
import kotlinx.coroutines.flow.Flow

interface OrderCatalog {
    val orders: Flow<List<Order>>

    suspend fun create(title: String, cents: Long)

    suspend fun edit(id: String, title: String, cents: Long)

    suspend fun resolve(id: String, useRemote: Boolean)
}

interface OrderSync {
    var online: Boolean
    val work: Flow<String>

    fun schedule(force: Boolean = false)

    suspend fun simulateRemoteEdit(id: String): Boolean
}

class SaveOrder(private val catalog: OrderCatalog, private val sync: OrderSync) {
    suspend operator fun invoke(title: String, rawAmount: String, editing: Order?) {
        val cents = requireNotNull(AmountInput.cents(rawAmount)) { "Valor inválido." }
        require(title.isNotBlank()) { "Título obrigatório." }
        if (editing == null) catalog.create(title.trim(), cents)
        else catalog.edit(editing.id, title.trim(), cents)
        sync.schedule()
    }
}
