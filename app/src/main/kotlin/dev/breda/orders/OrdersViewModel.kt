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
 private val namespace=app.namespace
 private val bundle=app.bundle(namespace)
 val orders = bundle.repository.orders.stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),emptyList())
 private val onlineState = MutableStateFlow(bundle.online)
 val online = onlineState.asStateFlow()
 private val operationState=MutableStateFlow("idle")
 val operation=operationState.asStateFlow()
 private val noticeState = MutableStateFlow("")
 val notice = noticeState.asStateFlow()
 fun setOnline(value: Boolean) { bundle.online = value; onlineState.value = value; app.scheduleSync(force = value,namespace=namespace) }
 fun save(title: String, rawAmount: String, editing: Order?) {
  val cents = runCatching { BigDecimal(rawAmount.trim().replace(',', '.')).movePointRight(2).longValueExact() }.getOrNull()
  if(title.isBlank() || cents == null || cents <= 0) { noticeState.value = "Preencha um título e valor positivo válido."; return }
  action {
   if(editing == null) bundle.repository.create(title.trim(),cents) else bundle.repository.edit(editing.id,title.trim(),cents)
   noticeState.value = "Pedido salvo localmente."; app.scheduleSync(namespace=namespace)
  }
 }
 fun resolve(order: Order, remote: Boolean) = action { bundle.repository.resolve(order.id,remote);app.scheduleSync(namespace=namespace) }
 fun remoteEdit(order: Order) = action { noticeState.value = if(bundle.remote.simulateRemoteEdit(order.id)) "Servidor simulado alterado. Edite o pedido local e sincronize para gerar conflito." else "Sincronize este pedido primeiro." }
 val work=androidx.work.WorkManager.getInstance(application).getWorkInfosForUniqueWorkFlow("orders-sync-$namespace").map {it.lastOrNull()?.state?.name ?: "IDLE"}.stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),"IDLE")
 fun sync() { app.scheduleSync(force = true,namespace=namespace);noticeState.value = "Sincronização agendada; depende de conexão e do modo online." }
 private fun action(block: suspend () -> Unit) { if(operationState.value=="loading")return;operationState.value="loading";noticeState.value="Processando…";viewModelScope.launch { try { block();operationState.value="success" } catch(c: CancellationException) { operationState.value="error";throw c } catch(_: Exception) { operationState.value="error";noticeState.value = "Operação não concluída. Tente novamente." } } }
}
