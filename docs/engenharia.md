# Engenharia — Field Orders

## Problema técnico

Demonstrar a criação e edição local de pedidos, retentativas e controle de versões sem perder uma edição mais recente durante a sincronização.

## Responsabilidades e código

| Fronteira | Responsabilidade real | Onde inspecionar |
|---|---|---|
| Núcleo | SyncEngine, reduce e regras de conflito | [Sync.kt](../core/src/main/kotlin/dev/breda/orders/core/Sync.kt) |
| Persistência | Room e adapter de pedidos | [OrdersDatabase.kt](../app/src/main/kotlin/dev/breda/orders/OrdersDatabase.kt) |
| Aplicação | SaveOrder e ports de catálogo/sync | [OrderActions.kt](../app/src/main/kotlin/dev/breda/orders/OrderActions.kt) |
| Apresentação | OrdersViewModel e composição | [OrdersViewModel.kt](../app/src/main/kotlin/dev/breda/orders/OrdersViewModel.kt) |

## Invariantes e decisões

- Um ACK antigo não pode sobrescrever uma edição local posterior.
- Retentativa não deve duplicar a mesma revisão de pedido.
- Troca de conta não pode reaproveitar uma tarefa de outra sessão.

## Como examinar os testes

Os arquivos abaixo fazem parte do código versionado. A existência de uma suíte não comprova sua aprovação; o resultado remoto está em [evidências](evidencias.md).

- [app/src/androidTest/kotlin/dev/breda/orders/ExperienceJourneyTest.kt](../app/src/androidTest/kotlin/dev/breda/orders/ExperienceJourneyTest.kt)
- [app/src/test/kotlin/dev/breda/orders/ArchitectureContractTest.kt](../app/src/test/kotlin/dev/breda/orders/ArchitectureContractTest.kt)
- [app/src/test/kotlin/dev/breda/orders/SaveOrderTest.kt](../app/src/test/kotlin/dev/breda/orders/SaveOrderTest.kt)
- [app/src/test/kotlin/dev/breda/orders/SessionIsolationTest.kt](../app/src/test/kotlin/dev/breda/orders/SessionIsolationTest.kt)
- [core/src/test/kotlin/dev/breda/orders/core/SyncTest.kt](../core/src/test/kotlin/dev/breda/orders/core/SyncTest.kt)

## Limites e compromissos

O remoto é um simulador persistente no próprio aparelho; não há servidor de pedidos conectado. Firebase opcional autentica, mas não transforma o simulador em integração com ERP. Não há cobrança financeira real.

A separação de responsabilidades documentada é a encontrada no código. Não se afirma certificação SOLID, paridade com produtos comerciais, auditoria de segurança ou operação em produção.

## Reproduzir a validação

```sh
./gradlew -PincludeAndroid=true :core:test :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

[Voltar ao README](../README.md) · [Fluxos](fluxos.md)
