[English](README.md) | **Português**

# Arquitetura da rede

Como a rede Muxe é dividida em servidores e como os jogadores se movem entre eles.

## Servidores

```text
                      Proxy Velocity (muxe-core-proxy)
                                 │
          ┌──────────────────────┼───────────────────────┐
          ▼                      ▼                       ▼
   auth-1 … auth-N        lobby-1 … lobby-N       skywars-1 … / bedwars-1 …
   Muxe-Core + Auth       Muxe-Core + Lobby       Muxe-Core + jogo
```

| Grupo | Tipo | Papel |
|---|---|---|
| `proxy` | `PROXY` | A única porta de entrada pública. Escolhe servidores, protege a autenticação, responde a buscas por jogadores. |
| `auth` | `AUTH` | Onde todo jogador começa. `/register` ou `/login`, depois o jogador é enviado a um lobby. |
| `lobby` | `LOBBY` | O lobby principal, onde os jogadores escolhem um jogo com `/jogar`. |
| `skywars`, `bedwars`, … | `MINIGAME` | Um jogo por servidor. Cada servidor roda várias partidas do mesmo jogo. |

Todo processo se conecta à infraestrutura compartilhada:

| Serviço | Papel |
|---|---|
| PostgreSQL | Fonte da verdade. Veja [Persistência](../persistence/README.pt-BR.md). |
| Redis | Transporte de pacotes, reservas de idempotência e, no futuro, cache compartilhado. Veja [Mensageria](../messaging/README.pt-BR.md). |

## Identidade

Cada processo tem uma `ServerIdentity`, lida do seu `config.yml`:

| Campo | Exemplo | Regra |
|---|---|---|
| `server.id` | `lobby-2` | Único na rede, igual ao nome do servidor no `velocity.toml`. |
| `server.group` | `lobby` | Servidores que rodam a mesma coisa compartilham um grupo. |
| `server.kind` | `LOBBY` | `PROXY`, `AUTH`, `LOBBY` ou `MINIGAME`. |

## Descoberta: o diretório de servidores

Os servidores se encontram sem nenhuma lista estática além do `velocity.toml`:

1. Todo processo (incluindo o proxy) publica um pacote `server.status` a cada **2 segundos** com seu id, grupo, tipo, jogadores online e máximo, e se aceita jogadores.
2. Todo processo mantém um `ServerDirectory` montado a partir desses pacotes. Um servidor que não reporta há **10 segundos** é considerado morto.
3. Um servidor que para de forma limpa publica `server.shutdown` e é removido imediatamente.
4. Status fora de ordem são ignorados: um status mais antigo do que o já conhecido para aquele servidor não muda nada.

O proxy usa o diretório para escolher o servidor menos cheio de um grupo que tenha vaga. Um servidor conhecido pelo Velocity mas ausente do diretório não é usado.

## Jornada do jogador

```text
conecta ─► o proxy escolhe o servidor de auth menos cheio
        ─► /register ou /login no servidor de auth
        ─► o auth publica auth.authenticated para o proxy
        ─► o proxy abre a barreira e envia o jogador ao lobby menos cheio
        ─► /jogar skywars no lobby publica player.connect para o proxy
        ─► o proxy envia o jogador ao servidor de skywars menos cheio
```

Até o proxy receber `auth.authenticated` para um jogador, ele recusa toda conexão a um servidor fora do grupo de auth. Veja [Roteamento da rede](../../features/network-routing/README.pt-BR.md).

## Escala

- **Horizontal.** Qualquer grupo pode rodar mais servidores: adicione o servidor ao `velocity.toml`, dê a ele um `server.id` único e inicie-o. Ele entra no diretório com o primeiro heartbeat.
- **Isolamento.** Cada jogo roda em seus próprios servidores, então uma partida pesada ou um crash em um jogo nunca deixa outro lento.
- **Reinícios independentes.** Um servidor pode ser reiniciado sem mexer nos outros; os jogadores são roteados para os irmãos dele enquanto ele está fora.
- **Vários proxies.** Várias instâncias do Velocity podem rodar atrás de um balanceador TCP. Pacotes para `proxy` chegam a todos os proxies, e cada um só age sobre os jogadores que tem.

## Ainda não decidido

- Como um lobby de jogo distribui jogadores para servidores de partida quando lobbies e partidas forem separados em servidores diferentes.
- Início e parada automáticos de servidores conforme a demanda.
- Dono da sessão entre servidores, descrito como planejado em [Persistência](../persistence/README.pt-BR.md#planejado-dono-da-sessão).
