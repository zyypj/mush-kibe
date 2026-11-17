[English](README.md) | **Português**

# Ultra+

As vantagens do rank Ultra+: emblemas ao lado do nick, o `Menu do Ultra+` com a recompensa mensal de créditos e os níveis de tag, a cor do `+`, voo na sala de espera das partidas e menos espera entre mensagens no chat.

## Histórias de usuário

- Como jogador Ultra+, escolho um emblema em `Meu perfil` > `Emblemas` e vejo ele depois do meu nick no chat e no TAB.
- Como jogador Ultra+, resgato os créditos mensais uma vez por mês em `Meu perfil` > `Menu do Ultra+`.
- Como jogador Ultra+, libero novas cores de tag e do `+` conforme somo meses de Ultra+.
- Como jogador Ultra+, voo enquanto espero a partida começar e espero menos entre mensagens no chat.

## Como funciona

- **Meses de Ultra+:** toda vez que o rank `ultra+` é dado com tempo, os dias dele somam no total do jogador (30 dias = 1 mês); estender um rank ativo soma só os dias novos. Ultra+ vitalício, ou herdado de um rank vitalício como o Partner, libera tudo. As concessões que já existiam antes desta funcionalidade foram contadas uma vez pela migração.
- **Emblemas** (4 linhas, `Voltar` no slot 30 para `Meu perfil`): `Nenhum` (barreira, slot 10) e dez emblemas em açúcar: `Coração ❤`, `Flor ✿`, `Yin Yang ☯`, `X ✖`, `Estrela ✦`, `Sorriso ツ` (slots 11 a 16), `Estrelado ✩`, `Seta ⬅`, `Correto ✔` e `Mais +` (slots 19 a 22). Cada um mostra um exemplo do nick com ele (`§5§lULTRA§6+ §5tadeu❤`). O emblema vai logo depois do nick, na cor do nick (`Mais` é sempre dourado), no chat e no TAB (o sufixo da name tag), qualquer que seja a tag exibida, enquanto o jogador for Ultra+.
- **Menu do Ultra+** (5 linhas, `Voltar` no slot 39 para `Meu perfil`):
  - `Recompensa mensal` (carrinho, slot 12) paga 450 créditos uma vez por mês do calendário no horário de Brasília, diz `Reinicia em <tempo>.` até o próximo mês e `Já resgatado.` ou `Clique para resgatar.`.
  - `Cor do + (/cormais)` (slot 14) abre as cores do `+`.
  - Os níveis de tag na linha 4: `Tag Nível I` (slot 28, incluso), `Tag Nível II` (30, 6 meses, ciano), `Tag Nível III` (32, 12 meses, dourado) e `Tag Nível IV` (34, 24 meses, vermelho). Entre eles, vidros amarelos mostram o progresso para o próximo nível com uma barra de dez traços (um verde a cada 10%) e `Meses necessários: <meses>/<necessários> (<porcentagem>%)`.
  - O nível escolhido pinta o `ULTRA` e o nick da tag Ultra+ (`§b§lULTRA§6+ §btadeu`).
- **Cor do +** (4 linhas, `Voltar` no slot 30 para `Menu do Ultra+`): doze cores do `+` da tag Ultra+, de `Dourado` (incluso) a `Preto` (12 meses), uma a mais por mês.
- Um nível, cor ou emblema mostra `Selecionada.`/`Selecionado.` (e brilha) quando escolhido, `Clique para selecionar.` quando liberado e `Bloqueado.` quando o jogador não é Ultra+ ou não tem os meses. Uma escolha que deixou de estar liberada volta para a inclusa.
- **Voo na sala de espera:** jogadores com `muxe.pregame.fly` (Ultra+) podem voar na sala de espera de qualquer partida; o voo acaba quando a partida começa.
- **Intervalo do chat:** membros esperam 3 s entre mensagens, Ultra+ (`muxe.chat.fast`) 1 s, e a staff (`muxe.chat.no-cooldown`) nada. Uma mensagem enviada antes da hora não é entregue e responde `messages.chat-cooldown`.

## Comandos

| Comando | Descrição | Permissão |
|---|---|---|
| `/cormais` | Abre `Cor do +`. | Nenhuma |
| `/emblemas` (`/emblema`) | Abre `Emblemas`. | Nenhuma |

## Permissões

| Permissão | Permite |
|---|---|
| `muxe.pregame.fly` | Voar na sala de espera das partidas. Dada ao `ultra+` pela migração V23. |
| `muxe.chat.fast` | O intervalo curto do chat. Dada ao `ultra+` pela migração V23. |
| `muxe.chat.no-cooldown` | Nenhum intervalo no chat. Dada ao `builder` (e portanto a todo rank da staff) pela migração V23. |

## Configuração

`config.yml` do core, com o inglês em `lang/en.yml`:

