[English](README.md) | **Português**

# Editor de permissões

Um site servido pelo proxy onde a equipe edita cargos (grupos), sua hierarquia e permissões, tags, trilhas e as permissões de cada jogador, e depois salva tudo de uma vez. Fica no pacote `br.com.muxe.core.proxy.editor` do `core:proxy` e trabalha sobre o modelo de permissões do `core:common` (`br.com.muxe.core.common.permission`).

## Histórias de usuário

- Como administrador, quero abrir um link privado a partir do jogo e editar cargos, tags e permissões no navegador, para não digitar comandos longos.
- Como administrador, quero criar um cargo que herda outro ou que começa como cópia de outro, para montar a hierarquia rapidamente.
- Como administrador, quero dar a um jogador um cargo temporário ou uma tag decorativa, para que recompensas expirem sozinhas.
- Como administrador, quero revisar cada alteração pendente antes de salvar e ver quem mudou o quê, para encontrar erros com facilidade.

## Como funciona

### Abrindo o editor

1. Um administrador usa `/editor` em um servidor de jogo (lado Bukkit). O comando abre uma sessão do editor (`EditorSessions.open`) e envia o link `<editor.public-url>/#<token>`.
2. O token vai no **fragmento** da URL, então o navegador nunca o envia ao servidor na linha da requisição, em logs ou no `Referer`. A página o lê, guarda só na memória e no `sessionStorage` daquela aba e o tira da barra de endereço.
3. Toda chamada da API envia `Authorization: Bearer <token>`. A sessão dura 30 minutos (`EditorSessions.DEFAULT_DURATION`); a barra do topo faz a contagem regressiva e a página se fecha quando ela acaba.
4. Em toda chamada o proxy também carrega as permissões do dono da sessão e confere se elas ainda resolvem `muxe.permissions.editor`. Quem perdeu a permissão é recusado e a sessão é encerrada.

### O que pode ser editado

- **Cargos (grupos):** lista ordenada por peso; criar (vazio, como cópia das permissões de um cargo existente ou herdando um cargo existente); mudar nome de exibição, peso e tag; duplicar; excluir (o cargo padrão `membro` não pode ser excluído; excluir um cargo também o tira dos cargos que o herdam e das trilhas). O id é fixo depois de criado.
- **Herança:** os pais são as permissões `group.<id>` do cargo. O editor os adiciona e remove, mostra a cadeia indireta e não oferece cargos que criariam um ciclo.
- **Permissões** de cargos e jogadores: uma tabela com busca e filtros (true, false, temporárias, com contexto); adicionar uma, adicionar várias de uma vez (uma por linha: `-chave` para false, contextos `chave=valor` e uma duração como `30d`), alternar true/false, editar e remover. Cada permissão pode ter contextos (`server`, `world` ou qualquer `chave=valor`) e expiração (uma duração como `12h`, `30d`, `1mo`, ou uma data).
- **Tags:** criar, editar e excluir; id, nome, cor escolhida entre os 16 códigos `&`, peso, prefixo de chat opcional (por padrão cor + negrito + NOME), marcação de decorativa. Prévias ao vivo da linha do chat (`&a&lVIP &atadeu: Olá!`), do `/tag` (`&aVIP`) e do nome acima da cabeça.
- **Trilhas:** listas ordenadas de cargos usadas para promover e rebaixar; reordenar, adicionar e remover cargos.
- **Jogadores:** busca por nome ou UUID; ver a conta, dar cargos (permanentes ou temporários, com contextos opcionais), promover ou rebaixar em cada trilha, dar ou tirar tags decorativas (`muxe.tag.<id>`, opcionalmente temporárias), escolher a tag selecionada e editar as demais permissões.
- **Histórico:** as últimas 300 alterações salvas com o autor, com filtro.

### Salvando

- Nada é gravado durante a edição. Um painel flutuante lista cada alteração pendente (nova, editada, excluída), cada uma com **Desfazer**; **Salvar** envia todas em uma requisição.
- O proxy transforma a requisição em `PermissionChange`s e as aplica de forma atômica com `PermissionSync.apply(ator, alterações)`: as regras rodam sobre o resultado (cargo padrão presente, cargos e tags conhecidos, sem ciclo de herança), as alterações são gravadas em uma transação com uma linha no registro de ações para cada uma, `permission.changed` é publicado para a rede e todos os servidores recarregam.
- Se uma regra falha, nada é salvo e a página mostra o problema; as alterações pendentes continuam lá para serem corrigidas.
- O log do proxy recebe uma linha por lote salvo, com o ator e um resumo das alterações.

