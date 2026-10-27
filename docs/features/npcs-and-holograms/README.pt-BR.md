[English](README.md) | **Português**

# NPCs e hologramas

NPCs e hologramas de texto que a equipe coloca por comando, em qualquer servidor. Eles ficam no Muxe-Core (`core:common` pacote `br.com.muxe.core.common.placement`, `core:bukkit` pacotes `placement`, `npc/placed` e `hologram/placed`), são salvos no PostgreSQL por grupo de servidores e aparecem em todos os servidores desse grupo.

## Histórias de usuário

- Como membro da equipe, quero colocar um NPC que leva os jogadores a um jogo, abre a loja ou só fica parado, em qualquer lobby ou servidor de jogo.
- Como membro da equipe, quero escrever hologramas de texto no mundo e editá-los linha por linha, sem reiniciar nada.
- Como membro da equipe, quero que o que eu coloco num servidor apareça em todos os servidores do mesmo grupo, para que o `lobby-1` e o `lobby-7` fiquem sempre iguais.
- Como jogador, quero clicar num NPC e ir para o jogo que ele mostra, com o número de jogadores acima da cabeça dele.

## Como funciona

### Compartilhados pelo grupo de servidores

- Todo NPC, holograma e [caixa registradora](../shop/README.pt-BR.md#caixas-registradoras) da loja pertence a um grupo de servidores (`server.group` na config do Muxe-Core). Todos os servidores desse grupo mostram os mesmos; os outros grupos têm os seus. Os ids são únicos dentro de um grupo.
- Os ids usam letras minúsculas, números, `-` e `_`, com até 32 caracteres.
- Um comando salva a mudança no PostgreSQL (`SavePlacementCommand` ou `RemovePlacementCommand`). Depois, o servidor onde ele rodou recarrega o que foi colocado no grupo e publica `placement.changed` para o grupo; todos os outros servidores do grupo recarregam do PostgreSQL (`FindPlacementsQuery`). Um servidor ignora os próprios pacotes, então nunca recarrega duas vezes.
- Ao ligar, cada servidor carrega o que foi colocado no seu grupo, então um servidor que liga depois mostra o que já tinha sido colocado.
- Depois de recarregar, só o que mudou é mostrado de novo: um NPC que não mudou não é recriado.
- Cada coisa só aparece se o mundo dela estiver carregado naquele servidor.

### NPCs

- O `/npc criar <id> <ação> <skin> <nome>` coloca um NPC onde você está, olhando para onde você olha (na altura da cabeça). O nome pode ter espaços e cores com `&`.
- A ação é uma palavra:

| Ação | Palavra | Linhas acima do NPC | Clique |
|---|---|---|---|
| Jogo | O grupo, como `skywars` | `npcs.lines.game`: `&b&l{name}`, `&e{players} jogando` | Manda o jogador para o servidor com menos jogadores do grupo, ou responde `&cNenhuma sala de &f{name} &cdisponível no momento.` |
| Loja | `loja` | `npcs.lines.shop`: `&b&l{name}`, `&eClique para abrir a loja` | Abre a [loja](../shop/README.pt-BR.md) na aba Ranks. |
| Nenhuma | `nenhuma` | `npcs.lines.none`: `&b&l{name}` | Nada. |

- `{name}` é o nome do NPC e `{players}` os jogadores online nos servidores vivos do grupo do jogo.
- A skin é a de um nome original, buscada na Mojang quando o NPC é criado ou a skin muda, e guardada com o NPC. Quando o nome não tem skin, o NPC fica com a skin padrão e a equipe vê `&eSkin de &f{skin} &enão encontrada. O NPC fica com a skin padrão.`
- Os NPCs existem só em pacotes, aparecem para jogadores a até 48 blocos e não ficam no tab. Os cliques esquerdo e direito contam. Veja a [ADR 0008](../../decisions/0008-packet-npcs-and-click-interception.pt-BR.md).
- Os cliques nos NPCs de jogo e de loja usam o anti-spam do Muxe-Core (`spam.*`): três cliques dentro de 1 segundo bloqueiam por 3 segundos com `&cAguarde 2,9s para usar novamente.`

### Hologramas

- O `/holograma criar <id> <texto>` cria um holograma de texto onde você está. A primeira linha fica `holograms.top` (2.0) blocos acima desse ponto e cada linha seguinte fica `holograms.line-spacing` (0.28) blocos mais baixo, então adicionar linhas faz o holograma crescer para baixo e a primeira linha nunca se move.
- As linhas usam cores com `&`. `{vazio}` como texto inteiro cria uma linha em branco que mantém o espaço.
- Os números de linha começam em 1. O `/holograma ver <id>` lista as linhas. Um holograma sempre tem pelo menos uma linha: remover a última é recusado; use `/holograma remover`.
- Os hologramas são suportes de armadura invisíveis que existem só em pacotes, como os [suportes de exibição](../../development/core-api/README.pt-BR.md#suportes-de-exibição).

### Importando o que já existia

Ao ligar, se o `plugins/Muxe-Core/import.yml` existir, o Muxe-Core o importa para o grupo do servidor e o renomeia para `import.yml.done`. Ele lê NPCs no formato antigo do `npcs.yml` do lobby em `npcs:` (cole o arquivo antigo ali, um nível para dentro, já que o arquivo antigo os guardava na raiz; o `group` vira a ação, então um grupo de jogo dá um NPC de jogo) e caixas registradoras em `registers:`:

```yaml
npcs:
  skywars:
    group: skywars
    name: "&bSkyWars"
    skin-name: SomePlayer
    skin:
      value: "..."
      signature: "..."
    location:
      world: lobby
      x: 10.5
      y: 33.0
      z: -4.5
      yaw: 90.0
      pitch: 0.0
registers:
  main:
    location:
      world: lobby
      x: 7.5
      y: 33.0
      z: -9.5
      yaw: 180.0
      pitch: 0.0
    seller:
      world: lobby
      x: 7.5
      y: 33.0
      z: -9.5
      yaw: 180.0
      pitch: 0.0
```

Entradas quebradas são puladas com um aviso. Entradas com um id que já existe o substituem. Se salvar falhar, o arquivo fica e a importação roda de novo no próximo início. Coloque o arquivo em um só servidor do grupo.

## Comandos

| Comando | Descrição | Permissão |
|---|---|---|
| `/npc criar <id> <ação> <skin> <nome>` | Cria um NPC onde você está. | `muxe.npc` |
| `/npc acao <id> <ação>` | Troca o que o NPC faz ao ser clicado. | `muxe.npc` |
| `/npc mover <id>` | Move um NPC para onde você está. | `muxe.npc` |
| `/npc skin <id> <skin>` | Troca a skin pela de um nome original. | `muxe.npc` |
| `/npc nome <id> <nome>` | Troca o nome mostrado acima do NPC. | `muxe.npc` |
| `/npc remover <id>` | Remove um NPC. | `muxe.npc` |
| `/npc listar` | Lista os NPCs deste grupo de servidores. | `muxe.npc` |
| `/holograma criar <id> <texto>` (`/hologram`, `/holo`) | Cria um holograma onde você está. | `muxe.hologram` |
| `/holograma adicionar <id> <texto>` | Adiciona uma linha no fim. | `muxe.hologram` |
| `/holograma linha <id> <número> <texto>` | Troca uma linha. | `muxe.hologram` |
| `/holograma removerlinha <id> <número>` | Remove uma linha. | `muxe.hologram` |
| `/holograma ver <id>` | Mostra as linhas numeradas. | `muxe.hologram` |
| `/holograma mover <id>` | Move um holograma para onde você está. | `muxe.hologram` |
| `/holograma remover <id>` | Remove um holograma. | `muxe.hologram` |
| `/holograma listar` | Lista os hologramas deste grupo de servidores. | `muxe.hologram` |

Sem argumentos, ou com um argumento errado, cada comando mostra o seu uso.

## Permissões

| Permissão | Libera | Padrão |
|---|---|---|
| `muxe.npc` | Usar `/npc`. Substitui a `muxe.lobby.npc`. | op |
| `muxe.hologram` | Usar `/holograma`. | op |

## Configuração

`plugins/Muxe-Core/config.yml`:

| Chave | Padrão | Descrição |
|---|---|---|
| `server.group` | `lobby` | O grupo cujas coisas colocadas este servidor mostra e edita. |
| `npcs.lines.game` | `&b&l{name}`, `&e{players} jogando` | Linhas acima dos NPCs de jogo. |
| `npcs.lines.shop` | `&b&l{name}`, `&eClique para abrir a loja` | Linhas acima dos NPCs da loja. |
| `npcs.lines.none` | `&b&l{name}` | Linhas acima dos NPCs sem ação. |
| `holograms.top` | `2.0` | Altura da primeira linha do holograma acima de onde ele foi criado. |
| `holograms.line-spacing` | `0.28` | Espaço entre as linhas do holograma. |
| `spam.max-clicks`, `spam.window`, `spam.cooldown` | `3`, `1s`, `3s` | Anti-spam dos cliques nos NPCs e nas caixas registradoras. |
| `messages.npc-*`, `messages.hologram-*`, `messages.placement-failed`, `messages.spam-cooldown` | Textos em português | Respostas dos comandos, listas de uso e mensagens de clique. |

O `/npc` não grava mais o `plugins/Muxe-Lobby/npcs.yml`; use o `import.yml` para trazer os NPCs antigos.

## Pacotes

| Id | Destino | Política | Propósito |
|---|---|---|---|
| `placement.changed` | o grupo de servidores | padrão | Algo do grupo foi criado, mudado ou removido; todo servidor do grupo recarrega do PostgreSQL. Leva o grupo. |
| `player.connect` | proxy | padrão | Enviado pelos NPCs de jogo para mover o jogador. |

## Dados

| Tabela | Conteúdo |
|---|---|
| `core.npc_placements` | `server_group`, `id`, posição (`world`, `x`, `y`, `z`, `yaw`, `pitch`), `name`, `action` (`NONE`, `GAME`, `SHOP`), `action_target` (o grupo do jogo, vazio nos outros casos), `skin_name`, `skin_value`, `skin_signature`. |
| `core.hologram_placements` | `server_group`, `id`, posição, `lines` (array de texto, string vazia para uma linha em branco). |
| `core.register_placements` | Caixas registradoras, veja [Loja](../shop/README.pt-BR.md#dados). |

A chave primária de cada tabela é `(server_group, id)`. Criadas pela migração do core `V4__create_placements_and_promotion.sql`.

Comandos e consultas: `SavePlacementCommand`, `RemovePlacementCommand` (falha com `placement.not_found` quando o id não existe), `FindPlacementsQuery(group)`, registrados pelo `PlacementModule`.

## Métricas

`muxe_db_transaction_seconds{operation="placement.save"|"placement.remove"|"placement.find"}`, `muxe_cqrs_handle_seconds` para os comandos e a consulta acima, e as métricas de pacote de `placement.changed`.

## Integrações

- A [API do core](../../development/core-api/README.pt-BR.md) fornece o `NpcService`, o `MojangSkins`, o `DisplayService` e o `LabelStack`.
- A [Loja](../shop/README.pt-BR.md) usa o mesmo armazenamento e a mesma sincronização para as caixas registradoras, e os NPCs da loja a abrem.
- O [Roteamento da rede](../network-routing/README.pt-BR.md) move os jogadores que clicam num NPC de jogo.

## Critérios de aceitação

- [x] As ações dos NPCs são lidas de uma palavra: `loja`, `nenhuma` ou um grupo de jogo; só as ações de jogo guardam um alvo.
- [x] Os ids são palavras curtas em minúsculas; as linhas do holograma são numeradas a partir de 1 e um holograma mantém pelo menos uma linha.
- [x] NPCs, hologramas e caixas sobrevivem a ir e voltar do PostgreSQL; salvar de novo substitui, e os grupos ficam separados.
- [x] Uma mudança num servidor aparece em todos os servidores do grupo, remoções também, e um servidor carrega o que foi colocado antes de ele ligar.
- [x] Só o que mudou é mostrado de novo, e o que sumiu é removido.
- [x] As linhas dos NPCs são preenchidas por ação com `{name}` e `{players}`.
- [x] As linhas do holograma descem a partir da primeira linha, que nunca se move.
- [x] O formato antigo do `npcs.yml` e as caixas são lidos do `import.yml` para o grupo do servidor.
- [ ] Colocar, editar e clicar em NPCs e hologramas funciona no jogo, e a mudança aparece num segundo servidor do grupo (conferido no jogo).

## Falhas e recuperação

- PostgreSQL indisponível quando um comando roda: a equipe vê `&cNão foi possível salvar agora. Tente novamente em instantes.` e nada muda.
- PostgreSQL indisponível ao ligar: nada aparece, com um aviso no log. A próxima mudança no grupo, ou um reinício, carrega tudo de novo.
- Redis indisponível: a mudança é salva e aparece no servidor onde foi feita; os outros servidores a mostram no próximo recarregamento.
- Mojang indisponível ou nome desconhecido: o NPC fica com a skin padrão; o `/npc skin` pode ser usado de novo depois.
- Proxy indisponível ou nenhum servidor vivo no grupo do jogo: o jogador vê uma mensagem de falha e continua onde está.
- Mundo não carregado num servidor: aquele NPC ou holograma é pulado naquele servidor com um aviso.
- Internos do servidor ausentes (outra versão de servidor): NPCs e hologramas são pulados com um aviso cada.

## Limitações conhecidas

- Os NPCs olham sempre para a mesma direção, a que você olhava ao colocá-los ou movê-los; eles não se viram para os jogadores.
- As linhas são as mesmas para todos os NPCs de uma ação; só `{name}` e `{players}` mudam.
- O que é colocado pertence ao grupo inteiro: um servidor sozinho do grupo não pode ter os seus.
- Duas pessoas da equipe editando a mesma coisa ao mesmo tempo: o último a salvar vence.
- O arquivo de importação precisa ser colocado à mão e só é lido ao ligar.
