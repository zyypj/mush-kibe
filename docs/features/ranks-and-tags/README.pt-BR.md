[English](README.md) | **Português**

# Cargos e tags

Cargos (grupos de permissões), tags (o que aparece antes do nome do jogador), medalhas (um símbolo mostrado antes da tag no chat) e os comandos para geri-los dentro do jogo. O modelo e o armazenamento ficam no `core:common` (pacote `br.com.muxe.core.common.permission`); todo servidor Bukkit os aplica pelo Muxe-Core (pacote `permission` do `core:bukkit`), então `hasPermission`, o chat, o tab e o nome acima da cabeça seguem os mesmos cargos na rede inteira.

## Histórias de usuário

- Como jogador, quero o meu cargo e a minha tag no chat, no tab e acima da minha cabeça, para que todos vejam.
- Como jogador, quero listar as tags que tenho, ver uma prévia e escolher uma com um clique.
- Como jogador, quero escolher uma das minhas medalhas, com prévia, para mostrá-la antes da minha tag no chat.
- Como staff, quero dar e tirar medalhas, temporárias ou para sempre.
- Como staff, quero dar e tirar cargos, temporários ou para sempre, promover e rebaixar por uma trilha e ver o que um jogador tem, sem sair do jogo.
- Como staff, quero definir permissões em jogadores e cargos com tab-complete e testar o que um jogador pode fazer.
- Como admin, quero abrir o editor web pelo jogo.

## Como funciona

### Hierarquia

Cargos são grupos. Cada cargo herda o de baixo, então um cargo maior tem todas as permissões dos menores. Todo jogador é `membro` sem nada guardado. O Admin tem `*`.

| Cargo | Peso | Tag | Herda |
|---|---|---|---|
| `membro` | 0 | `&7Membro` (sem prefixo) | |
| `vip` | 10 | `&aVIP` | `membro` |
| `mvp` | 20 | `&9MVP` | `vip` |
| `pro` | 30 | `&6Pro` | `mvp` |
| `ultra` | 40 | `&dUltra` | `pro` |
| `partner` | 110 | `&bPartner` | `ultra` |
| `partner+` | 120 | `&1Partner+` | `partner` |
| `mod` | 130 | `&5Mod` | `partner+` |
| `mod+` | 140 | `&5Mod+` | `mod` |
| `admin` | 150 | `&cAdmin` | `mod+`, mais `*` |

Trilhas listam cargos em ordem para `/cargo promover` e `/cargo rebaixar`: `doacao` (`membro`, `vip`, `mvp`, `pro`, `ultra`) e `staff` (`mod`, `mod+`, `admin`).

Um cargo é um nó `group.<id>` no jogador. Nós têm um valor (`true` ou `false`), contextos opcionais (`server=<grupo de servidores>`, `world=<mundo>`), uma expiração opcional e as datas em que foram adicionados e atualizados. Um nó com contextos só vale onde todos os contextos batem.

### Tags

Uma tag tem nome, cor, peso e prefixo. A exibição dela é a cor e o nome (`&aVIP`); o prefixo no chat é a cor, negrito, o nome em maiúsculas e um espaço (`&a&lVIP `). `membro` tem prefixo vazio, então o nome de um membro é só cinza (`&7tadeu`).

- **Tags de cargo** vêm com o cargo: ter `vip` (ou qualquer cargo acima) dá a tag `vip`.
- **Tags decorativas** não dão permissão; são só algo para mostrar: `2021` a `2026` (`&b`), `beta` (`&1`), `enderlore` (`&5`), `carnaval` (`&6`), `natal` (`&c`), `ferias` (`&a`) e `ferias-2` (`&2`). Um jogador tem uma pelo nó `muxe.tag.<id>`, dado com `/tag dar`.

A tag que um jogador mostra é, nesta ordem: a que ele escolheu com `/tag` (se ainda a tiver), a tag do seu maior cargo ou a sua tag de maior peso.

### Permissões nos servidores Bukkit