## API

Todos os endpoints ficam em `/api`, respondem JSON com `Cache-Control: no-store` e exigem `Authorization: Bearer <token>`. Erros são `{"error":{"code":"...","message":"..."}}`.

| Método e caminho | Resposta |
|---|---|
| `GET /api/session` | `actor`, `actorId`, `createdAt`, `expiresAt`, `now` (milissegundos epoch). |
| `GET /api/data` | `defaultGroup`, `groups` (por peso, com `nodes`), `tags` (com `display` e `chatPrefix`), `tracks`. |
| `GET /api/users/search?name=` | O jogador encontrado pelo nome (sem diferenciar maiúsculas) ou UUID: campos da conta, `nodes` e `selectedTag`. `404 editor.user-not-found`. |
| `GET /api/users/{uuid}` | O mesmo, pelo id. |
| `GET /api/groups/{id}/members` | `total` e até 200 `members` (`id`, `username`) com aquele cargo direto. |
| `GET /api/actions?limit=` | As últimas alterações salvas (`at`, `actor`, `description`), de 1 a 500, 100 por padrão. |
| `POST /api/changes` | Corpo `{"changes":[...]}` com até 500 alterações dos tipos `put-group`, `delete-group`, `put-tag`, `delete-tag`, `put-track`, `delete-track`, `put-user`. Responde `saved` e o novo `data`, ou `400` com o código do problema da regra. |
| `POST /api/logout` | Encerra a sessão. |

Uma permissão é `{"key","value","contexts":{...},"expiresAt"}` com `addedAt`/`updatedAt` opcionais (milissegundos; valores no futuro viram agora). Um cargo é `{"id","displayName","weight","tag","nodes"}`, uma tag `{"id","name","color","weight","prefix","decorative"}`, uma trilha `{"id","groups"}` e um jogador `{"id","nodes","selectedTag"}`.

Códigos de status: `400 editor.invalid-request` e os códigos de problema `permission.*`, `401 editor.unauthenticated` / `editor.session-expired`, `403 editor.forbidden` / `editor.bad-origin`, `404`, `405`, `413 editor.too-large`, `415`, `429 editor.too-many-requests`, `503 editor.not-ready` / `editor.unavailable`.

## Segurança

- **Acesso:** só um token de sessão válido e não expirado cujo dono ainda tenha `muxe.permissions.editor`. Tokens têm 32 bytes aleatórios; o Redis guarda só o SHA-256 deles, então a busca não depende dos caracteres do token. Não há cookies, então requisições de outros sites não aproveitam um login.
- **Limites por endereço:** `editor.requests-per-minute` requisições ao site e à API; depois de `editor.failed-logins-per-ten-minutes` tokens recusados o endereço fica bloqueado por 10 minutos.
- **Requisições:** corpos acima de `editor.max-body-bytes` são recusados (`413`); `POST` exige `Content-Type: application/json` e, quando o navegador envia `Origin`, ele precisa ser o endereço do próprio editor. Todo campo é validado de novo no servidor: ids, chaves de permissão, contextos, cores, tamanhos, números e limites (500 alterações, 5.000 permissões por cargo ou jogador, 8 contextos por permissão).
- **Cabeçalhos:** `Content-Security-Policy` com `default-src 'self'` e sem scripts ou estilos inline, `X-Frame-Options: DENY`, `frame-ancestors 'none'`, `Referrer-Policy: no-referrer`, `X-Content-Type-Options: nosniff`, cabeçalhos de isolamento entre origens, nenhum cabeçalho CORS. Só os arquivos conhecidos do site são servidos.
- **Exibição:** a página monta cada elemento com APIs do DOM e `textContent`; nada vindo do servidor é inserido como HTML.
- **Threads:** o servidor HTTP roda em threads virtuais próprias, nunca nas threads de eventos do Velocity, e para junto com o proxy.
- **HTTPS:** o editor fala HTTP simples na porta 1031. Enquanto a rede não tiver domínio, o token e as alterações atravessam a internet sem criptografia; quando houver domínio, coloque um proxy reverso com TLS (por exemplo Caddy ou nginx) na frente, defina `editor.bind` como `127.0.0.1` e `editor.public-url` com o endereço `https://`. O limite de requisições usa o endereço da conexão, então atrás de um proxy reverso ele vale para o proxy inteiro até que endereços encaminhados sejam suportados.

