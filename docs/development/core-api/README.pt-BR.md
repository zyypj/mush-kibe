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
- `Title.clear` esconde o título e o subtítulo de uma vez. O cliente 1.8 mantém o subtítulo na tela depois de um reset simples, então o `clear` primeiro zera os tempos, depois esvazia o subtítulo e só então faz o reset.

### Action bar

```java
ActionBar.send(player, "&eDigite o código em frente no chat!");
```

O Spigot 1.8.8 não tem API de action bar, então isso envia o pacote da 1.8 pelo `LegacyPackets` (reflexão sobre os internos do servidor, resolvida uma única vez). Se o servidor não os expuser, `send` retorna `false` e registra um único aviso; nada mais quebra. `ActionBar.isSupported()` diz se funciona.

O cliente 1.8 desenha a action bar a partir do texto puro da mensagem e descarta as cores do JSON, então as cores `&` ficam dentro do texto como códigos `§`, que o cliente desenha.

### Catálogo de mensagens

```java
MessageCatalog messages = MessageCatalog.from(getConfig().getConfigurationSection("messages"));
player.sendMessage(messages.get("wrong-password", "remaining", "3"));
messages.lines("challenge").forEach(player::sendMessage);
```

Os placeholders são escritos como `{name}` na configuração. `merge` combina catálogos (as mensagens do próprio plugin têm prioridade).

Uma mensagem pode ser uma lista YAML. `lines(key)` devolve uma entrada por linha, com os itens vazios (`""`) convertidos em uma linha que o chat mostra em branco, e `get(key)` junta as linhas com quebras de linha, o que serve para telas de expulsão. O `ProxyMessages` do proxy aceita listas do mesmo jeito.

```yaml
messages:
  challenge:
    - "&6&lDESAFIO:"
    - "&eDigite o &bcódigo &eem frente &ano chat&e!"
    - ""
    - "&7&o(É necessário ativar os hologramas para visualizar corretamente)"
```

## Efeitos

Pacote `br.com.muxe.core.bukkit.effect`.

### Texto em partículas

```java
ParticleText text = ParticleText.standard(ParticleColor.GREEN);
Location anchor = Anchors.inFrontOfEyes(player.getEyeLocation(), 4, text.height());
text.show(player, anchor, "A7KX");
```

- Desenha texto com partículas de pó colorido, visível só para o `player`. As partículas duram cerca de um segundo, então chame `show` a cada poucos ticks enquanto o texto precisar ficar na tela.
- `PixelFont.standard()` cobre `A-Z`, `0-9` e espaço, com 5×7 pixels cada. Os dígitos são desenhados sem barras nem serifas, então `0` e `O` ficam iguais. `layout(text)` devolve as posições dos pixels, que é o que os testes usam.
- `Anchors.inFrontOfEyes` posiciona o conteúdo na frente de onde o jogador está olhando, centralizado na linha dos olhos.
- `ParticleColor` tem `GREEN`, `WHITE`, `AQUA`, `YELLOW`, ou qualquer valor RGB.

### Hologramas de pixels

Pacote `br.com.muxe.core.bukkit.hologram`.

```java
PixelHolograms holograms = PixelHolograms.standard();
Location anchor = Anchors.inFrontOfEyes(player.getEyeLocation(), 7, holograms.height());
SpawnedHologram hologram = holograms.show(player, anchor, "4829");
hologram.resend();
hologram.remove();
```

- Desenha texto como uma grade de hologramas de nome (`⬛`), um por pixel da `PixelFont`, visível só para o `player`. Os hologramas são suportes de armadura invisíveis enviados por pacote: eles nunca existem no servidor, os outros jogadores nunca os veem e ninguém consegue interagir com eles.
- `HologramStyle.standard()` sombreia cada caractere: a linha de cima em branco (`&f`), o corpo em verde claro (`&a`) e a linha de baixo em verde escuro (`&2`). Dá para usar qualquer símbolo e quaisquer cores.
- Os hologramas ficam até `remove()`. Chame `resend()` a cada poucos segundos enquanto eles precisarem ficar, caso o cliente os tenha descartado.
- `PixelHolograms.layout(anchor, text)` devolve a posição e o nome de cada pixel, que é o que os testes usam. `ShadedLayout` faz o mesmo em unidades da grade, sem posições.
- Os hologramas dependem das classes internas do servidor 1.8, acessadas por reflexão em `PacketHologramTransport`. Quando elas não existem, `isAvailable()` é `false` e `show` não desenha nada.

### Hologramas de texto

```java
TextHolograms holograms = TextHolograms.standard();
SpawnedHologram hologram = holograms.show(player, top, List.of("&b&lSKYWARS", "&e37 jogando"));
```

