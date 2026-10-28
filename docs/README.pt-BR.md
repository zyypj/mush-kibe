[English](README.md) | **Português**

# Documentação

Tudo sobre a Muxe está documentado aqui, em inglês e português. As regras de documentação estão no [CONTRIBUTING.pt-BR.md](../CONTRIBUTING.pt-BR.md#documentação).

## Comece por aqui

- [Desenvolvimento](development/README.pt-BR.md): requisitos, módulos, regras de dependência, comandos de build e como rodar uma rede local.
- [Convenções](development/conventions/README.pt-BR.md): estilo de Java, organização de pacotes, nomes, configuração e mensagens.
- [API do core](development/core-api/README.pt-BR.md): o que o Muxe-Core oferece aos plugins, incluindo títulos, action bars e texto em partículas.
- [Comandos](development/commands/README.pt-BR.md): o framework de comandos com anotações, argumentos, tab e registro.
- [Processo](process/README.pt-BR.md): como o trabalho é planejado e entregue, e o que significam "pronto para começar" e "concluído".
- [Política legal e de sala limpa](legal/README.pt-BR.md): o que pode e o que não pode entrar neste repositório.

## Arquitetura

- [Visão geral](architecture/overview/README.pt-BR.md): princípios, camadas, CQRS e como uma requisição percorre o código.
- [Rede](architecture/network/README.pt-BR.md): quais servidores existem e como os jogadores se movem entre eles.
- [Mensageria](architecture/messaging/README.pt-BR.md): o sistema de pacotes pelo Redis, envelopes, assinaturas, idempotência e requisição/resposta.
- [Persistência](architecture/persistence/README.pt-BR.md): PostgreSQL, transações, migrações e o cache no Redis.
- [Observabilidade](architecture/observability/README.pt-BR.md): todas as métricas que a rede exporta.
- [Segurança](architecture/security/README.pt-BR.md): o que fica exposto e como servidores, serviços e segredos são protegidos.
- [Motor de jogo](architecture/game-engine/README.pt-BR.md): partidas, arenas e seus ciclos de vida.

## Decisões

- [Registros de decisões de arquitetura](decisions/README.pt-BR.md): por que as principais escolhas técnicas foram feitas.

## Features

- [Contas](features/accounts/README.pt-BR.md): uma conta por jogador, criada no primeiro login.
- [Autenticação](features/authentication/README.pt-BR.md): login automático de premium, desafio anti-bot, `/registrar`, `/logar` e sessões de 24 horas.
- [Roteamento da rede](features/network-routing/README.pt-BR.md): como o proxy escolhe servidores, o `/lobby` e a barreira de acesso.
- [Administração da rede](features/network-admin/README.pt-BR.md): comandos `/muxe` para a equipe.
- [Lobby](features/lobby/README.pt-BR.md): spawn, voo, proteção, scoreboard, boss bar e `/jogar`.
- [Hotbar](features/hotbar/README.pt-BR.md): a hotbar dos lobbys, do Muxe-Core, com o seletor de jogo, o perfil, o seletor de lobby e a visibilidade dos jogadores.
- [NPCs e hologramas](features/npcs-and-holograms/README.pt-BR.md): NPCs e hologramas de texto colocados por comando e compartilhados pelo grupo de servidores.
- [Cabeças de redes sociais](features/social-heads/README.pt-BR.md): uma cabeça de jogador que passa pelas redes sociais da rede e envia o link no clique, compartilhada pelo grupo de servidores.
- [Parkour](features/parkour/README.pt-BR.md): um parkour cronometrado no lobby, com checkpoints, itens para reiniciar ou voltar, e um recorde pessoal por mapa, compartilhado pelo grupo de servidores.
- [Loja](features/shop/README.pt-BR.md): `/loja`, os menus de compra, as caixas registradoras e a promoção da rede.
- [SkyWars](features/skywars/README.pt-BR.md): esqueleto do plugin e modos.
- [BedWars](features/bedwars/README.pt-BR.md): esqueleto do plugin e modos.

## Documentando uma nova feature

1. Copie [`features/_template/`](features/_template/) para `features/<feature>/`, usando o mesmo nome do escopo de commit.
2. Preencha o `README.md` e o `README.pt-BR.md`.
3. Adicione a feature à lista acima, neste arquivo e no [README.md](README.md).
