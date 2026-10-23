[English](README.md) | **Português**

# Desenvolvimento

Como o projeto é organizado, como as dependências são declaradas, como fazer o build e como rodar uma rede local.

## Requisitos

- **JDK 25.** Se não estiver instalado, o Gradle baixa automaticamente no primeiro build.
- **Docker**, para os testes de integração e para o PostgreSQL e o Redis locais. Sem Docker, os testes de integração são pulados, nunca simulados.
- Mais nada. O Gradle wrapper (`gradlew`) baixa sozinho a versão certa do Gradle.

## Build

```bash
./gradlew build
```

No Windows, use `gradlew.bat build`.

Isso compila todos os módulos, roda os testes unitários e de integração, confere as regras de dependência e monta uma pasta por servidor:

```text
build/servers/
├── proxy/plugins/    muxe-core-proxy.jar
├── auth/plugins/     muxe-core-bukkit.jar  muxe-auth.jar
├── lobby/plugins/    muxe-core-bukkit.jar  muxe-lobby.jar
├── skywars/plugins/  muxe-core-bukkit.jar  muxe-skywars.jar
└── bedwars/plugins/  muxe-core-bukkit.jar  muxe-bedwars.jar
```

Cada pasta tem exatamente os jars daquele servidor: copie o conteúdo para a pasta `plugins` do servidor. Os nomes dos jars não têm versão, então um novo build substitui o arquivo antigo. A versão fica dentro de cada `plugin.yml` ou `velocity-plugin.json`.

| Comando | O que faz |
|---|---|
| `./gradlew build` | Tudo: compila, testa, confere as regras e monta todos os servidores. |
| `./gradlew assembleServers` | Monta a pasta de todos os servidores, sem rodar os testes. |
| `./gradlew assembleLobbyServer` | Monta um único servidor. Existe uma task por servidor: `assembleProxyServer`, `assembleAuthServer`, `assembleSkywarsServer`, `assembleBedwarsServer`. |
| `./gradlew :servers:auth:test` | Roda os testes de um único módulo. |

## Módulos

```text
muxe/
├── build-logic/          convenções de build compartilhadas por todos os módulos
├── core/
│   ├── common/           core sem plataforma: pacotes, persistência, métricas, CQRS, contas
│   ├── bukkit/           Muxe-Core, o plugin base de todo servidor Spigot
│   └── proxy/            o plugin do Velocity
├── servers/
│   ├── auth/             Muxe-Auth, o servidor de autenticação
│   └── lobby/            Muxe-Lobby, o lobby principal
└── minigames/
    ├── engine/           partidas, arenas e modos, compartilhados por todos os jogos
    ├── skywars/          Muxe-SkyWars
    └── bedwars/          Muxe-BedWars
```

| Módulo | Tipo | O que contém | Onde vai parar |
|---|---|---|---|
| `core:common` | Biblioteca | Tudo o que não depende do Bukkit nem do Velocity. | Dentro do `muxe-core-bukkit.jar` e do `muxe-core-proxy.jar` |
| `core:bukkit` | Plugin Spigot | Inicia o core em um servidor Spigot e expõe o `MuxeCore` para os outros plugins. | `muxe-core-bukkit.jar` |
| `core:proxy` | Plugin Velocity | Inicia o core no Velocity, roteia os jogadores e protege a autenticação. | `muxe-core-proxy.jar` |
| `servers:auth` | Plugin Spigot | Registro e login. | `muxe-auth.jar` |
| `servers:lobby` | Plugin Spigot | O lobby principal. | `muxe-lobby.jar` |
| `minigames:engine` | Biblioteca que usa a API do Spigot | O [motor de jogo](../architecture/game-engine/README.pt-BR.md). | Dentro do jar de cada jogo |
| `minigames:skywars`, `minigames:bedwars` | Plugin Spigot | Um jogo cada. | `muxe-skywars.jar`, `muxe-bedwars.jar` |

Os pacotes seguem o módulo: `br.com.muxe.core.common`, `br.com.muxe.core.bukkit`, `br.com.muxe.core.proxy`, `br.com.muxe.auth`, `br.com.muxe.lobby`, `br.com.muxe.minigame.engine`, `br.com.muxe.minigame.skywars`...

## Servidores

Quais jars vão para cada servidor é declarado em um só lugar, o [`build.gradle.kts`](../../build.gradle.kts) da raiz:

```kotlin
network {
    server("proxy", projects.core.proxy)
    server("auth", projects.core.bukkit, projects.servers.auth)
    server("lobby", projects.core.bukkit, projects.servers.lobby)
}
```

Adicionar um servidor ao build é uma linha ali. A task `assemble<Name>Server` dele é criada automaticamente.

## Regras de dependência

Cada módulo só pode depender dos grupos abaixo. O build garante essas regras: uma dependência proibida faz o build falhar com uma mensagem dizendo o que é permitido.

| Módulo | Pode depender de |
|---|---|
| `core:common` | nada além dele mesmo |
| `core:bukkit`, `core:proxy` | `core:common` |
| `servers:*` | `core:common`, `core:bukkit` |
| `minigames:engine` | `core:common`, `core:bukkit` |
| `minigames:*` | `core:common`, `core:bukkit`, `minigames:engine` |

O que essas regras garantem:

