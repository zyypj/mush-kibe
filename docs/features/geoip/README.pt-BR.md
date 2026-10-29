[English](README.md) | **Português**

# Localização da conta (GeoIP)

Depois de cada login, o proxy encontra o país, a região, a cidade e a rede aproximados do endereço do jogador nas bases gratuitas GeoLite2 da MaxMind e os salva na conta, onde o `/account` os mostra. Fica no pacote `br.com.muxe.core.proxy.geoip` do `core:proxy`.

## Histórias de usuário

- Como membro da equipe, quero ver de onde uma conta costuma se conectar, para perceber contas compartilhadas ou roubadas.
- Como operador, quero que as bases sejam baixadas e atualizadas sozinhas, para só colar as chaves da MaxMind uma vez.
- Como jogador, quero que meu endereço em si nunca seja guardado.

## Como funciona

- Depois de um login bem-sucedido (`PostLoginEvent`), o proxy pega o endereço da conexão e, no executor de I/O, nunca no caminho do login:
  - ignora endereços privados, de loopback, link-local, de NAT de operadora, multicast e reservados;
  - procura o endereço na GeoLite2-City e na GeoLite2-ASN;
  - envia `SaveAccountLocationCommand` com o país, a região (primeira subdivisão) e a cidade em português (`pt-BR`, com inglês como reserva), e a rede como `AS<número> <organização>` (por exemplo `AS28573 Claro NXT Telecomunicacoes Ltda`).
- Nada é salvo quando o endereço não é encontrado. Uma busca que falha registra um aviso com o id do jogador e o tipo do erro, nunca o endereço.
- O endereço em si nunca é guardado nem registrado em log. Só os nomes acima ficam salvos, substituindo os anteriores.

### Bases

- O proxy baixa a `GeoLite2-City` e a `GeoLite2-ASN` do endpoint oficial da MaxMind (`https://download.maxmind.com/geoip/databases/<edição>/download?suffix=tar.gz`, com autenticação HTTP básica usando o id da conta e a chave de licença, e depois o redirecionamento que ele responde, sem a chave), extrai o `.mmdb` do `tar.gz` e o guarda em `plugins/muxe-proxy/geoip/`.
- Na inicialização e a cada 6 horas o proxy confere os arquivos; um arquivo mais velho que `geoip.refresh-after` (7 dias) é baixado de novo. O arquivo novo só substitui o antigo depois de um download completo, então uma atualização que falha mantém a base anterior.
- Se os downloads falharem, o proxy continua funcionando e as localizações ficam como estão.

### Configurando as chaves da MaxMind

1. Crie uma conta gratuita em maxmind.com, abra **Manage License Keys** e gere uma chave de licença. Anote o **account id** mostrado na mesma página.
2. **No arquivo de configuração (forma principal no painel Pterodactyl):** abra `plugins/muxe-proxy/config.yml` no gerenciador de arquivos do painel e escreva os valores:

   ```yaml
   geoip:
     enabled: true
     account-id: "123456"
     license-key: "sua-chave-de-licenca"
     refresh-after: 7d
   ```

   Reinicie o proxy. O arquivo guarda um segredo: não o compartilhe, não o commite e não o cole em chamados.
3. **Alternativa, variáveis de ambiente:** onde o host permitir adicionar variáveis, escreva `account-id: env:MUXE_MAXMIND_ACCOUNT_ID` e `license-key: env:MUXE_MAXMIND_LICENSE_KEY` e defina essas variáveis para o processo do proxy. `file:/caminho` também funciona, como em todo segredo.

Enquanto os dois valores estiverem vazios (o padrão enviado), o proxy registra uma vez `GeoIP is off: ...` e continua funcionando. A chave de licença nunca vai para o log: as credenciais a escondem no `toString`, e os erros de download só citam as chaves de configuração a conferir.

## Comandos

Nenhum. O `/account` nos servidores de jogo mostra a localização salva.

## Permissões

Nenhuma.

## Configuração

`plugins/muxe-proxy/config.yml` do proxy:

| Chave | Padrão | Descrição |
|---|---|---|
| `geoip.enabled` | `true` | Liga a busca quando as chaves estão definidas. |
| `geoip.account-id` | `""` | Id da conta MaxMind, escrito direto ou como `env:NOME` / `file:/caminho`. |
| `geoip.license-key` | `""` | Chave de licença MaxMind, escrita direto ou como `env:NOME` / `file:/caminho`. Segredo. |
| `geoip.refresh-after` | `7d` | Idade a partir da qual uma base é baixada de novo. |

## Pacotes

Nenhum.

## Dados

- As colunas de localização da conta (país, região, cidade, rede), gravadas pelo `SaveAccountLocationCommand`.
- `plugins/muxe-proxy/geoip/GeoLite2-City.mmdb` e `GeoLite2-ASN.mmdb`.

## Métricas

Nenhuma.

## Integrações

- MaxMind GeoLite2 por meio da biblioteca GeoIP2 Java (`com.maxmind.geoip2:geoip2`), embutida e realocada para `br.com.muxe.libs`.
- [Contas](../accounts/README.pt-BR.md): `SaveAccountLocationCommand` e `FindAccountDetailsQuery`.

## Critérios de aceitação

- [ ] Com chaves válidas, o proxy baixa as duas bases na primeira inicialização e um jogador de um endereço público ganha uma localização no `/account`.
- [ ] Um jogador vindo de `127.0.0.1` ou de uma rede privada não é buscado.
- [ ] Com as chaves vazias, o proxy registra `GeoIP is off` uma vez e os jogadores entram normalmente.
- [ ] Uma base com 7 dias é baixada de novo; um download quebrado mantém o arquivo antigo.
- [ ] Nem o endereço nem a chave de licença aparecem nos logs.

## Falhas e recuperação

- **MaxMind fora do ar ou chaves erradas:** um aviso por edição (`401`/`403` dizem quais chaves conferir); os arquivos existentes continuam em uso; a próxima verificação ocorre 6 horas depois.
- **Banco fora do ar ao salvar:** o comando falha, um aviso é registrado e o próximo login tenta de novo.

## Limitações conhecidas

- A GeoLite2 é aproximada: cidades podem estar erradas e redes móveis ou VPNs costumam mostrar a localização do provedor.
- Jogadores atrás de um proxy ou VPN mostram a localização desse serviço.
- A licença da MaxMind exige manter as bases atualizadas; a atualização semanal faz isso enquanto as chaves forem válidas.