- O `AsyncPlayerPreLoginEvent` carrega os nós do jogador do PostgreSQL (até 5 segundos) antes de ele entrar. Se falhar, ele entra como `membro` e é atualizado quando a carga terminar.
- No login, o permissible do jogador (`CraftHumanEntity#perm`) é trocado, por reflexão, por um que pergunta primeiro aos cargos resolvidos. Quando um cargo não diz nada sobre uma permissão, o Bukkit decide como antes (op e os padrões do `plugin.yml`), então operadores continuam funcionando. Se o campo não puder ser trocado, as permissões são copiadas para um `PermissionAttachment` (aí os curingas só valem para permissões escritas por inteiro).
- As permissões são resolvidas com os contextos `server=<server.group>` e `world=<nome do mundo>`, e de novo quando o jogador troca de mundo, quando os nós dele ou qualquer cargo ou tag mudam em qualquer lugar da rede (`permission.changed`) e quando um nó temporário expira (verificado a cada segundo).
- Ao sair, o permissible original volta e o jogador é esquecido. Desligar o core devolve o permissible de todos os jogadores online.

### Prefixos

- **Chat:** `messages.chat-format`, por padrão `{medal}{prefix}{color}{player}: &f{message}`: `&a&lVIP &atadeu: &fOlá!`, ou `&7tadeu: &fOlá!` para um membro. Com uma medalha escolhida, o símbolo dela e um espaço vêm primeiro: `&e☕ &5&lENDERLORE &5tadeu: &fOlá!`.
- **Tab e nome acima da cabeça:** um time da scoreboard por cargo e tag. Mostram só a tag, nunca a medalha, porque o 1.8 permite 16 caracteres de prefixo. O prefixo do time é o prefixo do chat seguido da cor da tag (`§a§lVIP §a`), cortado nos 16 caracteres que o 1.8 permite sem quebrar um código de cor. Os times são nomeados pelo peso do cargo e depois pelo peso da tag (`849849admin` vem antes de `989989vip`), então o tab do 1.8 ordena os jogadores pela hierarquia.
- Os times são colocados em toda scoreboard que os jogadores usam (a principal e a sidebar de cada jogador) em até 5 ticks, e atualizados assim que uma tag ou cargo muda.
- A linha `Tag:` da scoreboard do lobby mostra a exibição da tag (`&aVIP`), e o perfil, o `/account` e o `/accountmenu` citam cargos do mesmo jeito (veja [Conta](../account/README.pt-BR.md)).

### `/tag`

- `/tag` lista as tags que o jogador tem numa linha do chat, do menor peso para o maior:

```text
Suas tags: Membro, VIP, MVP, Pro, Ultra, Enderlore, Carnaval, Natal
```

- `Suas tags: ` é `&a`, cada tag fica na sua cor e as vírgulas são `&f`. Cada tag roda `/tag <id>` ao ser clicada e, ao passar o mouse, mostra uma prévia do chat com aquela tag (e a medalha que o jogador mostra, se houver):

```text
Prévia:
VIP tadeu: Olá!

Clique para selecionar!
```

- `/tag <id>` seleciona uma tag que o jogador tem: `&aVocê selecionou a tag &5Enderlore &acom sucesso.` A escolha é salva no PostgreSQL (`SelectTagCommand`) e o chat, o tab, o nome acima da cabeça e a scoreboard mudam na hora em todos os servidores.
- Uma tag que o jogador não tem é recusada (`tag-locked`), uma desconhecida também (`tag-unknown`). Se o jogador perder depois a tag escolhida, ele volta a mostrar a tag do seu cargo.

### Medalhas

Uma medalha é um símbolo numa cor (`&e☕`) com um nome (`Café`) e um peso. O jogador tem uma medalha pela permissão `muxe.medal.<id>`, dada a um cargo ou ao jogador; o Admin tem todas pelo `*`. Ninguém mostra medalha até escolher uma: não há medalha padrão.

