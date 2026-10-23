[English](0005-signed-binary-envelopes.md) | **Português**

# 0005. Envelopes binários com codecs explícitos, assinados com HMAC-SHA256

- **Status:** Aceita
- **Data:** 2026-10-23

## Contexto

Os pacotes precisam de um formato estável que suporte atualizações graduais, um conjunto fixo de metadados (ids, origem, horários, correlação) e proteção contra mensagens forjadas ou adulteradas por qualquer um que alcance o Redis.

## Decisão

- Todo pacote é embrulhado em um envelope binário com um número mágico, uma versão de protocolo, metadados e o payload.
- Os payloads são codificados por um `PacketCodec` explícito por tipo de pacote, com um id de tipo versionado.
- O envelope inteiro é assinado com HMAC-SHA256 usando um segredo compartilhado, com suporte a segredos anteriores durante a rotação.

## Alternativas consideradas

- **JSON com Gson:** legível, mas o Spigot 1.8 traz um Gson antigo, o mapeamento por reflexão esconde mudanças de layout e as mensagens ficam maiores.
- **Serialização Java:** insegura para desserializar dados vindos da rede.
- **Protocol Buffers:** evolução de schema de graça, mas exige um gerador de código e mais uma biblioteca de runtime para embutir.
- **Sem assinatura, confiando apenas nas ACLs do Redis:** uma única senha do Redis vazada permitiria injetar qualquer pacote.

## Consequências

- Os codecs têm poucas linhas por pacote e precisam ser mantidos simétricos; os testes cobrem as idas e voltas.
- Mudanças de layout viram novas versões do tipo, registradas lado a lado durante as atualizações.
- Todos os servidores precisam compartilhar o segredo de mensageria; um segredo errado aparece como rejeições `bad_signature`.
