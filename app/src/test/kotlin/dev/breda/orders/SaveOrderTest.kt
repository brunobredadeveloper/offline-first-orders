package dev.breda.orders

import dev.breda.orders.core.Order
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class SaveOrderTest {
    @Test
    fun validatesBeforePersistenceAndSchedulesOnlySuccessfulWrites() = runTest {
        val events = mutableListOf<String>()
        val catalog =
            object : OrderCatalog {
                override val orders = flowOf(emptyList<Order>())

                override suspend fun create(title: String, cents: Long) {
                    events += "create:$title:$cents"
                }

                override suspend fun edit(id: String, title: String, cents: Long) {
                    events += "edit:$id:$title:$cents"
                }

                override suspend fun resolve(id: String, useRemote: Boolean) = Unit
            }
        val sync =
            object : OrderSync {
                override var online = false
                override val work = flowOf("IDLE")

                override fun schedule(force: Boolean) {
                    events += "schedule"
                }

                override suspend fun simulateRemoteEdit(id: String) = false
            }
        val save = SaveOrder(catalog, sync)
        assertTrue(runCatching { save("", "1", null) }.isFailure)
        assertTrue(runCatching { save("Ana", "0.001", null) }.isFailure)
        assertTrue(events.isEmpty())
        save(" Ana ", "2,50", null)
        assertEquals(listOf("create:Ana:250", "schedule"), events)
        events.clear()
        save("Novo", "3", Order("order-1", "Anterior", 100))
        assertEquals(listOf("edit:order-1:Novo:300", "schedule"), events)
    }
}
