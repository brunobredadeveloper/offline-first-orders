# ADR 0001 — Revision-based outbox and explicit conflict resolution
Status: Accepted

Room is the source of truth. An order with revision greater than syncedRevision is an outbox item unless it has an unresolved conflict. Each immutable upload snapshot has an idempotency key composed of order ID and local revision.

Acknowledgments are applied in a Room transaction to the CURRENT row. A later local edit is preserved; only the sent revision is acknowledged. The new server version becomes the baseline for the next upload. Conflicts use optimistic version checking and require explicit Keep Local or Accept Remote action.

The simulated server and accepted-request receipts are persisted in separate Room tables. This allows safe replay after process termination between server acceptance and local acknowledgment. It is a local server stand-in, not an HTTP service or cross-device sync.

WorkManager persists scheduling with network constraints and exponential retry. APPEND_OR_REPLACE avoids losing a request added while a previous sync is running. Manual sync and enabling Online use REPLACE to restart a chain waiting in backoff; durable receipt replay handles a previous upload cancelled during acknowledgment. Initial application startup reschedules sync to recover from a crash between a database write and work enqueue. Scheduling and database write are not one transaction; the next startup is the recovery boundary.

A single engine serializes sync within the app process; unique work serializes workers. Manual sync only schedules work. Server-side idempotency remains necessary for a real implementation. No deletion, multi-device pull, authentication or schema migration is implemented in this increment.

References:
- https://developer.android.com/develop/background-work/background-tasks/persistent/how-to/manage-work
- https://developer.android.com/reference/androidx/room/Transaction
