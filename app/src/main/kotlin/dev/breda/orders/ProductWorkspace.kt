package dev.breda.orders

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale
private fun money(cents:Long)=NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR")).format(BigDecimal.valueOf(cents,2))

@Composable fun ProductWorkspace(account:AccountSession){
 val vm:OrdersViewModel=viewModel(key="orders-${account.session}");val orders by vm.orders.collectAsStateWithLifecycle();val online by vm.online.collectAsStateWithLifecycle();val notice by vm.notice.collectAsStateWithLifecycle();val work by vm.work.collectAsStateWithLifecycle()
 val operation by vm.operation.collectAsStateWithLifecycle()
 var selected by rememberSaveable {mutableStateOf("")};var title by rememberSaveable {mutableStateOf("")};var amount by rememberSaveable {mutableStateOf("25,90")};var filter by rememberSaveable {mutableStateOf("Todos")};var search by rememberSaveable {mutableStateOf("")};var creating by rememberSaveable {mutableStateOf(true)}
 Workspace(account,listOf("Painel","Pedidos","Sync","Conta")){route,go->
  when(route){
   "home"->Page("Operação em campo"){Hero(if(online)"ONLINE" else "OFFLINE-FIRST","Seu negócio continua","Cada pedido é salvo no aparelho antes de entrar na fila.");Row(horizontalArrangement=Arrangement.spacedBy(12.dp)){Metric("Pedidos",orders.size.toString(),Modifier.weight(1f));Metric("Volume",money(orders.sumOf {it.cents}),Modifier.weight(1f))};Row(horizontalArrangement=Arrangement.spacedBy(12.dp)){Metric("Pendentes",orders.count {it.pending}.toString(),Modifier.weight(1f));Metric("Conflitos",orders.count {it.conflict!=null}.toString(),Modifier.weight(1f))};ActionTile("Novo pedido","Crie agora. Sincronize quando puder.",Icons.Default.AddShoppingCart){creating=true;title="";amount="25,90";go("operation")};ActionTile("Central de sincronização","Worker: $work",Icons.Default.Sync){go("tools")};Text("Persistência Room separada por conta · servidor simulado no aparelho.")}
   "operation"->Page(if(creating)"Criar pedido" else "Editar pedido",{go("activity")}){OutlinedTextField(title,{title=it},label={Text("Descrição do pedido")},modifier=Modifier.fillMaxWidth());OutlinedTextField(amount,{amount=it},label={Text("Valor em reais")},modifier=Modifier.fillMaxWidth());Text("${if(online)"Online" else "Offline"}: o pedido será salvo localmente.");Button(onClick={vm.save(title,amount,if(creating)null else orders.find {it.id==selected});go("result")},modifier=Modifier.fillMaxWidth()){Text("Salvar pedido")}}
   "result"->Page("Pedido salvo",{go("activity")}){StatusPanel(operation.takeIf {it!="idle"} ?: "loading","Resultado da operação",notice.ifBlank {"Salvando no banco local…"});Button(onClick={go("activity")},modifier=Modifier.fillMaxWidth()){Text("Acompanhar pedidos")}}
   "activity"->Page("Seus pedidos"){OutlinedTextField(search,{search=it},label={Text("Buscar pedido")},modifier=Modifier.fillMaxWidth());Row{listOf("Todos","Pendentes","Conflitos").forEach {f->FilterChip(selected=filter==f,onClick={filter=f},label={Text(f)})}};val visible=orders.filter {it.title.contains(search,true)&&(filter=="Todos"||(filter=="Pendentes"&&it.pending)||(filter=="Conflitos"&&it.conflict!=null))};if(visible.isEmpty())Text("Nenhum pedido neste filtro.");visible.forEach {order->ActionTile(order.title,"${money(order.cents)} · "+when{order.conflict!=null->"Conflito";order.pending->"Na fila";else->"Sincronizado"},Icons.Default.Inventory2){selected=order.id;go("details")}}}
   "details"->Page("Detalhes do pedido",{go("activity")}){val order=orders.find {it.id==selected};if(order!=null){Hero("ORDER",order.title,money(order.cents));Text("Revisão local: ${order.revision} · sincronizada: ${order.syncedRevision}
Versão do servidor: ${order.serverVersion}");Text("ID: ${order.id}");if(order.conflict!=null){val remote=order.conflict!!;StatusPanel("error","Conflito de versões","Local: ${order.title}
Remoto: ${remote.title} · ${money(remote.cents)}");Button(onClick={vm.resolve(order,false)}){Text("Manter minha versão")};OutlinedButton(onClick={vm.resolve(order,true)}){Text("Usar versão remota")}}else {Button(onClick={creating=false;title=order.title;amount=BigDecimal.valueOf(order.cents,2).toPlainString();go("operation")}){Text("Editar pedido")};OutlinedButton(onClick={vm.remoteEdit(order)}){Text("Simular alteração remota")}};if(notice.isNotBlank())Text(notice)}}
   "tools"->Page("Sync Control Center"){Hero("OUTBOX",work,"A fila usa conexão de rede, retries e backoff exponencial.");Row{Switch(online,vm::setOnline);Text("Servidor simulado online")};if(work=="RUNNING")MotionState("loading");Text("${orders.count {it.pending}} operações aguardando · ${orders.count {it.conflict!=null}} conflitos");Button(vm::sync,Modifier.fillMaxWidth()){Text("Sincronizar agora")};Text(notice);Text("Roteiro de resiliência",style=MaterialTheme.typography.titleLarge);Text("1. Crie um pedido offline.
2. Ative o servidor e sincronize.
3. Simule edição remota nos detalhes.
4. Edite localmente e sincronize.
5. Compare as versões e resolva o conflito.");ActionTile("Ver fila de pedidos","Filtrar pendências",Icons.Default.List){filter="Pendentes";go("activity")}}
  }
 }
}
