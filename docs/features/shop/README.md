**English** | [Português](README.pt-BR.md)

# Shop

The shop where players start buying a rank, credits or medals, and the cash registers placed around the network that open the promoted product. Lives in Muxe-Core (`core:bukkit`, package `br.com.muxe.core.bukkit.shop`), so it runs on every server kind listed in `shop.server-kinds`: lobbies and minigames, never auth servers.

## User stories

- As a player, I want to open the shop from any lobby or game server and browse ranks, Ultra+, credits and medals.
- As a player, I want to click a cash register and go straight to the product on sale.
- As a staff member, I want to place several cash registers in a server group, each with an optional seller, without editing files.
- As a staff member, I want to change the promoted product and the text above every cash register of the network with one command.

## How it works

### Opening the shop

- `/loja` (`/shop`) opens the shop on the **Ranks** tab (`shop.default-category`). An NPC with the `loja` action does the same (see [NPCs and holograms](../npcs-and-holograms/README.md)).
- On a server whose kind is not in `shop.server-kinds`, `/loja` and shop NPCs only answer `&cA loja não está disponível neste servidor.`
- Every shop menu has 6 rows, the tabs **Ranks**, **Ultra+**, **Créditos**, **Medalhas** and **Meus pedidos** in the first row (the open tab glows), gray panes in the second row with a lime pane under the open tab, and the products in the fourth row (slots 28, 30, 32 and 34). Medal previews show the player's own name.

### Checkout

A left click on a product with `&eBotão esquerdo: &fComprar` starts the checkout:

1. **Compra: Método de pagamento**: Pix or credit card, a summary of the order and **Voltar**.
2. **Compra: Código de apoiador**: use a supporter code or not. Using one closes the menu and asks for the code in the chat (`cancelar` cancels, 60 seconds to answer). Codes are not checked yet.
3. **Compra: Carregando...**: a lime pane spins around the middle of the menu for 3 seconds.
4. The menu closes and the payment message is sent, centered, with `&cClique aqui para fazer o pagamento via o site.` opening `shop.checkout.payment-url`.

**Voltar** returns to the menu the checkout came from. A product page (a product with `page` in the config) shows the product and the payment methods directly, then follows the same steps.

Nothing is charged or delivered yet: no order is created, so **Meus pedidos** always shows `&cNenhum pedido encontrado!`. The **Vantagens** papers and the **Ultra+** products (which require Ultra) do nothing yet.

### Cash registers