- As linhas descem a partir de `top`, a 0,28 bloco uma da outra. Uma linha vazia mantém o espaço e não mostra nada. `height(lines)` diz a altura do texto.
- Mesmo transporte dos hologramas de pixels: suportes de armadura invisíveis enviados só para o `player`.

## Itens

Pacote `br.com.muxe.core.bukkit.item`.

```java
ItemStack star = ItemBuilder.of(Material.NETHER_STAR)
    .name("&aSelecionar lobby")
    .lore("&7Clique para abrir")
    .addLoreIf(player.hasPermission("muxe.vip"), "&6Vantagem VIP")
    .glow()
    .build();

ItemStack head = ItemBuilder.skull().name("&aMeu perfil").skullOwner(player.getName()).build();
ItemStack pane = ItemBuilder.from(config.getConfigurationSection("lobby-selector.current"))
    .replacing("number", "3", "online", "32", "max", "60")
    .build();
```

- O `ItemBuilder` é imutável: cada método devolve um builder novo, então um modelo pode ser compartilhado e preenchido várias vezes.
- Cobre material e data, quantidade, nome e lore (com cores `&`), `addLore`, `addLoreIf`, encantamentos, `glow()`, `unbreakable()`, `color` de couro, cabeças por dono (`skullOwner`) ou por textura (`skullTexture`, base64), e a troca de `{placeholder}` no nome e na lore com `replacing`.
- As flags do item ficam escondidas por padrão; `showFlags()` mostra.
- `ItemBuilder.from(section)` lê os mesmos campos da configuração:

```yaml
item:
  material: LEATHER_CHESTPLATE
  data: 0
  amount: 1
  name: "&cPeitoral"
  lore:
    - "&7Uma linha"
  glow: true
  unbreakable: true
  hide-flags: true
  color: "#FF5555"
  skull-owner: Notch
  skull-texture: "base64..."
  enchantments:
    DURABILITY: 3
```

- O `build()` precisa de um servidor rodando. O resto são dados simples, então os testes verificam os builders diretamente.

## Menus

Pacote `br.com.muxe.core.bukkit.menu`. O Muxe-Core registra o listener, então qualquer plugin pode abrir menus.

```java
Menu menu = Menu.builder("&8Perfil").rows(3)
    .border(ItemBuilder.of(Material.STAINED_GLASS_PANE, 15).name(" "))
    .button(1, 4, ItemBuilder.skull().name("&a" + player.getName()).skullOwner(player.getName()), context -> context.close())
    .button(22, ItemBuilder.of(Material.BARRIER).name("&cFechar"), ClickContext::close)
    .onClose(viewer -> viewer.sendMessage("Até mais!"))
    .build();
menu.open(player);
```

- Os botões vão por slot ou por linha e coluna (`Slots.of(row, column)`). `border` faz a moldura do menu, `fill` cobre os slots vazios; os botões sempre têm prioridade.
- Os cliques recebem um `ClickContext`: o jogador, o tipo de clique (`isLeftClick`, `isRightClick`, `isShiftClick`), o slot e ações para `close()`, `open(outroMenu)`, `update(slot, button)` ou `updateIcon(icon)` com o menu aberto.
- Todo clique dentro de um menu é cancelado: os jogadores nunca pegam nem largam itens dele.
- Os títulos são traduzidos e cortados nos 32 caracteres que a 1.8 permite.

### Menus paginados

```java
PaginatedMenu lobbies = PaginatedMenu.builder("Selecionar lobby ({page}/{pages})")
    .items(buttons)
    .previous(ItemBuilder.of(Material.ARROW).name("&aPágina anterior"))
    .next(ItemBuilder.of(Material.ARROW).name("&aPróxima página"))
    .build();
lobbies.open(player, 1);
```

- Até 21 itens por página, 7 por linha, cada linha centralizada (`MenuLayout.centered`): um item fica no meio, dois ficam à esquerda do centro, como no seletor de lobby.
- O menu mantém uma linha vazia acima e abaixo dos itens. As setas ficam nas pontas da última linha e só aparecem quando há página para ir.
- `slots(list)` coloca os itens em slots fixos, `rows(n)` fixa a altura, `extra(slot, button)` adiciona um botão em todas as páginas e `fill(icon)` cobre os slots vazios.
- `page(n)` monta o menu sem abrir, que é o que os testes usam.

## Scoreboard

Pacote `br.com.muxe.core.bukkit.scoreboard`.

```java
Sidebar sidebar = Sidebar.create(player);
sidebar.title("&b&lMUXE");
sidebar.lines(List.of("", "&fJogadores: &b2211", "", "&amuxe.com.br"));
```

