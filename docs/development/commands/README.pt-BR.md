[English](README.md) | **Português**

# Comandos

Todo comando Bukkit do Muxe é uma classe comum anotada com o framework de comandos do `core:bukkit` (pacote `br.com.muxe.core.bukkit.command.framework`). O framework lê as anotações uma vez, quando o comando é registrado, e depois encaminha os subcomandos, confere quem enviou e as permissões, transforma as palavras digitadas em argumentos tipados, completa com tab e responde aos erros com as mensagens do `config.yml`.

## Um comando

```java
@Command(name = "holograma", aliases = {"hologram", "holo"}, description = "Manages holograms.", usage = "hologram-usage")
@Permission("muxe.hologram")
public final class HologramCommand {

    @Subcommand("criar")
    public void create(Player player, @Arg("id") String id, @Arg("texto") @Greedy String text) {
        ...
    }

    @Subcommand("linha")
    public void changeLine(Player player, @Arg("id") HologramPlacement hologram, @Arg("número") int number, @Arg("texto") @Greedy String text) {
        ...
    }

    @Subcommand("listar")
    public void list(Player player) {
        ...
    }
}
```

A classe recebe as suas dependências no construtor, como qualquer outra classe. Os handlers são métodos públicos que não retornam nada.

## Anotações

| Anotação | Onde | O que faz |
|---|---|---|
| `@Command(name, aliases, description, usage)` | classe | O comando. `usage` é a chave da mensagem enviada quando o comando é usado errado (veja [Uso](#uso)). `description` aparece no `/help` do Bukkit e fica em inglês. |
| `@Command(...)` | método | Um comando feito só daquele método, para classes que juntam vários comandos pequenos (`AuthCommands` tem o `/login` e o `/register`). Uma classe usa `@Command` na classe ou nos métodos, nunca nos dois. |
| `@Default` | método | Roda quando nenhum subcomando corresponde, como o `/loja` abrindo a loja. |
| `@Subcommand(value, aliases)` | método | Um subcomando. `value` pode ter várias palavras (`"caixa criar"`); `aliases` são outros nomes para a última palavra (`@Subcommand(value = "inicio", aliases = "início")`). As palavras são comparadas sem diferenciar maiúsculas. |
| `@Permission(value)` | classe ou método | Na classe, todo handler exige a permissão e o Bukkit esconde o comando de quem não a tem. No método, aquele handler também a exige. |
| `@Arg(value, defaultValue)` | parâmetro | `value` é o nome mostrado no uso gerado (senão, o nome do parâmetro Java). Com `defaultValue` o argumento é opcional e, quando falta, recebe esse valor. |
| `@Greedy` | último parâmetro `String` | Pega o resto da linha, com os espaços. |
| `@Suggest(value, provider)` | parâmetro | Tab deste argumento: palavras fixas, um provedor com nome, ou os dois. Sem ela, o próprio tipo do argumento completa. |
| `@Suggestions(name)` | método | Um provedor declarado na própria classe do comando, que retorna uma coleção de strings e não recebe nada ou recebe quem enviou. |
| `@Usage` | método | Substitui a resposta de uso da classe, para usos que precisam de placeholders ou de verificações (`/jogar` lista os jogos, `/loja` esconde o uso de jogadores). |

## Quem envia

O primeiro parâmetro de um handler, quando é quem envia, diz quem pode rodá-lo:

| Tipo | Quem | Mensagem para os outros |
|---|---|---|
| `CommandSender` | todos | |
| `Player` | jogadores | `players-only` |
| `ConsoleCommandSender` | o console | `console-only` |

Quando um grupo é usado sem subcomando e nenhum dos seus handlers aceita quem enviou, essa mensagem é enviada no lugar do uso.

## Argumentos

| Tipo Java | Aceita | Mensagem de erro |
|---|---|---|
| `String` | qualquer palavra | |
| `int`, `Integer`, `long`, `Long` | números inteiros | `command-invalid-number` |
| `double`, `Double` | números, com `.` ou `,` | `command-invalid-number` |
| `boolean`, `Boolean` | `sim`, `s`, `true`, `on`, `não`, `nao`, `n`, `false`, `off` | `command-invalid-option` |
| qualquer `enum` | o nome da constante sem diferenciar maiúsculas, com `_` escrito como `-` | `command-invalid-option` |
| `Player` | o nome exato de um jogador online que quem enviou consegue ver | `command-player-offline` |
| `Optional<T>` | qualquer um dos anteriores, opcional: vazio quando falta | como `T` |

Argumentos opcionais vêm depois dos obrigatórios, e um argumento `@Greedy` é o último.

### Tipos próprios

Um `ArgumentType<T>` transforma uma palavra em um valor ou a rejeita com uma chave de mensagem e placeholders, e pode se completar:

```java
public final class PlacementArgument<T extends Placement> implements ArgumentType<T> {

    @Override
    public ArgumentResult<T> parse(CommandSender sender, String input) {
        String id = input.toLowerCase(Locale.ROOT);
        return find(id).map(ArgumentResult::of).orElseGet(() -> ArgumentResult.rejected("npc-unknown", "id", id));
    }

    @Override
    public Collection<String> suggest(CommandSender sender) {
        return ids();
    }
}
```

Os tipos são adicionados ao `Commands` antes dos comandos que os usam: `commands.argument(NpcPlacement.class, argument)`. O core registra `NpcPlacement`, `HologramPlacement`, `RegisterPlacement` e `SocialHeadPlacement` com `PlacementArgument.registerAll`, então o `/npc mover <id>` recebe o próprio NPC e completa os ids existentes.

## Tab

- Os subcomandos são completados quando quem enviou pode rodar pelo menos um handler abaixo deles. Os aliases aparecem depois que algo é digitado.
- Os argumentos completam pelo `@Suggest` ou pelo tipo. Textos, números e argumentos `String` simples não completam nada, então senhas nunca são completadas.
- Tudo é filtrado pelo que foi digitado, sem diferenciar maiúsculas, e ordenado.
- Provedores compartilhados são registrados por nome: `commands.suggestions("games", sender -> games)`. O `SuggestionRegistry.PLAYERS` (`"players"`) sempre existe e lista os jogadores online que quem enviou consegue ver.

## Ordem das verificações

1. As palavras que nomeiam subcomandos são seguidas até onde vão; as palavras que sobram são os argumentos.
2. O tipo de quem enviou, depois as permissões.
3. A quantidade de argumentos: palavras faltando ou sobrando mostram o uso.
4. Cada argumento é resolvido em ordem; o primeiro rejeitado para com a sua mensagem.
5. O handler roda. Uma exceção vai para o log e quem enviou recebe `command-failed`.

## Uso

Quando um comando é usado com as palavras erradas, a resposta é, nesta ordem:

1. o método `@Usage` da classe, se houver;
2. a mensagem em `usage`, enviada linha por linha, então uma lista YAML vira várias linhas;
3. um uso gerado: `command-usage` com a sintaxe daquele subcomando (`/warp criar <nome> [custo]`), ou `command-usage-header` e um `command-usage-line` para cada subcomando que quem enviou pode usar.

## Registrando

```java
Commands commands = Commands.of(plugin, messages);
commands.argument(NpcPlacement.class, npcs);
commands.suggestions("games", sender -> games);
commands.register(new NpcCommand(...), new HologramCommand(...));
```

- Os comandos são registrados direto no mapa de comandos do servidor (obtido de `CraftServer#getCommandMap`), então o `plugin.yml` não os lista mais. O `plugin.yml` mantém a seção `permissions:`, que define quem tem cada permissão por padrão.
- O prefixo reserva é o nome do plugin em minúsculas (`/muxe-core:npc`). Se outro plugin já tiver o nome, um aviso vai para o log e o comando responde no rótulo com prefixo.
- Quando o plugin é desativado, o `Commands` remove todos os rótulos que registrou, então nada fica sobrando depois de um `/reload`. O `commands.unregisterAll()` faz o mesmo sob demanda.
- Se o servidor não tiver um mapa de comandos acessível, o `Commands` usa as entradas `commands:` do `plugin.yml`.
- As anotações são conferidas no registro: uma declaração errada (argumento obrigatório depois de um opcional, tipo sem `ArgumentType`, provedor desconhecido, dois handlers no mesmo caminho) faz o plugin falhar ao iniciar com uma mensagem clara.

## Mensagens

Todo plugin que registra comandos precisa destas chaves em `messages` no seu `config.yml`. Plugins que juntam o `MuxeCore.get().messages()` as recebem do Muxe-Core.

| Chave | Padrão |
|---|---|
| `no-permission` | `&cVocê não possui permissão para usar este comando.` |
| `players-only` | `&cApenas jogadores podem usar este comando.` |
| `console-only` | `&cApenas o console pode usar este comando.` |
| `command-usage` | `&cUso: &f{usage}` |
| `command-usage-header` | `&eUso do /{command}:` |
| `command-usage-line` | `&f{usage}` |
| `command-invalid-number` | `&cO valor &f{input} &cnão é um número válido.` |
| `command-invalid-option` | `&cOpção inválida: &f{input}&c. Use: &f{options}&c.` |
| `command-player-offline` | `&cO jogador &f{input} &cnão está online.` |
| `command-failed` | `&cNão foi possível executar este comando agora.` |

## Testes

Os handlers são métodos comuns, e o framework roda sem servidor: monte `new Commands(ArgumentTypes.basic(), new SuggestionRegistry(), messages, logger, registrar)` com um `CommandRegistrar` que guarde os `RegisteredCommand`, e chame `run(sender, label, args)` e `complete(sender, args)` com remetentes proxy. O `core:bukkit` tem `TestCommands` e `FakeSender` nos testes para isso.