- A cash register is made of 8 heads on small invisible armor stands (`shop.register.parts`), two purple dyes that spin and bob around the seller, and the promotion lines above it. It only exists in packets, like the [display stands](../../development/core-api/README.md#display-stands).
- `/loja caixa criar <id>` creates a register where you stand, with its seller on the same spot. A server group can have as many registers as needed; every server of the group shows all of them.
- The seller is an NPC with the skin of `shop.register.seller-skin`. `/loja vendedor <id>` moves it to where you stand, so it can stand apart from the register, and `/loja vendedor <id> remover` leaves the register without a seller.
- Clicking the register (any head, dye or line) or its seller opens the page of the promoted product. If that product no longer exists or has no page, the shop opens instead. Clicks share the anti-spam of Muxe-Core (`spam.*`).
- Registers are saved in PostgreSQL per server group and synced like NPCs and holograms (see [NPCs and holograms](../npcs-and-holograms/README.md#how-it-works)). A register can also be brought in through `plugins/Muxe-Core/import.yml`.

### Network promotion

- The promotion is one product id and the lines shown above every cash register of the network, on every server group. It is stored in one row of `core.shop_promotion`.
- Until someone changes it, `shop.promotion` from the config is used (**Enderlore**, with `LOJA`, `RANK ENDERLORE`, `13% DE DESCONTO!`, the price and `Clique para comprar!`).
- `/loja promocao` shows the current product and its numbered lines. `/loja promocao produto <id>` changes the product (only products that have their own page are accepted); `adicionar`, `linha` and `removerlinha` edit the lines. `{vazio}` makes a blank line and `&` codes give colors.
- The server where the change was made applies it at once; the others receive `shop.promotion_changed` and reload it from PostgreSQL.
- The lines stack upward: the last line sits `shop.register.labels.bottom` (2.25625) blocks above the register, and each line above it is placed using `shop.register.labels.gaps`, counted from the bottom (0.40625, 0.28125, 0.28125, 0.375), then `shop.register.labels.spacing` (0.28125) for any extra line. The bottom line always sits at the same height, so adding a line pushes the others up.

## Commands

| Command | Description | Permission |
|---|---|---|
| `/loja` (`/shop`) | Opens the shop on the Ranks tab. | None |
| `/loja caixa criar <id>` | Creates a cash register where you stand, with its seller on the same spot. | `muxe.shop.manage` |
| `/loja caixa mover <id>` | Moves a register to where you stand. The seller stays where it is. | `muxe.shop.manage` |
| `/loja caixa remover <id>` | Removes a register and its seller. | `muxe.shop.manage` |
| `/loja caixa listar` | Lists the registers of this server group. | `muxe.shop.manage` |
| `/loja vendedor <id>` | Moves the seller of a register to where you stand. | `muxe.shop.manage` |
| `/loja vendedor <id> remover` | Removes the seller of a register. | `muxe.shop.manage` |
| `/loja promocao` | Shows the network promotion: product and numbered lines. | `muxe.shop.manage` |
| `/loja promocao produto <id>` | Changes the product every register opens. | `muxe.shop.manage` |
| `/loja promocao adicionar <texto>` | Adds a line at the bottom of the register text; the other lines move up. | `muxe.shop.manage` |
| `/loja promocao linha <número> <texto>` | Replaces a line. | `muxe.shop.manage` |
| `/loja promocao removerlinha <número>` | Removes a line. | `muxe.shop.manage` |

Ids use lowercase letters, digits, `-` and `_`, up to 32 characters. Line numbers start at 1 at the top. Subcommands, register ids and the products a register can open complete with tab; a line number that is not a number answers `command-invalid-number`.

## Permissions

| Permission | Grants | Default |
|---|---|---|
| `muxe.shop.manage` | Use `/loja caixa`, `/loja vendedor` and `/loja promocao`. Replaces `muxe.lobby.shop`. | op |

## Configuration

`plugins/Muxe-Core/config.yml`:

| Key | Default | Description |
|---|---|---|
| `shop.server-kinds` | `LOBBY`, `MINIGAME` | Server kinds that run the shop and show the registers. |
| `shop.title`, `shop.default-category` | `Loja: {name}`, `ranks` | Menu title and the tab `/loja` opens. |
| `shop.promotion.product`, `shop.promotion.lines` | `enderlore`, the 5 register lines | Promotion used until someone changes it with `/loja promocao`. |
| `shop.panes.other`, `shop.panes.current` | Gray and lime panes | Second row of the shop menus. |
| `shop.categories.<id>` | Ranks, Ultra+, Créditos, Medalhas, Meus pedidos | `name`, `tab-slot`, `tab` (item), `products` and `extras` (items that do nothing, such as the **Vantagens** papers). |
| `shop.categories.<id>.products.<id>` | See the config | `slot`, `price` (shown in the checkout), `purchasable` (default `true`), `item`, and `page` (`name`, `item`) for a product that has its own page. `{player}` in the lore becomes the player's name. Only products with `page` can be promoted. |
| `shop.checkout.*` | Reference titles and items | `method-title`, `supporter-title`, `loading-title`, `pix`, `card`, `summary`, `back`, `use-code`, `skip-code` (placeholders `{product}`, `{price}`, `{menu}`), `code-timeout` (`60s`), `payment-url`, `chat-center` (`114`). |
| `shop.checkout.loading.*` | `150ms`, `3s`, lime and gray panes | How often the lime pane moves, how long the loading lasts, and the two panes. |
| `shop.register.labels.bottom` | `2.25625` | Height of the last promotion line above the register. |
| `shop.register.labels.gaps` | `0.40625`, `0.28125`, `0.28125`, `0.375` | Space between each line and the one below it, counted from the bottom. |
| `shop.register.labels.spacing` | `0.28125` | Space for lines beyond the gaps. |
| `shop.register.seller-skin` | A fixed skin | `value` and `signature` of the seller NPC. |
| `shop.register.parts` | 8 heads and 2 dyes | Display stands: `x`, `y`, `z` (offset), `yaw`, `small`, `arms`, `helmet`, `hand`, `head-pose`, `right-arm-pose`, `motion` (`spin` degrees per tick, `bob-height`, `bob-period` ticks, `phase`). |
| `spam.max-clicks`, `spam.window`, `spam.cooldown` | `3`, `1s`, `3s` | Anti-spam shared by register, seller, NPC and hotbar clicks. |
| `messages.shop-*`, `messages.placement-failed`, `messages.spam-cooldown` | Portuguese texts | Shop, register and promotion messages. |

`shop.featured-product`, `shop.register.location`, `shop.register.labels.line-N` and `shop.register.seller.enabled` no longer exist: registers are placed by command and the promotion replaces the featured product. Items use the [`ItemBuilder` format](../../development/core-api/README.md#items).

## Packets

| Id | Target | Policy | Purpose |
|---|---|---|---|
| `shop.promotion_changed` | broadcast | standard | The promotion changed; every server reloads it from PostgreSQL. Carries the product id. |
| `placement.changed` | the server group | standard | A register was created, moved or removed; every server of the group reloads its placements. |

## Data

| Table | Content |
|---|---|
| `core.shop_promotion` | A single row (`id = 1`): `product_id`, `lines` (text array), `changed_at`. Missing until the first change. |
| `core.register_placements` | `server_group`, `id`, the register position (`world`, `x`, `y`, `z`, `yaw`, `pitch`) and the optional seller position (`seller_*`, all null without a seller). Primary key `(server_group, id)`. |

Commands and queries: `ChangePromotionCommand`, `FindPromotionQuery`, and the placement ones (`SavePlacementCommand`, `RemovePlacementCommand`, `FindPlacementsQuery`).

## Metrics

`muxe_db_transaction_seconds{operation="shop.change_promotion"|"shop.find_promotion"|"placement.save"|"placement.remove"|"placement.find"}`, `muxe_cqrs_handle_seconds` for the commands and queries above, and the packet metrics of `shop.promotion_changed` and `placement.changed`.

## Integrations

- The [Core API](../../development/core-api/README.md) provides the menus, items, display stands, NPCs, chat prompts and centered chat lines.
- [NPCs and holograms](../npcs-and-holograms/README.md) share the placement storage and sync, and NPCs with the `loja` action open the shop.
- The [Lobby](../lobby/README.md) and the minigame servers get the shop from Muxe-Core; they do not configure it.

## Acceptance criteria

- [x] The shop menus follow the reference layout: tabs, panes, products, the product page and the three checkout menus, with **Voltar** returning to the menu the checkout came from.
- [x] The loading pane goes around the middle slot, and the payment message is centered.
- [x] The promotion from the config is used until someone changes it; a change reaches every server of the network, and a server that starts later loads the stored promotion.
- [x] The register lines stack upward from the bottom line, using the gaps from the bottom and then the spacing.
- [x] Registers and sellers survive a round trip through PostgreSQL, and registers of one group never show up in another.
- [x] Registers listed in `import.yml` are imported into the server's group.
- [ ] Creating, moving and removing registers and sellers, the promotion commands, register clicks and the whole checkout work in game on a lobby and on a minigame server (checked in game).

## Failures and recovery

- PostgreSQL unavailable when a staff member changes a register or the promotion: they see `&cNão foi possível salvar agora. Tente novamente em instantes.` and nothing changes.
- PostgreSQL unavailable on startup: no register is shown and the config promotion is used, with a warning in the log. The next change in the group, or a restart, loads them again.
- Redis unavailable: the change is saved and shown on the server where it was made; the other servers show it after their next reload (another change or a restart).
- Register or seller in a world that is not loaded: it is skipped with a warning.
- Promoted product removed from the config: clicking a register opens the shop on the default tab.
- `plugins/Muxe-Core/config.yml` without a `shop` section (a config written by an older version): the shop is off on that server, with a warning in the log. Copy the `shop` section of the bundled config to turn it on.
- A player who leaves during the supporter code prompt: the prompt is dropped; nothing was ordered.

## Known limitations

- The shop does not sell anything yet: there are no orders, payments, supporter code checks or ranks, and the **Vantagens** papers and **Ultra+** products do nothing.
- There is one promotion for the whole network; registers cannot show different products.
- The register keeps its parts in world axes, so moving it does not turn it.
- The seller looks in a fixed direction: the one you faced when it was placed.
- The promotion product is checked only on the server where the command ran; a server with a different catalog opens the shop instead.
