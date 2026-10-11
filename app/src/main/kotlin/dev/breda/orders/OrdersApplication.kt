package dev.breda.orders
import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.work.*
import dev.breda.orders.core.SyncEngine
import java.util.concurrent.TimeUnit
import java.util.concurrent.ConcurrentHashMap
class OrdersSession(val repository:OrdersRepository,val remote:RoomSimulatedRemote,val engine:SyncEngine,val preferences:android.content.SharedPreferences){var online:Boolean get()=preferences.getBoolean("online",false);set(value){preferences.edit().putBoolean("online",value).apply()}}
class OrdersApplication:Application(){
 private val sessions=ConcurrentHashMap<String,OrdersSession>()
 val namespace get()=getSharedPreferences("portfolio-account",0).getString("namespace",sessionNamespace("local-demo"))!!
 fun bundle(namespace:String):OrdersSession {require(namespace.matches(Regex("[a-f0-9]{64}")));return sessions.computeIfAbsent(namespace){
  val db=Room.databaseBuilder(this,OrdersDatabase::class.java,"orders-$namespace.db").build();val prefs=getSharedPreferences("orders-$namespace",Context.MODE_PRIVATE)
  val repository=OrdersRepository(db);val remote=RoomSimulatedRemote(db){prefs.getBoolean("online",false)};OrdersSession(repository,remote,SyncEngine(repository,remote),prefs)
 }}
 fun scheduleSync(force:Boolean=false,namespace:String=this.namespace){
  val request=OneTimeWorkRequestBuilder<OrdersSyncWorker>().setInputData(workDataOf("namespace" to namespace)).setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()).setBackoffCriteria(BackoffPolicy.EXPONENTIAL,10,TimeUnit.SECONDS).build()
  WorkManager.getInstance(this).enqueueUniqueWork("orders-sync-$namespace",if(force)ExistingWorkPolicy.REPLACE else ExistingWorkPolicy.APPEND_OR_REPLACE,request)
 }
}
class OrdersSyncWorker(context:Context,parameters:WorkerParameters):CoroutineWorker(context,parameters){override suspend fun doWork():Result {
 val namespace=inputData.getString("namespace") ?: return Result.failure();val app=applicationContext as OrdersApplication
 if(app.namespace!=namespace)return Result.success() // do not run another account's queue while signed in elsewhere
 val bundle=app.bundle(namespace);if(!bundle.online)return Result.retry();val report=bundle.engine.sync();return if(report.retryRequired)Result.retry() else Result.success()
}}
