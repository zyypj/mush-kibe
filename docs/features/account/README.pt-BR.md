[English](README.md) | **Português**

# Conta

O que um jogador pode ver sobre uma conta: `/account` no chat, `/accountmenu` num menu e a cabeça do menu de perfil. Fica no Muxe-Core (pacote `account` do `core:bukkit`), em todo servidor Bukkit. Os dados vêm das [Contas](../accounts/README.pt-BR.md) (`FindAccountDetailsQuery`: conta, número da conta e localização) e os cargos de [Cargos e tags](../ranks-and-tags/README.pt-BR.md).

## Histórias de usuário

- Como jogador, quero ver a minha conta: tipo, cargos com as datas, primeiro e último login e de onde eu entro.
- Como staff, quero ver o mesmo de qualquer jogador, online ou não, e ter à mão as ferramentas de staff da conta.

## Como funciona

### `/account [jogador]`

Sem nome mostra a sua própria conta; com nome, a de outro jogador (precisa de `muxe.account.others`). Jogadores que nunca entraram recebem `account.not-found`. O console precisa dar um nome.

```text
Usuário: tadeu
Tipo: Premium
Discord: Em breve
Rank: VIP
  Adicionado em: 09/09/2022 14:09
  Atualizado em: 09/09/2022 14:09
Rank: Ultra
  Adicionado em: 21/10/2023 11:29
  Atualizado em: 21/10/2023 11:29
  Expira em: 20/11/2026 11:29 (23d 2h)
Primeiro login: 10/09/2021 16:29
Último login: 28/10/2026 13:07
Localização:
  País: Brasil
  Estado: Minas Gerais
  Cidade: Tupaciguara
  ASN: AS264207 Ponto Net Telecom
```

- Os rótulos são `&a` e os valores `&f`; as linhas recuadas são `&7`.
- `Tipo` é `Premium` ou `Pirata`. O Discord ainda não é vinculado e mostra `Em breve`.
- Um bloco `Rank` por cargo dado ao jogador, do menor para o maior: a data em que o cargo foi dado pela primeira vez e a da última mudança. Cargos temporários acrescentam `Expira em` com o tempo que falta. Um jogador sem cargos mostra `Rank: Membro`, sem datas.
- As datas usam `menus.account.date-format` (`dd/MM/yyyy HH:mm`) no fuso `menus.account.time-zone` (`America/Sao_Paulo`).
- A localização é preenchida pelo proxy no login. Partes desconhecidas ficam de fora; sem nada conhecido aparece `Desconhecida`.

### `/accountmenu [jogador]`

Abre um menu de 6 linhas com o título `Conta: <nome>`, com a mesma regra para outros jogadores.

- Slot 4: a cabeça do jogador, com o nome na cor da tag que ele mostra (`&atadeu`) e a lore:

```text
Tipo: Premium
ID: (1680062)
ID de Perfil: Em breve
UUID: 595a25f1-eb36-4999-baa8-9c73efc8fdb7

Rank: VIP
Rank: Ultra
Primeiro login: 10/09/2021 16:29:25
Último login: 28/10/2026 13:07:27
Discord: Em breve
```

- Os rótulos são `&7` e os valores `&f`; cada `Rank` é a exibição da tag (`&aVIP`). `ID` é o número da conta. As datas têm segundos (`menus.account.menu-date-format`).
- Slot 49: uma cama vermelha, `&cFechar`, que fecha o menu.
- Jogadores com `muxe.account.staff` também veem `Punições` (slot 20), `Histórico de punições` (21), `Estatísticas` (23) e `Migrar estatísticas` (24). Esses sistemas ainda não existem: clicar responde `&cEm breve.`

### Cabeça do perfil