| Id | Medalha | Peso | Quem tem |
|---|---|---|---|
| `toxico` | `&a☣` Tóxico | 10 | VIP e acima |
| `yin-yang` | `&f☯` Yin Yang | 20 | VIP e acima |
| `musico` | `&5♫` Músico | 30 | Ultra e acima |
| `flash` | `&6⚡` Flash | 40 | Ultra e acima |
| `paz-e-amor` | `&c✌` Paz e Amor | 50 | Ultra e acima |
| `amor` | `&4❤` Amor | 60 | Ultra e acima |
| `cafe` | `&e☕` Café | 70 | Ultra e acima |
| `carinha` | `&6ツ` Carinha | 80 | VIP e acima |
| `halloween` | `&4✝` Halloween | 90 | dada individualmente |
| `natal` | `&b✽` Natal | 100 | dada individualmente |
| `cacador-de-bruxas` | `&2∅` Caçador de Bruxas | 110 | dada individualmente |
| `casa-de-gengibre` | `&6۩` Casa de Gengibre | 120 | dada individualmente |
| `alerta` | `&e⚠` Alerta | 130 | dada individualmente |
| `atomo` | `&b⚛` Átomo | 140 | dada individualmente |
| `anel` | `&6℧` Anel | 150 | dada individualmente |
| `boneco-de-neve` | `&f☃` Boneco de Neve | 160 | dada individualmente |
| `carnaval` | `&6✴` Carnaval | 170 | dada individualmente |
| `feiticeiro-aprendiz` | `&a⊰⊱` Feiticeiro Aprendiz | 180 | dada individualmente |
| `biscoito-de-gengibre` | `&6۞` Biscoito de Gengibre | 190 | dada individualmente |
| `liga-prata` | `&f✶` Liga Prata (Bed Wars Champions) | 200 | dada individualmente |
| `fogo` | `&c火` Fogo | 210 | dada individualmente |
| `agua` | `&b水` Água | 220 | dada individualmente |
| `ar` | `&f気` Ar | 230 | dada individualmente |
| `estrela` | `&e✯` Estrela | 240 | dada individualmente |
| `sortudo` | `&2☘` Sortudo | 250 | dada individualmente |
| `carta` | `&f回` Carta | 260 | dada individualmente |
| `modo-aviao` | `&b✈` Modo Avião | 270 | dada individualmente |
| `celular` | `&a✆` Celular | 280 | dada individualmente |
| `flor` | `&d✿` Flor | 290 | dada individualmente |
| `briga-de-espadas` | `&7⚔` Briga de Espadas | 300 | dada individualmente |
| `imperial` | `&6⚜` Imperial | 310 | dada individualmente |

- VIP, MVP e Pro têm 3 medalhas (`carinha`, `toxico`, `yin-yang`); Ultra e todos os cargos acima têm 8. Medalhas sazonais e da loja não pertencem a nenhum cargo: a staff as dá uma a uma com `/medalha dar`. Comprar um pacote da loja que lista medalhas ainda não as dá.

### `/medalha`

- `/medalha` (ou `/medalhas`, `/medal`, ou o botão `Medalhas` do menu de perfil) lista, numa linha do chat, `Nenhuma` e depois todas as medalhas que o jogador tem, do menor peso para o maior:

```text
Medalhas disponíveis: Nenhuma, ☣, ☯, ♫, ⚡, ✌, ❤, ☕, ツ
```

- `Medalhas disponíveis: ` é `&a`, `Nenhuma` é `&7`, cada medalha é o seu símbolo na sua cor e as vírgulas são `&f`. Clicar em `Nenhuma` roda `/medalha nenhuma`; clicar numa medalha roda `/medalha <id>`.
- Ao passar o mouse, uma medalha mostra o nome na sua cor e uma prévia do chat com a tag que o jogador mostra agora:

```text
Café

Prévia:
☕ ENDERLORE tadeu: Olá!

Clique para selecionar!
```

