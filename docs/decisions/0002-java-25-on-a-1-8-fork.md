**English** | [Português](0002-java-25-on-a-1-8-fork.pt-BR.md)

# 0002. Java 25 everywhere, Spigot 1.8.8 API on a modern fork, Velocity 4

- **Status:** Accepted
- **Date:** 2026-10-23

## Context

The network targets Minecraft 1.8 clients and gameplay. The original Spigot 1.8.8 runs on Java 8, which would force every module, including the shared core and the proxy, to Java 8 syntax and to old library versions. Maintained forks of 1.8.8 (such as PandaSpigot) run on modern Java. Velocity 4.3 still accepts 1.8 clients and requires Java 25.

## Decision

- Every module is compiled for **Java 25**.
- Game servers compile against the **Spigot API 1.8.8** and run on a 1.8.8 fork that supports Java 25.
- The proxy is **Velocity 4.3**.

## Alternatives considered

- **Java 8 everywhere:** works with the original Spigot, but loses records, sealed types, pattern matching, virtual threads and `ScopedValue`, and pins libraries to old versions (HikariCP 4, Flyway 9).
- **Java 8 for game servers and a newer Java for the proxy:** two language levels in one codebase and a shared core limited to Java 8.
- **Modern Paper:** different gameplay and combat from 1.8, which is the point of the network.

## Consequences

- Modern Java features are used freely, including virtual threads for I/O and `ScopedValue` for transactions.
- Code must still respect the 1.8.8 Bukkit API: nothing newer, such as Adventure on the game servers.
- Operators must run a 1.8.8 fork compatible with Java 25. The fork is a deployment choice, not a build dependency.
- 1.8 servers cannot use Velocity's modern forwarding; firewalling the game servers is mandatory. See [Security](../architecture/security/README.md).
