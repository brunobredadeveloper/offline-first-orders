package dev.breda.orders

import androidx.lifecycle.viewModelScope
import dev.breda.orders.core.Order
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class OrdersViewModel(
    private val catalog: OrderCatalog,
    private val synchronization: OrderSync,
    private val saveOrder: SaveOrder,
) : androidx.lifecycle.ViewModel() {
    val orders =
        catalog.orders.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    private val onlineState = MutableStateFlow(synchronization.online)
    val online = onlineState.asStateFlow()
    private val operationState = MutableStateFlow("idle")
    val operation = operationState.asStateFlow()
    private val noticeState = MutableStateFlow("")
    val notice = noticeState.asStateFlow()

    init {
        synchronization.schedule()
    }

    fun setOnline(value: Boolean) {
        synchronization.online = value
        onlineState.value = value
        synchronization.schedule(force = value)
    }

    fun save(title: String, rawAmount: String, editing: Order?) {
        val cents = AmountInput.cents(rawAmount)
        if (title.isBlank() || cents == null || cents <= 0) {
            operationState.value = "error"
            noticeState.value = "Preencha um título e valor positivo válido."
            return
        }
        action {
            saveOrder(title, rawAmount, editing)
            noticeState.value = "Pedido salvo localmente."
        }
    }

    fun resolve(order: Order, remote: Boolean) = action {
        catalog.resolve(order.id, remote)
        synchronization.schedule()
    }

    fun remoteEdit(order: Order) = action {
        noticeState.value =
            if (synchronization.simulateRemoteEdit(order.id))
                "Servidor simulado alterado. Edite o pedido local e sincronize para gerar conflito."
            else "Sincronize este pedido primeiro."
    }

    val work =
        synchronization.work.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "IDLE")

    fun sync() {
        synchronization.schedule(force = true)
        noticeState.value = "Sincronização agendada; depende de conexão e do modo online."
    }

    private fun action(block: suspend () -> Unit) {
        if (operationState.value == "loading") return
        operationState.value = "loading"
        noticeState.value = "Processando…"
        viewModelScope.launch {
            try {
                kotlinx.coroutines.delay(2200)
                block()
                operationState.value = "success"
            } catch (c: CancellationException) {
                operationState.value = "error"
                throw c
            } catch (_: Exception) {
                operationState.value = "error"
                noticeState.value = "Operação não concluída. Tente novamente."
            }
        }
    }
}