- `Nenhuma` mostra o mesmo hover com `Nenhuma` na primeira linha e a prévia sem medalha.
- `/medalha <id>` seleciona uma medalha que o jogador tem: `&aVocê selecionou a medalha &e☕ &eCafé &acom sucesso.` A escolha é salva no PostgreSQL (`SelectMedalCommand`) e o chat muda na hora em todos os servidores. `/medalha nenhuma` a remove: `&aVocê removeu a sua medalha.`
- Uma medalha que o jogador não tem é recusada (`medal-locked`), uma desconhecida também (`medal-unknown`).
- Se o jogador perder depois a medalha escolhida (um cargo acaba, a staff a tira), ela simplesmente deixa de aparecer; a escolha continua salva e volta se ele ganhar a medalha de novo.

### Cargos e tags temporários

- `[tempo]` é escrito como quantidades e unidades, juntas: `30d`, `12h`, `1d12h`, `2w` (`s`, `m`, `h`, `d`, `w`). `permanente` (ou nada) significa para sempre; aparece como `Vitalício`.
- Dar um cargo ou tag temporário que o jogador já tem por um tempo soma o tempo ao que falta (`30d` com 10 dias restantes termina em 40 dias). Dar para quem já tem para sempre é recusado.
- Quando o tempo acaba, o cargo para de valer na próxima verificação (em até um segundo) em todos os servidores, sem ninguém rodar comando. O nó continua guardado, expirado.

### Gestão

- Jogadores são encontrados pelo nome, online ou não, pela [conta](../accounts/README.pt-BR.md).
- Toda mudança passa pelo `PermissionSync`: é salva no PostgreSQL, registrada em `perm_actions` com o nome de quem rodou o comando (`CONSOLE` pelo console) e anunciada para a rede inteira com `permission.changed`.
- As respostas são enviadas na thread principal depois que o banco responde; se o PostgreSQL falhar, quem rodou recebe `rank-failed`.
- Tudo completa com tab: subcomandos, jogadores online, cargos, tags (só as que o jogador tem em `/tag <id>`, só decorativas em `/tag dar`), medalhas (`nenhuma` e as que o jogador tem em `/medalha <id>`, todas em `/medalha dar` e `remover`), trilhas, tempos (`permanente`, `1h`, `12h`, `1d`, `7d`, `15d`, `30d`, `60d`, `90d`, `365d`), permissões já em uso (nos cargos, dos `plugin.yml`, `group.*`, `muxe.tag.*` e `muxe.medal.*`) e opções (`true`, `false`, tempos, `server=<grupo>` dos servidores vivos, `world=<mundo>` deste servidor).
- `/permissao ... definir` aceita até quatro opções depois da permissão, em qualquer ordem: `true`/`false` (padrão `true`), um tempo e contextos (`server=bedwars world=arena`). `remover` aceita os contextos do nó a remover.
- `/permissao jogador testar` testa a permissão com os contextos do servidor e do mundo onde o jogador está quando ele está online aqui, e sem contexto caso contrário.
- `/editor` abre uma sessão do editor web (`EditorSessions`): um token válido por 30 minutos, guardado com hash no Redis. Jogadores recebem um link clicável para `<permissions.editor-url>/#<token>` com hover; o console recebe o link puro.

## Comandos

