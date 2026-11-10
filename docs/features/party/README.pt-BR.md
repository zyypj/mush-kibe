[English](README.md) | **Português**

# Party

Os jogadores se juntam em uma party que vale para a rede inteira: convidam uns aos outros, falam no chat da party, entram juntos nas partidas e ficam no mesmo time. O proxy é o dono das parties; cada servidor Bukkit guarda uma cópia e executa os comandos.

## Histórias de usuário

- Como jogador, quero convidar meus amigos para uma party de qualquer servidor, para jogarmos juntos.
- Como jogador, quero um chat da party, e poder deixá-lo como meu canal padrão, para que só a minha party me leia.
- Como líder da party, quero procurar sala uma vez e a party inteira me seguir para uma sala com vagas suficientes.
- Como líder da party, quero puxar minha party para onde estou, um lobby ou a minha partida.
- Como jogador Ultra+ ou superior, quero abrir minha party para qualquer um entrar.
- Como jogador, quero ficar no time da minha party e saber quando uma partida não vai contar estatísticas porque a party precisou ser dividida.

## Como funciona

### A party

- `/party <jogadores>` convida um ou mais jogadores (nomes separados por espaço). O primeiro convite cria a party com quem convidou como líder. Só o líder convida.
- A party (`[PARTY]` em roxo claro) recebe `<nick> recebeu um convite para a party.`; o convidado recebe duas linhas, `<nick> convidou você para uma party` e `CLIQUE AQUI para aceitar o convite.`, e a segunda linha executa `/party aceitar <líder>` ao ser clicada.
- Os nomes que não estão online na rede aparecem juntos em uma linha vermelha depois dos convites: `Jogadores: <nomes> não foi encontrado ou está com o convite de party desativado.`
- Um convite dura 60 segundos (`party.invite-duration`). Quando expira, o líder e o convidado são avisados.
- Não há limite de membros.
- Todo nick nas mensagens da party tem a cor da tag do jogador, informada pelo servidor onde ele está.
- A party acaba quando o líder usa `/party acabar`, quando o último membro sai, e também quando sobra só o líder, com a party fechada e sem convites pendentes.

### Saindo e ficando offline

- `/party sair` sai da party. Quando o líder sai, o membro online mais antigo vira líder (`<nick> é agora líder da party.`).
- Um membro que sai da rede continua na party, em vermelho no `/party membros`, por 3 minutos (`party.offline-grace`). Os membros online recebem `<nick> desconectou-se e tem 3 minutos para voltar.` (em vermelho, com os minutos tirados de `party.offline-grace`) e, quando o membro volta a tempo, `<nick> reconectou.` (em amarelo). Depois dos 3 minutos o membro é removido (`<nick> foi removido da party por ficar offline.`).
- Quando o líder sai da rede, o membro online mais antigo vira líder na hora. Se ninguém está online, a liderança espera; a regra dos 3 minutos acaba removendo todos.
- `/party removeroffline` remove na hora todos os membros offline.

### Parties abertas

- `/party abrir` abre a party, ou fecha quando já está aberta. Só jogadores com `muxe.party.open` (o rank Ultra+ e todos acima dele) podem usar. Abrir sem party cria uma.
- Com a party aberta, qualquer um entra com `/party entrar <nick do líder>`, que completa os líderes das parties abertas.

### Chat

- `/pc <mensagem>` envia aos membros online, em todos os servidores: `[PARTY]` e depois a linha normal do chat (medalha, tag, nick e mensagem), como na referência.
- `/canal <canal>` troca o canal padrão: `default` (ou `padrao`/`padrão`), `party` ou `clan`. No canal `party`, toda mensagem do chat vai para o chat da party. Quando o jogador não está mais em uma party, o chat volta ao canal normal. `clan` responde `Você não está em um clan.` até existirem clãs. A escolha fica guardada por 7 dias no `MuxeCore.store()` (`chat:channel:<uuid>`).

### Procura de salas

- Só o líder procura sala. Um membro que clica em um modo, um mapa ou em "jogar novamente" recebe `Apenas o líder da party consegue procurar salas!`.
- Quando o líder entra, os membros online vão junto, estejam onde estiverem (um membro em outra partida sai dela e pode voltar com `/retornar` ou `/reconectar`). A sala precisa ter uma vaga livre para cada um: salas com menos vagas são puladas e uma nova sala é criada quando nenhuma serve. Um modo que não comporta a party inteira (por exemplo 9 jogadores no solo, de 8 vagas) é recusado com `Sua party tem jogadores demais para este modo.`
- O servidor do jogo reserva uma vaga para cada membro na mesma sala (a versão 2 do `<jogo>.join_request` leva os acompanhantes).
- Trocar de lobby (`/jogar`, o seletor de jogos, `/lobby`) não move a party; o `/party teleportar` move.

