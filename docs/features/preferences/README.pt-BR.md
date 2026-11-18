[English](README.md) | **Português**

# Preferências

O menu `Preferências` do `Meu perfil`, onde cada jogador escolhe como a rede o trata: chat, confirmações, convites e privacidade. Salvo por jogador e válido em todo servidor.

## Histórias de usuário

- Como jogador, desativo o bate-papo geral e deixo de ver as mensagens dos outros jogadores.
- Como jogador, ativo `Confirmar voltar para o lobby` para um `/lobby` sem querer não me tirar da partida.
- Como jogador, escondo minhas estatísticas ou deixo de receber convites para party.
- Como jogador Ultra+, escolho o formato em que vejo a tag de todos os jogadores no chat, no TAB e acima da cabeça.

## Como funciona

- **Menu:** `Preferências`, 6 linhas, aberto pelo `Preferências` (slot 31) do `Meu perfil`, com `Voltar` no slot 49 (`Para Meu perfil`). Três abas nos slots 1 a 3: `Geral` (repetidor), `Social` (cookie) e `Privacidade` (funil). A aba aberta diz `Selecionado.`, as outras `Clique para selecionar.`; a linha de vidros embaixo delas (slots 9 a 17) é cinza, com um vidro verde-limão sob a aba aberta.
- **Entradas:** cada preferência mostra o ícone e, logo abaixo, um corante com o mesmo nome e lore; clicar em qualquer um muda na hora e salva. Uma opção liga/desliga mostra `Valor: Ativado` (corante verde-limão, nome verde) ou `Valor: Desativado` (corante cinza, nome cinza) e `Clique para desativar.`/`Clique para ativar.`. `Mensagens privadas` alterna `Público` → `Limitado` → `Amigos` → `Desativado`, listando as quatro com a escolhida marcada e `Clique para trocar para <próxima>.`
- **Geral:** `Bate-papo` (slot 29, ativado), `Confirmar voltar para o lobby` (31, desativado) e `Formato da tag` (33, só Ultra+), que abre o menu `Formato da tag`.
- **Social:** `Mensagens privadas` (28, Público), `Convite para party` (30, ativado), `Convite para clan` (32, ativado) e `Pedidos de amizade` (34, ativado).
- **Privacidade:** `Visibilidade de estatísticas` (29, ativado), `Site: Mostrar Discord` (31, ativado) e `Site: Congelar estatísticas` (33, ativado).
- **Formato da tag:** 3 linhas, `Voltar` no slot 21 (`Para Preferências`). Seis corantes nos slots 10 a 15: `Padrão` (corante branco, para todos), `Cor` (laranja), `Colchetes (Maiúsculo)` (magenta), `Colchetes (Normal)` (azul-claro), `Padrão (Branco)` (amarelo) e `Padrão (Cinza)` (verde-limão), os cinco últimos `Exclusivo para ULTRA+`. Cada um mostra um exemplo com a própria tag de quem vê (`§5§lULTRA§6+ §5tadeu`, `§5tadeu§6+`, `§5[ULTRA§6+§5] tadeu`, `§5[Ultra§6+§5] tadeu`, `§5§lULTRA§6+ §ftadeu`, `§5§lULTRA§6+ §7tadeu`) e `Selecionado.` (brilhando), `Clique para selecionar.` ou, para quem não é Ultra+, `Bloqueado.`; clicar num bloqueado responde `messages.ultra-plus-only`.
- **O que o formato muda:** só o que o jogador que escolheu vê. A tag de todos os jogadores aparece nesse formato no chat, no TAB e na name tag acima da cabeça:
  - `Cor`: sem prefixo, o nick na cor da tag, seguido do final colorido do nome da tag (o `+` do Ultra+).
  - `Colchetes (Maiúsculo)` e `Colchetes (Normal)`: `[TAG]` ou `[Tag]` na cor da tag, sem negrito.
  - `Padrão (Branco)` e `Padrão (Cinza)`: o prefixo de sempre com o nick branco ou cinza.
  - Tags sem prefixo (`Membro`) ficam iguais; medalhas e emblemas continuam em volta do nome.
  - O formato só vale enquanto o jogador for Ultra+; sem isso ele vê o `Padrão`. Um scoreboard compartilhado por jogadores com formatos diferentes mostra o `Padrão`, e as name tags de time de uma partida não mudam.
- **O que cada uma já faz:**
  - `Bate-papo` desativado: o jogador deixa de receber o chat público dos outros jogadores; as próprias mensagens, as da party, da staff e do sistema continuam chegando.
  - `Confirmar voltar para o lobby` ativado: o primeiro `/lobby` responde `messages.lobby-confirm`; só manda o jogador se ele digitar de novo em até 5 s.
  - `Convite para party` desativado: convites para o jogador são recusados e quem convidou recebe `messages.party-invites-off`.
  - `Visibilidade de estatísticas` desativado: o `/stats <jogador>` dos outros responde `messages.statistics-hidden`.
  - `Mensagens privadas`, `Convite para clan`, `Pedidos de amizade` e as duas opções do `Site` são salvas, mas ainda não têm efeito, porque mensagens privadas, clans, amigos e o site ainda não existem.

