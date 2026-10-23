[English](0003-postgresql-with-schemas-per-module.md) | **Português**

# 0003. PostgreSQL com migrações do Flyway em um schema por módulo

- **Status:** Aceita
- **Data:** 2026-10-23

## Contexto

Os dados dos jogadores precisam sobreviver a reinícios, ser consistentes entre servidores e suportar transações. Vários plugins são donos de dados e evoluem de forma independente.

## Decisão

- O PostgreSQL é a fonte da verdade.
- Cada módulo é dono de um schema do PostgreSQL com o seu próprio nome e o seu próprio histórico do Flyway (`<module>.schema_history`), com as migrações em `db/migration/<module>/`.
- O acesso aos dados é JDBC puro via HikariCP, com SQL escrito à mão nos repositórios.

## Alternativas consideradas

- **MySQL/MariaDB:** comuns no Minecraft, mas o PostgreSQL oferece DDL transacional, índices de expressão (como `LOWER(username)`), melhor concorrência e tipos mais ricos.
- **Um schema compartilhado e um único histórico de migrações:** acopla o lançamento de cada plugin a um único histórico ordenado.
- **Um ORM (Hibernate, Ebean):** esconde o custo das consultas e adiciona peso e reflexão a cada jar de plugin.
- **Um executor de migrações próprio:** menos código para embutir, mas o Flyway traz travas, checksums e validação que são fáceis de errar.

## Consequências

- Os plugins podem adicionar tabelas sem coordenar a numeração das migrações entre si.
- O Flyway precisa ser embutido com os arquivos de serviço mesclados; um smoke test provou que a realocação só funciona com `duplicatesStrategy = INCLUDE` no Shadow 9.
- As consultas são explícitas e revisáveis; os repositórios precisam de testes de integração.
