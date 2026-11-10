[English](README.md) | **Português**

# API do core

O que o Muxe-Core oferece aos outros plugins. Utilitários reutilizáveis ficam aqui, no core, em vez de serem reescritos em cada feature: quando uma feature precisa de algo genérico (títulos, action bars, efeitos, menus, itens, scoreboards), isso é adicionado ao core e documentado nesta página.

## Obtendo o core

```java
MuxeCore core = MuxeCore.get();
```

Os plugins declaram `depend: [Muxe-Core]` no `plugin.yml` e `compileOnly(projects.core.bukkit)` no Gradle.

O Muxe-Core carrega todas as classes de `br.com.muxe.core` quando o servidor o carrega (`ClassPreloader`, no `onLoad`). O class loader de plugins do 1.8 não é seguro quando uma thread de outro plugin e uma thread do core carregam a mesma classe ao mesmo tempo (falha com `LinkageError: duplicate class definition`), e carregar tudo antes elimina essa corrida.

| Método | O que oferece |
|---|---|
| `identity()` | O id, o grupo e o tipo deste servidor. |
| `packets()`, `packetRegistry()`, `requestTimeout()` | O [barramento de pacotes](../../architecture/messaging/README.pt-BR.md). |
| `commands()`, `queries()` | Os barramentos de CQRS. Registre os handlers ao habilitar. |
| `transactions()`, `migrations()` | Transações do PostgreSQL e migrações Flyway do seu módulo. Escreva os repositórios com o [schema tipado e os comandos](../../architecture/persistence/README.pt-BR.md#schema-tipado-e-comandos) (`Table`, `Column`, `Sql`). |
| `store()` | `ExpiringStore`: valores de curta duração no Redis, sempre com expiração. |
| `metrics()` | O registro de métricas. |
| `servers()` | O diretório de servidores vivos. |
| `network()` | `NetworkGateway`: envia um jogador para um servidor ou grupo. |
| `mainThread()`, `async()` | Executores para a thread do servidor e para trabalho bloqueante. `async()` não é limitado: envie trabalho de banco como comando ou consulta, cujo bus é limitado (veja [Contrapressão](../../architecture/observability/README.pt-BR.md#contrapressão)). |
| `clock()` | O relógio para injetar nos seus serviços. |
| `messages()` | Mensagens compartilhadas do Muxe-Core. |
| `permissions()` | Cargos, tags e permissões dos jogadores online (veja [Cargos e permissões](#cargos-e-permissões)). |
| `parties()` | As parties da rede como este servidor as vê (veja [Parties](#parties)). |

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
- Todo servidor limpa o título quando um jogador entra (`LeftoverTitleCleaner`), antes de qualquer plugin mandar o seu. O Velocity só faz um reset do título na troca de servidor, o que deixaria o subtítulo do servidor anterior na tela.

### Action bar

```java
ActionBar.send(player, "&eDigite o código em frente no chat!");
```

O Spigot 1.8.8 não tem API de action bar, então isso envia o pacote da 1.8 pelo `LegacyPackets` (reflexão sobre os internos do servidor, resolvida uma única vez). Se o servidor não os expuser, `send` retorna `false` e registra um único aviso; nada mais quebra. `ActionBar.isSupported()` diz se funciona.

O cliente 1.8 desenha a action bar a partir do texto puro da mensagem e descarta as cores do JSON, então as cores `&` ficam dentro do texto como códigos `§`, que o cliente desenha.

### Configuração do plugin

```java
FileConfiguration config = PluginConfig.load(plugin);
```

`load` salva o `config.yml` padrão quando não há nenhum, depois escreve no arquivo do servidor todas as chaves do `config.yml` do jar que faltam nele, sem mudar os valores que já estão lá, e lê o arquivo de novo. Um plugin atualizado com seções ou mensagens novas continua funcionando num servidor cujo arquivo foi criado por uma versão antiga. O BedWars carrega a configuração assim.

### Editor de configuração

A equipe vê e altera o `config.yml` de um plugin dentro do jogo, sem editar arquivos. As peças ficam em `br.com.muxe.core.bukkit.config.edit`, e o comando em `br.com.muxe.core.bukkit.command.config`.

```java
ConfigReloads reloads = new ConfigReloads()
    .register("scoreboard", "scoreboard", config -> template.reload(() -> ScoreboardTemplate.from(config)));
ConfigEditor editor = new ConfigEditor(new PluginConfigStore(plugin, core.async()), reloads);
ConfigPathSuggestions.register(commands, editor);
commands.register(new LobbyConfigCommand(editor, new ConfigReplies(messages)));
```

- **Armazenamento.** O `PluginConfigStore` trabalha na `FileConfiguration` viva do plugin (`plugin.getConfig()`), então o que outro código do plugin salva nunca sobrescreve uma mudança da equipe. A mudança é gravada no arquivo no executor assíncrono do core pelo `LatestWriter`: o texto é montado na thread principal e, quando várias mudanças se acumulam, só o conteúdo mais recente é gravado. O resto do arquivo é mantido como o salvamento YAML do Bukkit o escreve (comentários não são mantidos, como no `saveConfig`).
- **Caminhos.** Palavras de letras, dígitos, `_` e `-` unidas por pontos (`ConfigPaths`). Todo caminho com `password`, `senha`, `secret`, `token` ou `credential` é protegido: nunca é mostrado, completado ou alterado. Uma seção nunca é trocada por um valor, e um valor nunca vira uma seção.
- **Valores.** O `ConfigValues` deduz o tipo pelo texto digitado: `true`/`false`, números inteiros, decimais e texto. Quando a chave já tem um valor, o tipo dele vence: uma chave numérica recusa texto, uma chave decimal aceita `27` e guarda `27.0`, uma chave de texto guarda `123` como texto. Durações como `60s` são texto. `"123"` entre aspas força texto. Uma lista é definida com valores separados por vírgula (`a, b, c`, ou `[]` para esvaziar) e os itens seguem o tipo do primeiro; `adicionar` e `remover` mexem em um item, vírgulas incluídas.
- **Locais.** `LocationValues.of(world, x, y, z, yaw, pitch, centered)` arredonda as coordenadas para centésimos e os ângulos para décimos; com `centered` as coordenadas horizontais vão para o meio do bloco (`.5`). É gravado como `x`, `y`, `z`, `yaw`, `pitch` sob o caminho, e também `world` quando a seção já tem uma chave `world`, ainda não existe, ou `--mundo` é usado.
- **Caminhos desconhecidos.** O editor só altera chaves que existem no arquivo ou nos padrões do jar; `--novo` no fim do comando cria uma nova.
- **Aplicação ao vivo.** Um componente registra um `Reloadable` para um prefixo de caminho com `ConfigReloads.register(name, prefix, callback)`. Depois de uma mudança, todo componente cujo prefixo cobre o caminho alterado (ou está abaixo dele) é chamado uma vez com a configuração do plugin; a equipe vê os nomes que foram aplicados, os que falharam com o motivo, ou que a mudança só vale depois de reiniciar. As configurações que os componentes leem por um `LiveValue<T>` (pacote `br.com.muxe.core.common.config.live`) são trocadas por `reload(loader)`, que mantém o valor antigo quando o loader lança exceção; quem já pegou uma cópia continua usando ela, então uma sessão em andamento nunca muda no meio. O editor lembra os caminhos alterados que nenhum componente cobre e o `recarregar` os lista.

O `ConfigCommand` é o comando abstrato que cada plugin estende com o seu nome e `@Permission(ConfigCommand.PERMISSION)`: `/coreconfig` (Muxe-Core), `/lobbyconfig` (lobby) e `/bwconfig` (BedWars). A permissão é `muxe.admin.config` (padrão op), declarada pelo Muxe-Core.

| Subcomando | O que faz |
|---|---|
| `ver [caminho]` | Mostra um valor e o seu tipo, os itens de uma lista ou as chaves de uma seção (as com `.` no fim são seções). Sem caminho lista o nível de cima. |
| `definir <caminho> <valor...> [--novo]` | Define um valor, salva e aplica ao vivo quando um componente o cobre. |
| `adicionar <caminho> <valor...>` | Acrescenta um item a uma lista. |
| `remover <caminho> <valor...>` | Remove um item de uma lista. |
| `local <caminho> [--novo] [--centro] [--mundo]` | Guarda o local de quem enviou (só jogadores) sob o caminho. |
| `recarregar` | Lê o arquivo de novo, aplica todos os componentes ao vivo e lista as mudanças que esperam um reinício. |

O Tab completa os subcomandos e os caminhos, um nível por vez.

```text
/lobbyconfig ver scoreboard
/lobbyconfig definir scoreboard.title &b&lMUXE
/lobbyconfig adicionar scoreboard.lines &7Novo
/lobbyconfig local spawn --novo
/bwconfig definir timings.respawn-seconds 7
/coreconfig definir join-greeting.delay 1s
```

Os textos são `messages.config.*` no `config.yml` do Muxe-Core, que todo plugin herda.

### Catálogo de mensagens

```java
MessageCatalog messages = MuxeCore.get().messages().merge(MessageCatalog.from(LocalizedConfig.load(this), "messages"));
player.sendMessage(messages.get(player, "wrong-password", "remaining", "3"));
messages.lines(player, "challenge").forEach(player::sendMessage);
```

Passe como primeiro argumento o jogador que vai ler o texto (`CommandSender`, `UUID` ou `Language`), para a mensagem sair no idioma dele. O `get(key)` sem leitor sai em português e serve para o console e os logs.

Os placeholders são escritos como `{name}` na configuração. `merge` combina catálogos (as mensagens do próprio plugin têm prioridade). Chaves que faltam no arquivo do servidor vêm dos padrões do jar, então uma mensagem adicionada numa versão nova funciona num servidor cujo `config.yml` foi criado por uma versão antiga.

Uma mensagem pode ser uma lista YAML. `lines(key)` devolve uma entrada por linha, com os itens vazios (`""`) convertidos em uma linha que o chat mostra em branco, e `get(key)` junta as linhas com quebras de linha, o que serve para telas de expulsão. O `ProxyMessages` do proxy aceita listas do mesmo jeito.

```yaml
messages:
  challenge:
    - "&6&lDESAFIO:"
    - "&eDigite o &bcódigo &eem frente &ano chat&e!"
    - ""
    - "&7&o(É necessário ativar os hologramas para visualizar corretamente)"
```

### Idiomas

Todo jogador tem um idioma: português (o padrão) ou inglês. Veja [Idioma](../../features/language/README.pt-BR.md).

- `MuxeCore.get().languages()` (`PlayerLanguages`) cuida do idioma de cada jogador. O `of(player)` diz qual é, o `change(player, language)` troca, e o `onChange(player -> ...)` é chamado na thread principal depois de uma troca. Use esse callback para remontar o que o jogador já está vendo.
- `LocalizedConfig.load(plugin)` lê o `config.yml` e o `lang/en.yml` do plugin.
  - `in(language)` é a raiz da configuração de um idioma. O inglês fica por cima do português, chave por chave.
  - `map(root -> Settings.from(...))` monta um `Localized<Settings>`, um por idioma. Leia com `settings.in(languages.of(player))`.
  - `MuxeCore.get().texts()` é o do próprio core.
- Um texto enviado a muitos jogadores nunca é montado uma vez só. Monte por leitor, ou passe um `Text` (`messages.text(key, ...)`) e chame `text.in(language)` para cada um.
- Um valor que também é traduzido, como o nome de um time ou de um modo, vai como placeholder `Text`: `messages.get(player, "team-eliminated", "team", Text.of("Vermelho", "Red"))`.
- `LocalizedConfig.bundled(MeuPlugin.class, "config.yml").untranslated(Language.ENGLISH, "")` lista os textos legíveis em português que não têm versão em inglês. Todo plugin tem um teste que garante que essa lista está vazia.

```java
LocalizedConfig texts = LocalizedConfig.load(this);
Localized<ShopSettings> shop = texts.map(root -> ShopSettings.from(root.getConfigurationSection("shop")));
Text announcement = messages.text("bed-destroyed", "team", team.name());
for (Player player : players) {
    player.sendMessage(announcement.in(messages.language(player)));
}
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

### Perguntas na bigorna

```java
AnvilPrompts anvils = AnvilPrompts.start(plugin);
anvils.ask(player, "Insira o usuário", nome -> procurar(nome));
```

- Abre uma bigorna funcionando (o `ContainerAnvil` do 1.8, sem precisar de bloco) com um papel com o nome do texto de exemplo. O jogador digita no campo de renomear e clica no slot de resultado; o texto sem cores chega ao callback na main thread, e a bigorna fecha.
- Cliques em outros lugares são cancelados, e fechar a bigorna não deixa cair nada. Texto vazio ou igual ao de exemplo é ignorado (`AnvilText.typed`).

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
- `TextHolograms.standard(spacing)` usa outro espaçamento. `show(player, top, lines, gaps)` e `layout(top, lines, gaps)` dão a cada par de linhas o seu próprio espaço, de cima para baixo; as linhas além da lista usam o espaçamento, e `depth(gaps, line)` diz o quanto uma linha fica abaixo do `top`.
- Mesmo transporte dos hologramas de pixels: suportes de armadura invisíveis enviados só para o `player`.
- A altura de uma linha é onde o texto dela começa: o topo do texto fica 0,09375 bloco acima dela. O cliente 1.8 desenha o nome de um suporte de armadura pequeno como se ele tivesse o tamanho normal (1,975 bloco), então o `PacketHologramTransport` cria cada suporte `LABEL_HEIGHT` (1,39375) bloco abaixo da linha.
- `hologram.rename(lines)` troca o texto de um holograma que mantém o mesmo número de linhas, sem criá-lo de novo; devolve `false` quando as linhas não batem e o holograma precisa ser mostrado de novo.
- Um clique num holograma chega a um `PacketInterceptor` com o id de um suporte de armadura (`hologram.owns(id)`), mas o suporte fica bem abaixo do texto. Para saber em que parte do texto o jogador mirou, `LineAim.heightAt(player.getEyeLocation(), x, z)` dá a altura em que a visão do jogador passa mais perto da coluna do holograma.

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

### Sons

```java
Optional<SoundCue> cue = SoundCue.from(config.getConfigurationSection("parkour.sounds.start"));
cue.ifPresent(sound -> sound.play(player));
```

- `from(section)` lê `sound` (nome de `Sound` do 1.8), `volume` e `pitch` (1.0 por padrão). Sem a seção ou com `sound` vazio, não há som; um som desconhecido é um erro. Chaves que faltam no arquivo do servidor vêm dos padrões do jar, então um som adicionado numa versão nova toca num servidor cujo `config.yml` foi criado por uma versão antiga.
- `play(player)` toca só para aquele jogador, na posição dele. O [parkour](../../features/parkour/README.pt-BR.md) usa.

## Missões

Pacotes `br.com.muxe.core.common.mission` e `br.com.muxe.core.bukkit.mission`. Veja [Missões](../../features/missions/README.pt-BR.md).

```java
MissionConfig missions = MissionConfig.from(config.getConfigurationSection("missions"), MyGoal::find);
core.missions().register("mygame", missions.catalog(), (player, completed, at) -> giveGameXp(player, completed));
MissionMenus menus = new MissionMenus("mygame", missions, core.queries(), core.mainThread(), core.clock(), MissionModule.ZONE, logger);

MissionTracker tracker = new MissionTracker(core, "mygame", missions, "room " + code, logger);
tracker.begin(players);
tracker.check(id -> Optional.of(Map.of("FINAL_KILLS", finals(id))));
tracker.finish(id -> Optional.of(totals(id)));
```

- Um jogo declara os seus objetivos como `MissionGoal(id, best)` e informa, para cada jogador, os totais da partida por id de objetivo. O tracker anuncia as conclusões na hora, salva e manda o resumo no fim.
- O core paga os créditos; a recompensa do jogo roda na mesma transação.

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

### Máscaras de bloco

`BlockMask.start(plugin)` (pacote `br.com.muxe.core.bukkit.nms`) devolve uma máscara que mostra a um jogador um bloco falso no lugar de um real, e o mantém falso mesmo quando o servidor envia o bloco real de novo: o servidor 1.8 reenvia um bloco quando uma quebra é abortada ou um clique é cancelado, sem nenhum evento do Bukkit.

```java
Optional<BlockMask> mask = BlockMask.start(plugin);
mask.ifPresent(blocks -> blocks.hide(player, location, Material.AIR, (byte) 0));
mask.ifPresent(blocks -> blocks.reveal(player, location));
```

- Um handler Netty de saída troca todo `PacketPlayOutBlockChange` de uma posição mascarada, só para aquele jogador, por um pacote novo com o bloco falso. O pacote original nunca é alterado, porque a mesma instância pode ir para outros jogadores.
- `reveal` tira a máscara e envia o bloco real. Sair do servidor tira todas as máscaras do jogador.
- Pacotes de vários blocos e de chunk não são mascarados.
- A prévia de cosméticos do BedWars usa isso para manter a cama escondida enquanto a animação roda.

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
- `MenuButton.animated(quadros, período, clique)` é um botão cujo ícone passa por `quadros`, um a cada `período` ticks, enquanto o menu está aberto (o `MenuAnimator` do core faz todo menu aberto andar); com um quadro só ele é um botão comum.
- Todo clique dentro de um menu é cancelado: os jogadores nunca pegam nem largam itens dele.
- Os títulos são traduzidos e cortados nos 32 caracteres que a 1.8 permite.
- Abrir um menu com o mesmo título e as mesmas linhas do menu que o jogador já tem aberto redesenha o aberto no lugar (`Menu.sameLayout`), então um menu que se reabre depois de um clique não fecha nem manda o cursor de volta para o meio. Os botões e o `onClose` do menu novo passam a valer.

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

- Uma scoreboard por jogador, com até 15 linhas. Cada linha fica em um time, então atualizar uma linha nunca faz a sidebar piscar; só as linhas que mudaram são enviadas, e os scores só são reescritos quando uma linha troca de entrada ou o número de linhas muda.
- Linhas de até uns 30 caracteres visíveis são divididas entre o prefixo e o sufixo do time (16 caracteres cada na 1.8), levando a cor junto (`SidebarText.split`). Linhas maiores usam também o nome da entrada da linha no meio (até 40 caracteres, começando com um código de cor escondido que mantém as linhas únicas), então uma linha chega a uns 70 caracteres (`SidebarText.layout`); só essas linhas trocam de entrada quando mudam. Linhas em branco funcionam.
- `core.sidebars()` (`SidebarOverrides`) deixa um plugin trocar a sidebar que outro plugin desenha: `provide(player -> Optional.of(new SidebarContent(title, lines)))`. A scoreboard do lobby pergunta a ele a cada atualização; o lobby do BedWars usa isso para o anexo 1. `clear()` devolve a sidebar.
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
- `NpcService.start(plugin, npc -> layout, lines)` escolhe um layout por NPC (o lobby do BedWars dá ao NPC de estatísticas os seus próprios espaços).
- `NpcService.start(plugin, layout, lines)` posiciona as linhas com um `NpcLabelLayout(bottom, gaps, spacing)`: a última linha `bottom` blocos acima dos pés e as outras subindo pelos `gaps`, depois pelo `spacing`, com o `LabelStack.upward`. `NpcLabelLayout.from(section)` lê isso da config; sem uma, o `NpcLabelLayout.STANDARD` deixa a última linha 2,05 blocos acima e as outras a 0,28 de distância.
- `new Npc(id, location, skin, Optional.of(item))` coloca um item na mão do NPC, enviado a cada jogador como equipamento logo depois que o NPC aparece. `PlacedNpcs.hand(HeldItem)` transforma um material e um data guardados nesse item.
- `MojangSkins` busca a skin assinada de um nome original (`api.mojang.com` e `sessionserver.mojang.com`). Guarde o resultado em vez de buscar a cada inicialização.
- `npcs.visibleTo((viewer, npc) -> ...)` esconde um NPC dos jogadores que a regra recusa, como se estivessem fora de alcance. Com isso um NPC pode existir para um jogador só, como o NPC de estatísticas do BedWars, que veste a skin de quem olha para ele.
- Quando só o texto das linhas muda e elas continuam nos mesmos lugares, o holograma do NPC renomeia os suportes de armadura com um pacote de metadata em vez de ser removido e criado de novo, então um texto que pisca não treme.
- `PlayerSkins.of(player)` (pacote `br.com.muxe.core.bukkit.nms`) lê a skin assinada de um jogador online pelo perfil dele, para vestir um NPC como aquele jogador.
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
- Nos tipos de `adventure-only` (`LOBBY` e `AUTH`), qualquer modo de jogo que não seja Aventura é cancelado, exceto o Criativo no modo construção (`BuildRules.allowsGameMode`). Um plugin que precisa de outro modo por um momento chama `releaseGameMode(player)` antes de trocá-lo e `lockGameMode(player)` quando devolve o jogador, como a prévia de cosméticos do BedWars faz com o Sobrevivência (um cliente 1.8 no Aventura não consegue bater em blocos). Sair do servidor trava o jogador de novo.
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

Um jogo é acessado pelo seu lobby: o grupo `<jogo>-lobby` (como `bedwars-lobby`) quando um servidor dele está vivo, senão o próprio grupo do jogo. `ServerDirectory.entryGroup(jogo)` dá esse grupo e `gamePlayers(jogo)` conta os dois.

- `players(group)` soma os jogadores online dos servidores vivos do jogo e do seu lobby.
- `connect` responde `game-unavailable` quando nem o lobby nem o jogo têm servidor vivo. Senão, pede ao proxy, sem mensagem no chat, que mande o jogador para o servidor com menos jogadores do grupo de entrada, com `connect-failed` se isso falhar.
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

## Mundos

- Todo mundo de todo servidor Spigot fica ao meio-dia e sem chuva: o `ClearMidday` (pacote `br.com.muxe.core.bukkit.world`) desliga o `doDaylightCycle`, põe a hora em 6000 e limpa o clima de todo mundo carregado, inclusive os que um plugin cria depois, como os mundos das partidas, e cancela toda mudança para chuva ou trovoada. Os plugins não precisam fazer nada.

## Créditos

Pacote `br.com.muxe.core.bukkit.credit`. O comportamento está em [Créditos e cosméticos](../../features/credits/README.pt-BR.md).

- `CreditBalancesListener.start(plugin, core, Duration.ofSeconds(30))` devolve um `CreditBalances` que carrega o saldo de cada jogador ao entrar, esquece ao sair e recarrega todos no período dado. `of(playerId)` lê o valor em cache (0 até carregar) sem bloquear; `refresh(playerId)` recarrega um agora.
- `CreditFormat.format(1750)` dá `1.750`.
- Todo menu de cosméticos lista primeiro os cosméticos possuídos, depois os outros, cada grupo em ordem alfabética sem diferenciar acentos e maiúsculas: ordene com `CosmeticOrder.ownedFirst(wallet::owns, Cosmetic::name)` (core:common).
- Créditos e cosméticos mudam pelos comandos do core: `ChangeCreditsCommand`, `BuyCosmeticCommand`, `GrantCosmeticCommand`, `SelectCosmeticCommand`; leituras por `FindCreditsQuery` e `FindCosmeticsQuery`.

## Animações de cama

Pacotes `br.com.muxe.core.bukkit.cosmetic.animation` (puro) e `br.com.muxe.core.bukkit.cosmetic.stage` (NMS).

- Uma `BedAnimation` escreve um roteiro: com o `BedFrame` (pé, cabeceira, frente, direita), a posição de quem quebrou, um `Stage` e uma `Timeline`, ela agenda o que acontece em cada tick.
- O `Stage` cria atores só de pacote (armor stands, mobs, falling blocks, itens, cabeças soltas como item com textura de skin pelo `skullItem`, fogos, bolas de neve), manda partículas e sons e finge mudanças de bloco. O `Actor` move, equipa, posiciona, monta e remove; `target(outro)` faz um guardião disparar o raio em outro ator (as outras entidades ignoram).
- `Timeline.at(tick, ação)` e `repeat(de, até, período, ação)` agendam passos; `lasts(ticks)` mantém a animação viva por mais tempo.
- `AnimationPlayer.play(animação, mundo, cama, quebrador, espectadores, aoTerminar)` roda um passo por tick só para os espectadores. Quando a timeline acaba, chama `aoTerminar` e o efeito fica como está: atores, blocos falsos, assentos e itens dados continuam até nenhum espectador estar online naquele mundo (no BedWars, até os jogadores saírem da partida; na prévia, até o espectador ser teleportado de volta). Só então o palco fecha, e os blocos e chunks falsos são reenviados só para quem ainda está naquele mundo, então ninguém nunca vê o mapa voltando ao normal. `cancel()` congela o efeito onde está e tira os jogadores dos assentos (para o espectador poder ser teleportado), e o palco fecha do mesmo jeito.
- `Fireballs.accelerate(bolaDeFogo, direção, aceleração)` (em `nms`) para uma bola de fogo lançada e define a aceleração como a direção unitária vezes `aceleração`, sem desvio aleatório, para ela acelerar como uma vanilla (o BedWars usa 0,1).
- Uma `SceneAnimation` é o mesmo sem cama: o roteiro recebe uma `Scene` (origem, direção horizontal e uma textura de skin opcional, como a da vítima), e `AnimationPlayer.play(animação, mundo, cena, espectadores, aoTerminar)` a roda. Os efeitos de vitória e de kill final do BedWars usam isso.
- Cenas interativas: uma `Scene` também leva `riders` (ids de jogadores, o principal primeiro). `Stage.mount(assento, yaw, riders)` senta eles em armor stands marcadores invisíveis de verdade (um para cada, enfileirados atrás do primeiro; sair é cancelado e os assentos não levam dano) e devolve um `Mount` que o roteiro move a cada tick (`move`), lendo para onde o primeiro olha (`lookYaw`, `lookPitch`) e se ele segura W (`pushing`). `give(jogador, slot, material, nome, aoClicar)` põe na hotbar um item cujos cliques esquerdo e direito chamam de volta (não dá para dropar), `push(jogador, velocidade)` lança um jogador, `letFly(jogador)` deixa um jogador voar até o palco fechar (quem não podia voar antes para de voar nessa hora e não leva dano de queda), `eyes`/`look` leem onde ele está e para onde olha, `hook(posição, dono)` mostra um anzol cuja linha o cliente desenha até o dono, `tnt(posição, velocidade)` lança uma TNT acesa, `fireball(posição, direção)` mostra uma bola de fogo grande que o cliente curva ao longo de `direção` (o roteiro a move e a remove no impacto), `breakEffect(x, y, z, material, data)` mostra as partículas e o som de bloco quebrando, `rain(força)` faz a chuva aparecer para quem vê (de 0 a 1) e devolve o clima do mundo quando o palco fecha, `leash(ator, jogador)` prende um ator do palco a um jogador de verdade com uma corda, `Mount.strafing()` lê o A/D do primeiro jogador montado (positivo para a esquerda), `silentLightning(posição)` solta um raio sem o som de trovão, uma `Scene` pode levar a skin assinada da vítima (`body`, definida com `withBody`), e `player(posição, yaw, skin)` faz o mesmo com uma `SignedSkin` dada (sem a assinatura, ela pega a assinatura de um jogador online que usa a mesma textura) (o valor da textura e a assinatura da Mojang, que o cliente 1.8 exige para mostrar a skin), e `player(posição, yaw)` mostra um jogador falso com um nome aleatório de 8 caracteres e o nome em cima dele escondido (o roteiro desenha a própria etiqueta), com a skin de um jogador online aleatório que não está vendo o efeito, ou uma skin padrão quando não tem mais ninguém (sai do tab depois de 2 segundos), `lightning(posição)` solta um raio inofensivo e `time(horaDoDia)` muda a hora do dia para quem vê. Para mudanças grandes no terreno, `type(x, y, z)` lê o bloco real (ar em chunks não carregados) e `blocks(mudanças)` manda muitos blocos falsos de uma vez, um pacote de vários blocos por chunk, `sections(mudanças)` faz o mesmo para preenchimentos enormes guardando uma cópia falsa de cada chunk tocado (feita do chunk real, com a luz, e seções vazias com luz do céu cheia) e mandando as seções tocadas inteiras, um pacote de chunk por chunk, para o cliente não recalcular a luz bloco a bloco, e `highest(x, z)` lê o y do bloco não-ar mais alto de uma coluna (-1 quando está vazia ou não carregada); esses chunks são reenviados inteiros no fim. Quando o palco fecha os assentos somem e os jogadores vão para o chão embaixo, os itens dados são retirados se ainda estiverem no slot, os blocos falsos voltam para quem ainda está no mundo e a hora volta ao normal.
- Uma `Scene` também tem uma `area` (`Area`, uma caixa em x e z): o mapa que o efeito pode usar, em volta da origem quando quem chama não passa nenhuma (`Scene.REACH`, 16 blocos). O meio dela (`middleX`, `middleZ`) é o centro da caixa, a não ser que quem chama defina outro com `area.middleAt(x, z)`, como o BedWars faz com os geradores de esmeralda. `area.pick(aleatório, y)` sorteia um ponto dentro dela e `area.copies()` diz quantas vezes repetir o que cai do céu (1, ou 2 em áreas maiores que 32 por 32).
- Uma `Scene` também pode levar um `dye` (`OptionalInt`, uma cor de lã de 0 a 15, definido com `withDye`), como a cor do time da vítima, para efeitos que tingem blocos ou partículas; fica vazio quando quem chama não conhece nenhuma.
- `Ground.surface(sólido, x, y, z)` acha o topo do chão abaixo de um ponto (de 2 blocos acima até 32 abaixo, ignorando tetos), então os efeitos começam no chão onde quer que aconteçam; `Stage::solid` ou uma consulta ao mundo servem de teste `sólido`. `Ground.hit(sólido, x, topo, z, profundidade)` é para algo caindo: o topo do primeiro bloco sólido de `topo` até `profundidade` blocos abaixo, ou vazio sobre o void.
- `BedFrame.ofBlock(x, y, z, data)` monta o frame a partir de qualquer metade de uma cama 1.8.
- Quem chama também pode conduzir um `PacketStage` sozinho, sem timeline: `new PacketStage(world, viewers, MountGuard.of(plugin))`, chamar os métodos do `Stage` quando quiser e fechá-lo com `close()` para remover todo ator e bloco falso que ele criou. Os rastros de projétil do BedWars abrem um por voo e o fecham depois do `linger` do rastro.
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

- **Um plugin pode trocar o name tag de um jogador**, o que também troca o prefixo no tab. `MuxeCore.get().nameTags().override(jogador -> Optional.of(tag))` responde um `NameTag(time, prefixo)` para os jogadores que quiser e `Optional.empty()` para os outros, que mantêm a tag do cargo. Chame `nameTags().refresh(jogador)` depois que a resposta mudar, e `clearOverride()` quando o plugin parar. O BedWars usa isso para a letra do time, como `§9§lA§9 `. Os times dos name tags deixam os companheiros se verem meio transparentes quando invisíveis.
- `ArmorVisibility.hide(quemVê, alvo)` (pacote `br.com.muxe.core.bukkit.nms`) manda para `quemVê` uma armadura vazia do `alvo` sem mexer na armadura de verdade, e `show(quemVê, alvo)` manda a de verdade de volta. O BedWars usa isso para jogadores invisíveis.
- `MatchReturns` (pacote `br.com.muxe.core.common.match`) guarda, num `ExpiringStore`, o servidor para onde um jogador pode voltar: `remember(jogador, servidor, ttl)`, `serverOf(jogador)` e `forget(jogador)`. O `/retornar` lê isso em todo servidor.

## Parties

Pacote `br.com.muxe.core.bukkit.party`. O Muxe-Core roda a [party](../../features/party/README.pt-BR.md) em todo servidor, menos nos de autenticação: `/party`, `/pc`, `/canal`, o canal de chat da party e a cópia de cada party enviada pelo proxy. Os jogos só leem.

```java
Parties parties = MuxeCore.get().parties();
Optional<PartySnapshot> party = parties.of(player.getUniqueId());
if (!parties.mayQueue(player.getUniqueId())) { ... }
List<UUID> companions = parties.companions(leader);
List<List<UUID>> groups = parties.groupsOf(playersInTheRoom);
parties.onGather((leader, members) -> acceptedMembers);
```

- `of(id)` dá a party de um jogador (`PartySnapshot`: id, líder, se está aberta e os membros com o estado online); `mayQueue(id)` só é `false` para um membro que não é o líder; `companions(leader)` lista os membros online além do líder.
- `groupsOf(players)` dá as parties entre esses jogadores, que é o que `PartyGroups` e `TeamAssigner` recebem.
- `onGather(handler)` responde o `/party teleportar` neste servidor: com o líder e os membros, devolve quem pode vir, na thread principal. O padrão aceita todos; a [API de minigames](../../architecture/minigame-api/README.pt-BR.md#parties) reserva vagas ou vagas de espectador.
- `NetworkPlayers` guarda os nomes dos jogadores online na rede (`network.players`, a cada 3 segundos, mais os deste servidor); o provedor de sugestões `network-players` usa ele.
- As regras ficam no `core:common` (pacote `br.com.muxe.core.common.party.domain`): `PartyBook` (as parties do proxy, testado com um `MutableClock`) e `PartyDirectory` (a cópia em cada servidor). `ChatChannels` (pacote `br.com.muxe.core.common.chat`) guarda o canal de chat escolhido em um `ExpiringStore`.

## Knockback

Pacote `br.com.muxe.core.bukkit.combat.knockback`, regras em `br.com.muxe.core.common.combat.knockback`. Cada modo de jogo tem o seu perfil de knockback; um jogador sem perfil fica com o knockback do próprio servidor.

```java
KnockbackService knockback = MuxeCore.get().knockback();
KnockbackProfile bedwars = knockback.named("bedwars").orElseThrow();
knockback.assign(player.getUniqueId(), bedwars);
knockback.clear(player.getUniqueId());
```

- **Perfis.** `KnockbackProfile` guarda os valores do menu da sala: `vertical`, `horizontal`, `vertical-boost`, `horizontal-boost`, `vertical-limit`, `vertical-friction`, `horizontal-friction`, `attacker-multiplier`, `restore-velocity`, `simulate-y`, `keep-sprint`, `ignore-sprint`, `use-rotation` e `click-limiter` (`off`, `low`, `normal`, `high`). `KnockbackSetting` lê e escreve cada um pela chave, que é o que a sala custom e as predefinições usam. `KnockbackProfile.VANILLA` é o knockback do 1.8.
- **Perfis com nome.** `knockback.profiles.<nome>` no `config.yml` do Muxe-Core (toda chave é opcional sobre os valores vanilla): `vanilla`, `bedwars`, `skywars` e um `duels-<modo>` para cada modo de Duels da referência. `knockback.click-limiter-ticks` dá os ticks de cada nível do limitador (`off` 0, `low` 2, `normal` 4, `high` 6). Um jogo escolhe os perfis no próprio `config.yml` (veja a [API de minigames](../../architecture/minigame-api/README.pt-BR.md#configuração)).
- **Como um golpe empurra.** Num golpe corpo a corpo entre dois jogadores, o `EntityDamageByEntityEvent` (monitor) lê a vítima e o atacante direto dos objetos do servidor e calcula o empurrão no `KnockbackState` da vítima. O servidor dispara o `PlayerVelocityEvent` desse mesmo golpe logo depois; o listener coloca nele o empurrão calculado, então o cliente recebe um único pacote de velocidade com o novo valor. Nada é alocado por golpe: um `KnockbackHit`, um `Motion` e um `Vector` por jogador são reaproveitados.
- **As regras** (`KnockbackMath`, `KnockbackState`, testadas sem servidor): o movimento anterior é dividido pelos atritos, o empurrão base aponta para longe do atacante (ou na direção do olhar do atacante com `use-rotation`), a subida base é limitada por `vertical-limit` e cada nível de knockback (encantamento, mais o sprint quando não há `ignore-sprint`) soma os boosts na direção do olhar do atacante. Uma vítima ainda se recuperando do último golpe só recebe o boost. Sem `restore-velocity` o movimento anterior é o último empurrão que o jogador recebeu, freado pelos ticks desde então; com ele, o movimento do próprio servidor. `simulate-y` prevê a velocidade vertical da vítima a partir do último empurrão, dos ticks desde então e do ping. `attacker-multiplier` freia o movimento do próprio atacante depois de um golpe, `keep-sprint` mantém o sprint dele, e o limitador de cliques pula um empurrão que vem menos ticks depois do último do que o nível permite.

## Salas custom

Pacotes `br.com.muxe.core.bukkit.customroom` e `br.com.muxe.core.common.customroom`. Veja [Sala custom](../../features/custom-room/README.pt-BR.md).

```java
CustomRoomKit kit = MuxeCore.get().customRooms();
kit.options().open(player, target, schema.root());
kit.modes().open(player, choice);
kit.presets().open(player, presetTarget);
```

- **Opções** (`RoomOptions`, `core:common`): só os valores diferentes do padrão, como `chave=valor`; vão em pacotes e ficam guardadas nas predefinições.
- **Esquema** (`RoomSchema`, `OptionPage`, `OptionEntry`): as páginas de opções de um jogo, escritas em YAML pelo servidor de jogo e lidas com o `SchemaReader`. Tipos de entrada: `link`, `stepper` (`integer`, `multiplier` com vírgula, `seconds`; os limites podem ser `mode-team-size`, `mode-players` ou `mode-teams`), `toggle`, `decimal`, `choice`, `timer`, `team`, `shop-item`, `upgrade` e `free`.
- **Regras sem servidor**: `RoomEditor` lê cada valor e transforma um clique (`LEFT`, `RIGHT`, os com shift, `DROP`) em novas opções, uma página para abrir ou uma pergunta na bigorna, e confere as respostas; `RoomLore` monta nome, lore e ícone de cada entrada e dos editores da loja e das melhorias com os textos do `RoomTexts`.
- **Menus**: `OptionMenus` desenha qualquer página de um esquema para um `OptionsTarget` (o rascunho do lobby ou uma sala ativa); `ModeSelector`, `MapSelector` e `PresetMenu` são os menus de modo, mapas e predefinições.
- **`AnvilEdits`** (`prompt`): uma bigorna com um papel com o valor atual cujo resultado é renomeado enquanto o jogador digita (`&a<rótulo>: &f<valor>`); a resposta é o que foi digitado, mesmo sem mudar.
- **Predefinições**: `SavePresetCommand`, `DeletePresetCommand`, `ListPresetsQuery` e `FindPresetQuery` sobre `core.custom_room_presets`.
- **Lobby**: `CustomRoomLobby` e `/salacustom` rodam nos lobbies; `CatalogDirectory` guarda os catálogos que os servidores de jogo publicam.

## Comandos

Os comandos são classes anotadas registradas com `Commands`; veja [Comandos](../commands/README.pt-BR.md).

## Adicionando um utilitário

1. Coloque-o no módulo certo: `core:bukkit` se ele precisa da API do Bukkit, `core:common` caso contrário.
2. Dê a ele um pacote que nomeie a sua área (`text`, `effect`, `item`, `menu`, …).
3. Separe a lógica que pode ser testada sem servidor da chamada ao Bukkit, e teste-a.
4. Documente-o nesta página, em inglês e em português.