| Comando | Descrição | Permissão |
|---|---|---|
| `/tag` | Lista as suas tags, clicáveis, com prévia. | todos |
| `/tag <tag>` | Seleciona uma das suas tags. | todos |
| `/tag dar <jogador> <tag> [tempo]` | Dá uma tag decorativa. | `muxe.tags.manage` |
| `/tag remover <jogador> <tag>` | Tira uma tag decorativa. | `muxe.tags.manage` |
| `/medalha` | Lista as suas medalhas, clicáveis, com prévia. | todos |
| `/medalha <medalha>` | Seleciona uma das suas medalhas. | todos |
| `/medalha nenhuma` | Remove a sua medalha. | todos |
| `/medalha dar <jogador> <medalha> [tempo]` | Dá uma medalha (o nó `muxe.medal.<id>`). | `muxe.medals.manage` |
| `/medalha remover <jogador> <medalha>` | Tira uma medalha dada com `/medalha dar`. | `muxe.medals.manage` |
| `/cargo ver <jogador>` | Cargo principal, tag mostrada e todos os cargos com o tempo que falta. | `muxe.ranks.manage` |
| `/cargo dar <jogador> <cargo> [tempo]` | Dá um cargo, para sempre ou por um tempo. | `muxe.ranks.manage` |
| `/cargo remover <jogador> <cargo>` | Tira um cargo. | `muxe.ranks.manage` |
| `/cargo promover <jogador> <trilha>` | Troca o cargo do jogador na trilha pelo próximo. | `muxe.ranks.manage` |
| `/cargo rebaixar <jogador> <trilha>` | Troca pelo anterior (ou nenhum, abaixo do primeiro). | `muxe.ranks.manage` |
| `/cargo listar` | Cargos do maior para o menor, com id, peso e herança, e as trilhas. | `muxe.ranks.manage` |
| `/cargo info <cargo>` | Peso, tag, prefixo no chat, herança e permissões de um cargo. | `muxe.ranks.manage` |
| `/permissao jogador ver <jogador>` | Os nós do próprio jogador. | `muxe.permissions.manage` |
| `/permissao jogador definir <jogador> <permissão> [opções]` | Define um nó no jogador. | `muxe.permissions.manage` |
| `/permissao jogador remover <jogador> <permissão> [contexto]` | Remove um nó do jogador. | `muxe.permissions.manage` |
| `/permissao jogador testar <jogador> <permissão>` | Diz se o jogador tem a permissão. | `muxe.permissions.manage` |
| `/permissao cargo ver <cargo>` | O mesmo que `/cargo info`. | `muxe.permissions.manage` |
| `/permissao cargo definir <cargo> <permissão> [opções]` | Define um nó no cargo. | `muxe.permissions.manage` |
| `/permissao cargo remover <cargo> <permissão> [contexto]` | Remove um nó do cargo. | `muxe.permissions.manage` |
| `/editor` | Abre uma sessão de 30 minutos do editor web. | `muxe.permissions.editor` |

Aliases: `/tags`, `/medalhas` e `/medal`, `/cargos` e `/rank`, `/permissoes` e `/perm`, `/editorpermissoes`; `promote` e `demote` para `promover` e `rebaixar`. `membro` não pode ser dado nem tirado: todos já têm. Uma mudança de cargo que quebraria as regras (um cargo herdando a si mesmo, uma herança desconhecida) é recusada com `perm-group-refused`.

Exemplos:

```text
/cargo dar tadeu vip
/cargo dar tadeu ultra 30d
/cargo promover tadeu doacao
/tag dar tadeu natal 15d
/medalha dar tadeu natal 30d
/permissao jogador definir tadeu muxe.lobby.fly server=lobby 7d
/permissao cargo definir vip muxe.kit.vip false world=arena
/permissao jogador testar tadeu muxe.build
```

## Permissões

