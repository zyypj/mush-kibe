[English](CONTRIBUTING.md) | **Português**

# Como contribuir

Estas regras valem para todos que alteram este repositório, pessoas e agentes de IA. Os agentes de IA também seguem o [AGENTS.pt-BR.md](AGENTS.pt-BR.md).

Código, commits e pull requests são em inglês. A documentação é em inglês e português. As mensagens exibidas aos jogadores são em português e ficam nos arquivos de configuração.

## Antes de começar

- Leia a [política legal e de sala limpa](docs/legal/README.pt-BR.md). Contribuições copiadas de outra rede, descompiladas ou vazadas são rejeitadas.
- Leia o [processo](docs/process/README.pt-BR.md): como o trabalho é planejado e o que significam "pronto para começar" e "concluído".
- Leia a [visão geral da arquitetura](docs/architecture/overview/README.pt-BR.md) e as [convenções](docs/development/conventions/README.pt-BR.md).

## Branches

| Branch | O que contém |
|---|---|
| `production` | O que está rodando em produção. É a branch padrão. |
| `homologation` | O que está nos servidores de teste e sendo validado. |
| `development` | O que foi desenvolvido e ainda não está em produção. |

As mudanças andam em um único sentido:

```text
work branch → development → homologation → production
```

- As branches de trabalho são criadas a partir da `development` e voltam para ela por meio de um pull request.
- Quando um conjunto de mudanças está pronto para ser testado, a `development` é mesclada na `homologation` por meio de um pull request.
- Depois de validada nos servidores de teste, a `homologation` é mesclada na `production` por meio de um pull request.
- `production` e `homologation` só recebem mudanças por pull request.

Nomes das branches de trabalho: `<type>/<scope>-<descricao-curta>`, como `feat/auth-premium-bypass` ou `fix/proxy-lobby-routing`.

## Commits

Os commits seguem o [Conventional Commits](https://www.conventionalcommits.org/pt-br/v1.0.0/) e são escritos em **inglês**.

```text
<type>(<scope>): <description>

[body]

[footer]
```

### Cabeçalho

- `type` é um dos tipos abaixo.
- `scope` é o módulo ou a feature afetada, em kebab-case. Escopos de feature são iguais ao nome da pasta de documentação da feature (`authentication` ↔ `docs/features/authentication/`). Os escopos da plataforma estão listados abaixo. Omita o escopo só em mudanças que atravessam o repositório inteiro.
- `description` usa o imperativo ("add", e não "added" ou "adds"), começa com letra minúscula e não termina com ponto.
- O cabeçalho inteiro cabe em 72 caracteres.

| Tipo | Quando usar |
|---|---|
| `feat` | Uma nova feature ou uma nova capacidade em uma existente. |
| `fix` | Correção de bug. |
| `docs` | Apenas documentação. |
| `refactor` | Mudança de código que não corrige bug nem altera comportamento. |
| `perf` | Mudança que melhora o desempenho. |
| `test` | Apenas adição ou correção de testes. |
| `build` | Sistema de build ou dependências. |
| `ci` | Configuração e workflows de CI. |
| `chore` | Manutenção que não se encaixa nos outros tipos. |
| `revert` | Reverte um commit anterior. |

| Escopo da plataforma | Área |
|---|---|
| `build` | `build-logic`, arquivos do Gradle e o catálogo de versões. |
| `messaging` | Pacotes, envelopes, transportes, idempotência. |
| `persistence` | Banco de dados, transações e migrações. |
| `metrics` | Registro de métricas e exportadores. |
| `cqrs` | Buses de commands e queries. |
| `config` | Leitura de configuração e segredos. |
| `core` | Montagem do runtime compartilhada por todos os servidores. |
| `bukkit` | O plugin `core:bukkit`. |
| `proxy` | O plugin Velocity `core:proxy`. |
| `engine` | O motor de minigames. |

### Corpo e rodapé

- O corpo explica **por que** a mudança foi feita, e não o que o diff já mostra. Quebre as linhas em 72 caracteres.
- Mudanças incompatíveis adicionam `!` depois do tipo ou do escopo (`feat(messaging)!: ...`) e um rodapé `BREAKING CHANGE:` descrevendo o que quebra e como migrar.
- Referencie issues no rodapé com `Refs #N` ou `Closes #N`.

### Exemplos

```text
feat(authentication): lock accounts after five wrong passwords
fix(proxy): keep unauthenticated players on the auth server
docs(messaging): document the exclusive idempotency level
refactor(persistence)!: bind transactions with scoped values
```

### Commits organizados

Commits gerais são proibidos. O trabalho é dividido em commits separados, cada um agrupando uma única mudança lógica.

- Uma mudança lógica por commit. Não misture mudanças sem relação no mesmo commit.
- Agrupe por mudança, e não por arquivo: um commit pode tocar vários arquivos quando eles fazem parte da mesma mudança.
- Descrições vagas como `update files`, `fixes`, `changes` ou `wip` são proibidas.
- Os testes e a documentação de uma mudança vão junto com ela, no mesmo commit ou no mesmo pull request.

### Histórico

- Nunca faça force-push nem reescreva histórico já enviado para uma branch compartilhada.

## Documentação

Todo arquivo de documentação existe em **inglês e português**. As duas versões dizem a mesma coisa, e nenhuma é mesclada sem a outra.

| Idioma | Arquivo |
|---|---|
| Inglês | `README.md`, `CONTRIBUTING.md`, `AGENTS.md`, `SECURITY.md`, `docs/**/README.md`, `docs/decisions/*.md` |
| Português | `README.pt-BR.md`, `CONTRIBUTING.pt-BR.md`, `AGENTS.pt-BR.md`, `SECURITY.pt-BR.md`, `docs/**/README.pt-BR.md`, `docs/decisions/*.pt-BR.md` |

O `CLAUDE.md` não é documentação: ele só aponta o Claude Code para o `AGENTS.md`.

### Toda feature tem a sua documentação

Cada feature fica em sua própria pasta dentro de [`docs/features/`](docs/features/), com o mesmo nome do escopo de commit.

- **Feature nova:** o pull request que a adiciona também cria a sua documentação, a partir de [`docs/features/_template/`](docs/features/_template/).
- **Feature alterada:** o pull request que muda comportamento, comandos, permissões, configuração, pacotes, métricas ou integrações também atualiza a documentação da feature, nos dois idiomas.
- **Feature removida:** o pull request que a remove também remove a sua documentação.

A documentação descreve como a feature funciona **hoje**. O histórico fica no git e nos pull requests. Decisões que moldam a arquitetura são registradas como [ADRs](docs/decisions/README.pt-BR.md).

## Pull requests

- Pull requests de trabalho são abertos para a `development`.
- O título segue a convenção de commit.
- O corpo segue o [modelo](.github/pull_request_template.md): **Problem**, **Solution**, **How it was done** e **Validations** (apenas verificações que de fato rodaram, com os resultados).
- O checklist de documentação do modelo é obrigatório.
- A CI precisa estar verde: `./gradlew build` compila, testa e confere as regras de módulos.