- **Nada depende de um jogo, de um plugin de servidor ou do proxy.** Eles são o fim da cadeia.
- **Jogos e plugins de servidor nunca dependem uns dos outros.** Remover ou alterar um nunca quebra outro.
- **O `core:common` não sabe nada sobre Bukkit ou Velocity.** Ele é testado sem servidor algum.
- **O core nunca é embutido duas vezes.** Plugins de servidor e de jogo usam o `core:bukkit` com `compileOnly`, já que o Muxe-Core já está no servidor.
- **O motor de jogo é embutido em cada jogo.** Dois jogos nunca rodam no mesmo servidor, então as cópias nunca se encontram.

As regras ficam em [`build-logic/src/main/kotlin/ModuleRules.kt`](../../build-logic/src/main/kotlin/ModuleRules.kt).

## Declarando dependências

- **As versões ficam só no [`gradle/libs.versions.toml`](../../gradle/libs.versions.toml).** Os módulos as referenciam como `libs.<name>`.
- **Os módulos são referenciados com acessores tipados**, como `projects.core.bukkit` e `projects.minigames.engine`.
- **Escolha a configuração pela origem das classes em tempo de execução:**

| Configuração | Quando usar | Embutida no jar do plugin |
|---|---|---|
| `api` | Uma biblioteca expõe os tipos da dependência na própria API pública. | Sim |
| `implementation` | A dependência é usada internamente. | Sim |
| `compileOnly` | O servidor já fornece em tempo de execução: a API do Spigot, a API do Velocity ou o Muxe-Core. | Não |

- **Bibliotecas de terceiros são realocadas** para `br.com.muxe.libs`, para nunca conflitarem com outro plugin. A realocação é aplicada em todo jar de plugin, inclusive nos que só referenciam o Muxe-Core, para que as referências deles apontem para as mesmas classes realocadas. A lista fica em `Relocations`, no [`ServerPlugins.kt`](../../build-logic/src/main/kotlin/ServerPlugins.kt).
- **Os arquivos de serviço são mesclados** (`META-INF/services`), o que o Flyway e o driver do PostgreSQL precisam depois da realocação.
- **O Velocity já fornece Gson e SLF4J**, então o jar do proxy não os embute.
- **As test fixtures** do `core:common` (`DirectTransactions`, `MutableClock`, `InMemoryAccountRepository`, `RecordingTransport`, `TestNetwork`) são compartilhadas com `testImplementation(testFixtures(projects.core.common))`.

## Convenções de build

| Plugin | Para | O que configura |
|---|---|---|
| `muxe.java` | Aplicado por todos os outros | Java 25, UTF-8, avisos do compilador, JUnit, AssertJ, BOM do Testcontainers e as regras de dependência. |
| `muxe.library` | Bibliotecas | Uma biblioteca Java simples. |
| `muxe.bukkit-library` | Bibliotecas que usam a API do Spigot | A API do Spigot em tempo de compilação. |
| `muxe.server-plugin` | Aplicado pelas convenções de plugin | Shadow jar com o nome do módulo, realocações, arquivos de serviço mesclados e o artefato `serverPlugin`. |
| `muxe.bukkit-plugin` | Plugins Spigot | A API do Spigot, realocações exclusivas do Bukkit e o `plugin.yml` com `${version}` preenchido. |
| `muxe.velocity-plugin` | Plugins Velocity | A API do Velocity e o `velocity-plugin.json` com `${version}` preenchido. |
| `muxe.network` | O projeto raiz | O bloco `network { }` e as tasks de montagem dos servidores. |

## Rodando uma rede local

### 1. Suba o PostgreSQL e o Redis

```bash
cp .env.example .env
docker compose up -d
```

Edite o `.env` antes e defina as suas senhas e um segredo de mensageria com pelo menos 32 caracteres. Os dois serviços escutam apenas em `127.0.0.1`.

### 2. Exporte as mesmas variáveis para os servidores

Todo servidor lê os segredos do ambiente (valores `env:` no `config.yml`):

```bash
export MUXE_DATABASE_PASSWORD=...
export MUXE_REDIS_PASSWORD=...
export MUXE_MESSAGING_SECRET=...
```

### 3. Prepare os servidores

| Pasta | Software | `config.yml` do Muxe-Core ou do proxy |
|---|---|---|
| `proxy` | Velocity 4.3 | `server.id: proxy-1`, `kind: PROXY` |
| `auth` | Um fork do Spigot 1.8.8 com Java 25 | `server.id: auth-1`, `group: auth`, `kind: AUTH` |
| `lobby` | Um fork do Spigot 1.8.8 com Java 25 | `server.id: lobby-1`, `group: lobby`, `kind: LOBBY` |
| `skywars` | Um fork do Spigot 1.8.8 com Java 25 | `server.id: skywars-1`, `group: skywars`, `kind: MINIGAME` |

- O `server.id` precisa ser igual ao nome do servidor no `velocity.toml`.
- Dê a cada servidor uma `metrics.port` diferente, ou desative as métricas.
- No `velocity.toml`: `player-info-forwarding-mode = "legacy"` (servidores 1.8 não suportam o modern forwarding) e `online-mode = false`. Jogadores premium são colocados em modo online individualmente pela Muxe, como descrito em [Autenticação](../features/authentication/README.pt-BR.md).
- Em cada `spigot.yml`: `settings.bungeecord: true`. No servidor de autenticação, defina também `commands.log: false`, para que as senhas nunca cheguem ao console.
- Copie `build/servers/<name>/plugins/*` para a pasta `plugins` de cada servidor.

### 4. Inicie tudo

Inicie os servidores Spigot e depois o Velocity. Cada servidor registra `Muxe-Core ready as <id>` no log, e `/muxe servers` lista todos os servidores ativos.
