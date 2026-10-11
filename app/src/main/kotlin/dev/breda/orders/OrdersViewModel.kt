package dev.breda.orders
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.breda.orders.core.Order
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.math.BigDecimal

class OrdersViewModel(application: Application) : AndroidViewModel(application) {
 private val app = application as OrdersApplication
 val orders = app.repository.orders.stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),emptyList())
 private val onlineState = MutableStateFlow(app.online)
 val online = onlineState.asStateFlow()
 private val noticeState = MutableStateFlow("")
 val notice = noticeState.asStateFlow()
 fun setOnline(value: Boolean) { app.online = value; onlineState.value = value; app.scheduleSync(force = value) }
 fun save(title: String, rawAmount: String, editing: Order?) {
  val cents = runCatching { BigDecimal(rawAmount.trim().replace(',', '.')).movePointRight(2).longValueExact() }.getOrNull()
  if(title.isBlank() || cents == null || cents <= 0) { noticeState.value = "Preencha um título e valor positivo válido."; return }
  action {
   if(editing == null) app.repository.create(title.trim(),cents) else app.repository.edit(editing.id,title.trim(),cents)
   noticeState.value = "Pedido salvo localmente."; app.scheduleSync()
  }
 }
 fun resolve(order: Order, remote: Boolean) = action { app.repository.resolve(order.id,remote);app.scheduleSync() }
 fun remoteEdit(order: Order) = action { noticeState.value = if(app.remote.simulateRemoteEdit(order.id)) "Servidor simulado alterado. Edite o pedido local e sincronize para gerar conflito." else "Sincronize este pedido primeiro." }
 fun sync() { app.scheduleSync(force = true);noticeState.value = "Sincronização agendada; depende de conexão e do modo online." }
 private fun action(block: suspend () -> Unit) { viewModelScope.launch { try { block() } catch(c: CancellationException) { throw c } catch(_: Exception) { noticeState.value = "Operação não concluída. Tente novamente." } } }
}
