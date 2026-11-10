[English](README.md) | **Português**

# Idioma

Cada jogador lê a rede no seu próprio idioma: português (o padrão) ou inglês. A escolha fica salva na conta e acompanha o jogador em todos os servidores e no proxy.

## Histórias de usuário

- Como jogador que não fala português, quero mudar a rede para inglês, para entender as mensagens, os menus e os placares.
- Como jogador, quero que a minha escolha seja lembrada, para não ter que escolher de novo em cada servidor ou login.
- Como desenvolvedor, quero um único jeito de escrever um texto traduzível, para que as features novas já saiam traduzidas sem trabalho extra.

## Como funciona

- Todo jogador novo começa em português. O `/idioma <português|inglês>` troca o idioma; o comando também responde como `/language`, `/lang` e `/linguagem`.
  - Ele aceita nomes e siglas com ou sem acento: `português`, `portugues`, `portuguese`, `pt`, `pt-BR`, `inglês`, `ingles`, `english`, `en`.
  - Sem argumento, abre o menu de idiomas:
    - O menu tem 3 linhas e um livro por idioma: `Português` no slot 10 e `English` no slot 11.
    - Cada livro é escrito no próprio idioma, para que quem escolheu o idioma errado ainda encontre o seu. Só o título segue quem lê: `Selecionar idioma` ou `Select language`.
    - O idioma atual brilha. Clicar num livro fecha o menu e troca para aquele idioma; clicar no atual responde `language-already`.
  - Com um idioma desconhecido depois do comando, mostra o uso e o idioma atual.
- A escolha fica salva em `core.player_languages`.
  - Cada servidor Bukkit carrega o idioma antes do login, junto com as permissões, e o mantém na memória enquanto o jogador está online. Assim, mostrar uma mensagem nunca consulta o banco.
  - O proxy carrega o idioma no login e é avisado das trocas com o pacote `language.changed`.
- Depois da troca, o que o jogador já está vendo é atualizado no novo idioma: os itens da hotbar, a sidebar e os outros textos por jogador. Um menu aberto é fechado.
- Um texto sem tradução em inglês aparece em português, chave por chave. Uma tradução que falta nunca deixa uma linha vazia ou quebrada.
- Os textos que só a staff vê continuam em português: setup de arena, edição de cargos e permissões, comandos de posicionamento e de config.
- O MOTD não tem um jogador por trás, então continua em português. Os kicks enviados antes de a conta do jogador ser conhecida (antes do login) também são em português.

### Onde ficam os textos

- O português continua onde sempre esteve: no `config.yml` do plugin.
- O inglês fica em `lang/en.yml`, ao lado dele.
  - O arquivo tem os mesmos caminhos do `config.yml`.
  - Ele guarda só os textos: mensagens, nomes de itens, lore, títulos, linhas de placar e títulos de menu. Materiais, slots, números e sons nunca são repetidos nele.
- O arquivo em inglês é criado na pasta do plugin na primeira inicialização. As chaves que faltam na cópia do servidor vêm do jar, como acontece com o `config.yml`.

## Comandos

| Comando | Descrição | Permissão |
|---|---|---|
| `/idioma [idioma]` | Troca o idioma do jogador; sem argumento, abre o menu de idiomas. Também responde como `/language`, `/lang` e `/linguagem`. | Nenhuma. |

## Permissões

Nenhuma.

## Configuração

| Chave | Padrão | Descrição |
|---|---|---|
| `messages.language-usage` | `&eUso: /idioma <português\|inglês>...` | Uso e idioma atual. |
| `messages.language-changed` | `&aIdioma alterado para &f{language}&a.` | Enviada no novo idioma depois da troca. |
| `messages.language-already` | `&cVocê já está usando o idioma &f{language}&c.` | O jogador escolheu o idioma que já usa. |
| `messages.language-failed` | `&cNão foi possível alterar o idioma agora...` | A escolha não pôde ser salva. |
| `messages.language-name-pt-br`, `messages.language-name-en` | `Português`, `Inglês` | Nomes dos idiomas mostrados nas mensagens acima. |
| `language-menu.title`, `language-menu.rows` | `Selecionar idioma`, `3` | Título e altura do menu de idiomas. |
| `language-menu.languages.<sigla>.slot`, `.item` | `pt-br` no slot 10, `en` no slot 11, os dois livros | Slot e [item](../../development/core-api/README.pt-BR.md#itens) de cada idioma. A sigla é `pt-br` ou `en`; um slot fora do menu ou uma sigla desconhecida falha na inicialização. |
| `lang/en.yml` | os textos em inglês | Versão em inglês de cada texto legível do `config.yml` do plugin, nos mesmos caminhos. |

## Pacotes

| Id | Destino | Política | Para quê |
|---|---|---|---|
| `language.changed` | proxy | padrão, idade máxima de 30s | Avisa o proxy que um jogador trocou de idioma, para que os textos do proxy acompanhem. |

## Dados

`core.player_languages` (migração `V20`):

| Coluna | Tipo | Descrição |
|---|---|---|
| `player_id` | `UUID` | Chave primária. |
| `language` | `VARCHAR(8)` | A sigla do idioma: `pt-BR` ou `en`. |
| `updated_at` | `TIMESTAMPTZ` | Quando o jogador trocou pela última vez. |

Um jogador sem linha na tabela fala português.

## Métricas

Nenhuma. Os handlers do comando e da consulta são medidos pelas métricas de CQRS, como todos os outros.

## Integrações

- Todos os plugins Bukkit: o `MessageCatalog` e o `LocalizedConfig` mostram cada texto no idioma de quem lê. Veja a [API do core](../../development/core-api/README.pt-BR.md#idiomas).
- Proxy: os kicks depois do login, as respostas no chat e o tab seguem o idioma do jogador.
- [Party](../party/README.pt-BR.md): os avisos são enviados como chaves e montados no servidor de destino, no idioma de cada membro.

## Critérios de aceite

- [ ] Um jogador novo vê todas as mensagens em português.
- [ ] O `/idioma inglês` salva a escolha e responde em inglês.
- [ ] Depois da troca, os itens da hotbar e a sidebar ficam no novo idioma sem relogar.
- [ ] Ao ir para outro servidor, o jogador continua lendo em inglês.
- [ ] Um anúncio da partida chega a cada jogador no idioma dele.
- [ ] Um texto que falta no `lang/en.yml` aparece em português.
- [ ] O `/idioma` com um idioma desconhecido mostra o uso.
- [x] O `/idioma` abre o menu com o idioma atual brilhando; o título segue quem lê e cada livro fica no próprio idioma.
- [ ] Clicar num livro do menu troca o idioma (conferido no jogo).

## Falhas e recuperação

- **O idioma não pôde ser carregado antes do login (banco lento ou fora do ar):** o jogador entra e lê em português. A próxima troca de servidor carrega o idioma de novo.
- **A escolha não pôde ser salva:** o jogador vê a mensagem `language-failed` e nada muda.
- **O proxy perde o `language.changed`:** ele carrega o idioma de novo no próximo login.

## Limitações conhecidas

- Os hologramas e as linhas de NPC escritos pela staff no banco aparecem como foram escritos, em um idioma só.
- O MOTD e os kicks antes do login são só em português.
