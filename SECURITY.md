**English** | [Português](SECURITY.pt-BR.md)

# Security policy

## Reporting a vulnerability

Do not open a public issue for a security problem. Report it privately through GitHub's **Security → Report a vulnerability** form on this repository.

Include what is affected, how to reproduce it and what an attacker could do with it. You will get an answer as soon as possible, and the fix will credit you unless you ask otherwise.

## Scope

In scope: everything in this repository, such as the packet protocol, authentication, the proxy access gate, persistence and the default configuration.

Out of scope: vulnerabilities in Minecraft itself, in the server software (Spigot forks, Velocity) or in third-party libraries, unless Muxe uses them in an unsafe way.

## How the network is meant to be deployed

The security model is described in [Network security](docs/architecture/security/README.md). A deployment that exposes the game servers, PostgreSQL or Redis to the internet is outside the supported model.
