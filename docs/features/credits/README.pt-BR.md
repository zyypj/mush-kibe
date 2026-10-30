[English](README.md) | **Português**

# Créditos e cosméticos

Créditos são a moeda da rede para comprar cosméticos. O Muxe-Core guarda o saldo de cada jogador, os cosméticos que ele possui e o que ele selecionou em cada categoria. Jogos como o [BedWars](../bedwars/README.pt-BR.md#cosméticos-de-destruição-de-cama) definem os próprios catálogos em cima disso.

## Histórias de usuário

- Como jogador, quero ver meus créditos na scoreboard do lobby, para saber o que posso comprar.
- Como jogador, quero comprar um cosmético com créditos e ficar com ele para sempre, para usar em todas as partidas.
- Como staff, quero dar ou remover créditos e dar cosméticos de evento, para fazer eventos e corrigir erros.

## Como funciona

- Todo jogador começa com 0 créditos. O saldo nunca fica negativo: uma compra ou remoção que deixaria o saldo negativo falha com `credits.not_enough` e não muda nada.
- Toda mudança é registrada com a quantia, o motivo (`staff`, `cosmetic:<id>`...) e a hora.
- Uma compra gasta o preço e dá o cosmético na mesma transação. Comprar algo que já possui falha com `cosmetic.already_owned` e não gasta nada.
- A staff pode dar um cosmético sem gastar créditos (`GrantCosmeticCommand`); dar um que o jogador já tem não muda nada.
- O jogador seleciona no máximo um cosmético por categoria. Cada jogo decide o que significa uma seleção desconhecida ou ausente (o BedWars trata como `aleatorio`).
- A scoreboard do lobby mostra o saldo do `CreditBalances`, que carrega ao entrar e recarrega a cada 30 segundos, então compras feitas em outro servidor aparecem dentro desse tempo.
- As quantias usam `.` como separador de milhar (`1.750`).

## Comandos

| Comando | Descrição | Permissão |
|---|---|---|
| `/creditos` (`/credito`, `/credits`) | Mostra o seu saldo. | nenhuma |
| `/creditos ver <jogador>` | Mostra o saldo de um jogador. | `muxe.credits.admin` |
| `/creditos dar <jogador> <quantia>` | Adiciona créditos (1 a 1.000.000.000). | `muxe.credits.admin` |
| `/creditos remover <jogador> <quantia>` | Remove créditos, nunca abaixo de zero. | `muxe.credits.admin` |

Os cosméticos do BedWars têm o próprio `/cosmeticos dar`, descrito no [BedWars](../bedwars/README.pt-BR.md#comandos).

## Permissões

| Permissão | Concede |
|---|---|
| `muxe.credits.admin` | `/creditos ver`, `dar` e `remover`. |
| `muxe.cosmetic.ultra` | Possuir os cosméticos marcados `Incluso no ULTRA`. A migração V14 dá essa permissão ao grupo `ultra`. |

## Configuração

`config.yml` do Muxe-Core, `messages`: `credits-own`, `credits-of`, `credits-given`, `credits-taken`, `credits-not-enough` e `credits-invalid-amount`. Preços e catálogos são de cada jogo.

## Pacotes

Nenhum.

## Dados

Schema `core`, migração `V14__create_credits_and_cosmetics.sql`:

| Tabela | O que guarda |
|---|---|
| `credits` | Saldo por jogador (`balance >= 0`). |
| `credit_changes` | Toda mudança: jogador, quantia, motivo, hora. |
| `owned_cosmetics` | Jogador, categoria, id do cosmético, origem (`compra`, `staff`...), quando. |
| `selected_cosmetics` | Jogador, categoria, id do cosmético selecionado. |

Comandos e consultas em `br.com.muxe.core.common`: `ChangeCreditsCommand`, `FindCreditsQuery`, `BuyCosmeticCommand`, `GrantCosmeticCommand`, `SelectCosmeticCommand`, `FindCosmeticsQuery`.

## Métricas

Nenhuma.

## Integrações

- [Lobby](../lobby/README.pt-BR.md): o `{credits}` da scoreboard.
- [BedWars](../bedwars/README.pt-BR.md#cosméticos-de-destruição-de-cama): os cosméticos de destruição de cama.

## Critérios de aceitação

- [x] O saldo começa em zero, cresce e nunca fica negativo (`CosmeticHandlersTest`).
- [x] Uma compra gasta uma vez e dá o cosmético uma vez (`CosmeticHandlersTest`).
- [x] A staff dá cosméticos sem gastar (`CosmeticHandlersTest`).
- [x] A scoreboard do lobby mostra o saldo (`RankInfoSourceTest`, `CreditBalancesTest`).
- [ ] `/creditos` conferido no jogo.

## Falhas e recuperação

Se o banco estiver fora, o comando responde `rank-failed` e nada muda. A scoreboard mantém o último saldo carregado.

## Limitações conhecidas

- Ainda não existe loja de créditos; créditos só vêm da staff.
- Outros servidores só ficam sabendo de uma mudança quando recarregam o saldo (o lobby a cada 30 segundos, o BedWars quando o jogador entra ou abre um menu).
