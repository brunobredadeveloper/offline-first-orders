package dev.breda.orders
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.breda.orders.core.Order
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale
private fun money(cents: Long) = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR")).format(BigDecimal.valueOf(cents,2))
class MainActivity : ComponentActivity() {
 override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState);setContent { MaterialTheme(colorScheme = darkColorScheme(primary = Color(0xFFFFC77D))) { Surface(Modifier.fillMaxSize()) { OrdersScreen() } } } }
}
@Composable
fun OrdersScreen(vm: OrdersViewModel = viewModel()) {
 val orders by vm.orders.collectAsStateWithLifecycle();val online by vm.online.collectAsStateWithLifecycle();val notice by vm.notice.collectAsStateWithLifecycle()
 var title by rememberSaveable { mutableStateOf("") };var amount by rememberSaveable { mutableStateOf("25,90") };var editingId by rememberSaveable { mutableStateOf<String?>(null) }
 val editing = orders.find { it.id == editingId }
 LazyColumn(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(24.dp),verticalArrangement = Arrangement.spacedBy(12.dp)) {
  item { Text("OFFLINE ORDERS",style = MaterialTheme.typography.labelLarge);Text("Venda agora. Sincronize depois.",style = MaterialTheme.typography.headlineMedium) }
  item { Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) { Text(if(online) "Servidor simulado online" else "Modo offline");Switch(online,vm::setOnline) } }
  item { OutlinedTextField(title,{ title = it },label = { Text("Título do pedido") },modifier = Modifier.fillMaxWidth()) }
  item { OutlinedTextField(amount,{ amount = it },label = { Text("Valor em reais") },modifier = Modifier.fillMaxWidth(),singleLine = true) }
  item { Button(onClick = { vm.save(title,amount,editing) },enabled = editing?.conflict == null) { Text(if(editing == null) "Criar pedido" else "Salvar alteração") };if(editingId != null) TextButton(onClick = { editingId = null;title = "" }) { Text("Encerrar edição") } }
  item { OutlinedButton(onClick = vm::sync) { Text("Agendar sincronização") };Text(notice) }
  items(orders,key = { it.id }) { order -> Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp),verticalArrangement = Arrangement.spacedBy(8.dp)) {
   Text(order.title,style = MaterialTheme.typography.titleMedium);Text(money(order.cents))
   Text(when { order.conflict != null -> "Conflito · escolha uma versão";order.pending -> "Pendente de sincronização";else -> "Sincronizado · v${order.serverVersion}" })
   if(order.conflict != null) {
    Text("Remoto: ${order.conflict!!.title} · ${money(order.conflict!!.cents)}")
    TextButton(onClick = { vm.resolve(order,false) }) { Text("Manter minha versão") }
    TextButton(onClick = { vm.resolve(order,true) }) { Text("Usar versão remota") }
   } else {
    TextButton(onClick = { editingId = order.id;title = order.title;amount = BigDecimal.valueOf(order.cents,2).toPlainString() }) { Text("Editar") }
    TextButton(onClick = { vm.remoteEdit(order) }) { Text("Simular alteração no servidor") }
   }
  } } }
  item { Text("Room persiste pedidos e servidor fictício no aparelho. WorkManager executa a fila com conexão. Nenhum servidor externo é utilizado.",style = MaterialTheme.typography.bodySmall) }
 }
}
