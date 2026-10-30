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
| `permissions()` | Cargos, tags e permissões dos jogadores online (veja [Cargos e permissões](#cargos-e-permissões)). |

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

### Linhas de chat centralizadas e clicáveis

```java
ChatLines.send(player, messages.lines("shop-payment"), CenteredText.DEFAULT_CENTER, "https://muxe.com.br/loja");
```

- Uma linha que começa com `<center>` ganha espaços para que o meio dela fique no pixel indicado do chat (`CenteredText.DEFAULT_CENTER` é 114, o meio de uma linha de 57 espaços riscados). As larguras vêm do `TextWidth` do `core:common` (veja [Utilitários independentes de plataforma](#utilitários-independentes-de-plataforma)).
- Uma linha que começa com `<link>` abre a URL quando clicada. As duas marcações podem ser usadas juntas.
- `ChatLines.send(player, lines, center, url, hover)` também mostra `hover` (uma lista de linhas) quando o mouse passa sobre uma linha `<link>`. `ChatLines.linked(text, url, hover)` monta essa linha clicável, que é o que os testes verificam.

### Botões de comando no chat

```java
ClickableText.send(player,
    ChatButton.text("&c✘ &fCama do &9Azul"),
    ChatButton.run(" &e[Definir]", "/arena definir cama azul", "&7Clique para definir."),
    ChatButton.suggest(" &b[Nome]", "/arena nome "));
```

- `ChatButton.run` executa o comando no clique, `ChatButton.suggest` coloca o comando na caixa do chat, e `ChatButton.text` é texto simples. Cada um aceita linhas de hover opcionais.
- `ClickableText.line(botões)` monta os componentes, que é o que os testes verificam.

### Perguntas no chat

```java
ChatPrompts prompts = ChatPrompts.start(plugin, core.clock());
prompts.ask(player, Duration.ofSeconds(60), code -> apply(code), () -> cancelled(), () -> expired());
```

- A próxima mensagem do jogador no chat vira a resposta e não aparece no chat. Digitar `cancelar` cancela; depois do tempo limite roda o callback de expiração. As respostas chegam na main thread.
- A lista de espera é o `PendingPrompts` do `core:common`, testado com um `MutableClock`.

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
- A altura de uma linha é onde o texto dela começa: o topo do texto fica 0,09375 bloco acima dela. O cliente 1.8 desenha o nome de um suporte de armadura pequeno como se ele tivesse o tamanho normal (1,975 bloco), então o `PacketHologramTransport` cria cada suporte `LABEL_HEIGHT` (1,39375) bloco abaixo da linha.

### Explosão de fogo de artifício

```java
FireworkEffect effect = FireworkBurst.effect(config.getConfigurationSection("parkour.finish-firework"));
FireworkBurst.explode(plugin, location, effect);
```

- O `explode(plugin, location, effect)` cria um fogo de artifício com esse efeito em `location` e o faz estourar ali mesmo 2 ticks depois (`DETONATE_AFTER_TICKS`), em vez de subir. Todos por perto o veem.
- O `effect(section)` lê um efeito da config: `type` (um `FireworkEffect.Type`, `BALL_LARGE` por padrão), `colors` (pelo menos uma), `fade-colors`, `flicker` e `trail` (os dois `false` por padrão). Uma seção sem cores é um erro.
- O `color(hex)` lê uma cor escrita como `#RRGGBB` (o `#` é opcional).
- A explosão depende das classes internas do servidor 1.8, acessadas por reflexão. Quando elas não existem, um aviso vai para o log uma vez e o fogo de artifício sobe um pouco antes de explodir.
- O [parkour](../../features/parkour/README.pt-BR.md) o usa quando um jogador termina.

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
ItemStack pane = ItemBuilder.from(config.getConfigurationSection("menus.lobby-selector.current"))
    .replacing("number", "3", "online", "32", "max", "60")
    .build();
```

- O `ItemBuilder` é imutável: cada método devolve um builder novo, então um modelo pode ser compartilhado e preenchido várias vezes.
- Cobre material e data, quantidade, nome e lore (com cores `&`), `addLore`, `addLoreIf`, encantamentos, `glow()`, `unbreakable()`, `color` de couro, cabeças por dono (`skullOwner`) ou por textura (`skullTexture`: o valor em base64, a URL da textura ou só o hash depois de `textures.minecraft.net/texture/`), e a troca de `{placeholder}` no nome e na lore com `replacing`.
- As flags do item ficam escondidas por padrão; `showFlags()` mostra.
- O `SkullTextures.apply(Skull, Optional<String>)` (pacote `br.com.muxe.core.bukkit.nms`) põe uma textura num bloco de cabeça já colocado no mundo; uma textura vazia dá a cabeça padrão. Passe o valor de `SkullTexture.value(...)` e depois chame `skull.update(...)`. As [cabeças de redes sociais](../../features/social-heads/README.pt-BR.md) o usam.
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
  skull-texture: "d545b0ddd82cc4fd2eac64e445bee91b4706408514f71594d18c4b90a5491d74"
  enchantments:
    DURABILITY: 3
```

- O `build()` precisa de um servidor rodando. O resto são dados simples, então os testes verificam os builders diretamente.

### Atualização de blocos

O `BlockUpdates.resend(Block)` (pacote `br.com.muxe.core.bukkit.nms`) envia um bloco de novo para os jogadores que o veem, para mudanças que o cliente não percebe sozinho, como uma textura nova num bloco de cabeça. Ele não faz nada quando os internos do servidor estão ausentes.

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

### Botões configurados

Pacote `br.com.muxe.core.bukkit.menu.button`. Botões cujo slot, item e ação vêm da configuração.

```java
List<ConfiguredButton> buttons = ConfiguredButton.list(config.getConfigurationSection("menus.game-selector.buttons"));
ConfiguredButton.requireFit(buttons, 4);
ButtonActions actions = new ButtonActions(games, shopAccess);
Menu.Builder menu = Menu.builder("Selecionar jogo").rows(4);
for (ConfiguredButton button : buttons) {
    menu.button(button.slot(), button.item(), context -> actions.run(context, button));
}
```

```yaml
buttons:
  skywars:
    slot: 14
    game: skywars
    item:
      material: EYE_OF_ENDER
      name: "&b&lSKY WARS"
  nick:
    slot: 39
    shop: ultra-plus
    item:
      material: NAME_TAG
      name: "&aNick (/nick)"
```

- Cada botão tem um `slot` (um slot do inventário, de 0 a 53), um `item` no [formato do `ItemBuilder`](#itens) e, opcionalmente, uma ação.
- O `ButtonAction` é `NONE`, `GAME` (`game: <grupo de servidores>`), `SHOP` (`shop: <id da categoria>`, vazio para a categoria padrão) ou `COMMAND` (`command: <comando>`, com ou sem `/`: fecha o menu e o jogador roda o comando).
- O `ButtonActions.run` fecha o menu e conecta o jogador pelo [`GameConnector`](#conector-de-jogos) no `GAME`, e abre a loja pelo [`ShopAccess`](#acesso-à-loja) no `SHOP`. O `players(action)` dá os jogadores do grupo de um botão `GAME`, para o `{players}`.
- O `requireFit(buttons, rows)` recusa um botão fora das linhas do menu e dois botões no mesmo slot, então uma configuração errada falha ao ligar.
- O seletor de jogo e o perfil da [Hotbar](../../features/hotbar/README.pt-BR.md) são montados assim.

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
- O Muxe-Core coloca os times de cargo (prefixo no tab e acima da cabeça, veja [Cargos e tags](../../features/ranks-and-tags/README.pt-BR.md#prefixos)) em toda scoreboard que um jogador usa, sidebars inclusive, em até 5 ticks. Os nomes deles começam com dígitos, então nunca colidem com os times `muxe-line-*` nem com o time dos NPCs.

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
- Os cliques são lidos da rede: o `PacketInterceptor` adiciona um handler do Netty na conexão de cada jogador e avisa sobre o `PacketPlayInUseEntity`, porque o servidor ignora cliques em entidades que ele não conhece. Cada interceptador tem um nome de handler próprio, então vários serviços (NPCs, suportes de exibição) podem ler cliques ao mesmo tempo. O tratamento do clique roda na main thread.
- `NpcService.start(plugin, layout, lines)` posiciona as linhas com um `NpcLabelLayout(bottom, gaps, spacing)`: a última linha `bottom` blocos acima dos pés e as outras subindo pelos `gaps`, depois pelo `spacing`, com o `LabelStack.upward`. `NpcLabelLayout.from(section)` lê isso da config; sem uma, o `NpcLabelLayout.STANDARD` deixa a última linha 2,05 blocos acima e as outras a 0,28 de distância.
- `new Npc(id, location, skin, Optional.of(item))` coloca um item na mão do NPC, enviado a cada jogador como equipamento logo depois que o NPC aparece. `PlacedNpcs.hand(HeldItem)` transforma um material e um data guardados nesse item.
- `MojangSkins` busca a skin assinada de um nome original (`api.mojang.com` e `sessionserver.mojang.com`). Guarde o resultado em vez de buscar a cada inicialização.
- Veja a [ADR 0008](../../decisions/0008-packet-npcs-and-click-interception.pt-BR.md).

## Suportes de exibição

Pacote `br.com.muxe.core.bukkit.display`.

```java
DisplayService displays = DisplayService.start(plugin);
displays.onClick((player, display) -> openShop(player));
displays.put(new Display("shop", anchor, List.of(
    new DisplayStand(0.19, -0.03, -0.81, 0f, true, false, Optional.of(ItemBuilder.skull().skullTexture(hash)), Optional.empty(),
        StandPose.ZERO, StandPose.ZERO, StandMotion.NONE),
    new DisplayStand(0, -0.1, 0, 115f, false, true, Optional.empty(), Optional.of(ItemBuilder.of(Material.INK_SACK, 5).glow()),
        StandPose.ZERO, new StandPose(0, 90, 90), new StandMotion(4.5, 0.25, 40, 180))
), List.of(new DisplayLabel("&b&lLOJA", 3.6))));
```

- Suportes de armadura invisíveis que só existem em pacotes, como os NPCs: aparecem a até 48 blocos e somem além de 64. Cada suporte tem um deslocamento em relação à âncora, um yaw, tamanho pequeno ou normal, braços, um capacete e um item na mão, as poses da cabeça e do braço direito (em graus) e um movimento.
- O `StandMotion` gira o suporte em `degreesPerTick` e o faz subir e descer (`bobHeight` blocos a cada `bobPeriodTicks`, deslocado por `phaseDegrees`). Os suportes que se movem são atualizados a cada tick.
- Os rótulos são hologramas de texto numa altura acima da âncora (a altura do próprio texto).
- O `labelText((viewer, text) -> ...)` muda o texto dos rótulos para cada jogador antes de enviá-lo, então uma mesma exibição pode mostrar algo diferente para cada um, como o recorde pessoal do [parkour](../../features/parkour/README.pt-BR.md). O `refresh(player)` reenvia os rótulos para aquele jogador depois que o que ele deve ver mudou. O `Display.labelPixels(UnaryOperator<String>)` aplica a mesma mudança às linhas de uma exibição.
- O `LabelStack` transforma uma lista de linhas em rótulos. O `LabelStack.downward(lines, top, spacing)` mantém a primeira linha em `top` e põe cada linha seguinte `spacing` mais baixo, como fazem os hologramas colocados. O `LabelStack.upward(lines, bottom, gaps, spacing)` mantém a última linha em `bottom` e sobe usando os `gaps` de baixo para cima, e depois o `spacing`, como fazem as caixas registradoras da loja. Linhas em branco mantêm o espaço.
- Clicar em qualquer suporte ou rótulo, com qualquer botão, roda o tratamento de clique uma vez na main thread. O botão direito num suporte de armadura só envia `INTERACT_AT`, então é essa a ação que conta. Os deslocamentos usam os eixos do mundo: mover a âncora move a exibição inteira sem girá-la.

## Posicionamentos

Pacotes `br.com.muxe.core.common.placement` e `br.com.muxe.core.bukkit.placement`.

O próprio Muxe-Core coloca os NPCs, os hologramas, as caixas registradoras da loja, as cabeças de redes sociais e os pontos do parkour que a equipe cria com `/npc`, `/holograma`, `/loja caixa`, `/redes` e `/parkour`, em todos os servidores, como descrito em [NPCs e hologramas](../../features/npcs-and-holograms/README.pt-BR.md). Os plugins não precisam de armazenamento próprio de NPCs ou hologramas: a equipe os coloca por comando.

- O `Placement` é selado: `NpcPlacement`, `HologramPlacement`, `RegisterPlacement`, `SocialHeadPlacement` (veja [Cabeças de redes sociais](../../features/social-heads/README.pt-BR.md)) e `ParkourPointPlacement` (veja [Parkour](../../features/parkour/README.pt-BR.md)), cada um com um grupo de servidores, um id e uma `Position`. O `Placements` guarda tudo de um grupo.
- O `PlacementSync` carrega o que foi colocado no grupo deste servidor ao ligar, salva e remove pelo `SavePlacementCommand` e pelo `RemovePlacementCommand`, publica `placement.changed` para o grupo, e recarrega com a `FindPlacementsQuery` quando esse pacote chega. Os ouvintes registrados com `onChange` rodam na main thread.
- O `Reconciler<T>` compara a lista desejada com o que está aparecendo, por id, e devolve só o que foi adicionado ou mudou e o que foi removido, então recarregar nunca recria o que não mudou.
- O `PlacementImport` lê o `plugins/Muxe-Core/import.yml` ao ligar e no `/npc importar`: NPCs (com linhas próprias e item na mão), caixas registradoras e pontos do parkour. O `PlacementImport.withSkins` preenche a skin dos NPCs que têm `skin-name` e não têm textura, e o `run` devolve um `Outcome` (arquivo encontrado, entradas importadas).

## Modo construção

Pacote `br.com.muxe.core.bukkit.buildmode`. O Muxe-Core registra o `/build <on|off>` (permissão `muxe.build`) em todos os servidores.

```java
BuildMode build = MuxeCore.get().buildMode();
if (build.isBuilding(player)) { ... }
build.addHook(new BuildModeHook() {
    @Override
    public boolean allows(Player player) { return authenticated(player); }

    @Override
    public void left(Player player) { giveItemsBack(player); }
});
```

- **Ligado:** limpa o inventário, coloca no Criativo e repete `build-action-bar` na action bar a cada segundo.
- **Desligado:** limpa o inventário e a action bar e volta para o Aventura nos tipos de `build-mode.adventure-only`, ou para o modo de jogo padrão do servidor nos outros. Depois os hooks devolvem os itens.
- O estado fica no Redis (`muxe:build:<uuid>`) por `build-mode.duration` (6 horas), renovado a cada entrada num servidor. Quem liga o modo continua com ele ao trocar de servidor; quem não tem `muxe.build` o perde na próxima entrada.
- Nos tipos de `adventure-only` (`LOBBY` e `AUTH`), qualquer modo de jogo que não seja Aventura é cancelado, exceto o Criativo no modo construção (`BuildRules.allowsGameMode`).
- Os plugins só leem `isBuilding` para decidir o que o jogador pode fazer. Um hook pode recusar o modo construção (`allows`), como o servidor de autenticação faz antes do login, e reagir a `entered` e `left`.

## Hotbar

Pacote `br.com.muxe.core.bukkit.hotbar`. O próprio Muxe-Core dá a hotbar dos lobbys nos tipos de servidor de `hotbar.server-kinds` (`LOBBY` por padrão), como descrito em [Hotbar](../../features/hotbar/README.pt-BR.md). Os plugins não a criam.

- Ao entrar, ele limpa o inventário e a armadura, dá os cinco itens, seleciona o slot 1 e aplica a visibilidade do jogador (`VisibilityService` em `br.com.muxe.core.bukkit.visibility`, apoiado no `ChangeVisibilityCommand` e na `FindVisibilityQuery` de `br.com.muxe.core.common.visibility`).
- O `HotbarBuildHook` devolve a hotbar quando o jogador sai do modo construção. Um plugin só adiciona um hook para o que mais precisar, como o lobby faz para devolver o voo.
- O lobby de um jogo recebe a mesma hotbar rodando com um tipo listado em `hotbar.server-kinds`; o seu seletor de lobby lista os servidores do seu próprio grupo.

## Conector de jogos

Pacote `br.com.muxe.core.bukkit.network`.

```java
GameConnector games = new GameConnector(core.servers(), core.network(), core.mainThread(), core.messages());
int playing = games.players("skywars");
games.connect(player, "skywars", "Sky Wars");
```

- `players(group)` soma os jogadores online dos servidores vivos do grupo.
- `connect` responde `game-unavailable` quando o grupo não tem servidor vivo. Senão, mostra `game-connecting` e pede ao proxy que mande o jogador para o servidor com menos jogadores do grupo, com `connect-failed` se isso falhar.
- Os NPCs de jogo e o seletor de jogo o usam, então os dois mostram as mesmas mensagens.

## Acesso à loja

Pacote `br.com.muxe.core.bukkit.shop`.

```java
shopAccess.open(player, "ultra-plus");
```

- Abre a [loja](../../features/shop/README.pt-BR.md) numa categoria; uma categoria vazia ou desconhecida abre a `shop.default-category`. Quando a loja está desligada neste tipo de servidor, responde `shop-unavailable`.
- O Muxe-Core o cria ao ligar. Os NPCs de loja e os botões `shop` o usam.

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

## Créditos

Pacote `br.com.muxe.core.bukkit.credit`. O comportamento está em [Créditos e cosméticos](../../features/credits/README.pt-BR.md).

- `CreditBalancesListener.start(plugin, core, Duration.ofSeconds(30))` devolve um `CreditBalances` que carrega o saldo de cada jogador ao entrar, esquece ao sair e recarrega todos no período dado. `of(playerId)` lê o valor em cache (0 até carregar) sem bloquear; `refresh(playerId)` recarrega um agora.
- `CreditFormat.format(1750)` dá `1.750`.
- Créditos e cosméticos mudam pelos comandos do core: `ChangeCreditsCommand`, `BuyCosmeticCommand`, `GrantCosmeticCommand`, `SelectCosmeticCommand`; leituras por `FindCreditsQuery` e `FindCosmeticsQuery`.

## Animações de cama

Pacotes `br.com.muxe.core.bukkit.cosmetic.animation` (puro) e `br.com.muxe.core.bukkit.cosmetic.stage` (NMS).

- Uma `BedAnimation` escreve um roteiro: com o `BedFrame` (pé, cabeceira, frente, direita), a posição de quem quebrou, um `Stage` e uma `Timeline`, ela agenda o que acontece em cada tick.
- O `Stage` cria atores só de pacote (armor stands, mobs, falling blocks, itens, fogos, bolas de neve), manda partículas e sons e finge mudanças de bloco. O `Actor` move, equipa, posiciona, monta e remove.
- `Timeline.at(tick, ação)` e `repeat(de, até, período, ação)` agendam passos; `lasts(ticks)` mantém a animação viva por mais tempo.
- `AnimationPlayer.play(animação, mundo, cama, quebrador, espectadores, aoTerminar)` roda um passo por tick só para os espectadores e, no fim ou no `cancel()`, remove todos os atores e restaura todos os blocos fingidos.
- `BedFrame.ofBlock(x, y, z, data)` monta o frame a partir de qualquer metade de uma cama 1.8.
- As animações são testadas sem servidor com um `Stage` que grava, como o `BedAnimationsTest` faz no BedWars.

## Utilitários independentes de plataforma

No `core:common`, usáveis também no proxy:

| Utilitário | Uso |
|---|---|
| `Result` e `Problem` | Falhas de negócio esperadas, com códigos estáveis. |
| `ExpiringStore` | Dados chave-valor de curta duração no Redis (`RedisExpiringStore`), em memória nos testes (`InMemoryExpiringStore` nas test fixtures). |
| `MetricRegistry`, `Tags` | Counters, timers e gauges. |
| `ConfigSource`, `DurationParser`, `SecretResolver` | Leitura de configuração, durações (`5s`) e segredos (`env:`, `file:`). |
| `SpamGuard`, `CooldownFormat` | Anti-spam de cliques: um número de usos dentro de uma janela inicia um cooldown (`3` usos em `1s` iniciam `3s`), e o tempo restante aparece como `2,9`. |
| `PendingPrompts` | Jogadores esperando para responder algo no chat, cada um com um prazo (usado pelo `ChatPrompts`). |
| `TextWidth` | Largura em pixels de um texto na fonte padrão do 1.8 (`TextWidth.of`), lendo os códigos `&` e `§`; o negrito soma um pixel por caractere. `TextWidth.center(texto, área)` coloca espaços para que o texto fique no meio de uma área, como a lista de servidores (`TextWidth.SERVER_LIST_WIDTH`, 271). |

## Cargos e permissões

Pacote `br.com.muxe.core.bukkit.permission`. O Muxe-Core carrega os cargos de todo jogador e faz o `hasPermission` segui-los em todo servidor Bukkit, como descrito em [Cargos e tags](../../features/ranks-and-tags/README.pt-BR.md). Os plugins só perguntam.

```java
PermissionService ranks = MuxeCore.get().permissions();
if (player.hasPermission("muxe.lobby.fly")) { ... }
RankDisplay display = ranks.display(player.getUniqueId());
String name = display.formatName(player.getName());
Optional<ResolvedPermissions> resolved = ranks.resolved(player.getUniqueId());
ranks.onChange(changed -> refresh(changed));
```

- `hasPermission` funciona como sempre: o permissible do jogador é trocado por um que pergunta primeiro aos cargos resolvidos e volta ao Bukkit (op e padrões do `plugin.yml`) quando o cargo não diz nada.
- `display(id)` dá a tag que o jogador mostra: `prefix()` (`&a&lVIP `), `color()` (`&a`), `formatName(name)` (`&a&lVIP &atadeu`), `coloredName(name)` (`&atadeu`), `tagDisplay()` (`&aVIP`) e `chatFormat(template)`.
- `resolved(id)` e `user(id)` dão as permissões resolvidas e os nós crus dos jogadores online; `data()` dá os grupos, tags e trilhas; `sync()` muda tudo isso para a rede inteira.
- `onChange` roda na thread principal sempre que as permissões de um jogador online mudam (cargo novo, tag nova, troca de mundo, expiração).
- Comandos e menus dependem da interface `PlayerRanks`, que os testes implementam com um `PermissionCache` e um `PermissionSync` sobre os fixtures em memória.

- **Um plugin pode trocar o name tag de um jogador**, o que também troca o prefixo no tab. `MuxeCore.get().nameTags().override(jogador -> Optional.of(tag))` responde um `NameTag(time, prefixo)` para os jogadores que quiser e `Optional.empty()` para os outros, que mantêm a tag do cargo. Chame `nameTags().refresh(jogador)` depois que a resposta mudar, e `clearOverride()` quando o plugin parar. O BedWars usa isso para a letra do time, como `§9A §9`.

## Comandos

Os comandos são classes anotadas registradas com `Commands`; veja [Comandos](../commands/README.pt-BR.md).

## Adicionando um utilitário

1. Coloque-o no módulo certo: `core:bukkit` se ele precisa da API do Bukkit, `core:common` caso contrário.
2. Dê a ele um pacote que nomeie a sua área (`text`, `effect`, `item`, `menu`, …).
3. Separe a lógica que pode ser testada sem servidor da chamada ao Bukkit, e teste-a.
4. Documente-o nesta página, em inglês e em português.
