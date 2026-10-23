[English](0002-java-25-on-a-1-8-fork.md) | **Português**

# 0002. Java 25 em tudo, API do Spigot 1.8.8 sobre um fork moderno, Velocity 4

- **Status:** Aceita
- **Data:** 2026-10-23

## Contexto

A rede tem como alvo os clientes e a jogabilidade do Minecraft 1.8. O Spigot 1.8.8 original roda no Java 8, o que obrigaria todos os módulos, inclusive o núcleo compartilhado e o proxy, a usar a sintaxe do Java 8 e versões antigas das bibliotecas. Forks mantidos do 1.8.8 (como o PandaSpigot) rodam em Java moderno. O Velocity 4.3 ainda aceita clientes 1.8 e exige Java 25.

## Decisão

- Todos os módulos são compilados para **Java 25**.
- Os servidores de jogo compilam contra a **API do Spigot 1.8.8** e rodam em um fork do 1.8.8 que suporta Java 25.
- O proxy é o **Velocity 4.3**.

## Alternativas consideradas

- **Java 8 em tudo:** funciona com o Spigot original, mas perde records, tipos selados, pattern matching, virtual threads e `ScopedValue`, e prende as bibliotecas a versões antigas (HikariCP 4, Flyway 9).
- **Java 8 nos servidores de jogo e um Java mais novo no proxy:** dois níveis de linguagem em uma mesma base de código e um núcleo compartilhado limitado ao Java 8.
- **Paper moderno:** jogabilidade e combate diferentes do 1.8, que é justamente o objetivo da rede.

## Consequências

- Os recursos modernos do Java são usados livremente, incluindo virtual threads para I/O e `ScopedValue` para transações.
- O código ainda precisa respeitar a API do Bukkit 1.8.8: nada mais novo, como Adventure, nos servidores de jogo.
- Os operadores precisam rodar um fork do 1.8.8 compatível com Java 25. O fork é uma escolha de implantação, não uma dependência do build.
- Servidores 1.8 não podem usar o modern forwarding do Velocity; proteger os servidores de jogo com firewall é obrigatório. Veja [Segurança](../architecture/security/README.pt-BR.md).