A cabeça do `Meu perfil` (veja [Hotbar](../hotbar/README.pt-BR.md#perfil)) mostra o nome com a tag exibida (`&c&lNATAL &ctadeu`), o primeiro login, os créditos e uma linha por cargo: `&7Rank: &aVIP &7(Vitalício)` ou o tempo que falta, como `(29d 23h)`.

## Comandos

| Comando | Descrição | Permissão |
|---|---|---|
| `/account` | A sua conta no chat. | todos |
| `/account <jogador>` | A conta de outro jogador. | `muxe.account.others` |
| `/accountmenu` | O menu da sua conta. | todos |
| `/accountmenu <jogador>` | O menu da conta de outro jogador. | `muxe.account.others` |

Aliases: `/conta` e `/contamenu`. Os nomes completam com os jogadores online.

## Permissões

| Permissão | Libera | Padrão |
|---|---|---|
| `muxe.account.others` | `/account` e `/accountmenu` em outros jogadores. | op; semeada no `mod` e acima pela `V10` |
| `muxe.account.staff` | Os botões de staff do `/accountmenu`. | op; semeada no `mod` e acima pela `V10` |

## Configuração

`plugins/Muxe-Core/config.yml`, em `menus.account`:

| Chave | Padrão | Descrição |
|---|---|---|
| `title`, `rows` | `Conta: {player}`, `6` | Título e altura do menu. |
| `time-zone`, `date-format`, `menu-date-format` | `America/Sao_Paulo`, `dd/MM/yyyy HH:mm`, `dd/MM/yyyy HH:mm:ss` | Datas do `/account` e do menu. |
| `head` | Slot `4`, `SKULL_ITEM:3` com o nome `{colored-name}` | `slot` e `item` da cabeça. Placeholders: `{player}`, `{name}` (com o prefixo da tag), `{colored-name}`, `{type}`, `{number}`, `{profile-id}`, `{uuid}`, `{first-login}`, `{last-login}`, `{discord}`. |
| `rank-line` | `&7Rank: {rank}` | Linha da lore adicionada depois da lore da cabeça para cada cargo. |
| `head-footer` | `Primeiro login`, `Último login`, `Discord` | Linhas da lore depois dos cargos. Os mesmos placeholders. |
| `close` | Slot `49`, `BED` com o nome `&cFechar` | O botão de fechar. |
| `staff-buttons.<id>` | `punishments`, `punishment-history`, `statistics`, `migrate-statistics` | Botões que só a staff vê. |

### Mensagens

Em `messages.account` do `plugins/Muxe-Core/config.yml`:

| Chave | Padrão |
|---|---|
| `usage` | `&cUso: &f/account <jogador>` |
| `not-found` | `&cO jogador &f{player} &cnunca entrou no servidor.` |
| `premium`, `cracked`, `soon` | `Premium`, `Pirata`, `Em breve` |
| `soon-click` | `&cEm breve.` |
| `user`, `type`, `discord` | `&aUsuário: &f{player}`, `&aTipo: &f{type}`, `&aDiscord: &f{discord}` |
| `rank` | `&aRank: &f{rank}` (`{rank-display}` dá a tag colorida) |
| `rank-added`, `rank-updated`, `rank-expires` | `  &7Adicionado em: {date}`, `  &7Atualizado em: {date}`, `  &7Expira em: {date} ({time})` |
| `first-login`, `last-login` | `&aPrimeiro login: &f{date}`, `&aÚltimo login: &f{date}` |
| `location`, `country`, `region`, `city`, `asn`, `location-unknown` | `&aLocalização:`, `  &7País: {value}`, ..., `  &7Desconhecida` |

## Pacotes

Nenhum.

## Dados

Só leitura: `core.accounts` (com `number`, `V9`), `core.account_locations` (`V9`) e as tabelas de cargos de [Cargos e tags](../ranks-and-tags/README.pt-BR.md#dados), com `FindAccountDetailsQuery` e `FindPermissionUserQuery`.

## Métricas

Nenhuma própria.

## Integrações

- [Contas](../accounts/README.pt-BR.md): conta, número e localização.
- [Cargos e tags](../ranks-and-tags/README.pt-BR.md): cargos, datas e a tag exibida.
- [Hotbar](../hotbar/README.pt-BR.md): o menu de perfil.

## Critérios de aceitação

- [x] `/account` mostra usuário, tipo, Discord, cada cargo com as datas, os logins e a localização, com as cores da referência.
- [x] Outros jogadores precisam de `muxe.account.others`; jogadores desconhecidos e o console sem nome recebem respostas claras.
- [x] `/accountmenu` tem a cabeça no slot 4 com a lore da referência e a cama no slot 49; a staff vê mais quatro botões.
- [x] A cabeça do perfil mostra a tag exibida e os cargos com `Vitalício` ou o tempo que falta.
- [ ] O menu e as mensagens ficam iguais à referência no jogo (verificado no jogo).

## Falhas e recuperação

- PostgreSQL indisponível: o comando responde `rank-failed` e nada abre.

## Limitações conhecidas

- Discord, ID de perfil, punições e estatísticas ainda não existem.
- Jogadores são buscados pelo nome atual; um nome liberado para outra pessoa mostra a conta dela.