| Permissão | Libera | Padrão |
|---|---|---|
| `muxe.tags.manage` | `/tag dar` e `/tag remover`. | op |
| `muxe.medals.manage` | `/medalha dar` e `/medalha remover`. | op |
| `muxe.ranks.manage` | `/cargo`. | op |
| `muxe.permissions.manage` | `/permissao`. | op |
| `muxe.permissions.editor` | `/editor`. | op |
| `muxe.tag.<id>` | Ter a tag `<id>`. Tags de cargo vêm com o cargo; tags decorativas são dadas com `/tag dar`. | |
| `muxe.medal.<id>` | Ter a medalha `<id>`. Semeada em `vip` e `ultra` (veja [Medalhas](#medalhas)); as outras são dadas com `/medalha dar`. | |
| `group.<id>` | Ter o cargo `<id>`. | |
| `*` | Tudo. Semeada no `admin`, então o Admin tem todas as permissões acima. | |

## Configuração

`plugins/Muxe-Core/config.yml`:

```yaml
permissions:
  editor-url: "http://151.244.40.191:1031"
```

| Chave | Padrão | Descrição |
|---|---|---|
| `permissions.editor-url` | `http://151.244.40.191:1031` | Endereço do editor web. O link é `<editor-url>/#<token>`. |
| `messages.chat-format` | `{medal}{prefix}{color}{player}: &f{message}` | Formato do chat. `{medal}` é a medalha escolhida e um espaço (vazio sem medalha), `{prefix}` o prefixo da tag no chat, `{color}` a cor dela. Uma `config.yml` escrita antes das medalhas não tem `{medal}`: adicione para mostrar medalhas no chat. |
| `menus.profile.buttons.medals.command` | `medalha` | Comando rodado pelo botão `Medalhas` do menu de perfil. |

Cargos, tags e trilhas são dados, não configuração: mude-os com os comandos, com o editor ou no PostgreSQL.

### Mensagens

Todas em `messages` do `plugins/Muxe-Core/config.yml`:

| Chave | Padrão |
|---|---|
| `no-permission` | `&cVocê não possui permissão para usar este comando.` |
| `tag-list-header`, `tag-list-separator` | `&aSuas tags: `, `&f, ` |
| `tag-hover` | Lista: `&fPrévia:`, `{preview}`, `""`, `&eClique para selecionar!` |
| `tag-preview-message` | `Olá!` (a mensagem da prévia) |
| `tag-none` | `&cVocê ainda não possui nenhuma tag.` |
| `tag-selected` | `&aVocê selecionou a tag {tag} &acom sucesso.` |
| `tag-unknown`, `tag-locked` | `&cA tag &f{tag} &cnão existe.`, `&cVocê não possui a tag {tag}&c.` |
| `tag-not-decorative` | `&cA tag {tag} &cvem de um cargo. Use &f/cargo dar&c.` |
| `tag-given`, `tag-removed` | `&aTag {tag} &adada para &f{player} &7({time})&a.`, `&aTag {tag} &aremovida de &f{player}&a.` |
| `tag-already-held`, `tag-not-held` | `&f{player} &cjá possui a tag {tag} &cpara sempre.`, `&f{player} &cnão possui a tag {tag}&c.` |
| `tag-usage`, `tag-usage-staff` | Listas: `&cUso: &f/tag [tag]` para jogadores, todos os subcomandos para a staff. |
| `medal-list-header`, `medal-list-separator`, `medal-list-none` | `&aMedalhas disponíveis: `, `&f, `, `&7Nenhuma` |
| `medal-hover` | Lista: `{name}`, `""`, `&fPrévia:`, `{preview}`, `""`, `&eClique para selecionar!` (`{name}` é o nome na cor da medalha) |
| `medal-none-hover` | Lista: `&7Nenhuma`, `""`, `&fPrévia:`, `{preview}`, `""`, `&eClique para selecionar!` |
| `medal-preview-message` | `Olá!` |
| `medal-selected`, `medal-cleared` | `&aVocê selecionou a medalha {medal} &acom sucesso.`, `&aVocê removeu a sua medalha.` (`{medal}` é o símbolo e o nome, `&e☕ &eCafé`) |
| `medal-unknown`, `medal-locked` | `&cA medalha &f{medal} &cnão existe.`, `&cVocê não possui a medalha {medal}&c.` |
| `medal-given`, `medal-removed` | `&aMedalha {medal} &adada para &f{player} &7({time})&a.`, `&aMedalha {medal} &aremovida de &f{player}&a.` |
| `medal-already-held`, `medal-not-held` | `&f{player} &cjá possui a medalha {medal} &cpara sempre.`, `&f{player} &cnão possui a medalha {medal}&c.` |
| `medal-usage`, `medal-usage-staff` | Listas: `&cUso: &f/medalha [medalha]` para jogadores, todos os subcomandos para a staff. |
| `rank-usage`, `perm-usage` | Listas com todos os subcomandos e como escrever tempos e opções. |
| `rank-failed` | `&cNão foi possível concluir agora. Tente novamente em instantes.` |
| `rank-permanent`, `rank-remaining` | `Vitalício`, `{time}` |
| `rank-invalid-time` | `&cTempo inválido: &f{input}&c. Use &f30d&c, &f12h&c, &f1d12h &cou &fpermanente&c.` |
| `rank-player-unknown` | `&cO jogador &f{player} &cnunca entrou no servidor.` |
| `rank-unknown`, `rank-default`, `rank-track-unknown` | Cargo desconhecido, `membro` dado ou tirado, trilha desconhecida. |
| `rank-given`, `rank-already-held`, `rank-removed`, `rank-not-held` | `&aCargo {rank} &adado para &f{player} &7({time})&a.` e parecidas. |
| `rank-promoted`, `rank-demoted`, `rank-track-top`, `rank-track-bottom` | `&f{player} &afoi promovido para {rank}&a.` e parecidas. |
| `rank-view-*`, `rank-list-*`, `rank-info-*`, `rank-nothing` | Linhas de `/cargo ver`, `/cargo listar` e `/cargo info`. |
| `node-line`, `node-true`, `node-false`, `node-contexts`, `node-time`, `node-none` | Como um nó é escrito: `&8• &f{key} &8= {value}{contexts}{time}`. |
| `perm-invalid-node`, `perm-invalid-option` | Uma permissão ou opção que não pode ser lida. |
| `perm-user-set`, `perm-user-removed`, `perm-user-missing`, `perm-user-header` | Respostas de `/permissao jogador`. |
| `perm-test`, `perm-test-true`, `perm-test-false`, `perm-test-undefined`, `perm-test-online`, `perm-test-offline` | Respostas de `/permissao jogador testar`. |
| `perm-group-set`, `perm-group-removed`, `perm-group-missing`, `perm-group-refused` | Respostas de `/permissao cargo`. |
| `editor-opened`, `editor-hover` | Listas: o link clicável (a linha `<link>`) e o hover dele. `{minutes}`, `{url}`. |
| `editor-console` | `&aEditor de permissões: &f{url} &7(expira em {minutes} minutos)` |

## Pacotes

| Id | Alvo | Política | Para quê |
|---|---|---|---|
| `permission.changed` | todos os servidores | padrão | Os nós, a tag ou a medalha de um jogador mudaram (com o id do jogador), ou cargos, tags, trilhas ou medalhas mudaram (sem id). Os servidores recarregam aquele jogador, se estiver carregado, ou tudo. |

## Dados

| Tabela | Conteúdo |
|---|---|
| `core.perm_tags` | `id`, `name`, `color`, `weight`, `prefix` (nulo para o prefixo padrão em negrito), `decorative`. |
| `core.perm_groups` | `id`, `display_name`, `weight`, `tag_id`. |
| `core.perm_group_nodes` | Nós de cada cargo: `group_id`, `permission`, `value`, `contexts` (`server=lobby;world=arena`, ordenados), `expires_at`, `added_at`, `updated_at`. |
| `core.perm_user_nodes` | Nós de cada jogador: `player_id` e as mesmas colunas. Indexada por `permission`. |
| `core.perm_tracks` | `id`, `groups` (array em ordem). |
| `core.player_tags` | A tag que cada jogador escolheu. |
| `core.perm_medals` | `id`, `name`, `symbol`, `color`, `weight`. |
| `core.player_medals` | A medalha que cada jogador escolheu (`player_id`, `medal_id`, `updated_at`). |
| `core.perm_actions` | Toda mudança: `at`, `actor`, `description`. |

Criadas pela migração do core `V8__create_permissions.sql`, que também semeia os cargos, tags e trilhas acima. A `V10__seed_staff_permissions.sql` dá ao `mod` (e, por herança, a todos os cargos acima) `muxe.account.others` e `muxe.account.staff`. A `V11__create_medals.sql` cria as tabelas de medalhas, semeia as 31 medalhas e dá ao `vip` e ao `ultra` as suas. Lidas com `LoadPermissionDataQuery` e `FindPermissionUserQuery`; mudadas com `ApplyPermissionChangesCommand`, `AddUserNodeCommand`, `RemoveUserNodeCommand`, `SelectTagCommand` e `SelectMedalCommand`.

## Métricas

Nenhuma própria. Comandos e consultas contam nas métricas de CQRS, e o `permission.changed` nas métricas de pacotes.

## Integrações

- [Contas](../accounts/README.pt-BR.md): jogadores são encontrados pelo nome, online ou não.
- [Conta](../account/README.pt-BR.md): `/account`, `/accountmenu` e a cabeça do perfil mostram os cargos.
- [Hotbar](../hotbar/README.pt-BR.md): a cabeça do perfil mostra a tag e os cargos, e o botão `Medalhas` roda `/medalha`.
- [Lobby](../lobby/README.pt-BR.md): a linha `Tag:` da scoreboard.
- A [API do Core](../../development/core-api/README.pt-BR.md#cargos-e-permissões) dá aos outros plugins o `MuxeCore.permissions()`.

## Critérios de aceitação

- [x] Cargos herdam os de baixo; o admin tem tudo; contextos, negação, curingas e expiração são respeitados.
- [x] As permissões seguem o grupo de servidores e o mundo, e um cargo temporário para de valer quando acaba.
- [x] O chat mostra `&a&lVIP &atadeu: &fOlá!`, e `&7tadeu: &fOlá!` para membros.
- [x] Os prefixos dos times cabem em 16 caracteres e os nomes dos times ordenam por cargo e depois por tag.
- [x] `/tag` lista as tags do jogador da menor para a maior, cada uma clicável com a prévia; `/tag <id>` só seleciona tags que o jogador tem e salva a escolha.
- [x] A staff dá e tira tags decorativas e cargos, para sempre ou por um tempo que se soma, promove e rebaixa por trilhas, e toda mudança fica registrada com o nome dela.
- [x] Permissões de jogadores e cargos são definidas, listadas, testadas e removidas, com opções em qualquer ordem, e mudanças de cargo que quebram as regras são recusadas.
- [x] `/editor` cria uma sessão de 30 minutos e envia o link.
- [x] `/medalha` lista `Nenhuma` e as medalhas que o jogador tem a partir da menor, cada uma clicável com a prévia; `/medalha <id>` seleciona só medalhas que o jogador tem e `/medalha nenhuma` remove a medalha.
- [x] O chat mostra a medalha escolhida antes da tag (`&e☕ &5&lENDERLORE &5tadeu: &fOlá!`); uma medalha perdida deixa de aparecer e a escolha é mantida.
- [x] A staff dá e tira medalhas, para sempre ou por um tempo que se soma.
- [x] O botão `Medalhas` do menu de perfil fecha o menu e envia a lista de medalhas.
- [x] Tudo completa com tab e recusa jogadores sem permissão.
- [ ] Permissões, chat, tab e nome acima da cabeça mudam no jogo em todos os servidores logo depois de um comando (verificado no jogo).

## Falhas e recuperação

- PostgreSQL indisponível no login: o jogador entra como `membro` e recebe os cargos quando a carga der certo; um aviso vai para o log.
- PostgreSQL indisponível durante um comando: quem rodou recebe `rank-failed` e nada muda.
- Redis indisponível: as mudanças são salvas, mas os outros servidores só as veem quando o jogador entra neles de novo; `/editor` falha com `rank-failed`.
- O campo do permissible não pode ser trocado (outra build do servidor): um aviso vai para o log e as permissões passam por um attachment; aí os curingas só cobrem permissões escritas por inteiro num cargo ou jogador.

## Limitações conhecidas

- O nome acima da cabeça e o prefixo no tab seguem o limite de 16 caracteres do 1.8; um prefixo maior é cortado. Por isso nunca mostram a medalha, só o chat mostra.
- Comprar um pacote da loja que lista medalhas ainda não as dá; a staff as dá com `/medalha dar`.
- `/medalha remover` só tira uma medalha dada ao jogador; uma medalha que vem de um cargo fica enquanto o cargo ficar.
- `/permissao jogador ver` lista os nós do próprio jogador, não os herdados dos cargos; use `/cargo info` para esses.
- `/tag <id>` sugere só as tags de jogadores online, do cache.
