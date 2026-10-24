[English](README.md) | **Português**

# API do core

O que o Muxe-Core oferece aos outros plugins. Utilitários reutilizáveis ficam aqui, no core, em vez de serem reescritos em cada feature: quando uma feature precisa de algo genérico (títulos, action bars, efeitos, menus, itens, scoreboards), isso é adicionado ao core e documentado nesta página.

## Obtendo o core

```java
MuxeCore core = MuxeCore.get();
```

Os plugins declaram `depend: [Muxe-Core]` no `plugin.yml` e `compileOnly(projects.core.bukkit)` no Gradle.

| Método | O que oferece |
|---|---|
| `identity()` | O id, o grupo e o tipo deste servidor. |
| `packets()`, `packetRegistry()`, `requestTimeout()` | O [barramento de pacotes](../../architecture/messaging/README.pt-BR.md). |
| `commands()`, `queries()` | Os barramentos de CQRS. Registre os handlers ao habilitar. |
| `transactions()`, `migrations()` | Transações do PostgreSQL e migrações Flyway do seu módulo. |
| `store()` | `ExpiringStore`: valores de curta duração no Redis, sempre com expiração. |
| `metrics()` | O registro de métricas. |
| `servers()` | O diretório de servidores vivos. |
| `network()` | `NetworkGateway`: envia um jogador para um servidor ou grupo. |
| `mainThread()`, `async()` | Executores para a thread do servidor e para trabalho bloqueante. |
| `clock()` | O relógio para injetar nos seus serviços. |
| `messages()` | Mensagens compartilhadas do Muxe-Core. |

## Texto

Pacote `br.com.muxe.core.bukkit.text`.

### Cores

```java
String text = Colors.translate("&aOlá");
String plain = Colors.strip("&aOlá");
```

### Títulos

```java
Title.builder()
    .title("&b&lMUXE")
    .subtitle("&eUse /logar <senha>")
    .timings(0, 100, 10)
    .build()
    .send(player);

Title.of("&aVitória!", "&7Você venceu a partida").send(player);
Title.clear(player);
```

- As cores com `&` são traduzidas automaticamente.
- Os tempos são em ticks (padrões 10, 70 e 20). Em um servidor que não expõe os tempos de título, o título ainda aparece, com os tempos padrão do cliente.

### Action bar

```java
ActionBar.send(player, "&eDigite o código em frente no chat!");
```

O Spigot 1.8.8 não tem API de action bar, então isso envia o pacote da 1.8 pelo `LegacyPackets` (reflexão sobre os internos do servidor, resolvida uma única vez). Se o servidor não os expuser, `send` retorna `false` e registra um único aviso; nada mais quebra. `ActionBar.isSupported()` diz se funciona.

### Catálogo de mensagens

```java
MessageCatalog messages = MessageCatalog.from(getConfig().getConfigurationSection("messages"));
player.sendMessage(messages.get("wrong-password", "remaining", "3"));
```

Os placeholders são escritos como `{name}` na configuração. `merge` combina catálogos (as mensagens do próprio plugin têm prioridade).

## Efeitos

Pacote `br.com.muxe.core.bukkit.effect`.

### Texto em partículas

```java
ParticleText text = ParticleText.standard(ParticleColor.GREEN);
Location anchor = Anchors.inFrontOfEyes(player.getEyeLocation(), 4, text.height());
text.show(player, anchor, "A7KX");
```

- Desenha texto com partículas de pó colorido, visível só para o `player`. As partículas duram cerca de um segundo, então chame `show` a cada poucos ticks enquanto o texto precisar ficar na tela.
- `PixelFont.standard()` cobre `A-Z`, `0-9` e espaço, com 5×7 pixels cada. `layout(text)` devolve as posições dos pixels, que é o que os testes usam.
- `Anchors.inFrontOfEyes` posiciona o conteúdo na frente de onde o jogador está olhando, centralizado na linha dos olhos.
- `ParticleColor` tem `GREEN`, `WHITE`, `AQUA`, `YELLOW`, ou qualquer valor RGB.

## Localizações

Pacote `br.com.muxe.core.bukkit.location`.

```java
ConfigLocation spawn = new ConfigLocation(plugin, "locations.spawn");
spawn.set(player.getLocation());
Location target = spawn.location().orElseGet(world::getSpawnLocation);
```

- `StoredLocation`: mundo, coordenadas, yaw e pitch, lidos e gravados na configuração (`from`, `toMap`), convertidos para uma `Location` do Bukkit só quando o mundo está carregado, e `describe()` para mensagens da equipe.
- `ConfigLocation`: uma `StoredLocation` salva em um caminho do `config.yml` de um plugin, gravada em disco a cada mudança. Implementa `LocationSlot`, então os serviços podem ser testados com uma posição em memória.
- `Anchors.lookingAt(content, viewerEyes, height)`: posiciona um conteúdo, como um `ParticleText`, virado para quem o vê de outro lugar.

## Utilitários independentes de plataforma

No `core:common`, usáveis também no proxy:

| Utilitário | Uso |
|---|---|
| `Result` e `Problem` | Falhas de negócio esperadas, com códigos estáveis. |
| `ExpiringStore` | Dados chave-valor de curta duração no Redis (`RedisExpiringStore`), em memória nos testes (`InMemoryExpiringStore` nas test fixtures). |
| `MetricRegistry`, `Tags` | Counters, timers e gauges. |
| `ConfigSource`, `DurationParser`, `SecretResolver` | Leitura de configuração, durações (`5s`) e segredos (`env:`, `file:`). |

## Adicionando um utilitário

1. Coloque-o no módulo certo: `core:bukkit` se ele precisa da API do Bukkit, `core:common` caso contrário.
2. Dê a ele um pacote que nomeie a sua área (`text`, `effect`, `item`, `menu`, …).
3. Separe a lógica que pode ser testada sem servidor da chamada ao Bukkit, e teste-a.
4. Documente-o nesta página, em inglês e em português.
