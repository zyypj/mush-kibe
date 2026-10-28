**English** | [Português](README.pt-BR.md)

# Commands

Every Bukkit command of Muxe is a plain class annotated with the command framework of `core:bukkit` (package `br.com.muxe.core.bukkit.command.framework`). The framework reads the annotations once, when the command is registered, and then routes subcommands, checks the sender and the permissions, turns the words typed into typed arguments, completes with tab and answers mistakes with the messages from `config.yml`.

## A command

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

The class keeps its dependencies in the constructor, as any other class. Handlers are public methods that return nothing.

## Annotations

| Annotation | Where | What it does |
|---|---|---|
| `@Command(name, aliases, description, usage)` | class | The command. `usage` is the key of the message sent when the command is used wrong (see [Usage](#usage)). `description` is shown by Bukkit's `/help` and stays in English. |
| `@Command(...)` | method | A command made of that single method, for classes that hold several small commands (`AuthCommands` holds `/login` and `/register`). A class uses `@Command` either on the class or on its methods, never both. |
| `@Default` | method | Runs when no subcommand matches, such as `/loja` opening the shop. |
| `@Subcommand(value, aliases)` | method | A subcommand. `value` can have several words (`"caixa criar"`); `aliases` are other names for its last word (`@Subcommand(value = "inicio", aliases = "início")`). Words match ignoring case. |
| `@Permission(value)` | class or method | On the class, every handler needs it and Bukkit hides the command from players without it. On a method, that handler also needs it. |
| `@Arg(value, defaultValue)` | parameter | `value` is the name shown in the generated usage (the Java parameter name otherwise). With `defaultValue` the argument is optional and missing words take that value. |
| `@Greedy` | last `String` parameter | Takes the rest of the line, spaces included. |
| `@Suggest(value, provider)` | parameter | Tab completion for this argument: fixed words, a named provider, or both. Without it, the argument type completes itself. |
| `@Suggestions(name)` | method | A provider declared in the command class itself, returning a collection of strings and taking nothing or the sender. |
| `@Usage` | method | Replaces the usage reply of the class, for usages that need placeholders or checks (`/jogar` lists the games, `/loja` hides its usage from players). |

## Senders

The first parameter of a handler, when it is a sender, says who may run it:

| Type | Who | Message for anyone else |
|---|---|---|
| `CommandSender` | everyone | |
| `Player` | players | `players-only` |
| `ConsoleCommandSender` | the console | `console-only` |

When a group is used without a subcommand and none of its handlers accepts the sender, the sender gets that message instead of the usage.

## Arguments

| Java type | Accepts | Error message |
|---|---|---|
| `String` | any word | |
| `int`, `Integer`, `long`, `Long` | whole numbers | `command-invalid-number` |
| `double`, `Double` | numbers, with `.` or `,` | `command-invalid-number` |
| `boolean`, `Boolean` | `sim`, `s`, `true`, `on`, `não`, `nao`, `n`, `false`, `off` | `command-invalid-option` |
| any `enum` | the constant name ignoring case, `_` written as `-` | `command-invalid-option` |
| `Player` | the exact name of an online player the sender can see | `command-player-offline` |
| `Optional<T>` | any of the above, optional: empty when missing | as `T` |

Optional arguments come after the required ones, and a `@Greedy` argument is the last one.

### Custom types

An `ArgumentType<T>` turns a word into a value or rejects it with a message key and placeholders, and may complete itself:

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

Types are added to `Commands` before the commands that use them: `commands.argument(NpcPlacement.class, argument)`. The core registers `NpcPlacement`, `HologramPlacement`, `RegisterPlacement` and `SocialHeadPlacement` with `PlacementArgument.registerAll`, so `/npc mover <id>` receives the NPC itself and completes the existing ids.

## Tab completion

- Subcommands are completed when the sender may run at least one handler under them. Their aliases appear once something is typed.
- Arguments complete from `@Suggest` or from their type. Text, numbers and plain `String` arguments complete nothing, so passwords are never completed.
- Everything is filtered by what was typed, ignoring case, and sorted.
- Shared providers are registered by name: `commands.suggestions("games", sender -> games)`. `SuggestionRegistry.PLAYERS` (`"players"`) is always there and lists the online players the sender can see.

## Order of the checks

1. Words that name subcommands are followed as deep as they go; the remaining words are the arguments.
2. The sender type, then the permissions.
3. The number of arguments: missing or extra words show the usage.
4. Each argument is resolved in order; the first one rejected stops with its message.
5. The handler runs. An exception is logged and the sender gets `command-failed`.

## Usage

When a command is used with the wrong words, the reply is, in this order:

1. the `@Usage` method of the class, if there is one;
2. the message under `usage`, sent line by line, so a YAML list becomes several lines;
3. a generated usage: `command-usage` with the syntax of that subcommand (`/warp criar <nome> [custo]`), or `command-usage-header` and one `command-usage-line` for each subcommand the sender may use.

## Registering

```java
Commands commands = Commands.of(plugin, messages);
commands.argument(NpcPlacement.class, npcs);
commands.suggestions("games", sender -> games);
commands.register(new NpcCommand(...), new HologramCommand(...));
```

- Commands are registered straight in the server's command map (taken from `CraftServer#getCommandMap`), so `plugin.yml` no longer lists them. `plugin.yml` keeps the `permissions:` section, which sets who has each permission by default.
- The fallback prefix is the plugin name in lowercase (`/muxe-core:npc`). If another plugin already owns the name, a warning is logged and the command answers on the prefixed label.
- When the plugin is disabled, `Commands` removes every label it registered, so nothing stale is left after a `/reload`. `commands.unregisterAll()` does the same on demand.
- If the server has no reachable command map, `Commands` falls back to the `commands:` entries of `plugin.yml`.
- Annotations are checked when registering: a wrong declaration (a required argument after an optional one, a type without `ArgumentType`, an unknown provider, two handlers on the same path) fails the plugin start with a clear message.

## Messages

Every plugin that registers commands needs these keys under `messages` in its `config.yml`. Plugins that merge `MuxeCore.get().messages()` get them from Muxe-Core.

| Key | Default |
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

## Testing

Handlers are plain methods, and the framework runs without a server: build `new Commands(ArgumentTypes.basic(), new SuggestionRegistry(), messages, logger, registrar)` with a `CommandRegistrar` that keeps the `RegisteredCommand`s, then call `run(sender, label, args)` and `complete(sender, args)` with proxy senders. `core:bukkit` has `TestCommands` and `FakeSender` in its tests for that.
