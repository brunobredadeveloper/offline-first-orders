# Evidências e distribuição — Field Orders

Consulta realizada em **14/10/2026**. Esta página separa o que existe no código, o que foi executado e o que ainda precisa de validação física.

## Execução consultada

- [GitHub Actions #38172947996](https://github.com/brunobredadeveloper/offline-first-orders/actions/runs/38172947996)
- Revisão executada: `2bc73be315ea84309e7a276dfd0f37ce8d290789`.
- Workflow: **Tests and APK**.
- Conclusão retornada pelo GitHub: **success**.

Alterações documentais posteriores não equivalem a uma nova compilação do aplicativo.

## Artefatos encontrados

- `offline-first-orders-ux-evidence` — ID `11714887251`; disponível na consulta; sujeito à expiração pelo GitHub.
- `offline-first-orders-reports` — ID `11714502525`; disponível na consulta; sujeito à expiração pelo GitHub.
- `offline-first-orders-debug-apk` — ID `11714432702`; disponível na consulta; sujeito à expiração pelo GitHub.

Para baixar: abra a execução, vá a **Artifacts**, baixe o ZIP e extraia o conteúdo. O GitHub pode exigir login. APK é para Android; bundle JavaScript, relatório e ZIP de fontes não são instaladores. Builds iOS de simulador ou sem assinatura não instalam diretamente no iPhone.

## Capturas incorporadas

- [03-home.png](imagens/03-home.png) — artefato de testes de interface do aplicativo.
- [05-success.png](imagens/05-success.png) — artefato de testes de interface do aplicativo.
- [07-history.png](imagens/07-history.png) — artefato de testes de interface do aplicativo.

Origem: https://github.com/brunobredadeveloper/offline-first-orders/actions/runs/38172947996. Os dados exibidos são de demonstração. As capturas foram mantidas sem retoques.

## Avaliação física pendente

Testes automatizados e compilação não substituem avaliação de acessibilidade, interrupções, recursos do aparelho e instalação/distribuição pelo titular. O remoto é um simulador persistente no próprio aparelho; não há servidor de pedidos conectado. Firebase opcional autentica, mas não transforma o simulador em integração com ERP. Não há cobrança financeira real.

[Fluxos e checklist](fluxos.md) · [README](../README.md)
