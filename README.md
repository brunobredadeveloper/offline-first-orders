# Offline First Orders

An Android portfolio project by **Bruno Breda**: create and edit orders offline, persist them with Room, and synchronize through WorkManager against a durable local simulated server.

## Implemented
- Compose UI, ViewModel and lifecycle-aware Flow collection.
- Room order storage and a revision-based outbox.
- WorkManager unique work, connected-network constraint and exponential retries.
- Optimistic server versions, stable per-revision request keys and persisted accepted-request receipts.
- Explicit conflict resolution: keep local or accept remote.
- Pure Kotlin synchronization rules with JVM tests.

## Run
Requires JDK 17 and Android SDK 35; minimum device API 26.
```sh
./gradlew :core:test
./gradlew -PincludeAndroid=true :app:lintDebug :app:assembleDebug
```
Use `gradlew.bat` on Windows. For Android Studio, add `includeAndroid=true` to your local Gradle user properties before syncing. The Android module is optional so core tests can run without an SDK.

## Demo
1. Leave Offline mode enabled; create a few orders.
2. Restart the app: orders remain stored.
3. Enable the simulated server and use a connected device; schedule sync and wait for WorkManager.
4. After an order is synced, select Simulate Server Edit.
5. Edit that order locally and save; sync detects the version conflict.
6. Choose Keep My Version to rebase and upload, or Use Remote Version to accept it.

The mode switch controls the local server stand-in. WorkManager also requires actual connectivity. Work execution is scheduled and is not guaranteed to start immediately.

## Modules
| Module | Responsibility |
| --- | --- |
| `core` | Immutable models, sync orchestration and pure reconciliation rules |
| `app` | Room adapters, durable simulated server, WorkManager and Compose UI |

[ADR 0001](docs/adr/0001-outbox-and-conflicts.md) explains race handling and recovery boundaries.

## Limits and roadmap
The simulated server is stored on the same device, not accessed through HTTP. No multi-device pull, deletion, login or migrations yet. Next steps: HTTP fake service, DAO/Worker instrumentation tests, accessibility checks and screenshots after device validation.

## Validation
Core compiled with Kotlin 2.1.20 and Coroutines 1.10.1; **10 JUnit tests passed** via the JUnit Platform runner. See `docs/validation.md`. Android build, generated Room schemas, instrumented database behavior and device UI remain unverified. CI is configured for JVM tests, lint and debug APK generation.
