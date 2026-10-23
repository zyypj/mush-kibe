[English](0004-own-metrics-registry.md) | **Português**

# 0004. Um registro de métricas próprio e pequeno, com exportação no formato texto do Prometheus

- **Status:** Aceita
- **Data:** 2026-10-23

## Contexto

Cada pacote, requisição, comando, transação e tick precisa ser medido sem deixar o jogo mais lento, no Spigot e no Velocity, e coletado por uma stack de monitoramento padrão.

## Decisão

A Muxe tem a sua própria interface `MetricRegistry`, com contadores, timers (histogramas com buckets fixos) e gauges, implementada pelo `SimpleMetricRegistry` com `LongAdder`s e exportada no formato texto do Prometheus por um servidor HTTP embutido do JDK.

## Alternativas consideradas

- **Micrometer com o registro do Prometheus:** o padrão na JVM, mas adiciona várias bibliotecas para embutir em cada plugin, e a Muxe só precisa de uma pequena parte dele.
- **Dropwizard Metrics:** o mesmo dilema, com uma exportação menos padronizada.

## Consequências

- Nenhuma dependência e controle total sobre o custo: o registro não usa travas, e os gauges só são lidos na coleta.
- As features dependem do `MetricRegistry`, então a implementação pode ser trocada pelo Micrometer no futuro sem mudar as features.
- Exportadores além do Prometheus precisam ser escritos se algum dia forem necessários.
