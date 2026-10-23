[English](README.md) | **Português**

# Processo

Como o trabalho é planejado, construído e entregue. A Muxe segue práticas ágeis adaptadas a um time pequeno e open-source: iterações curtas, incrementos pequenos, integração contínua e software funcionando acima de grandes planos.

## Princípios

- **Fatias pequenas e verticais.** Cada mudança entrega algo que um jogador, alguém da equipe ou um operador consegue observar, de ponta a ponta, em vez de uma camada por vez.
- **Software funcionando é a medida do progresso.** Uma feature está concluída quando roda, está testada e está documentada, e não quando o código foi escrito.
- **Feedback cedo.** Os pull requests ficam pequenos para serem revisados no mesmo dia. Os servidores de homologação são usados para validar com jogadores reais antes da produção.
- **Ritmo sustentável e qualidade.** Nada de atalhos "temporários" sem uma issue que os acompanhe. Testes e documentação fazem parte do trabalho, não são trabalho extra.
- **Simplicidade.** Construa o que a iteração atual precisa, atrás de interfaces que deixem o próximo passo barato.

## Itens de trabalho

O trabalho é acompanhado em issues do GitHub, usando os modelos em [.github/ISSUE_TEMPLATE](../../.github/ISSUE_TEMPLATE/).

### Histórias de usuário

Comportamentos novos são descritos como histórias de usuário com critérios de aceitação:

```text
As a <player | staff member | operator>
I want <capability>
So that <benefit>

Acceptance criteria
- Given <context>, when <action>, then <observable result>.
```

Os critérios de aceitação são observáveis e testáveis. Eles viram testes e a seção "Critérios de aceitação" da documentação da feature.

### Bugs

Os bugs descrevem o comportamento esperado, o comportamento atual e como reproduzi-los. A correção começa com um teste que falha.

### Tarefas técnicas

Refatorações, atualizações e trabalho de infraestrutura explicam o problema que resolvem e como o resultado será verificado.

## Iterações

- O trabalho é organizado em **iterações de duas semanas**.
- **Planejamento:** o time escolhe as histórias que estão *prontas para começar* (veja abaixo), as mais valiosas primeiro, e divide as que não caberiam em poucos dias de trabalho.
- **Sincronização diária:** curta e assíncrona serve: o que foi feito, o que vem a seguir, o que está bloqueado.
- **Revisão:** no fim da iteração, o que foi entregue é mostrado rodando nos servidores de homologação.
- **Retrospectiva:** o que manter, o que mudar e uma ação concreta para a próxima iteração.

## Definição de Pronto para Começar

Uma história pode entrar em uma iteração quando:

- [ ] Tem um usuário, uma capacidade e um benefício claros.
- [ ] Os critérios de aceitação estão escritos e são testáveis.
- [ ] As dependências de outras histórias ou de infraestrutura são conhecidas.
- [ ] Cabe em poucos dias de trabalho; caso contrário, é dividida.
- [ ] Os pacotes, dados ou configurações que mudam estão identificados.

## Definição de Concluído

Uma mudança está concluída quando:

- [ ] Todos os critérios de aceitação são atendidos e cobertos por um teste.
- [ ] `./gradlew build` passa localmente e na CI.
- [ ] Comportamentos novos que dependem do PostgreSQL ou do Redis têm um teste de integração.
- [ ] Existem métricas para novos pacotes, commands e operações lentas.
- [ ] A documentação da feature foi criada ou atualizada, em inglês e português.
- [ ] As decisões de arquitetura que mudaram estão registradas como um [ADR](../decisions/README.pt-BR.md).
- [ ] O pull request foi revisado e mesclado na `development`.
- [ ] Foi validada nos servidores de homologação antes de chegar à produção.

## Releases

- A `development` sempre compila. A `homologation` é o que os servidores de teste rodam. A `production` é o que os jogadores usam.
- Um release é um pull request da `homologation` para a `production`, com a tag `vMAJOR.MINOR.PATCH`, seguindo o [Versionamento Semântico](https://semver.org/lang/pt-BR/).
- A versão do projeto fica no `gradle.properties`.

## Decisões de arquitetura

Decisões difíceis de reverter, ou sobre as quais futuros contribuidores vão perguntar "por quê?", são escritas como Registros de Decisões de Arquitetura em [docs/decisions](../decisions/README.pt-BR.md). Um ADR é curto: contexto, decisão, consequências.
