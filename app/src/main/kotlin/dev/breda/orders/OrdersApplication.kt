package dev.breda.orders
import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.work.*
import dev.breda.orders.core.SyncEngine
import java.util.concurrent.TimeUnit

class OrdersApplication : Application() {
 lateinit var repository: OrdersRepository; private set
 lateinit var remote: RoomSimulatedRemote; private set
 lateinit var engine: SyncEngine; private set
 private val preferences by lazy { getSharedPreferences("demo",Context.MODE_PRIVATE) }
 var online: Boolean
  get() = preferences.getBoolean("online",false)
  set(value) { preferences.edit().putBoolean("online",value).apply() }
 override fun onCreate() {
  super.onCreate()
  val db = Room.databaseBuilder(this,OrdersDatabase::class.java,"orders.db").build()
  repository = OrdersRepository(db)
  remote = RoomSimulatedRemote(db) { online }
  engine = SyncEngine(repository,remote)
  scheduleSync()
 }
 fun scheduleSync(force: Boolean = false) {
  val request = OneTimeWorkRequestBuilder<OrdersSyncWorker>()
   .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
   .setBackoffCriteria(BackoffPolicy.EXPONENTIAL,10,TimeUnit.SECONDS).build()
  WorkManager.getInstance(this).enqueueUniqueWork("orders-sync",if(force) ExistingWorkPolicy.REPLACE else ExistingWorkPolicy.APPEND_OR_REPLACE,request)
 }
}
class OrdersSyncWorker(context: Context, parameters: WorkerParameters) : CoroutineWorker(context,parameters) {
 override suspend fun doWork(): Result {
  val app = applicationContext as OrdersApplication
  if (!app.online) return Result.retry()
  val report = app.engine.sync()
  return if(report.retryRequired) Result.retry() else Result.success()
 }
}
