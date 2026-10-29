**English** | [Português](README.pt-BR.md)

# Legal and clean-room policy

Muxe is an independent project. It is inspired by the gameplay of classic Brazilian minigame networks, but it is written from scratch and has no affiliation with any of them, nor with Mojang Studios or Microsoft.

## What may enter this repository

- Code written by the contributor for Muxe.
- Libraries with licenses compatible with the project license, added through Gradle.
- Ideas about gameplay, described in your own words. Game mechanics in general are not protected, but their concrete expression (code, text, art, maps) is.
- Minecraft player skins for NPCs, referenced by their signed texture on Mojang's texture servers (`textures.minecraft.net`). Skins are shared openly by the Minecraft community, so the project owner allows them; the repository keeps only the signed reference, never the skin image.

## What must never enter this repository

- Code copied, decompiled or deobfuscated from another server's plugins, even partially or "rewritten" line by line.
- Leaked source code, databases, configuration or credentials of any network.
- Maps, builds, textures, sounds, logos, icons or other assets made by someone else without a license that allows it.
- Names, logos or trademarks of other networks, in code, configuration, messages or documentation.
- Minecraft's own code or assets.

A pull request that contains any of the above is closed, and the content is removed from the history when possible.

## Clean-room practice

When a feature is inspired by something seen on another server:

1. Describe the expected behavior as a user story, from the player's point of view, without looking at anyone's implementation.
2. Implement it from that description, following the Muxe architecture.
3. Write your own text and messages.

## Minecraft EULA and usage guidelines

A server running Muxe must follow the [Minecraft EULA](https://www.minecraft.net/eula) and the [Minecraft Usage Guidelines](https://www.minecraft.net/usage-guidelines). In particular, anything sold to players must not give gameplay advantages that the guidelines forbid. Features involving purchases document how they comply.

## License

Muxe is released under the [MIT License](../../LICENSE). Contributions are accepted under that license.
