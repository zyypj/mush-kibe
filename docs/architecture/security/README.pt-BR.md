[English](README.md) | **Português**

# Segurança da rede

Como a rede protege seus servidores, serviços e jogadores. O princípio: os jogadores só alcançam o proxy, todo o resto é privado, e todo segredo fica fora do repositório.

## O que fica exposto

| Serviço | Acessível pelos jogadores | Acessível por |
|---|---|---|
| Velocity | Sim, é a única porta de entrada pública | Todos |
| Servidores Spigot | Não | Só o proxy |
| PostgreSQL | Não | Só os servidores da rede |
| Redis | Não | Só os servidores da rede |
| Endpoints de métricas | Não | Só o sistema de monitoramento |

## Proxy e servidores de jogo

- Servidores Spigot 1.8 não suportam o modern forwarding do Velocity, então o proxy usa **legacy forwarding (BungeeCord)**, ou **BungeeGuard** quando o software do servidor suporta.
- O legacy forwarding não tem assinatura. **As portas dos servidores de jogo precisam estar fechadas para a internet** no firewall ou ligadas a uma rede privada. Um servidor Spigot acessível pela internet deixa qualquer pessoa entrar como qualquer jogador.
- O `spigot.yml` tem `settings.bungeecord: true` em todo servidor de jogo.

## Autenticação

A rede aceita jogadores sem conta do Minecraft (modo offline), então o proxy não pode confiar só no nome de usuário. O `velocity.toml` roda com `online-mode = false`, e o proxy decide a cada conexão:

- **Nomes premium** (uma conta premium existente, ou qualquer nome que existe na Mojang, mesmo que uma conta pirata o tenha registrado aqui antes) são forçados ao modo online: o Velocity verifica a sessão com a Mojang, então um cliente pirata não consegue usar um nome premium. Jogadores premium são autenticados sem senha.
- **Os nomes seguem a identidade verificada pela Mojang:** um login premium toma o seu nome de qualquer outra conta, e um jogador premium que trocou de nome mantém a mesma conta, porque o UUID da Mojang não muda. Veja [Contas](../../features/accounts/README.pt-BR.md#dono-do-nome).
- **Nomes piratas** vão para um servidor de auth, resolvem um código desenhado com hologramas (anti-bot) e depois se registram ou fazem login com senha.
- **Sessões de login:** depois de um login com senha, o proxy guarda no Redis, por 24 horas, o SHA-256 do id do jogador e do IP. Uma reconexão da mesma conta, do mesmo IP, dentro desse tempo é autenticada sem senha e não renova a sessão. O IP puro nunca é guardado.
- Se a Mojang não puder ser consultada, nomes novos são recusados em vez de serem registrados como piratas.
- Até um jogador ser autenticado, o proxy **nega toda conexão a um servidor fora do grupo de auth** e ignora pacotes `player.connect` desse jogador.
- O proxy só aceita `auth.authenticated` de uma origem que seu diretório de servidores conhece como um servidor `AUTH` do grupo de auth.
- O estado de autenticado é esquecido quando o jogador desconecta.
- Nomes que diferem de uma conta existente só em maiúsculas/minúsculas são recusados no login, porque as identidades do modo offline são derivadas do nome exato.

Senhas:

- Hash com **PBKDF2-HMAC-SHA256**, um salt aleatório de 16 bytes por senha e 600.000 iterações por padrão (`security.pbkdf2-iterations`). Aumentar as iterações atualiza cada hash no próximo login bem-sucedido.
- Comparadas em tempo constante. Nunca registradas em log: comandos e records que guardam senhas sobrescrevem `toString`.
- **Defina `commands.log: false` no `spigot.yml` do servidor de auth**, senão o Spigot imprime todo comando, incluindo `/login <senha>`, no console e nos logs.
- Senhas erradas são limitadas por jogador (5 a cada 5 minutos por padrão); o jogador é expulso ao atingir o limite.
- Jogadores não autenticados só podem andar, digitar o código do desafio e usar `/logar` e `/registrar`. Eles não podem falar no chat, usar outros comandos, interagir com blocos ou entidades, construir nem ver outros jogadores.

## Pacotes

- Todo envelope é assinado com HMAC-SHA256 usando `messaging.secret`. Envelopes com assinatura inválida são rejeitados antes de serem lidos. Veja [Mensageria](../messaging/README.pt-BR.md#assinaturas).
- Envelopes têm tamanho máximo e expiração, o que limita replay e abuso de memória.
- **Acesso ao Redis mais o segredo significa controle da rede.** Os dois são protegidos: o Redis é privado e exige autenticação; o segredo só é fornecido pelo ambiente.

## PostgreSQL

- Escuta só na rede privada e só aceita conexões dos servidores da rede.
- As permissões são separadas: os servidores usam um usuário que lê e escreve dados mas não altera o schema; as migrações rodam com outro usuário (`database.run-migrations: false` nos servidores que não migram). Nenhum dos dois é superusuário.

## Redis

- Escuta só na rede privada, com `requirepass` ou usuários de ACL.
- Com ACLs, cada serviço tem seu próprio usuário, limitado às chaves e canais `muxe:*`.

## Segredos

- Senhas e o segredo de mensageria nunca são commitados e nunca são escritos em texto puro na configuração padrão.
- Os valores de configuração usam `env:NAME` para ler uma variável de ambiente ou `file:/path` para ler um arquivo, como um secret do Docker ou do Kubernetes.
- O `.env` é ignorado pelo git; o `.env.example` documenta as variáveis sem valores.
- Um segredo que foi exposto, mesmo que por pouco tempo, é trocado. O segredo de mensageria pode ser rotacionado sem downtime com `messaging.previous-secrets`.

## Tráfego entre máquinas

Enquanto todos os serviços rodam na mesma máquina, o tráfego fica local. Quando PostgreSQL, Redis ou os servidores rodarem em máquinas diferentes, o tráfego usa TLS (`redis.ssl`, `sslmode` na URL JDBC), a menos que passe por uma rede privada criptografada.
