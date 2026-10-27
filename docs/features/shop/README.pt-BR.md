[English](README.md) | **Português**

# Loja

A loja onde os jogadores começam a compra de um rank, créditos ou medalhas, e as caixas registradoras espalhadas pela rede que abrem o produto em promoção. Fica no Muxe-Core (`core:bukkit`, pacote `br.com.muxe.core.bukkit.shop`), então funciona em todo tipo de servidor listado em `shop.server-kinds`: lobbys e minigames, nunca nos servidores de autenticação.

## Histórias de usuário

- Como jogador, quero abrir a loja de qualquer lobby ou servidor de jogo e ver ranks, Ultra+, créditos e medalhas.
- Como jogador, quero clicar numa caixa registradora e ir direto ao produto em promoção.
- Como membro da equipe, quero colocar várias caixas registradoras num grupo de servidores, cada uma com um vendedor opcional, sem editar arquivos.
- Como membro da equipe, quero trocar o produto em promoção e o texto acima de todas as caixas da rede com um comando.

## Como funciona

### Abrindo a loja

- O `/loja` (`/shop`) abre a loja na aba **Ranks** (`shop.default-category`). Um NPC com a ação `loja` faz o mesmo (veja [NPCs e hologramas](../npcs-and-holograms/README.pt-BR.md)).
- Num servidor cujo tipo não está em `shop.server-kinds`, o `/loja` e os NPCs da loja só respondem `&cA loja não está disponível neste servidor.`
- Todo menu da loja tem 6 linhas, as abas **Ranks**, **Ultra+**, **Créditos**, **Medalhas** e **Meus pedidos** na primeira linha (a aba aberta brilha), painéis cinza na segunda linha com um painel verde-limão embaixo da aba aberta, e os produtos na quarta linha (slots 28, 30, 32 e 34). A prévia das medalhas mostra o nome do próprio jogador.

### Compra

Um clique esquerdo num produto com `&eBotão esquerdo: &fComprar` começa a compra:

1. **Compra: Método de pagamento**: Pix ou cartão de crédito, um resumo do pedido e **Voltar**.
2. **Compra: Código de apoiador**: usar um código de apoiador ou não. Usar fecha o menu e pede o código no chat (`cancelar` cancela, 60 segundos para responder). Os códigos ainda não são validados.
3. **Compra: Carregando...**: um painel verde-limão gira em volta do meio do menu por 3 segundos.
4. O menu fecha e a mensagem de pagamento é enviada, centralizada, com `&cClique aqui para fazer o pagamento via o site.` abrindo o `shop.checkout.payment-url`.

O **Voltar** leva ao menu de onde a compra veio. A página de um produto (um produto com `page` na config) mostra o produto e os métodos de pagamento direto, e depois segue os mesmos passos.

Nada é cobrado nem entregue ainda: nenhum pedido é criado, então **Meus pedidos** sempre mostra `&cNenhum pedido encontrado!`. Os papéis de **Vantagens** e os produtos **Ultra+** (que exigem Ultra) ainda não fazem nada.

### Caixas registradoras

