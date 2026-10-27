[English](README.md) | **Português**

# Cabeças de redes sociais

Uma cabeça de jogador colocada no mundo que mostra, uma por vez, as redes sociais da Muxe, com um rótulo logo acima dela. A equipe a coloca por comando em qualquer servidor. Ela fica no Muxe-Core (`core:common` pacote `br.com.muxe.core.common.placement`, `core:bukkit` pacote `social`) e é compartilhada pelo grupo de servidores como os [NPCs e hologramas](../npcs-and-holograms/README.pt-BR.md).

## Histórias de usuário

- Como jogador, quero ver as redes sociais da rede no lobby e receber o link com um clique, para poder segui-las.
- Como membro da equipe, quero colocar, mover e remover a cabeça por comando, em todos os servidores de um grupo de uma vez.
- Como operador, quero trocar as redes, os rótulos, as texturas e os links na config, sem mexer no código.

## Como funciona

### A cabeça

- O `/redes criar <id>` coloca uma cabeça de jogador no chão do bloco onde você está, virada para onde você olha (16 direções, como uma cabeça colocada à mão). A cabeça substitui o que estava naquele bloco.
- Um rótulo fica `social-heads.label-height` (0.5125) blocos acima da base do bloco da cabeça, centralizado nele.
- A cada `social-heads.interval` (1 minuto) a cabeça passa para a próxima rede de `social-heads.networks`, na ordem da config, e recomeça depois da última. A textura da cabeça e o rótulo mudam juntos.
- A rede mostrada vem do relógio: `floor(epochMillis / interval) % networks`. Todo servidor de todo grupo calcula o mesmo índice, então todas as cabeças da rede mostram a mesma rede ao mesmo tempo, sem nenhuma mensagem entre servidores. Cada servidor confere o relógio uma vez por segundo.
- Uma rede sem `texture` mostra a cabeça de jogador padrão.
- O bloco da cabeça é um bloco real do mundo (um `SKULL`), reenviado para os jogadores próximos quando muda. Ele não pode ser quebrado. O rótulo é um holograma que existe só em pacotes, como os [suportes de exibição](../../development/core-api/README.pt-BR.md#suportes-de-exibição).

### Clique

- Um clique esquerdo ou direito na cabeça, ou no rótulo dela, envia a `message` da rede mostrada naquele momento, linha por linha, com cores `&`. Com a config padrão, o Instagram envia:

```
&d&lINSTAGRAM:
&fhttps://www.instagram.com/muxe/
```

- Os cliques usam o anti-spam do Muxe-Core (`spam.*`) junto com os NPCs, as caixas registradoras, a hotbar e os itens do parkour: três cliques dentro de 1 segundo bloqueiam por 3 segundos com `&cAguarde 2,9s para usar novamente.`
- Clicar na cabeça nunca coloca nem quebra nada: o clique é cancelado.

### Compartilhada pelo grupo de servidores

- As cabeças são posicionamentos do tipo `SOCIAL_HEAD` (`SocialHeadPlacement(group, id, position)`): pertencem ao grupo de servidores (`server.group`), são salvas no PostgreSQL e sincronizadas pelo `placement.changed`, exatamente como descrito em [NPCs e hologramas](../npcs-and-holograms/README.pt-BR.md#compartilhados-pelo-grupo-de-servidores). Os ids seguem as mesmas regras (letras minúsculas, números, `-` e `_`, com até 32 caracteres).
- O `/redes mover <id>` tira a cabeça do bloco antigo (o bloco vira ar) e a coloca onde você está, virada para onde você olha. O `/redes remover <id>` transforma o bloco da cabeça em ar e remove o rótulo em todos os servidores do grupo que estão ligados.
- Uma cabeça só aparece se o mundo dela estiver carregado naquele servidor.

## Comandos

| Comando | Descrição | Permissão |
|---|---|---|
| `/redes criar <id>` | Coloca uma cabeça onde você está, virada para onde você olha. | `muxe.social` |
| `/redes mover <id>` | Move uma cabeça para onde você está, virada para onde você olha. | `muxe.social` |
| `/redes remover <id>` | Remove uma cabeça. | `muxe.social` |
| `/redes listar` | Lista as cabeças deste grupo de servidores. | `muxe.social` |

Sem argumentos, ou com um argumento errado, o comando mostra o seu uso (`messages.social-usage`).

## Permissões

| Permissão | Libera | Padrão |
|---|---|---|
| `muxe.social` | Usar `/redes`. | op |

Clicar numa cabeça não precisa de permissão.

## Configuração

`plugins/Muxe-Core/config.yml`:

```yaml
social-heads:
  interval: 1m
  label-height: 0.5125
  networks:
    instagram:
      label: "&d@muxe"
      message:
        - "&d&lINSTAGRAM:"
        - "&fhttps://www.instagram.com/muxe/"
    youtube:
      label: "&c@muxe"
      texture: "ffb6ce188b84bdf96985a356bf3f485416f33d3b6dfce3a3fc144e77fe8dbb25"
      message:
        - "&c&lYOUTUBE:"
        - "&fhttps://www.youtube.com/@muxe"
```

| Chave | Padrão | Descrição |
|---|---|---|
| `social-heads.interval` | `1m` | Quanto tempo cada rede fica aparecendo. Precisa ser positivo. |
| `social-heads.label-height` | `0.5125` | Altura do rótulo acima da base do bloco da cabeça. |
| `social-heads.networks.<id>` | `instagram`, `twitch`, `tiktok`, `twitter`, `youtube` | As redes, mostradas nesta ordem. Precisa de pelo menos uma. |
| `social-heads.networks.<id>.label` | `&d@muxe`, `&5@muxe`, `&b@muxe`, `&b@muxe`, `&c@muxe` | Texto do rótulo acima da cabeça. |
| `social-heads.networks.<id>.texture` | Só Twitch, TikTok e YouTube | Textura da cabeça: o hash depois de `textures.minecraft.net/texture/`, a URL da textura ou o valor em base64, como o `skull-texture` dos [itens](../../development/core-api/README.pt-BR.md#itens). Sem ela, a cabeça de jogador padrão. |
| `social-heads.networks.<id>.message` | Nome da rede e o link dela | Linhas de chat enviadas no clique. |
| `spam.max-clicks`, `spam.window`, `spam.cooldown` | `3`, `1s`, `3s` | Anti-spam compartilhado com os NPCs, as caixas registradoras, a hotbar e os itens do parkour. |

Sem a seção `social-heads`, as cabeças ficam desligadas naquele servidor: o core registra `The social heads are off: config.yml has no 'social-heads' section`, não mostra nenhuma cabeça e ignora os cliques. O `/redes` continua salvando os posicionamentos, que os outros servidores do grupo mostram. Uma `networks` vazia ou um `interval` que não é positivo é um erro ao ligar.

### Mensagens

Todas em `messages` do `plugins/Muxe-Core/config.yml`:

| Chave | Padrão |
|---|---|
| `social-usage` | Lista: `&eUso do /redes:` e uma linha por subcomando. |
| `social-invalid-id` | `&cO id precisa ter só letras minúsculas, números, - e _.` |
| `social-exists` | `&cJá existe uma cabeça de redes sociais com o id &f{id}&c.` |
| `social-unknown` | `&cNão existe cabeça de redes sociais com o id &f{id}&c.` |
| `social-created` | `&aCabeça de redes sociais &f{id} &acolocada.` |
| `social-moved` | `&aCabeça de redes sociais &f{id} &amovida para onde você está.` |
| `social-removed` | `&aCabeça de redes sociais &f{id} &aremovida.` |
| `social-list-empty` | `&7Nenhuma cabeça de redes sociais. Use &f/redes criar&7.` |
| `social-list-header` | `&eCabeças de redes sociais do grupo &f{group} &e({count}):` |
| `social-list-line` | `&8- &f{id} &8{location}` |
| `placement-failed`, `spam-cooldown` | Compartilhadas com os NPCs e hologramas. |

## Pacotes

| Id | Destino | Política | Propósito |
|---|---|---|---|
| `placement.changed` | o grupo de servidores | padrão | Uma cabeça do grupo foi criada, movida ou removida; todo servidor do grupo recarrega do PostgreSQL. |

A troca de rede não envia pacote: cada servidor a lê do relógio.

## Dados

| Tabela | Conteúdo |
|---|---|
| `core.social_head_placements` | `server_group`, `id`, posição (`world`, `x`, `y`, `z`, `yaw`, `pitch`). Chave primária `(server_group, id)`. |

Criada pela migração do core `V6__create_social_head_placements.sql`. Salva e removida pelo `SavePlacementCommand` e pelo `RemovePlacementCommand` (`PlacementKind.SOCIAL_HEAD`), lida com a `FindPlacementsQuery`. As redes são configuração, não dados.

## Métricas

Nenhuma própria. Salvar, remover e carregar cabeças conta nas métricas de posicionamento listadas em [NPCs e hologramas](../npcs-and-holograms/README.pt-BR.md#métricas).

## Integrações

- [NPCs e hologramas](../npcs-and-holograms/README.pt-BR.md): o mesmo armazenamento, os mesmos ids e a mesma sincronização por grupo.
- A [API do core](../../development/core-api/README.pt-BR.md) fornece o `DisplayService` para o rótulo, o `SkullTextures.apply(Skull, Optional<String>)` para a textura da cabeça e o `BlockUpdates.resend(Block)` para mostrar a nova textura aos jogadores próximos.

## Critérios de aceitação

- [x] A config padrão tem cinco redes e troca a cada minuto.
- [x] A rede mostrada segue o relógio, então todo servidor mostra a mesma ao mesmo tempo.
- [x] Cada rede tem o seu rótulo, a sua textura opcional e a sua mensagem.
- [x] Um intervalo que não é positivo, ou nenhuma rede, é recusado.
- [x] A cabeça olha para onde olhava o membro da equipe que a colocou.
- [x] Uma cabeça sobrevive a ir e voltar do PostgreSQL, pode ser movida e removida, e os grupos ficam separados.
- [x] Uma cabeça colocada num lobby aparece em outro lobby do grupo, e removê-la em qualquer um a remove dos dois.
- [ ] A cabeça, o rótulo e a textura mudam juntos no jogo, os cliques na cabeça e no rótulo enviam a mensagem, e a cabeça não pode ser quebrada (conferido no jogo).

## Falhas e recuperação

- PostgreSQL indisponível quando um comando roda: a equipe vê `&cNão foi possível salvar agora. Tente novamente em instantes.` e nada muda.
- PostgreSQL indisponível ao ligar, ou Redis indisponível: igual aos [NPCs e hologramas](../npcs-and-holograms/README.pt-BR.md#falhas-e-recuperação).
- Mundo não carregado num servidor: aquela cabeça é pulada naquele servidor com um aviso.
- Internos do servidor ausentes (outra versão de servidor): a cabeça mostra a textura padrão e pode só mudar para os jogadores depois que eles recarregarem o chunk.

## Limitações conhecidas

- A cabeça é um bloco real do mundo do servidor. Se uma cabeça é removida ou movida enquanto um servidor do grupo está desligado, esse servidor fica com um bloco de cabeça comum onde ela estava (sem rótulo, sem clique, igual a uma decoração) até alguém removê-lo à mão. O mesmo acontece quando a seção `social-heads` é tirada da config de um servidor.
- Remover ou mover uma cabeça deixa ar: o bloco que estava ali antes da cabeça não volta.
- A rede mostrada é a mesma para todas as cabeças; uma cabeça não pode mostrar uma rede só.
- Servidores com relógios diferentes podem mostrar redes diferentes por um instante perto de cada troca.

## Pendências

- Os nomes e links da config padrão são provisórios (`muxe`): troque pelas contas reais da rede.
- O Instagram e o Twitter ainda não têm textura e mostram a cabeça de jogador padrão até uma ser definida.
