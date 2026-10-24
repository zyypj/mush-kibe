[English](README.md) | **Português**

# Convenções

Como código, configuração e mensagens são escritos na Muxe. O objetivo é que qualquer arquivo pareça ter sido escrito pela mesma pessoa.

## Java

- **Java 25.** Use records para valores e mensagens, interfaces seladas para hierarquias fechadas, `switch` com patterns, `var` só quando o tipo for óbvio e virtual threads por meio dos executores do core.
- **API do Bukkit 1.8.8.** Os servidores rodam um fork da 1.8.8. Nunca chame APIs que não existem no Spigot 1.8.8.
- **Sem comentários.** Nada de Javadoc, comentário de bloco ou de linha, nem código comentado. `TODO` e `FIXME` são as únicas exceções, com o motivo e a issue. Um trecho que parece precisar de comentário vira um método ou uma variável com um bom nome.
- **Sem nomes totalmente qualificados no meio do código.** Sempre importe, exceto para diferenciar duas classes com o mesmo nome simples em um arquivo.
- **Injeção pelo construtor, montagem explícita.** Cada plugin tem um `*Bootstrap` que monta o seu grafo de objetos. Nada de framework de injeção de dependência nem estado estático além do `MuxeCore.get()`.
- **`final` por padrão.** Classes são `final`, a não ser que tenham sido projetadas para extensão. Campos são `final`, a não ser que precisem mudar.
- **Imutabilidade para valores.** Records copiam entradas mutáveis (`List.copyOf`, `clone()` para arrays).
- **Falhe cedo.** Valide nos construtores dos records e lance `IllegalArgumentException` com uma mensagem que diga o que era esperado.
- **Falhas esperadas são valores.** Resultados de negócio como "senha incorreta" são `Result.failure(Problem)`, com um `code` estável como `auth.wrong_password`. Exceções são para bugs e falhas de infraestrutura.

## Pacotes

A Muxe prefere vários sub-pacotes pequenos e com nome a pacotes grandes e planos. O nome do pacote diz o papel das suas classes.

```text
br.com.muxe.auth
├── bootstrap                      wiring of the plugin
├── command                        Bukkit command executors
├── config                         settings records read from config.yml
├── credential                     a bounded context
│   ├── domain                     entities, value objects, repository interfaces, problem codes
│   ├── policy                     business rules that are not entities
│   ├── crypto                     hashing
│   ├── application
│   │   ├── command                commands and their handlers
│   │   └── query                  queries and their handlers
│   └── infrastructure
│       └── postgres               repository implementations
├── flow                           orchestration of the player experience
├── listener                       Bukkit listeners
└── session                        in-memory state of online players
```

Regras:

- **`domain` não depende de nada** além do JDK e do `core.common.result`.
- **`application` depende do `domain`** e de portas como `Transactions`, nunca do Bukkit ou do Velocity.
- **`infrastructure` implementa as portas do `domain`** (repositórios, hashers, transportes).
- **Os adaptadores de plataforma** (`listener`, `command`, `bootstrap`) traduzem o Bukkit ou o Velocity em commands e queries.
- **Pacotes `internal`** (como `messaging.bus.internal`) são detalhes de implementação. Outros módulos nunca os importam.

## Nomes

| Coisa | Padrão | Exemplo |
|---|---|---|
| Command | verbo + substantivo + `Command` | `RecordLoginCommand` |
| Handler de command | nome do command com `Handler` | `RecordLoginHandler` |
| Query | `Find…Query`, `Has…Query` | `FindAccountQuery`, `HasCredentialQuery` |
| Pacote | substantivo + `Packet`, ou `…Request`/`…Response` | `ConnectPlayerPacket`, `PlayerLocateRequest` |
| Id de pacote | `area.name`, minúsculo com underscores | `player.connect`, `auth.authenticated` |
| Código de problema | `area.snake_case` | `account.name_case_conflict` |
| Métrica | `muxe_<area>_<what>_<unit>`, contadores terminam com `_total`, timers com `_seconds` | `muxe_packets_published_total` |
| Migração | `V<n>__<description>.sql` em `db/migration/<module>/` | `V1__create_credentials.sql` |
| Plugin | `Muxe-<Name>` | `Muxe-Auth` |
| Jar | `muxe-<name>.jar` | `muxe-auth.jar` |

## Testes

- As classes de teste espelham o pacote da classe testada e terminam com `Test`. Testes de integração terminam com `IntegrationTest`.
- Os nomes dos métodos de teste descrevem o comportamento em inglês simples: `refusesANameThatDiffersOnlyByCase`.
- Preparação, ação e verificação, separadas por linhas em branco. AssertJ em todas as asserções.
- Injete `Clock` e use `MutableClock` em vez de dormir.

## Configuração

- Cada plugin tem um `config.yml` com padrões que funcionam localmente.
- O YAML é escrito no estilo de bloco expandido, com dois espaços por nível. Nada de mapas inline (`{ a: 1 }`) nem listas inline (`[a, b]`).
- As chaves são em `kebab-case`.
- Durações são strings como `500ms`, `5s`, `2m`, `1h`.
- Segredos nunca são escritos em texto puro nos padrões. Eles usam `env:NAME` (variável de ambiente) ou `file:/path` (um arquivo, como um Docker secret).

## Mensagens

- Os textos para jogadores ficam em `messages`, no `config.yml` do plugin, nunca no código Java.
- As mensagens são em português, com códigos de cor `&`, e os placeholders são escritos como `{name}`.
- As chaves são em `kebab-case` e agrupadas por feature quando ajuda (`admin.servers-line`).
- Uma mensagem mostrada em várias linhas de uma vez é uma lista YAML em uma única chave, nunca uma chave por linha. Uma linha em branco é um item vazio (`""`).
- Quando uma mensagem é copiada de um print de referência, mantenha as cores, a ordem e as linhas em branco.
- Os códigos de problema da camada de aplicação são convertidos em chaves de mensagem na camada de plataforma.