## Comandos

| Comando | Descrição | Permissão |
|---|---|---|
| `/editor` | Abre uma sessão do editor e envia o link (servidores de jogo). | `muxe.permissions.editor` |

## Permissões

| Permissão | Concede |
|---|---|
| `muxe.permissions.editor` | Abrir e usar o editor. O cargo `admin` a tem por meio de `*`. |

## Configuração

`plugins/muxe-proxy/config.yml` do proxy:

| Chave | Padrão | Descrição |
|---|---|---|
| `editor.enabled` | `true` | Liga o servidor HTTP do editor. |
| `editor.bind` | `0.0.0.0` | Endereço em que escuta. Use `127.0.0.1` atrás de um proxy reverso. |
| `editor.port` | `1031` | Porta em que escuta (alocada ao proxy no painel). |
| `editor.public-url` | `http://151.244.40.191:1031` | Endereço que os jogadores abrem; o começo do link do `/editor` e o `Origin` aceito. |
| `editor.requests-per-minute` | `240` | Requisições por endereço por minuto. |
| `editor.failed-logins-per-ten-minutes` | `20` | Tokens recusados por endereço antes de bloqueá-lo por 10 minutos. |
| `editor.max-body-bytes` | `1048576` | Maior corpo de requisição. |

Se a porta não puder ser aberta, o proxy registra isso no log e continua funcionando sem o editor.

## Pacotes

| Id | Destino | Política | Para quê |
|---|---|---|---|
| `permission.changed` | broadcast | standard | Enviado depois de cada salvamento para todo servidor recarregar cargos, tags e os jogadores afetados. |

## Dados

Usa as tabelas de permissões e o registro de ações do `core:common`. As sessões do editor ficam no Redis em `muxe:permissions:editor:<sha256 do token>` por 30 minutos.

## Métricas

Nenhuma.

## Integrações

- Permissões do `core:common`: `PermissionSync`, `PermissionResolver`, `PermissionRules`, `EditorSessions`, `FindAccountDetailsQuery`, `FindPermissionUserQuery`, `FindUsersWithNodeQuery`, `RecentPermissionActionsQuery`.
- O comando `/editor` nos servidores de jogo cria as sessões.

## Critérios de aceitação

- [ ] Abrir o link do `/editor` mostra os cargos; abrir o site sem token ou com um link antigo explica como conseguir um novo.
- [ ] Um cargo criado como cópia tem as mesmas permissões do original; um cargo criado por herança tem só `group.<original>`.
- [ ] Adicionar um pai que criaria um ciclo não é oferecido, e uma requisição forjada que faça isso é recusada com `permission.inheritance-cycle`.
- [ ] Dar a um jogador um cargo por 30 dias grava uma permissão `group.<id>` que expira em 30 dias.
- [ ] Mudar a cor de uma tag atualiza na hora as prévias do chat e do `/tag`.
- [ ] Depois de **Salvar**, a alteração aparece no histórico com o autor, e jogadores em outros servidores a recebem sem relogar.
- [ ] Um jogador que perde `muxe.permissions.editor` é recusado na requisição seguinte.

## Falhas e recuperação

- **Banco fora do ar ao salvar:** a transação é desfeita, a página mostra o erro e mantém as alterações pendentes.
- **Redis fora do ar:** as sessões não podem ser lidas; a página mostra a sessão como inválida. Os dados salvos não são afetados.
- **Cargos ainda não carregados:** a API responde `503 editor.not-ready` em vez de recusar a sessão.
- **Sessão expirada durante a edição:** a página mostra "Sessão expirada"; alterações não salvas se perdem, então edições longas devem ser salvas em partes.
- **Dois editores ao mesmo tempo:** o último salvamento vence para cada cargo, tag, trilha ou jogador que ele envia.

## Limitações conhecidas

- Sem HTTPS até existir domínio e proxy reverso (veja Segurança).
- O id de um cargo não pode ser renomeado; crie uma cópia e exclua o antigo.
- Membros são listados só para membros diretos, até 200.
- A prévia das tags aproxima a fonte e as cores do jogo.