### Teleporte

`/party teleportar` (ou `/party warp`) puxa os membros online para o servidor onde o líder está:

- **Líder em um lobby:** os membros vão para aquela instância do lobby. Quem está em uma partida sai dela e pode voltar com `/retornar`/`/reconectar`.
- **Líder em uma partida que não começou:** cada membro ganha uma vaga naquela sala enquanto houver lugar; com a sala cheia, o membro fica onde está.
- **Líder em uma partida começada:** os membros entram como espectadores. Eles veem a partida, o chat dela e a hotbar de espectador, mas nunca jogam nem contam no resultado.

### Times

- No início, os membros de uma party na sala ficam juntos em um time (`TeamAssigner` os agrupa antes de todos os outros).
- Quando a party é maior que um time, ou o modo tem times de um jogador, parte dela fica em um time inimigo. A partida então diz `AVISO Esta partida não conta estatísticas.` (`AVISO` em vermelho escuro e negrito) a todos, e não registra estatísticas dos jogadores daquela party. O BedWars os deixa de fora dos resultados salvos e do XP da partida.

## Comandos

| Comando | Descrição | Permissão |
|---|---|---|
| `/party` | Mostra a lista de uso. | Nenhuma |
| `/party <jogadores>` | Convida jogadores. | Nenhuma (líder) |
| `/party aceitar <jogador>` | Aceita um convite. | Nenhuma |
| `/party sair` | Sai da party. | Nenhuma |
| `/party acabar` | Acaba com a party. | Nenhuma (líder) |
| `/party expulsar <jogador>` | Remove um membro. | Nenhuma (líder) |
| `/party transferir <jogador>` | Passa a liderança a um membro online. | Nenhuma (líder) |
| `/party membros` | Lista os membros: online em verde, offline em vermelho. | Nenhuma |
| `/party teleportar` (`warp`) | Puxa os membros para o servidor do líder. | Nenhuma (líder) |
| `/party entrar <nome>` | Entra em uma party aberta pelo nick do líder. | Nenhuma |
| `/party removeroffline` | Remove os membros offline. | Nenhuma (líder) |
| `/party abrir` | Abre ou fecha a party. | `muxe.party.open` |
| `/pc <mensagem>` | Fala no chat da party. | Nenhuma |
| `/canal <canal>` | Troca o canal padrão do chat. | Nenhuma |

O tab completa os subcomandos, os jogadores da rede (cada nome do `/party <jogadores>`), os membros da party (`expulsar`, `transferir`), os líderes das parties abertas (`entrar`) e os canais. Os nomes vêm do proxy a cada 3 segundos (`network.players`). Os comandos não existem nos servidores de autenticação.

## Permissões

| Permissão | Concede |
|---|---|
| `muxe.party.open` | `/party abrir`. Semeada no rank `ultra+`, herdada por `partner` e todos os ranks acima. |

## Configuração

`config.yml` do proxy:

| Chave | Padrão | Descrição |
|---|---|---|
| `party.invite-duration` | `60s` | Quanto tempo dura um convite. |
| `party.offline-grace` | `3m` | Quanto tempo um membro offline fica na party. |
| `party.gather-timeout` | `3s` | Quanto o proxy espera o servidor do líder responder um teleporte. |
| `party.players-period` | `3s` | A cada quanto tempo o proxy envia os nomes online para o tab. |

`config.yml` do Muxe-Core, em `messages`: `party-usage` (a lista de uso), `party-invite-sent`, `party-invite` (duas linhas, a última clicável), `party-disbanded`, `party-joined`, `party-kicked`, `party-new-leader`, `party-members`, `party-members-online`, `party-members-offline`, `party-members-separator`, `party-chat`, `party-chat-usage`, `party-left`, `party-you-left`, `party-opened`, `party-closed`, `party-offline-removed`, `party-offline-kicked`, `party-member-disconnected` (com `{minutes}`), `party-member-reconnected`, `party-summoned`, `party-invite-expired-sender`, `party-invite-expired`, as recusas (`party-player-not-found` com `{names}`, `party-not-in-party`, `party-already-in-party`, `party-not-leader`, `party-target-in-party`, `party-already-member`, `party-already-invited`, `party-no-invite`, `party-not-member`, `party-member-offline`, `party-invite-self`, `party-kick-self`, `party-already-leader`, `party-not-open`, `party-open-no-permission`, `party-no-offline`), os textos da procura de salas (`party-leader-only`, `party-too-big`, `party-split`) e os do canal (`channel-usage`, `channel-changed`, `channel-name-default`, `channel-name-party`, `channel-no-clan`).