- Uma caixa registradora é feita de 8 cabeças em suportes de armadura pequenos e invisíveis (`shop.register.parts`), dois corantes roxos que giram e sobem e descem em volta do vendedor, e as linhas da promoção em cima. Ela só existe em pacotes, como os [suportes de exibição](../../development/core-api/README.pt-BR.md#suportes-de-exibição).
- O `/loja caixa criar <id>` cria uma caixa onde você está, com o vendedor no mesmo lugar. Um grupo de servidores pode ter quantas caixas precisar; todo servidor do grupo mostra todas elas.
- O vendedor é um NPC com a skin de `shop.register.seller-skin`. O `/loja vendedor <id>` o move para onde você está, para que ele fique separado da caixa, e o `/loja vendedor <id> remover` deixa a caixa sem vendedor.
- Clicar na caixa (em qualquer cabeça, corante ou linha) ou no vendedor abre a página do produto em promoção. Se esse produto não existir mais ou não tiver página, a loja abre no lugar. Os cliques usam o anti-spam do Muxe-Core (`spam.*`).
- As caixas ficam salvas no PostgreSQL por grupo de servidores e são sincronizadas como os NPCs e os hologramas (veja [NPCs e hologramas](../npcs-and-holograms/README.pt-BR.md#como-funciona)). Uma caixa também pode ser trazida pelo `plugins/Muxe-Core/import.yml`.

### Promoção da rede

- A promoção é um id de produto e as linhas mostradas acima de todas as caixas da rede, em todos os grupos de servidores. Ela fica numa linha de `core.shop_promotion`.
- Até alguém trocá-la, vale o `shop.promotion` da config (**Enderlore**, com `LOJA`, `RANK ENDERLORE`, `13% DE DESCONTO!`, o preço e `Clique para comprar!`).
- O `/loja promocao` mostra o produto atual e as linhas numeradas. O `/loja promocao produto <id>` troca o produto (só são aceitos produtos com página própria); `adicionar`, `linha` e `removerlinha` editam as linhas. `{vazio}` cria uma linha em branco e os códigos `&` dão cor.
- O servidor onde a mudança foi feita a aplica na hora; os outros recebem o `shop.promotion_changed` e a recarregam do PostgreSQL.
- As linhas se empilham para cima: a última linha fica `shop.register.labels.bottom` (2.25625) blocos acima da caixa, e cada linha acima dela é posicionada com `shop.register.labels.gaps`, contados de baixo (0.40625, 0.28125, 0.28125, 0.375), e depois com `shop.register.labels.spacing` (0.28125) para as linhas extras. A linha de baixo fica sempre na mesma altura, então adicionar uma linha empurra as outras para cima.

## Comandos

| Comando | Descrição | Permissão |
|---|---|---|
| `/loja` (`/shop`) | Abre a loja na aba Ranks. | Nenhuma |
| `/loja caixa criar <id>` | Cria uma caixa registradora onde você está, com o vendedor no mesmo lugar. | `muxe.shop.manage` |
| `/loja caixa mover <id>` | Move uma caixa para onde você está. O vendedor fica onde está. | `muxe.shop.manage` |
| `/loja caixa remover <id>` | Remove uma caixa e o vendedor dela. | `muxe.shop.manage` |
| `/loja caixa listar` | Lista as caixas deste grupo de servidores. | `muxe.shop.manage` |
| `/loja vendedor <id>` | Move o vendedor de uma caixa para onde você está. | `muxe.shop.manage` |
| `/loja vendedor <id> remover` | Remove o vendedor de uma caixa. | `muxe.shop.manage` |
| `/loja promocao` | Mostra a promoção da rede: produto e linhas numeradas. | `muxe.shop.manage` |
| `/loja promocao produto <id>` | Troca o produto que todas as caixas abrem. | `muxe.shop.manage` |
| `/loja promocao adicionar <texto>` | Adiciona uma linha embaixo do texto das caixas; as outras linhas sobem. | `muxe.shop.manage` |
| `/loja promocao linha <número> <texto>` | Troca uma linha. | `muxe.shop.manage` |
| `/loja promocao removerlinha <número>` | Remove uma linha. | `muxe.shop.manage` |

Os ids usam letras minúsculas, números, `-` e `_`, com até 32 caracteres. Os números de linha começam em 1, no topo.

## Permissões

| Permissão | Libera | Padrão |
|---|---|---|
| `muxe.shop.manage` | Usar `/loja caixa`, `/loja vendedor` e `/loja promocao`. Substitui a `muxe.lobby.shop`. | op |

## Configuração

`plugins/Muxe-Core/config.yml`:

| Chave | Padrão | Descrição |
|---|---|---|
| `shop.server-kinds` | `LOBBY`, `MINIGAME` | Tipos de servidor que têm a loja e mostram as caixas. |
| `shop.title`, `shop.default-category` | `Loja: {name}`, `ranks` | Título dos menus e a aba que o `/loja` abre. |
| `shop.promotion.product`, `shop.promotion.lines` | `enderlore`, as 5 linhas da caixa | Promoção usada até alguém trocá-la com o `/loja promocao`. |
| `shop.panes.other`, `shop.panes.current` | Painéis cinza e verde-limão | Segunda linha dos menus da loja. |
| `shop.categories.<id>` | Ranks, Ultra+, Créditos, Medalhas, Meus pedidos | `name`, `tab-slot`, `tab` (item), `products` e `extras` (itens que não fazem nada, como os papéis de **Vantagens**). |
| `shop.categories.<id>.products.<id>` | Veja a config | `slot`, `price` (mostrado na compra), `purchasable` (padrão `true`), `item`, e `page` (`name`, `item`) para um produto com página própria. `{player}` na lore vira o nome do jogador. Só produtos com `page` podem entrar em promoção. |
| `shop.checkout.*` | Títulos e itens de referência | `method-title`, `supporter-title`, `loading-title`, `pix`, `card`, `summary`, `back`, `use-code`, `skip-code` (placeholders `{product}`, `{price}`, `{menu}`), `code-timeout` (`60s`), `payment-url`, `chat-center` (`114`). |
| `shop.checkout.loading.*` | `150ms`, `3s`, painéis verde-limão e cinza | De quanto em quanto tempo o painel verde anda, quanto tempo o carregamento dura e os dois painéis. |
| `shop.register.labels.bottom` | `2.25625` | Altura da última linha da promoção acima da caixa. |
| `shop.register.labels.gaps` | `0.40625`, `0.28125`, `0.28125`, `0.375` | Espaço entre cada linha e a de baixo, contado de baixo para cima. |
| `shop.register.labels.spacing` | `0.28125` | Espaço das linhas além dos `gaps`. |
| `shop.register.seller-skin` | Uma skin fixa | `value` e `signature` do vendedor NPC. |
| `shop.register.parts` | 8 cabeças e 2 corantes | Suportes de exibição: `x`, `y`, `z` (deslocamento), `yaw`, `small`, `arms`, `helmet`, `hand`, `head-pose`, `right-arm-pose`, `motion` (`spin` em graus por tick, `bob-height`, `bob-period` em ticks, `phase`). |
| `spam.max-clicks`, `spam.window`, `spam.cooldown` | `3`, `1s`, `3s` | Anti-spam compartilhado pelos cliques nas caixas, nos vendedores, nos NPCs e na hotbar. |
| `messages.shop-*`, `messages.placement-failed`, `messages.spam-cooldown` | Textos em português | Mensagens da loja, das caixas e da promoção. |

`shop.featured-product`, `shop.register.location`, `shop.register.labels.line-N` e `shop.register.seller.enabled` não existem mais: as caixas são colocadas por comando e a promoção substitui o produto em destaque. Os itens usam o [formato do `ItemBuilder`](../../development/core-api/README.pt-BR.md#itens).

## Pacotes

| Id | Destino | Política | Propósito |
|---|---|---|---|
| `shop.promotion_changed` | broadcast | padrão | A promoção mudou; todo servidor a recarrega do PostgreSQL. Leva o id do produto. |
| `placement.changed` | o grupo de servidores | padrão | Uma caixa foi criada, movida ou removida; todo servidor do grupo recarrega o que foi colocado. |

## Dados

| Tabela | Conteúdo |
|---|---|
| `core.shop_promotion` | Uma única linha (`id = 1`): `product_id`, `lines` (array de texto), `changed_at`. Não existe até a primeira mudança. |
| `core.register_placements` | `server_group`, `id`, a posição da caixa (`world`, `x`, `y`, `z`, `yaw`, `pitch`) e a posição opcional do vendedor (`seller_*`, todas nulas sem vendedor). Chave primária `(server_group, id)`. |

Comandos e consultas: `ChangePromotionCommand`, `FindPromotionQuery`, e os de posicionamento (`SavePlacementCommand`, `RemovePlacementCommand`, `FindPlacementsQuery`).

## Métricas

`muxe_db_transaction_seconds{operation="shop.change_promotion"|"shop.find_promotion"|"placement.save"|"placement.remove"|"placement.find"}`, `muxe_cqrs_handle_seconds` para os comandos e consultas acima, e as métricas de pacote de `shop.promotion_changed` e `placement.changed`.

## Integrações

- A [API do core](../../development/core-api/README.pt-BR.md) fornece os menus, os itens, os suportes de exibição, os NPCs, as perguntas no chat e as linhas centralizadas no chat.
- [NPCs e hologramas](../npcs-and-holograms/README.pt-BR.md) compartilham o armazenamento e a sincronização, e os NPCs com a ação `loja` abrem a loja.
- O [Lobby](../lobby/README.pt-BR.md) e os servidores de minigame recebem a loja do Muxe-Core; eles não a configuram.

## Critérios de aceitação

- [x] Os menus da loja seguem o layout de referência: abas, painéis, produtos, a página do produto e os três menus de compra, com o **Voltar** levando ao menu de onde a compra veio.
- [x] O painel do carregamento anda em volta do slot do meio, e a mensagem de pagamento fica centralizada.
- [x] A promoção da config vale até alguém trocá-la; a mudança chega a todos os servidores da rede, e um servidor que liga depois carrega a promoção salva.
- [x] As linhas da caixa se empilham para cima a partir da linha de baixo, usando os `gaps` de baixo para cima e depois o `spacing`.
- [x] Caixas e vendedores sobrevivem a ir e voltar do PostgreSQL, e as caixas de um grupo nunca aparecem em outro.
- [x] As caixas listadas no `import.yml` são importadas para o grupo do servidor.
- [ ] Criar, mover e remover caixas e vendedores, os comandos da promoção, os cliques nas caixas e a compra inteira funcionam no jogo num lobby e num servidor de minigame (conferido no jogo).

## Falhas e recuperação

- PostgreSQL indisponível quando alguém da equipe muda uma caixa ou a promoção: aparece `&cNão foi possível salvar agora. Tente novamente em instantes.` e nada muda.
- PostgreSQL indisponível ao ligar: nenhuma caixa aparece e vale a promoção da config, com um aviso no log. A próxima mudança no grupo, ou um reinício, as carrega de novo.
- Redis indisponível: a mudança é salva e aparece no servidor onde foi feita; os outros servidores a mostram no próximo recarregamento (outra mudança ou um reinício).
- Caixa ou vendedor num mundo que não está carregado: é pulado com um aviso.
- Produto em promoção removido da config: clicar numa caixa abre a loja na aba padrão.
- `plugins/Muxe-Core/config.yml` sem a seção `shop` (uma config escrita por uma versão antiga): a loja fica desligada naquele servidor, com um aviso no log. Copie a seção `shop` da config que vem no plugin para ligá-la.
- Jogador que sai durante a pergunta do código de apoiador: a pergunta é descartada; nada foi pedido.

## Limitações conhecidas

- A loja ainda não vende nada: não há pedidos, pagamentos, validação de código de apoiador nem ranks, e os papéis de **Vantagens** e os produtos **Ultra+** não fazem nada.
- Existe uma só promoção para a rede inteira; as caixas não podem mostrar produtos diferentes.
- A caixa guarda as peças nos eixos do mundo, então movê-la não a gira.
- O vendedor olha sempre para a mesma direção: a que você olhava quando ele foi colocado.
- O produto da promoção só é validado no servidor onde o comando rodou; um servidor com outro catálogo abre a loja no lugar.
