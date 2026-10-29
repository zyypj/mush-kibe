[English](0008-packet-npcs-and-click-interception.md) | **Português**

# 0008. NPCs, hologramas e boss bars existem só em pacotes

- **Status:** Aceita
- **Data:** 2026-10-24

## Contexto

O lobby precisa de NPCs com skin de jogador que levam aos jogos, de texto acima deles e de uma boss bar. O Spigot 1.8.8 não tem API para nenhum deles: não há NPC de jogador, holograma por jogador nem boss bar. Cada jogador também precisa ver só a sua própria visão (o seu código do desafio, o texto da sua boss bar), e o clique num NPC precisa chegar ao plugin.

## Decisão

- NPCs, hologramas e a boss bar são entidades que existem só em pacotes enviados para cada jogador: um jogador falso (`EntityPlayer`) com a skin no perfil, suportes de armadura invisíveis com nome e um wither invisível mantido à frente do jogador. Eles são montados por reflexão sobre os internos do servidor (`PacketNpcTransport`, `PacketHologramTransport`, `PacketBossBarTransport`) e nunca entram no mundo.
- Os cliques nos NPCs são lidos da rede: o `PacketInterceptor` adiciona um handler do Netty antes do `packet_handler` do Minecraft em cada conexão e avisa sobre o `PacketPlayInUseEntity`; o serviço de NPCs roda o clique na main thread. O Netty entra só na compilação (`compileOnly`), porque o servidor já o traz.

## Alternativas consideradas

- **Entidades de NPC reais no mundo** (como o Citizens faz): elas seriam processadas a cada tick, salvas com o chunk, vistas por todos e precisariam de uma conexão de rede falsa. Pesado demais para decoração.
- **Uma entidade real invisível em cima do NPC falso para receber os eventos de clique do Bukkit:** o cliente escolhe a hitbox mais próxima, que costuma ser a do NPC falso, então os cliques se perderiam.
- **Um plugin de NPC ou de holograma:** mais uma dependência, outro formato de configuração e nenhuma visão por jogador.

## Consequências

- Nada fica salvo no mundo: reiniciar o servidor ou recarregar um chunk não deixa entidades soltas.
- O código depende dos internos da 1.8 (nomes do `v1_8_R3`). Cada transporte resolve a reflexão uma vez e informa `isAvailable()`; quando falta algo, ele registra um aviso e o recurso é pulado em vez de quebrar o plugin.
- É preciso acompanhar quem vê: os NPCs aparecem a até 48 blocos e somem além de 64, os hologramas são reenviados de tempos em tempos e a boss bar segue o jogador.
- Mudar de versão do servidor significa atualizar esses três transportes e o interceptor, não as features.
