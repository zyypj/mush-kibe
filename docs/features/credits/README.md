**English** | [Português](README.pt-BR.md)

# Credits and cosmetics

Credits are the network currency that buys cosmetics. Muxe-Core keeps each player's balance, the cosmetics they own and the one they selected in every category. Games such as [BedWars](../bedwars/README.md#bed-destruction-cosmetics) define their own catalogs on top of it.

## User stories

- As a player, I want to see my credits on the lobby scoreboard, so that I know what I can buy.
- As a player, I want to buy a cosmetic with credits and keep it forever, so that I can use it in every match.
- As a staff member, I want to give or remove credits and give event cosmetics, so that I can run events and fix mistakes.

## How it works

- Every player starts with 0 credits. A balance never goes below zero: a purchase or removal that would make it negative fails with `credits.not_enough` and changes nothing.
- Every change is recorded with its amount, reason (`staff`, `cosmetic:<id>`...) and time.
- A purchase spends the price and owns the cosmetic in one transaction. Buying something already owned fails with `cosmetic.already_owned` and spends nothing.
- Staff can give a cosmetic without spending credits (`GrantCosmeticCommand`); giving one the player already owns changes nothing.
- A player selects at most one cosmetic per category. A game decides what an unknown or missing selection means (BedWars treats it as `aleatorio`).
- The lobby scoreboard shows the balance from `CreditBalances`, which loads it on join and reloads every 30 seconds, so purchases made on another server show up within that time.
- Amounts are shown with `.` as the thousands separator (`1.750`).

## Commands

| Command | Description | Permission |
|---|---|---|
| `/creditos` (`/credito`, `/credits`) | Shows your balance. | none |
| `/creditos ver <jogador>` | Shows a player's balance. | `muxe.credits.admin` |
| `/creditos dar <jogador> <quantia>` | Adds credits (1 to 1.000.000.000). | `muxe.credits.admin` |
| `/creditos remover <jogador> <quantia>` | Removes credits, never below zero. | `muxe.credits.admin` |

The BedWars cosmetics have their own `/cosmeticos dar`, described in [BedWars](../bedwars/README.md#commands).

## Permissions

| Permission | Grants |
|---|---|
| `muxe.credits.admin` | `/creditos ver`, `dar` and `remover`. |
| `muxe.cosmetic.all` | Owning every BedWars cosmetic (ops by default). |
| `muxe.cosmetic.ultra` | Owning the cosmetics marked `Incluso no ULTRA`. Migration V14 gives it to the `ultra` group. |

## Configuration

Muxe-Core `config.yml`, `messages`: `credits-own`, `credits-of`, `credits-given`, `credits-taken`, `credits-not-enough` and `credits-invalid-amount`. Prices and catalogs belong to each game.

## Packets

None.

## Data

Schema `core`, migration `V14__create_credits_and_cosmetics.sql`:

| Table | What it holds |
|---|---|
| `credits` | Balance per player (`balance >= 0`). |
| `credit_changes` | Every change: player, amount, reason, time. |
| `owned_cosmetics` | Player, category, cosmetic id, source (`compra`, `staff`...), when. |
| `selected_cosmetics` | Player, category, selected cosmetic id. |

Commands and queries in `br.com.muxe.core.common`: `ChangeCreditsCommand`, `FindCreditsQuery`, `BuyCosmeticCommand`, `GrantCosmeticCommand`, `SelectCosmeticCommand`, `FindCosmeticsQuery`.

## Metrics

None.

## Integrations

- [Lobby](../lobby/README.md): the scoreboard `{credits}`.
- [BedWars](../bedwars/README.md#bed-destruction-cosmetics): the bed destruction cosmetics.

## Acceptance criteria

- [x] Balances start at zero, grow and never go negative (`CosmeticHandlersTest`).
- [x] A purchase spends once and owns once (`CosmeticHandlersTest`).
- [x] Staff grants own without spending (`CosmeticHandlersTest`).
- [x] The lobby scoreboard shows the balance (`RankInfoSourceTest`, `CreditBalancesTest`).
- [ ] `/creditos` checked in game.

## Failures and recovery

If the database is unavailable, the command answers `rank-failed` and nothing changes. The scoreboard keeps the last balance it loaded.

## Known limitations

- There is no credit store yet; credits only come from staff.
- Other servers learn about a change only when they reload the balance (lobby every 30 seconds, BedWars when the player joins or opens a menu).
