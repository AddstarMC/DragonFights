# DragonFights Addon
[![Discord](https://img.shields.io/discord/272499714048524288.svg?logo=discord)](https://discord.bentobox.world)
[![Build Status](https://ci.codemc.io/buildStatus/icon?job=BONNePlayground/DragonFights)](https://ci.codemc.io/job/BONNePlayground/job/DragonFights/)

DragonFights addon for BentoBox — per-island Ender Dragon fights in the End.

**Requirements:** Paper 1.21.11, Java 21, BentoBox.

## Installation

1. Put the addon jar in BentoBox’s `addons` folder.
2. Restart the server.
3. Adjust `DragonFights/config.yml` as needed.
4. Restart again if you changed the config.

## Usage

Works only in game modes that have end islands enabled. No exit portal is required in the blueprint; placing an End Crystal on the centre bedrock will generate one.

To summon a dragon, place 4 end crystals on the centre block on each side of the end trophy. This starts the summoning sequence.

## Dragon customisation

In `config.yml` under `battle.dragon-characteristics` you can define dragon variants. One is chosen at random per fight.

- **Format:** `COLOUR:HEALTH:SPEED`
- **COLOUR:** Glow and boss bar colour — `PINK`, `BLUE`, `RED`, `GREEN`, `YELLOW`, `PURPLE`, `WHITE`
- **HEALTH:** Max health (vanilla default 200)
- **SPEED:** Speed multiplier (1.0 = vanilla)

Boss bar colour and style can be set globally under `boss-bar` in the config.

## Placeholders

Use with BentoBox’s placeholder system; they are prefixed by the game mode (e.g. `BSkyBlock_dragonfights_kills`).

| Placeholder | Description |
|-------------|-------------|
| `%<gamemode>_dragonfights_kills%` | Dragons killed on the user’s island |
| `%<gamemode>_dragonfights_visited_kills%` | Dragons killed on the island the user is on |
| `%<gamemode>_dragonfights_alive%` | Whether a dragon is currently alive on the user’s island |
| `%<gamemode>_dragonfights_visited_alive%` | Whether a dragon is alive on the island the user is on |

## Rewards

- **Advancements** (config: `advancements`): Grant on first summon, resummon, to the killer, and to everyone in the End when the dragon dies. Use standard advancement IDs and criteria.
- **Dragon egg** (config: `dragon-egg`): Optionally drop on first kill; subsequent kills use a configurable chance (e.g. `drop-chance: 0.2` for 20%).

## Configuration

See [config.yml](https://github.com/BONNePlayground/DragonFights/blob/develop/src/main/resources/config.yml) for the full file and comments. Main options: battle (towers, start-on-join, music, fog), dragon characteristics, advancements, and dragon-egg behaviour.

## FAQ

1. **End trophy created but nothing happens.**  
   Ensure the game mode has end islands enabled and the setup is correct. If it still fails, report a bug.

2. **Dragon disappeared when I teleported to the End.**  
   Add `ENDER_DRAGON` to the game mode’s “remove-mobs-whitelist” so the dragon isn’t cleared.

3. **No obsidian towers.**  
   Towers are generated when a player starts a dragon fight.

4. **Where to get the original addon?**  
   [Releases](https://github.com/BONNePlayground/DragonFights/releases) — dev builds: [CI](https://ci.codemc.io/job/BONNePlayground/job/DragonFights/).

## Compatibility

- Paper **1.21.11**
- Java **21**
- BentoBox 1.23.0 or compatible