## Pacotes

| Id | Destino | Política | Propósito |
|---|---|---|---|
| `party.action` | proxy | standard, idade máxima 10 s | Um jogador usou um comando da party; leva a ação, os argumentos e, no `abrir`, se o jogador pode abrir. |
| `party.notice` | servidor | standard, idade máxima 10 s | Mensagens para os jogadores daquele servidor: uma chave com placeholders e um comando de clique opcional, ou uma linha pronta (chat da party). |
| `party.updated` | broadcast | standard, idade máxima 30 s | Os membros, o líder e se está aberta, de uma party que mudou. |
| `party.removed` | broadcast | standard, idade máxima 30 s | Uma party acabou. |
| `party.sync_request` / `party.sync_response` | proxy / quem pediu | standard, idade máxima 5 s | Um servidor que inicia carrega todas as parties. |
| `party.gather_request` / `party.gather_response` | servidor do líder / proxy | standard, idade máxima 5 s | `/party teleportar`: o servidor do líder diz quem pode vir (e reserva as vagas); o proxy os conecta. |
| `player.display` | proxy | standard, idade máxima 30 s | A cor do nick de um jogador (a tag), enviada ao entrar e quando o rank muda. |
| `network.players` | broadcast | volátil, 10 s | Os nomes dos jogadores autenticados, para o tab. |

## Dados

- As parties vivem na memória do proxy (`PartyBook` no `core:common`); os servidores guardam uma cópia (`PartyDirectory`). Nada é gravado no PostgreSQL.
- `chat:channel:<uuid>` no `MuxeCore.store()`: o canal de chat escolhido, por 7 dias.
- A migração `V18__create_ultra_plus.sql` cria o rank `ultra+` e sua permissão.

## Métricas

Nenhuma além das métricas dos pacotes acima.

## Integrações

- [API de minigames](../../architecture/minigame-api/README.pt-BR.md): procura de salas só pelo líder, salas com vagas para a party inteira, agrupamento nos times, espectadores e o aviso de estatísticas.
- [BedWars](../bedwars/README.pt-BR.md): deixa os jogadores de uma party dividida fora das estatísticas.
- [Ranks e tags](../ranks-and-tags/README.pt-BR.md): as cores dos nicks, a linha do chat e o rank Ultra+.
- [Roteamento da rede](../network-routing/README.pt-BR.md): o proxy move os membros; `/retornar` e `/reconectar` os levam de volta a uma partida.

## Critérios de aceite

- [x] Os convites chegam a jogadores em outros servidores, com a linha clicável de aceitar, e expiram em 60 segundos.
- [x] Só o líder convida, expulsa, transfere, teleporta e remove os offline.
- [x] Desconexões e voltas são avisadas, e membros offline saem depois de 3 minutos; a liderança passa adiante quando o líder sai.
- [x] `/party abrir` exige Ultra+ e `/party entrar` só entra em parties abertas.
- [x] `/pc` e o canal `party` chegam a todos os membros online; sem party o chat fica normal.
- [x] Só o líder procura sala, e a party só vai para uma sala com vaga para todos.
- [x] Uma party fica em um time; quando é dividida, a partida avisa e seus jogadores não contam estatísticas.
- [x] `/party teleportar` reserva vagas em uma sala esperando e deixa os membros espectadores de uma partida começada.
- [ ] As mensagens batem com as prints de referência (verificar no jogo).
- [ ] A party segue o líder entre servidores reais (verificar no jogo).

## Falhas e recuperação

- **Reinício do proxy:** todos são desconectados e as parties se perdem.
- **Um servidor inicia:** ele pede todas as parties ao proxy; até a resposta chegar, não vê ninguém em party.
- **Um pacote perdido:** um aviso ou uma atualização pode se perder (o Redis Pub/Sub entrega no máximo uma vez); a próxima mudança daquela party envia a party inteira de novo.
- **O servidor do líder não responde um teleporte:** ninguém é movido; o líder pode tentar de novo.
- **Um membro não pode ser movido** (o servidor do líder está cheio ou caiu): o membro fica onde está.

## Limitações conhecidas

- Com mais de um proxy, cada proxy teria as próprias parties.
- Clãs ainda não existem: `/canal clan` só responde que o jogador não tem clã, e a linha do chat da party não tem tag de clã.
- Trocar de lobby não move a party; só o `/party teleportar` e entrar em uma partida movem.
