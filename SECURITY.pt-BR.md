[English](SECURITY.md) | **Português**

# Política de segurança

## Reportando uma vulnerabilidade

Não abra uma issue pública para um problema de segurança. Reporte-o de forma privada pelo formulário **Security → Report a vulnerability** do GitHub, neste repositório.

Inclua o que é afetado, como reproduzir e o que um atacante poderia fazer com isso. Você terá uma resposta o quanto antes, e a correção vai dar o crédito a você, a não ser que você peça o contrário.

## Escopo

No escopo: tudo neste repositório, como o protocolo de pacotes, a autenticação, a barreira de acesso do proxy, a persistência e a configuração padrão.

Fora do escopo: vulnerabilidades no próprio Minecraft, no software dos servidores (forks do Spigot, Velocity) ou em bibliotecas de terceiros, a não ser que a Muxe as use de forma insegura.

## Como a rede deve ser implantada

O modelo de segurança está descrito em [Segurança da rede](docs/architecture/security/README.pt-BR.md). Uma implantação que expõe os servidores de jogo, o PostgreSQL ou o Redis à internet está fora do modelo suportado.
