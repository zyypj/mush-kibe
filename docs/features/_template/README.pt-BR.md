[English](README.md) | **Português**

# Nome da feature

Uma ou duas frases sobre o que a feature faz e para quem ela existe.

<!-- Mantenha todas as seções. Quando uma não se aplicar, escreva "Não se aplica." para deixar claro que ela foi considerada. -->

## Histórias de usuário

- Como <jogador | membro da equipe | operador>, quero <capacidade>, para que <benefício>.

## Como funciona

Como a feature se comporta do ponto de vista do jogador e da equipe, incluindo casos-limite e restrições.

## Comandos

| Comando | Descrição | Permissão |
|---|---|---|
| `/example <player>` | O que ele faz. | `muxe.example` |

## Permissões

| Permissão | Libera |
|---|---|
| `muxe.example` | O que ela permite. |

## Configuração

| Chave | Padrão | Descrição |
|---|---|---|
| `example.enabled` | `true` | O que ela controla. |

## Pacotes

| Id | Destino | Política | Propósito |
|---|---|---|---|
| `area.name` | proxy | standard | Por que ele é enviado. |

## Dados

Tabelas, schemas e chaves do Redis que pertencem à feature.

## Métricas

Métricas adicionadas pela feature, também listadas em [Observabilidade](../../architecture/observability/README.pt-BR.md).

## Integrações

Módulos, features e serviços externos dos quais esta feature depende ou que ela afeta.

## Critérios de aceitação

Os comportamentos observáveis que provam que a feature funciona. Cada um pode ser conferido em um teste ou dentro do jogo.

- [ ] Quando um jogador faz X, acontece Y.

## Falhas e recuperação

O que acontece quando uma operação não pode ser concluída: um jogador desconecta no meio, um servidor reinicia, o banco ou o Redis não respondem. Para cada caso, o que o jogador vê e como o sistema se recupera.

## Limitações conhecidas

O que a feature ainda não faz, ou faz de propósito de um jeito que pode surpreender alguém.