- Uma scoreboard por jogador, com até 15 linhas. Cada linha fica em um time, então atualizar uma linha nunca faz a sidebar piscar; só as linhas que mudaram são enviadas.
- Linhas de até uns 30 caracteres visíveis são divididas entre o prefixo e o sufixo do time (16 caracteres cada na 1.8), levando a cor junto (`SidebarText.split`). Linhas em branco funcionam.
- `scoreboard()` expõe a scoreboard do Bukkit para outros times, como o que esconde o nome dos NPCs.

## Boss bar

Pacote `br.com.muxe.core.bukkit.bossbar`.

```java
BossBars bars = BossBars.start(plugin);
BossBarFrames frames = new BossBarFrames(List.of("&e&lMUXE", "&6&lMUXE"), 10);
bars.show(player, frames.at(tick));
```

- A 1.8 não tem API de boss bar: a barra é um wither invisível enviado só para o jogador, mantido 32 blocos à frente dos olhos para continuar à vista. Ela acompanha o jogador a cada 2 ticks quando ele anda ou vira.
- `show` renomeia a barra quando ela já está na tela. A barra é criada de novo depois de um respawn ou de uma troca de mundo.
- `BossBarFrames` transforma uma lista de textos e um período em ticks no quadro de qualquer tick, para títulos animados.

## NPCs

Pacote `br.com.muxe.core.bukkit.npc`.

```java
NpcService npcs = NpcService.start(plugin, npc -> List.of("&b&lSKYWARS", "&e37 jogando"));
npcs.onClick((player, npc) -> player.sendMessage("Clicou em " + npc.id()));
npcs.put(new Npc("skywars", location, Optional.of(skin)));

MojangSkins.standard().byName("Notch").thenAccept(found -> ...);
```

- Os NPCs são jogadores que só existem em pacotes: são enviados para cada jogador a até 48 blocos e removidos além de 64. Eles ficam no tab por 2 segundos para o cliente carregar a skin, e o nome em cima deles é escondido com um time; o texto acima deles é um holograma de texto.
- Os cliques são lidos da rede: o `PacketInterceptor` adiciona um handler do Netty na conexão de cada jogador e avisa sobre o `PacketPlayInUseEntity`, porque o servidor ignora cliques em entidades que ele não conhece. O tratamento do clique roda na main thread.
- `MojangSkins` busca a skin assinada de um nome original (`api.mojang.com` e `sessionserver.mojang.com`). Guarde o resultado em vez de buscar a cada inicialização.
- Veja a [ADR 0008](../../decisions/0008-packet-npcs-and-click-interception.pt-BR.md).

## Localizações

Pacote `br.com.muxe.core.bukkit.location`.

```java
ConfigLocation spawn = new ConfigLocation(plugin, "locations.spawn");
spawn.set(player.getLocation());
Location target = spawn.location().orElseGet(world::getSpawnLocation);
```

- `StoredLocation`: mundo, coordenadas, yaw e pitch, lidos e gravados na configuração (`from`, `toMap`), convertidos para uma `Location` do Bukkit só quando o mundo está carregado, e `describe()` para mensagens da equipe.
- `ConfigLocation`: uma `StoredLocation` salva em um caminho do `config.yml` de um plugin, gravada em disco a cada mudança. Implementa `LocationSlot`, então os serviços podem ser testados com uma posição em memória.
- `Anchors.lookingAt(content, viewerEyes, height)`: posiciona um conteúdo, como um `ParticleText` ou um `PixelHolograms`, virado para quem o vê de outro lugar.

## Utilitários independentes de plataforma

No `core:common`, usáveis também no proxy:

| Utilitário | Uso |
|---|---|
| `Result` e `Problem` | Falhas de negócio esperadas, com códigos estáveis. |
| `ExpiringStore` | Dados chave-valor de curta duração no Redis (`RedisExpiringStore`), em memória nos testes (`InMemoryExpiringStore` nas test fixtures). |
| `MetricRegistry`, `Tags` | Counters, timers e gauges. |
| `ConfigSource`, `DurationParser`, `SecretResolver` | Leitura de configuração, durações (`5s`) e segredos (`env:`, `file:`). |
| `SpamGuard`, `CooldownFormat` | Anti-spam de cliques: um número de usos dentro de uma janela inicia um cooldown (`3` usos em `1s` iniciam `3s`), e o tempo restante aparece como `2,9`. |

## Adicionando um utilitário

1. Coloque-o no módulo certo: `core:bukkit` se ele precisa da API do Bukkit, `core:common` caso contrário.
2. Dê a ele um pacote que nomeie a sua área (`text`, `effect`, `item`, `menu`, …).
3. Separe a lógica que pode ser testada sem servidor da chamada ao Bukkit, e teste-a.
4. Documente-o nesta página, em inglês e em português.