| Chave | Padrão | Descrição |
|---|---|---|
| `menus.ultra-plus.title`, `rows`, `back` | `Menu do Ultra+`, `5`, slot 39 | O menu do Ultra+. |
| `menus.ultra-plus.prefix` | `{color}&lULTRA{plus}+ ` | Prefixo da tag Ultra+; `{color}` é a cor do nível e `{plus}` a cor do `+`. |
| `menus.ultra-plus.example` | `{prefix}{color}{player}{emblem}` | A linha de exemplo dos menus. |
| `menus.ultra-plus.reward.slot`, `credits`, `time-zone`, `item` | `12`, `450`, `America/Sao_Paulo` | A recompensa mensal; `{credits}` na lore. |
| `menus.ultra-plus.plus-color` | slot 14 | O botão que abre `Cor do +`. |
| `menus.ultra-plus.levels.<n>` | níveis 1 a 4 | `slot`, `color` (um código de cor sem `&`), `months` e `item`. |
| `menus.ultra-plus.progress` | vidro amarelo, `dashes: 10` | Os vidros entre os níveis. |
| `menus.ultra-plus.texts.*` | a referência | `included`, `required`, `progress`, `example`, `selected`, `emblem-selected`, `select`, `locked`, `exclusive`, `resets`, `claimed`, `claim`. |
| `menus.plus-colors.title`, `rows`, `back`, `colors.<id>` | `Cor do +`, `4`, slot 30, doze cores | Cada cor tem `slot`, `color`, `months` e `item`. |
| `menus.emblems.title`, `rows`, `back`, `none`, `emblems.<id>` | `Emblemas`, `4`, slot 30, dez emblemas | Cada emblema tem `slot`, `symbol`, uma `color` opcional e `item`. |
| `chat-interval.seconds`, `fast-seconds` | `3`, `1` | Segundos entre mensagens do chat para membros e para `muxe.chat.fast`. |
| `messages.ultra-plus-only`, `ultra-plus-locked`, `ultra-plus-reward-claimed`, `ultra-plus-reward-already`, `ultra-plus-reward-failed`, `chat-cooldown` | veja o arquivo | Respostas dos menus e do chat. |

## Pacotes

Não se aplica.

## Dados

`core.ultra_plus_players` (migração V23): `player_id`, `granted_days`, `emblem`, `plus_color`, `tag_level`, `reward_month` (`AAAA-MM` do último resgate) e `updated_at`. `ChangeUltraPlusStyleCommand` salva as escolhas, `ClaimUltraPlusRewardCommand` registra o mês e soma os créditos na mesma transação (motivo `ultra-plus-monthly-reward`), `FindUltraPlusQuery` lê um jogador. Os handlers de permissão chamam `UltraPlusGrants` para somar os dias de cada concessão de Ultra+.

## Métricas

Não se aplica.

## Integrações

- [Hotbar](../hotbar/README.pt-BR.md): `Emblemas` e `Menu do Ultra+` do `Meu perfil`.
- [Ranks e tags](../ranks-and-tags/README.pt-BR.md): a tag Ultra+ é reestilizada e o emblema entra pelo `PermissionService.style`.
- [Créditos](../credits/README.pt-BR.md): a recompensa mensal.
- [API de minigames](../../architecture/minigame-api/README.pt-BR.md): `Stage.waitingFlight` para a sala de espera.

## Critérios de aceitação

- [x] Os três menus seguem os exports de referência, com os vidros de progresso e a recompensa resgatada (`UltraPlusPagesTest`).
- [x] O nível da tag, a cor do `+` e o emblema mudam o chat, o prefixo e a name tag, e escolhas bloqueadas voltam para a inclusa (`UltraPlusLookTest`).
- [x] Os dias de cada concessão se somam e extensões contam só os dias novos (`UltraPlusGrantTest`).
- [x] A recompensa paga uma vez por mês (`ClaimUltraPlusRewardHandlerTest`) e é salva junto com as escolhas (`PostgresUltraPlusRepositoryIntegrationTest`).
- [x] Membros esperam 3 s e Ultra+ 1 s entre mensagens do chat (`ChatIntervalTest`).
- [x] Entrar numa partida deixa o jogador voar na sala de espera (`MatchRuntimeTest`).
- [ ] Conferido no jogo.

## Falhas e recuperação

Se o perfil Ultra+ não puder ser carregado, o jogador fica com o visual incluso até a próxima entrada e o erro vai para o log. Uma recompensa que não pode ser resgatada responde `messages.ultra-plus-reward-failed` e não paga nada.

## Limitações conhecidas

- O `Efeito de entrada` (cosméticos de entrada no lobby) ainda não foi feito.
- O emblema e a tag reestilizada não aparecem nas name tags de time dentro da partida.
- Os meses contam só concessões feitas pelos comandos de permissão ou pelo editor.
