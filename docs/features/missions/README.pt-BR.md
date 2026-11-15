[English](README.md) | **Português**

# Missões

Missões diárias e semanais compartilhadas por todos os modos de jogo, com um caminho dos créditos que paga por completar todas as semanais. O Muxe-Core guarda o catálogo de cada jogo, o progresso e as recompensas; cada jogo só informa quanto o jogador fez de cada objetivo durante a partida.

## Histórias de usuário

- Como jogador, quero missões diárias e semanais em cada jogo, para ter objetivos além de ganhar.
- Como jogador, quero saber na hora em que completo uma missão, e ver no fim da partida o que avançou.
- Como desenvolvedor de jogo, quero colocar missões num modo novo declarando os objetivos e informando os totais, sem escrever banco nem menus.

## Como funciona

### Catálogo

- Cada jogo registra o seu catálogo ao ligar: `core.missions().register(game, catalog, rewards)`. O catálogo é lido da seção `missions` do config do jogo por `MissionConfig.from(section, goals)`, em que `goals` transforma o nome de um objetivo num `MissionGoal(id, best)`.
- Um objetivo ou soma (kills finais, blocos destruídos) ou guarda o melhor valor alcançado (kills seguidas, esmeraldas numa vida). O `WEEKLY_MISSIONS` já vem pronto: ele conta as missões semanais completadas na semana.
- Cada jogador recebe `daily-count` missões diárias sorteadas do conjunto de diárias, diferentes por jogador e por dia e fixas durante o dia; `weekly-count` semanais sorteadas por semana, iguais para todos; e todas as missões do caminho dos créditos.
- Dias e semanas seguem o calendário no horário de São Paulo; a semana começa na segunda. O progresso pertence ao dia ou à semana em que foi feito, então um dia ou semana novo começa do zero.

### Durante a partida

- Quando a partida começa, o jogo cria um `MissionTracker` e carrega as missões dos jogadores.
- O jogo informa os totais da partida de cada jogador (`Map<objetivo, quantidade>`) a cada segundo e no fim. O `MissionSession` os transforma em progresso: a diferença desde o último envio para os objetivos que somam, o próprio valor para os outros.
- Quando uma missão completa, o jogador vê, na hora, `&b&lMISSÕES &aVocê completou ` seguido do nome e da categoria da missão (`&eNinguém me para! &7(Diária)`). Passar o mouse no nome mostra o nome, uma linha em branco e a descrição com o progresso (`&7Elimine 2 adversários seguidos em uma partida: &f2/2`). Em seguida vêm as recompensas, uma por linha: `&b+625 XP (Bed Wars)`, `&d+15 créditos`.
- O progresso é salvo quando uma missão completa e no fim da partida, com o `RecordMissionProgressCommand(game, player, amounts)`. Salvar paga as recompensas do que completou na mesma transação: créditos pelo repositório de créditos (motivo `missions:<jogo>`) e a recompensa do jogo (o BedWars soma o XP nas estatísticas do jogador).
- No fim da partida, cada jogador que teve missões avançando vê um resumo: uma borda, depois, para cada categoria com mudanças, o cabeçalho, uma linha em branco, uma linha por missão (`" &eFim da linha: &73/35 &a(+3)"`, com `" &bCOMPLETO!"` quando completou) e uma linha em branco, e a borda de novo.

### Menu

- `MissionMenus.open(player, back)` abre `Missões do Bed Wars` com uma linha por categoria: o ícone da categoria (`Progresso: feitas/total` e o tempo até o dia ou a semana acabar) seguido das missões dela. Uma missão em andamento tem o nome amarelo, brilha e termina com `&eEm progresso.`; uma completa tem o nome verde (`&a`), não brilha e termina com `&aCompleta!`.
- `back` segue a regra do servidor para menus: um menu aberto por outro menu mostra o `Voltar` (slot 48) para ele e tem 6 linhas; aberto direto (NPC, item, comando) não tem e tem 5 linhas, como no export.

## Comandos

Nenhum.

## Permissões

Nenhuma.

## Configuração

No config de cada jogo, em `missions`:

| Chave | Padrão | Descrição |
|---|---|---|
| `daily-count`, `weekly-count` | `3`, `5` | Quantas missões diárias e semanais cada jogador recebe. |
| `daily.<id>`, `weekly.<id>`, `credit-path.<id>` | os exports | `name`, `goal`, `target`, `xp`, `credits` e `description` (linhas com `{progress}`). |
| `announce.*` | a referência | `prefix`, `name` (`{name}`, `{category}`), `description`, `progress`, `xp`, `credits`. |
| `summary.*` | a referência | `border`, `headers.DAILY/WEEKLY/CREDIT_PATH`, `line` (`{name}`, `{progress}`, `{target}`, `{added}`) e `completed`. |
| `menu.*` | os exports | Título, linhas (`rows` 5 sem `Voltar`, `rows-with-back` 6 com ele), slots das categorias, itens e rótulos, o nome da missão completa (`completed-name`, `&a{name}`), textos da lore, o botão `back` e as unidades de tempo. |

## Pacotes

Nenhum.

## Dados

| Tabela | Conteúdo |
|---|---|
| `core.mission_progress` | Uma linha por jogador, jogo, missão e início do dia ou semana, com o progresso e se completou. Migração `V15__create_mission_progress.sql`. |

`RecordMissionProgressCommand` (transação `missions.record`) e `FindMissionsQuery(game, player)` (transação `missions.find`), registrados pelo `MissionModule`.

## Métricas

Nenhuma além das métricas de comandos e consultas.

## Integrações

- [Créditos](../credits/README.pt-BR.md): os créditos das missões entram no saldo.
- [BedWars](../bedwars/README.pt-BR.md#missões): o primeiro jogo com missões.

## Critérios de aceitação

- [x] Os totais viram progresso, os objetivos que guardam o melhor valor o usam, e as missões completam na hora (`MissionsTest`).
- [x] Completar todas as semanais avança o caminho dos créditos (`MissionsTest`).
- [x] Salvar paga os créditos e a recompensa do jogo uma vez, e um dia ou semana novo começa do zero (`MissionsTest`).
- [x] O progresso sobrevive a ir e voltar do PostgreSQL, por jogo, missão e período (`PostgresMissionRepositoryIntegrationTest`).
- [x] A linha de conclusão, o hover e o resumo da partida seguem a referência (`BedWarsMissionsTest`).
- [ ] As missões completam e aparecem no jogo (conferido no jogo).

## Falhas e recuperação

- PostgreSQL indisponível quando a partida começa: as missões do jogador não carregam e não avançam naquela partida; um aviso vai para o log.
- Falha ao salvar: um aviso vai para o log e o progresso daquele envio se perde.

## Limitações conhecidas

- As missões de quem sai antes do fim só são salvas quando uma missão completa ou a partida termina.
- Missões de evento (como as de Halloween) ainda não entraram.
