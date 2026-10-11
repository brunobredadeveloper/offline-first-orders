package dev.breda.orders

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.map

/** A composition root owns adapter construction; ViewModels receive abstractions. */
class InjectedViewModelFactory<T : ViewModel>(private val create: () -> T) :
    ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <V : ViewModel> create(modelClass: Class<V>): V = create() as V
}

@Composable
fun accountViewModel(): AccountSession {
    val context = LocalContext.current.applicationContext
    val factory =
        remember(context) {
            InjectedViewModelFactory {
                AccountSession(FirebaseAuthentication(context), PreferenceSessionStore(context))
            }
        }
    return viewModel(factory = factory)
}

class WorkManagerOrderSync(
    private val application: OrdersApplication,
    private val namespace: String,
) : OrderSync {
    private val session = application.bundle(namespace)
    override var online: Boolean
        get() = session.online
        set(value) {
            session.online = value
        }

    override val work =
        androidx.work.WorkManager.getInstance(application)
            .getWorkInfosForUniqueWorkFlow("orders-sync-$namespace")
            .map { it.lastOrNull()?.state?.name ?: "IDLE" }

    override fun schedule(force: Boolean) {
        application.scheduleSync(force, namespace)
    }

    override suspend fun simulateRemoteEdit(id: String) = session.remote.simulateRemoteEdit(id)
}

@Composable
fun ordersViewModel(uid: String?): OrdersViewModel {
    val application = LocalContext.current.applicationContext as OrdersApplication
    val factory =
        remember(application, uid) {
            InjectedViewModelFactory {
                val namespace = sessionNamespace(uid ?: "local-demo")
                val catalog = application.bundle(namespace).repository
                val sync = WorkManagerOrderSync(application, namespace)
                OrdersViewModel(catalog, sync, SaveOrder(catalog, sync))
            }
        }
    return viewModel(key = "orders-$uid", factory = factory)
}
