# Roteiro da experiência avançada

Desative o servidor, crie pedido, feche e reabra. Ative e sincronize. Simule edição remota nos detalhes, edite localmente e sincronize para comparar versões. Resolva escolhendo uma delas. Troque de conta e confira isolamento.

## Autenticação e estados

- Inicie sem configuração e confirme identificação de demo local.
- Com Firebase configurado: cadastre conta, saia, teste senha incorreta, entre, recupere senha e envie verificação.
- Reinicie para verificar restauração da sessão.
- Teste sem rede e confira mensagem de erro e liberação do formulário.
- Teste voltar, rotação, texto grande, teclado aberto, carregamento e navegação pelas quatro abas.
- Troque de usuário e confirme ausência dos dados do anterior.

## Limites

Room e preferências têm namespace por usuário. WorkManager recebe a conta na entrada e ignora tarefas de outra sessão; fila com constraints/retries/backoff. Login agenda retomada da fila. O servidor remoto é um simulador persistente no aparelho, não um serviço externo.

Firebase e interação visual aguardam teste manual com configuração pessoal e celular. Não existe certificação para aceitação de cartões, pagamento real ou armazenamento de dados bancários.
