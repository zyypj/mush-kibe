[English](AGENTS.md) | **Português**

# Agentes de IA

Regras para agentes de IA que trabalham neste repositório. Tudo o que está no [CONTRIBUTING.pt-BR.md](CONTRIBUTING.pt-BR.md) também vale para os agentes; este arquivo acrescenta o que é específico deles. Quando uma regra daqui contradiz um hábito ou padrão genérico, vale este arquivo.

Os agentes leem a versão em inglês, o [AGENTS.md](AGENTS.md). As duas versões dizem a mesma coisa e são atualizadas juntas.

## Orientação

Leia antes de alterar código, nesta ordem:

1. [Visão geral da arquitetura](docs/architecture/overview/README.pt-BR.md): camadas, CQRS, organização de pacotes.
2. [Convenções](docs/development/conventions/README.pt-BR.md): estilo de Java, nomes, pacotes, configuração e mensagens.
3. [Desenvolvimento](docs/development/README.pt-BR.md): módulos, regras de dependência e comandos.
4. O documento de arquitetura da área que você vai mexer ([mensageria](docs/architecture/messaging/README.pt-BR.md), [persistência](docs/architecture/persistence/README.pt-BR.md), [observabilidade](docs/architecture/observability/README.pt-BR.md), [segurança](docs/architecture/security/README.pt-BR.md), [motor de jogo](docs/architecture/game-engine/README.pt-BR.md)).
5. A documentação das features em [docs/features/](docs/features/).

## Legal

- Nunca adicione código, assets, mapas, nomes ou textos copiados de outra rede, descompilados de um plugin ou tirados de um vazamento. Veja a [Política legal e de sala limpa](docs/legal/README.pt-BR.md).
- Nunca use o nome de outra rede em código, configuração ou textos para jogadores. O projeto se chama **Muxe**.

## Build

- Rode `./gradlew build` antes de dar uma mudança como pronta. Ele compila todos os módulos, roda os testes e confere as regras de dependência.
- Nunca escreva uma versão no build de um módulo. As versões ficam só no `gradle/libs.versions.toml`.
- Respeite as regras de dependência entre módulos em `build-logic/src/main/kotlin/ModuleRules.kt`. Nunca enfraqueça uma regra para uma dependência compilar.
- Bibliotecas de terceiros são embutidas e realocadas para `br.com.muxe.libs`. Uma nova biblioteca embutida em um plugin precisa ser adicionada em `Relocations`, no `build-logic/src/main/kotlin/ServerPlugins.kt`.

## Código

- Java 25. Records, tipos selados, pattern matching e virtual threads são bem-vindos.
- A API do Bukkit é a **1.8.8**. Nunca chame APIs que não existem na 1.8.8 (nada de Adventure, `PersistentDataContainer` ou `api-version`).
- Comentários são proibidos, exceto `TODO` e `FIXME` com o motivo e a issue, quando houver. Nada de Javadoc, comentário de bloco ou código comentado. Deixe que os nomes expliquem.
- Nunca escreva nomes de classe totalmente qualificados no meio do código. Sempre importe.
- Mantenha as classes pequenas e em um sub-pacote que diga o seu papel (`domain`, `application/command`, `application/query`, `infrastructure/postgres`, `listener`, `command`, `bootstrap`...). Nunca junte classes sem relação em um mesmo pacote.
- As classes de entrada dos plugins ficam enxutas: elas criam um `*Bootstrap` que monta tudo com injeção explícita pelo construtor. Nada de framework de injeção de dependência nem singletons estáticos além do `MuxeCore.get()`.
- As regras de negócio ficam em classes Java simples, que podem ser testadas sem servidor. As classes do Bukkit e do Velocity só adaptam eventos e comandos para elas.
- Mudanças de estado passam por um `Command` e seu handler; leituras passam por uma `Query`. Falhas de negócio esperadas retornam `Result.failure(...)` com um código de problema; exceções são para falhas de infraestrutura.
- Nunca bloqueie a thread principal. Trabalho com banco e Redis roda no executor assíncrono do core; tudo o que mexe no Bukkit volta pelo `MuxeCore.mainThread()`.
- Nunca acesse o Redis diretamente a partir de uma feature. Use o `PacketBus` para mensagens e o `MuxeCore.store()` para valores de curta duração.
- Utilitários reutilizáveis ficam no core, não em uma feature. Antes de escrever algo genérico (títulos, action bars, efeitos, itens, menus), confira a [API do core](docs/development/core-api/README.pt-BR.md); se faltar, adicione-o ao `core:bukkit` ou ao `core:common`, teste-o e documente-o lá.
- Os textos para jogadores ficam no `config.yml` de cada plugin, em `messages`, em português, com códigos de cor `&`.
- O YAML usa o estilo de bloco expandido: nada de mapas nem listas inline.

## Testes

- Toda mudança de comportamento vem com testes que a cobrem.
- Uma correção de bug começa com um teste que reproduz o bug e falha antes da correção.
- Teste a regra de negócio com testes unitários rápidos usando os fakes de `core/common/src/testFixtures` (`DirectTransactions`, `MutableClock`, `InMemoryAccountRepository`, `TestNetwork`).
- Comportamentos que dependem do PostgreSQL ou do Redis são cobertos por testes de integração com Testcontainers (`@Testcontainers(disabledWithoutDocker = true)`).
- Os testes são determinísticos: injete `Clock` e nunca durma para esperar o tempo real, a não ser que o teste seja sobre uma ida e volta real pela rede.
- Nunca apague, pule, desative ou enfraqueça um teste ou uma asserção para ele passar.
- Rode os testes que cobrem a mudança antes de dar a tarefa como pronta, e informe os comandos e os resultados. Nunca diga que uma validação passou sem tê-la rodado.

## Commits e pull requests

- Conventional Commits em inglês, uma mudança lógica por commit, como no [CONTRIBUTING.pt-BR.md](CONTRIBUTING.pt-BR.md#commits).
- Crie as branches de trabalho a partir da `development` e abra os pull requests para a `development`. Nunca faça commit direto na `production` ou na `homologation`.
- Nunca faça force-push, nunca reescreva histórico já enviado e nunca pule hooks.
- Preencha todas as seções do [modelo de pull request](.github/pull_request_template.md). **Validations** lista só as verificações que de fato rodaram.

## Documentação

- Toda mudança de documentação é feita em inglês e em português, na mesma mudança.
- Antes de terminar uma tarefa, confira se alguma documentação descreve algo que você mudou (features, pacotes, métricas, configuração, comandos, permissões) e atualize-a.
- Um novo tipo de pacote, métrica, comando, permissão ou chave de configuração é documentado na feature dona dele.
