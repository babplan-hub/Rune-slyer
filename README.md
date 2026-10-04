# Rune Slayer

A Java Swing top-down action roguelite. Run commands from the project folder because the game loads images from `res/` using relative paths.

## Run the game

```sh
javac -d out src/*.java
java -cp out Main
```

The project uses Java 11+ and Swing/AWT and does not need Maven, Gradle, or external libraries.

## Controls

| Key | Action |
| --- | --- |
| Enter | Start the game; restart after victory or defeat |
| W, A, S, D / Arrow keys | Move |
| Space | Attack 1 |
| F | Attack 2 |
| E | Form skill |
| Q | Dash attack |
| J | Use a Potion (heals 30 HP) |
| K | Evolve |
| R | Summon (Form 2 and Boss 1 defeated) |
| X | Enter the next boss dungeon or return to the forest |

## Progression

1. Farm monsters in the forest. They respawn after 3 seconds and can drop Potions or Runes.
2. Evolve from Form 1 to Form 2 for 5 Runes.
3. Evolve from Form 2 to Form 3 for 10 more Runes.
4. Press X to enter Boss Room 1. Leaving before defeating the boss resets that boss's HP.
5. Defeating Boss 1 unlocks Summon. Summon requires Form 3, deals 35 damage, and returns after an 8-second recovery if defeated.
6. Return to the forest and press X to enter each next boss room. Bosses 1–4 use their matching room and sprite assets.
7. Defeating Boss 4 completes the run. Enter starts a fresh run after the result screen.

## Bosses

Each boss has a 450 ms attack windup, so move away when the boss starts its attack. Later bosses have more HP, move faster, and attack harder or more often. Boss 4 also uses the skill animation.

The player has 200 / 300 / 400 HP for Forms 1 / 2 / 3; evolving increases both maximum and current HP by 100. Forms 2 and 3 lifesteal 10% and 20% of damage dealt. E-skill damage is 35 / 50 / 120 for Forms 1 / 2 / 3; monsters have 100 HP, so the skill takes three hits, two hits, or one hit respectively. Form 3's E skill hits an area around the player. Boss HP is 1,000 / 1,250 / 1,500 / 2,000.

| Boss | HP | Contact attack | Attack interval |
| --- | ---: | ---: | ---: |
| Flame Knight | 1,000 | 18 | 1.2 s |
| Frost Warden | 1,250 | 22 | 1.1 s |
| Storm Reaper | 1,500 | 26 | 1.0 s |
| Demon King | 2,000 | 32 (skill: 44) | 0.9 s |

## Project files

- `src/Main.java`, `src/Game.java`: window and 60 FPS game loop
- `src/GamePanel.java`: maps, game state, keyboard, monsters, drops, progression, and HUD
- `src/Player.java`: movement, combat, HP, items, and evolutions
- `src/Monster.java`, `src/Boss.java`, `src/Summon.java`: enemy and companion behavior
- `src/Item.java`, `src/GameMap.java`: item and map drawing helpers
- `res/`: player, monster, boss, item, and map artwork

## Character slicing and verification

All nine current character PNGs have independent `.png.sprites` annotations beside them. Source rows have different pose counts; they are **not** sliced as a shared 16×8 grid. `SpriteSheet` extracts masked, tight crops, preserves each pose's foot pivot, and renders with a constant per-character scale. Original PNGs are unchanged.

```sh
javac -d out src/*.java tools/SpriteAudit.java tools/SpriteRegressionTest.java
java -Djava.awt.headless=true -cp out SpriteAudit
java -Djava.awt.headless=true -cp out SpriteRegressionTest
```

The audit exports individual transparent PNGs, contact sheets, source-boundary overlays, and a CSV with pivots. Orange `[source]` labels on contact sheets mark poses omitted from gameplay, including alternate facing directions and ambiguous overlapping source effects.

See [sprite annotation notes](docs/sprites.md) for the format, reviewed exceptions, and replacement workflow. A changed source PNG fails with a clear error until its annotation is reviewed again, instead of silently reverting to incorrect grid cuts.
# Rune-slyer
