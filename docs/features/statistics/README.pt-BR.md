[English](README.md) | **Português**

# Estatísticas

Um menu com as estatísticas de um jogador em todos os jogos, aberto pelo `Meu perfil` ou pelo `/stats`, para qualquer jogador em qualquer servidor.

## Histórias de usuário

- Como jogador, abro `Ver estatísticas` no meu perfil, vejo as minhas estatísticas e volto ao perfil.
- Como jogador, digito `/stats Steve` e vejo as estatísticas do Steve, mesmo com ele offline ou em outro servidor.

## Como funciona

- **Menu dos jogos:** `Suas estatísticas` (ou `Estatísticas de <nome>` para outro jogador), 4 linhas, com os dez jogos da referência: HG (slot 10), Duels (11), PvP (12), Sky Wars (13), Bed Wars (14), Quick Builders (15), CTF (16), Murder (19), Build Battle (20) e Hide and Seek (21). Os jogos que o Muxe ainda não tem mostram as linhas zeradas; o modo mini do HG se chama Minimuxe. Duels e Sky Wars dizem `Clique para ver mais.` e respondem `messages.statistics-soon` ao clicar. Aberto pelo `Meu perfil`, tem `Voltar` no slot 31 (`Para Meu perfil`); aberto pelo `/stats`, não tem.
- **Página do Bed Wars:** clicar no Bed Wars abre `Suas estatísticas (Bed Wars)` (ou `Estatísticas de <nome> (Bed Wars)`), 6 linhas: `Geral` no slot 4 com o ícone de nível, o XP e todos os contadores, e Solo, Duplas, Trios, Quartetos, 1v1, 2v2, 3v3, 4v4 e Mega (camas, slots 10 a 40) com os contadores daquele modo. Os números usam ponto a cada três dígitos. `Voltar` no slot 49 volta ao menu dos jogos (`Para <título do menu dos jogos>`).
- **Dados:** os contadores são os da vida toda (`period = total`) de `bedwars.player_stats`, a tabela que o plugin do BedWars grava. O core lê essa tabela (`FindBedWarsTotalsQuery`), então o menu funciona em servidores sem o plugin do BedWars, como o lobby principal. Um jogador sem linha mostra zeros.
- **Nível:** o nível vem do XP total pela fórmula do BedWars, agora no core (`BedWarsLevels`), e o ícone vem de `menus.statistics.bedwars.level-badges` (`LevelBadges`, também no core e usado pelo plugin do BedWars).

## Comandos

| Comando | Apelidos | Descrição |
|---|---|---|
| `/stats [jogador]` | `/estatisticas` | Abre as estatísticas do jogador, ou as suas. Um nome que ninguém usou responde `messages.statistics-not-found`; um jogador que desativou `Visibilidade de estatísticas` nas [preferências](../preferences/README.pt-BR.md) responde `messages.statistics-hidden`. |

## Permissões

Não se aplica.

## Configuração

`config.yml` do core, com o inglês em `lang/en.yml`:

| Chave | Padrão | Descrição |
|---|---|---|
| `menus.statistics.title`, `title-other`, `rows` | `Suas estatísticas`, `Estatísticas de {name}`, `4` | Títulos e altura do menu dos jogos. |
| `menus.statistics.back` | slot 31, flecha | `Voltar` quando aberto pelo perfil. |
| `menus.statistics.games.<id>` | os dez jogos | `slot`, `item` e `opens` (`nothing`, `soon` ou `bedwars`). |
| `menus.statistics.bedwars.title`, `title-other`, `rows`, `back` | `Suas estatísticas (Bed Wars)`, `Estatísticas de {name} (Bed Wars)`, `6`, slot 49 | Página do Bed Wars; `{parent}` na lore do voltar é o título do menu dos jogos. |
| `menus.statistics.bedwars.general`, `lore`, `modes.<modo>` | a referência | `Geral`, as linhas dos modos e o slot e a cama de cada modo; placeholders `{level}`, `{xp}`, `{games}`, `{beds-broken}`, `{beds-lost}`, `{kills}`, `{deaths}`, `{assists}`, `{final-kills}`, `{final-deaths}`, `{final-assists}`, `{wins}`, `{losses}`, `{winstreak}`, `{best-winstreak}`. |
| `menus.statistics.bedwars.level-badges` | os ícones do BedWars | Ícone por nível e o arco-íris a partir do nível 1000; mantenha igual a `levels` do `config.yml` do BedWars. |
| `messages.statistics-soon`, `messages.statistics-not-found` | `&cEm breve.`, `&cO jogador &f{player} &cnunca entrou no servidor.` | Respostas do menu e do `/stats`. |

## Pacotes

Não se aplica.

## Dados

Lê `bedwars.player_stats` (do plugin do BedWars, veja [BedWars](../bedwars/README.pt-BR.md)); não grava nada.

## Métricas

Não se aplica.

## Integrações

- [Hotbar](../hotbar/README.pt-BR.md): `Ver estatísticas` do `Meu perfil`.
- [BedWars](../bedwars/README.pt-BR.md): os contadores, a fórmula de nível e os ícones.
- `MuxeCore.statistics()` abre os menus a partir de outro plugin (`openHub`, `openBedWars`).

## Critérios de aceitação

- [x] O menu dos jogos lista os dez jogos nos slots da referência, com `Voltar` só quando aberto pelo perfil (`StatsPagesTest`).
- [x] A página do Bed Wars mostra os contadores da vida toda com ponto nos milhares e o ícone de nível (`StatsPagesTest`).
- [x] Os contadores da vida toda são lidos por modo e os períodos menores são ignorados (`PostgresBedWarsTotalsRepositoryIntegrationTest`).
- [x] `Ver estatísticas` abre as estatísticas com um jeito de voltar ao perfil (`ProfileMenuTest`).
- [ ] Conferido no jogo.

## Falhas e recuperação

Se os contadores não puderem ser lidos, a página do Bed Wars abre zerada e o erro vai para o log.

## Limitações conhecidas

- Só o Bed Wars tem estatísticas de verdade; os outros jogos mostram zeros até existirem.
- Os ícones de nível ficam configurados duas vezes, no core e no plugin do BedWars.
