package dev.breda.orders.core
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import kotlin.test.*
class SyncTest {
 private val local = Order("order", "Pedido", 2590)
 private val remote = RemoteOrder("order", "Remoto", 3990, 1)
 @Test fun successfulAckMarksRevisionSynced() {
  val result = reduce(local, local, PushResult.Accepted(remote))
  assertFalse(result.pending); assertEquals(1L, result.serverVersion)
 }
 @Test fun ackDoesNotEraseEditMadeDuringUpload() {
  val edited = local.copy(title = "Alterado", revision = 2)
  val result = reduce(edited, local, PushResult.Accepted(remote))
  assertEquals("Alterado", result.title); assertTrue(result.pending)
  assertEquals(1L, result.syncedRevision); assertEquals(1L, result.serverVersion)
 }
 @Test fun retryPreservesOutbox() { assertEquals(local, reduce(local, local, PushResult.Retry)) }
 @Test fun conflictNeedsExplicitResolution() {
  val result = reduce(local, local, PushResult.Conflict(remote))
  assertFalse(result.pending); assertEquals(remote, result.conflict)
 }
 @Test fun staleConflictDoesNotOverwriteNewEdit() {
  val edited = local.copy(revision = 2)
  assertEquals(edited, reduce(edited, local, PushResult.Conflict(remote)))
 }
 @Test fun keepingLocalRebasesAndChangesRequestKey() {
  val conflict = local.copy(conflict = remote)
  val resolved = keepLocal(conflict)
  assertTrue(resolved.pending); assertEquals(1L, resolved.serverVersion)
  assertEquals(local.title, resolved.title); assertNotEquals(local.requestKey, resolved.requestKey)
 }
 @Test fun acceptingRemoteStopsPendingUpload() {
  val resolved = acceptRemote(local.copy(conflict = remote))
  assertFalse(resolved.pending); assertEquals(remote.title, resolved.title); assertEquals(remote.cents, resolved.cents)
 }
 @Test fun engineContinuesAfterOneNetworkFailure() = runTest {
  val a = local; val b = local.copy(id = "other")
  val results = mutableListOf<PushResult>()
  val store = object : OrderStore {
   override suspend fun pending() = listOf(a,b)
   override suspend fun apply(sent: Order, result: PushResult) { results += result }
  }
  val report = SyncEngine(store, OrderRemote { if(it.id == a.id) throw IllegalStateException("network") else PushResult.Accepted(remote.copy(id = b.id)) }).sync()
  assertEquals(SyncReport(1,0,true),report); assertEquals(2,results.size)
 }
 @Test fun cancellationDoesNotAcknowledgeAnUnfinishedUpload() = runTest {
  var applied = false
  val store = object : OrderStore { override suspend fun pending() = listOf(local); override suspend fun apply(sent: Order,result: PushResult) { applied = true } }
  val engine = SyncEngine(store, OrderRemote { delay(1000); PushResult.Accepted(remote) })
  val work = launch { engine.sync() }; yield(); work.cancelAndJoin()
  assertFalse(applied)
 }
 @Test fun olderAckCannotRollBackServerVersion() {
  val current = local.copy(serverVersion = 5, revision = 3, syncedRevision = 2)
  assertEquals(current, reduce(current,local,PushResult.Accepted(remote)))
 }
}
