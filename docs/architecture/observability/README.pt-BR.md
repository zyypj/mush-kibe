[English](README.md) | **Português**

# Observabilidade

O que a rede mede e como ler. Problemas são vistos com números, e não com palpites.

Código: `core/common/src/main/java/br/com/muxe/core/common/metrics`.

## Como as métricas são expostas

Todo servidor e proxy serve suas métricas no [formato de texto do Prometheus](https://prometheus.io/docs/instrumenting/exposition_formats/) em:

```text
http://<metrics.host>:<metrics.port>/metrics
```

| Chave | Padrão | Descrição |
|---|---|---|
| `metrics.enabled` | `true` | Inicia o endpoint HTTP. |
| `metrics.host` | `127.0.0.1` | Endereço de escuta. Mantenha-o privado. |
| `metrics.port` | `9400` (`9410` no proxy) | Precisa ser único por processo na mesma máquina. |

Toda amostra carrega os labels `server` e `group` do processo, então um único Prometheus pode coletar a rede inteira.

`/muxe metrics` mostra um resumo curto dentro do jogo.

## Instrumentos

| Instrumento | Tipo no Prometheus | Observações |
|---|---|---|
| `Counter` | `counter` | Só sobe. Nomes terminam com `_total`. |
| `Timer` | `histogram` | Buckets de 0,5 ms a 10 s, mais `_sum`, `_count` e `_max`. Nomes terminam com `_seconds`. |
| Gauge | `gauge` | Um valor lido quando o endpoint é coletado. |

O registro (`SimpleMetricRegistry`) não tem dependências e usa `LongAdder`s, então registrar é barato e nunca bloqueia. A interface `MetricRegistry` permite substituí-lo (por exemplo, pelo Micrometer) sem mexer nas features. Veja a [ADR 0004](../../decisions/0004-own-metrics-registry.pt-BR.md).

## Mensageria

Labels: `type` é o id do pacote, `reason` o motivo da rejeição, `outcome` o resultado da requisição.

| Métrica | Tipo | O que mostra |
|---|---|---|
| `muxe_packets_published_total{type}` | counter | Pacotes publicados. |
| `muxe_packets_published_bytes_total{type}` | counter | Bytes publicados, incluindo o envelope. |
| `muxe_packets_publish_seconds{type}` | histogram | Tempo entre a chamada de publicação e a confirmação do Redis. |
| `muxe_packets_publish_failures_total{type}` | counter | Publicações que falharam (codificação, tamanho ou Redis). |
| `muxe_packets_received_total{type}` | counter | Envelopes válidos recebidos de um tipo conhecido. |
| `muxe_packets_received_bytes_total{type}` | counter | Bytes recebidos. |
| `muxe_packets_latency_seconds{type}` | histogram | Do relógio de quem enviou ao relógio de quem recebeu. Inclui a diferença de relógio entre máquinas. |
| `muxe_packets_handle_seconds{type}` | histogram | Tempo gasto em cada handler. |
| `muxe_packets_handler_failures_total{type}` | counter | Handlers que lançaram exceção. |
| `muxe_packets_duplicates_total{type}` | counter | Pacotes descartados pela idempotência. |
| `muxe_packets_unhandled_total{type}` | counter | Pacotes recebidos sem handler neste servidor. |
| `muxe_packets_rejected_total{reason}` | counter | `too_large`, `malformed`, `bad_magic`, `unsupported_protocol`, `bad_signature`, `unknown_type`, `expired`, `malformed_payload`, `late_response`, `idempotency_unavailable`. |
| `muxe_requests_total{type,outcome}` | counter | Requisições por `success`, `timeout` ou `failure`. |
| `muxe_request_seconds{type,outcome}` | histogram | Ida e volta das requisições. |
| `muxe_requests_pending` | gauge | Requisições esperando resposta. |
| `muxe_transport_connected` | gauge | 1 enquanto a assinatura do Redis está ativa. |
| `muxe_transport_reconnects_total` | counter | Vezes que a assinatura do Redis caiu. |

`bad_signature` acima de zero indica um servidor mal configurado ou alguém publicando no Redis sem o segredo: investigue.

## Autenticação

| Métrica | Tipo | O que mostra |
|---|---|---|
| `muxe_auth_login_modes_total{mode}` | counter | Modos de login decididos pelo proxy: `premium`, `cracked`, `undecided`. |
| `muxe_auth_logins_total{mode}` | counter | Como os jogadores entraram: `premium`, `session` ou `password`. |
| `muxe_auth_premium_lookups_total{result,source}` | counter | Consultas à Mojang por resultado (`premium`, `cracked`, `unknown`) e origem (`cache`, `remote`). |
| `muxe_auth_challenges_total{outcome}` | counter | Respostas ao desafio anti-bot: `passed`, `wrong`, `failed`. |

Uma parcela crescente de `undecided` ou `unknown` indica que a Mojang está inacessível ou limitando as requisições do proxy.

## Aplicação

| Métrica | Tipo | O que mostra |
|---|---|---|
| `muxe_cqrs_handle_seconds{kind,message,outcome}` | histogram | Todo comando e consulta. `outcome` é `success`, `failure` (um `Result.Failure`) ou `error` (uma exceção). |

## Persistência

| Métrica | Tipo | O que mostra |
|---|---|---|
| `muxe_db_transaction_seconds{operation,outcome}` | histogram | Toda transação, por operação e por `commit`, `rollback` ou `error`. |
| `muxe_db_connections_active` | gauge | Conexões em uso. |
| `muxe_db_connections_idle` | gauge | Conexões ociosas. |
| `muxe_db_connections_pending` | gauge | Threads esperando uma conexão. Acima de zero por muito tempo significa pool pequeno demais ou consulta lenta demais. |
| `muxe_db_connections_total` | gauge | Conexões no pool. |

## Rede

| Métrica | Tipo | O que mostra |
|---|---|---|
| `muxe_network_servers_alive` | gauge | Servidores no diretório deste processo. |
| `muxe_network_players_online` | gauge | Jogadores nos servidores de jogo, vistos por este processo. |
| `muxe_proxy_authenticated_players` | gauge | Jogadores que passaram pela autenticação neste proxy. |

## Servidores Spigot

| Métrica | Tipo | O que mostra |
|---|---|---|
| `muxe_server_tps` | gauge | Ticks por segundo nos últimos 100 ticks (máximo 20). |
| `muxe_server_tick_interval_seconds` | histogram | Tempo entre ticks. Acima de 0,05 s o servidor não consegue manter 20 TPS. |
| `muxe_server_players_online` | gauge | Jogadores neste servidor. |
| `muxe_server_players_max` | gauge | Vagas deste servidor. |

## Minigames

| Métrica | Tipo | O que mostra |
|---|---|---|
| `muxe_matches_active{game}` | gauge | Partidas abertas no momento. |
| `muxe_arenas_available{game}` | gauge | Instâncias de arena prontas para uso. |
| `muxe_arenas_disabled{game}` | gauge | Instâncias de arena desativadas após resets que falharam. Acima de zero precisa da equipe. |

## JVM

`jvm_memory_heap_used_bytes`, `jvm_memory_heap_committed_bytes`, `jvm_memory_heap_max_bytes`, `jvm_memory_non_heap_used_bytes`, `jvm_threads_live`, `jvm_threads_daemon`, `jvm_uptime_seconds`, `jvm_cpu_available_processors`, `jvm_gc_collections{gc}` e `jvm_gc_collection_seconds{gc}`.

## Regras

- **Medir nunca deixa o jogo lento.** O registro não usa locks; gauges só são lidos na coleta.
- **Labels têm baixa cardinalidade.** Tipos de pacote, operações e nomes de mensagens podem; nomes de jogadores, UUIDs ou ids de partida não.
- **Uma feature documenta suas próprias métricas** aqui e na documentação da feature.
- **Um número sem referência diz pouco.** Assim que a rede estiver rodando, o valor normal de cada métrica é registrado para que mudanças se destaquem.