## Comandos

Não se aplica. O menu é aberto pelo `Meu perfil`.

## Permissões

Não se aplica.

## Configuração

`config.yml` do core, com o inglês em `lang/en.yml`:

| Chave | Padrão | Descrição |
|---|---|---|
| `menus.preferences.title`, `rows`, `first-pane` | `Preferências`, `6`, `9` | Título, altura e primeiro slot da linha de vidros. |
| `menus.preferences.tab-selected`, `tab-unselected` | `Selecionado.`, `Clique para selecionar.` | Lore das abas. |
| `menus.preferences.name-on`, `name-off`, `value`, `enabled`, `disabled`, `click-enable`, `click-disable` | a referência | Cor do nome e lore de uma opção liga/desliga. |
| `menus.preferences.option-selected`, `option-other`, `click-cycle`, `options.<public\|limited\|friends\|"off">` | a referência | Linhas de `Mensagens privadas`, e o nome e o corante de cada opção (use aspas em `"off"`). |
| `menus.preferences.back` | slot 49 | `Voltar`. |
| `menus.preferences.tabs.<aba>` | `geral`, `social`, `privacidade` | `slot`, `item` e `entries.<id>` com `slot`, `preference` (ou nenhuma, para uma entrada que responde "em breve"), `opens` (um menu que a entrada abre: `tag-format`), `below.material` e `item`. |
| `menus.tag-format.title`, `rows`, `example` | `Formato da tag`, `3`, `{prefix}{color}{player}` | Título, altura e linha de exemplo do menu `Formato da tag`. |
| `menus.tag-format.texts` | a referência | Linhas `example`, `exclusive`, `selected`, `select` e `locked`. |
| `menus.tag-format.formats.<default\|color\|brackets-upper\|brackets\|default-white\|default-gray>` | slots 10 a 15 | `slot`, `exclusive` (só Ultra+) e `item` de cada formato. |
| `menus.tag-format.back` | slot 21 | `Voltar`. |
| `messages.preferences-soon`, `lobby-confirm`, `statistics-hidden`, `party-invites-off` | veja o arquivo | Respostas do menu, do `/lobby`, do `/stats` e dos convites de party. |

## Pacotes

Não se aplica.

## Dados

`core.player_preferences` (migração V22): `player_id`, `preference` (`chat`, `confirm-lobby`, `private-messages`, `party-invites`, `clan-invites`, `friend-requests`, `stats-visible`, `site-discord`, `site-freeze-stats`, `tag-format`), `value` e `updated_at`. Só as preferências alteradas têm linha; as outras usam o padrão. `ChangePreferenceCommand` salva uma, `FindPreferencesQuery` lê todas de um jogador.

## Métricas

Não se aplica.

## Integrações

- [Hotbar](../hotbar/README.pt-BR.md): `Preferências` do `Meu perfil`.
- [Estatísticas](../statistics/README.pt-BR.md): `Visibilidade de estatísticas`.
- [Party](../party/README.pt-BR.md): `Convite para party`, conferido pelo proxy.
- `MuxeCore.preferences()` dá a outros plugins as preferências carregadas dos jogadores online (`of`) e de qualquer jogador (`load`).

## Critérios de aceitação

- [x] As três abas mostram as entradas, lores e corantes da referência (`PreferencesPagesTest`).
- [x] Padrões, liga/desliga e o ciclo de mensagens privadas (`PreferencesTest`), salvos por preferência (`PostgresPreferenceRepositoryIntegrationTest`).
- [x] Bate-papo desativado esconde o chat dos outros (`PreferenceServiceTest`); o `/lobby` pede duas vezes em 5 s (`PreferencesPagesTest`); convites de party desativados são recusados (`PartyCoordinatorTest`).
- [x] `Preferências` abre com um jeito de voltar ao perfil (`ProfileMenuTest`).
- [x] `Formato da tag` mostra os seis formatos com o exemplo de quem vê e bloqueia cinco deles sem Ultra+ (`TagFormatPagesTest`); cada formato monta a linha do chat, o exemplo e a name tag, e quem usa outro formato recebe a própria linha do chat (`TagFormatLookTest`); o formato é salvo e lido (`PreferencesTest`).
- [ ] Conferido no jogo.

## Falhas e recuperação

Se as preferências não puderem ser carregadas ou salvas, valem os padrões e o erro vai para o log; uma mudança vale na hora no servidor mesmo se salvar falhar.

## Limitações conhecidas

- Mensagens privadas, convites de clan, pedidos de amizade e as opções do site ainda não fazem nada.
- O formato da tag não muda o chat da party, que o proxy monta.
- Só o `/lobby` pede confirmação; outros jeitos de voltar ao lobby (como itens) não.
