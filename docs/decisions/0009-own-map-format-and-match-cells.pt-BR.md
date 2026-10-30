[English](0009-own-map-format-and-match-cells.md) | **Português**

# 0009. Os mapas usam um formato próprio e as partidas dividem um mundo em células

- **Status:** Aceita
- **Data:** 2026-10-30

## Contexto

Um servidor de jogo roda muitas partidas ao mesmo tempo, muitas vezes no mesmo mapa: um mapa popular pode ter dezenas de partidas simultâneas na rede. Cada partida precisa de uma cópia limpa do mapa, e a cópia tem de sumir quando a partida acaba.

No 1.8, criar um mundo por partida é lento na thread principal. Nosso código antigo mediu cerca de 67 ms por mundo com o SlimeWorldManager, e cada mundo a mais traz o próprio tick, mapa de chunks e rastreador de entidades. Copiar pastas de região no disco custa ainda mais e deixa arquivos para trás depois de uma queda.

Os mapas são preparados pela staff no setup da arena e copiados como arquivos para cada servidor de jogo.

## Decisão

- **Os mapas ficam no nosso formato, `.mxw`.** Cada chunk é guardado comprimido (Deflate) com os blocos, a luz, os biomas, o mapa de altura e as tile entities. Entidades não são guardadas. O mapa inteiro fica na memória comprimido, e um chunk só é descomprimido quando o servidor o carrega.
  - Formato: `MUXEMAP`, byte de versão, quantidade de chunks e, para cada chunk, `x`, `z`, tamanho e os bytes comprimidos (`MapFiles`).
  - Uma pasta de mundo (regiões Anvil `.mca`) é importada com o `AnvilImporter`, sem servidor rodando.
- **Os mundos recebem os chunks da memória, nunca do disco.** O `MuxeWorlds` cria um mundo vazio e, no `WorldInitEvent`, troca o carregador de chunks dele pelo `SourceChunkLoader`. Esse carregador monta os chunks do servidor direto do `ChunkData` e devolve os chunks salvos para uma `ChunkSource`. O mundo nunca grava arquivos de região, e a pasta dele é apagada quando ele é destruído.
- **Todas as partidas de um servidor dividem um mundo, separado em células** (`MatchCells`).
  - Uma célula é um quadrado de `sizeChunks` (64 por padrão, 1024 blocos) numa grade.
  - Cada partida recebe uma célula livre, e o mapa é servido em volta do centro dela.
  - Os chunks que uma partida altera ficam guardados comprimidos na célula quando o servidor os descarrega, e voltam enquanto a partida durar.
  - Quando a partida acaba, os chunks da célula são descarregados sem salvar e a célula é liberada, então a próxima partida vê o mapa original.
- **O setup da arena edita um mundo separado**, ligado a um `EditableMap`. Salvar o mundo grava cada chunk de volta nele, e o setup escreve o arquivo `.mxw` a partir dele.
- As classes do servidor são usadas diretamente (`muxe.server-internals`), não por reflexão, porque esse código roda para cada chunk.

## Alternativas consideradas

- **SlimeWorldManager:** um plugin de terceiros que precisa estar instalado em cada servidor e cria um mundo por partida, que é justamente o custo que queremos evitar.
- **Um mundo por partida, carregado do nosso formato:** isolamento mais simples, mas cada mundo ainda custa o tempo de criação e o próprio tick.
- **Colar o mapa bloco a bloco num mundo compartilhado:** milhares de alterações de bloco e atualizações de luz na thread principal a cada partida.
- **Guardar os mapas no PostgreSQL:** a staff escolheu arquivos copiados para cada servidor.

## Consequências

Medido no PandaSpigot 226 com um mapa de quatro times e 107 chunks:

| Medida | Resultado |
|---|---|
| Arquivo do mapa | 101 KB, contra 952 KB de arquivos de região |
| Criar um mundo | 22 ms |
| Vinte partidas | 2.140 chunks carregados em 409 ms, cerca de 0,19 ms por chunk |
| Resetar uma partida | 2 ms |
| Fidelidade | 0 diferenças em 199.826 blocos conferidos, e as tile entities mantidas |

- As partidas precisam rotear todo evento pela célula. Um jogador, bloco ou entidade pertence à partida cuja célula o contém.
- As células precisam ser maiores que o mapa mais a distância de visão. O `MatchCells.fits` recusa um mapa largo demais para a célula e a margem configuradas.
- As configurações do mundo valem para todas as partidas do servidor, como o horário e o clima.
- O carregador depende das classes internas do 1.8: `Chunk`, `ChunkSection`, `TileEntity`, NBT e os campos privados `ChunkProviderServer.chunkLoader` e `Chunk.q` (modificado). Uma nova versão do servidor exige atualizá-los.
- Entidades colocadas no mapa (suportes de armadura, molduras, pinturas) não são mantidas.
