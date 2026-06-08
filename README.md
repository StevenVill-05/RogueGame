# Dungeon Crawler — Code Reference

A turn-based roguelike built with JavaFX. This document walks through every source file in the order you would encounter it when running the game: startup → screens → gameplay → entities → map → persistence.

---

## Table of Contents

1. [Project Structure](#1-project-structure)
2. [Entry Point — `Main.java`](#2-entry-point--mainjava)
3. [UI Layer](#3-ui-layer)
   - [StartScreen.java](#31-startscreenjava)
   - [CharacterSelectScreen.java](#32-characterselectscreenjava)
   - [GameView.java](#33-gameviewjava)
4. [Core — Game Logic](#4-core--game-logic)
   - [GameState.java](#41-gamestatejava)
   - [ScoreManager.java](#42-scoremanagerjava)
5. [Entities — Characters](#5-entities--characters)
   - [Player.java](#51-playerjava)
   - [Warrior / Rogue / Mage](#52-warrior--rogue--mage)
   - [Skill.java](#53-skilljava)
6. [Entities — Enemies](#6-entities--enemies)
   - [Enemy.java](#61-enemyjava)
   - [HostileActions.java](#62-hostileactionsjava)
   - [Goblin / ArcherGoblin / Troll](#63-goblin--archergoblin--troll)
7. [Items](#7-items)
   - [Item.java](#71-itemjava)
8. [Map](#8-map)
   - [DungeonMap.java](#81-dungeonmapjava)
   - [Room.java](#82-roomjava)
   - [Tile.java](#83-tilejava)
9. [Resources](#9-resources)
10. [Build — `pom.xml`](#10-build--pomxml)
11. [Navigation Flow Diagram](#11-navigation-flow-diagram)

---

## 1. Project Structure

```
RogueGame/
├── pom.xml                            Maven build descriptor
├── highscores.db                      SQLite database (auto-created)
└── src/main/
    ├── java/game/
    │   ├── core/
    │   │   ├── Main.java              JavaFX application entry point
    │   │   ├── GameState.java         All gameplay logic (movement, combat, AI)
    │   │   └── ScoreManager.java      SQLite high-score persistence
    │   ├── ui/
    │   │   ├── StartScreen.java       Title / leaderboard screen
    │   │   ├── CharacterSelectScreen.java  Class picker screen
    │   │   └── GameView.java          Main game canvas + HUD rendering
    │   ├── entity/
    │   │   ├── characters/
    │   │   │   ├── Player.java        Base player class
    │   │   │   ├── Warrior.java       HP 20 / ATK 4 — tank subclass
    │   │   │   ├── Rogue.java         HP 12 / ATK 6 — striker subclass
    │   │   │   ├── Mage.java          HP 8  / ATK 8 — glass-cannon subclass
    │   │   │   └── Skill.java         Active-skill data + activation logic
    │   │   ├── hostile/
    │   │   │   ├── Enemy.java         Base enemy class
    │   │   │   ├── HostileActions.java  Hit-chance interface
    │   │   │   ├── Goblin.java        Melee, low HP
    │   │   │   ├── ArcherGoblin.java  Ranged attacker
    │   │   │   └── Troll.java         High HP tank enemy
    │   │   └── item/
    │   │       └── Item.java          Collectible floor items (POTION / GOLD)
    │   └── map/
    │       ├── DungeonMap.java        Procedural room+corridor generator
    │       ├── Room.java              Axis-aligned room rectangle
    │       └── Tile.java              WALL / FLOOR / STAIR enum
    └── resources/
        ├── fonts/                     alagard.ttf, Faith Collapsing.ttf, Jacquard12-Regular.ttf
        └── sprites/                   background.png, warrior2.png, rogue2.png, mage.png, floor.png, wall.png
```

---

## 2. Entry Point — `Main.java`

**Package:** `game.core`  
**Extends:** `javafx.application.Application`

`Main` owns the primary `Stage` and is responsible for one thing: wiring the three screens together. It never holds game data itself.

### Key design decisions

**Single-element array trick for lambdas.**
JavaFX event handlers must close over effectively-final references. To allow lambdas to reference `sceneHolder` and `showSelect` before they are assigned, both are declared as `Scene[1]` and `Runnable[1]` — one-element arrays whose _reference_ is final even though the element inside can change.

**Scene reuse across restarts.**
The first launch creates a `Scene`. Every subsequent restart (press R in game) calls `showSelect[0].run()` again, which swaps only the scene _root_ node — avoiding the overhead of destroying and recreating the entire JavaFX window.

**Screen navigation flow:**
```
showSelect[0].run()
  └─► new StartScreen(playerName -> ...)
        └─► new CharacterSelectScreen(playerName, chosenPlayer -> ...)
              └─► state.init(chosenPlayer)
                  new GameView(state, showSelect[0], stage)
                  scene.setRoot(gameView.getRoot())
```

**Attack callback wiring.**
After `GameView` is created, `state.setOnAttack(...)` is called to register a lambda that fires `gameView.triggerSwipe(...)` on the JavaFX thread via `Platform.runLater`. This decouples `GameState` (which has no JavaFX dependency) from animation concerns.

---

## 3. UI Layer

### 3.1 `StartScreen.java`

**Package:** `game.ui`  
**Extends:** `StackPane`

The first screen the player sees. Uses a `StackPane` with three layers stacked back-to-front:

| Layer (back → front) | Purpose |
|---|---|
| `bgRegion` | Fills the pane with `background.png` using `BackgroundSize` COVER mode |
| `dimOverlay` | 40% opaque black `Region` — improves text contrast over the image |
| `content` (`VBox`) | All interactive UI elements |

#### Background image loading
`background.png` is loaded via `getResourceAsStream("/sprites/background.png")`. The `BackgroundSize` constructor is called with `cover = true`, which scales the image to fill the entire pane (cropping if the aspect ratio differs) — the same behaviour as CSS `background-size: cover`. If the resource is missing an exception is caught and a solid fallback colour is used instead.

#### Text contrast strategy
All panel backgrounds use RGBA colours with ~82% opacity (`rgba(30,26,46,0.82)`). The title uses a drop-shadow technique: a darker copy of the `Text` node is placed 3 px right/down in a `StackPane`, then the real title sits on top — this gives a readable shadow without requiring JavaFX `DropShadow` effects on text, which can cause blur artefacts with bitmap fonts.

#### Title component
```
StackPane (titleStack)
├── titleShadow  ← same text, black 70% opacity, offset 3 px right+down
└── title        ← Jacquard 12, 120pt, lavender #e8e0f0
```

#### Score board (`buildScoreBoard`)
Calls `ScoreManager.load()` and formats up to 10 rows using `String.format` with fixed-width columns. The first-place row is coloured gold; all others use the standard text colour.

#### Name validation (the `doStart` lambda)
- Trims whitespace
- Falls back to `"Adventurer"` if the trimmed string is empty
- Strips commas because `ScoreManager` uses comma-delimited CSV internally

#### Tutorial overlay (`showTutorialOverlay`)
Pushed as an additional layer onto the `StackPane` when the `❓ HOW TO PLAY` button is clicked. Consists of a dim `Pane` (clicking it dismisses the overlay) and a card `VBox` with three columns (`buildTutorialColumn`). The overlay is removed from `getChildren()` on dismiss.

---

### 3.2 `CharacterSelectScreen.java`

**Package:** `game.ui`  
**Extends:** `VBox`

A simple screen with three character cards side-by-side inside an `HBox`. Each card is built by `makeCard(name, desc, onPick)`.

**Card structure:**
```
VBox (card, 180px wide)
├── Text  — class name + emoji, blue
├── Text  — HP / ATK stats and flavour text, dim
└── Button — "Select" → calls onPick.run()
```

When `onPick` fires, the lambda instantiates the appropriate `Player` subclass at position (0,0), sets the player name, and calls `onSelect.accept(player)` — which is wired in `Main` to proceed to `GameView`.

---

### 3.3 `GameView.java`

**Package:** `game.ui`  
**750 lines — the most complex class in the project.**

Renders the game world every turn onto a JavaFX `Canvas` and handles all keyboard input.

#### Canvas architecture
The view uses a single `Canvas` with a `GraphicsContext`. Every call to `render()` redraws the entire visible tile grid — there is no dirty-region or incremental update system.

#### Tile rendering
- **Fog of war:** only tiles in `GameState.visible[][]` are drawn at full brightness. Tiles in `revealed[][]` but not currently visible are drawn at ~30% opacity. Unrevealed tiles are skipped entirely (remain black).
- **Sprites vs text fallback:** if a `Player` or `Enemy` has a sprite path, `GameView` loads the image and draws it scaled to the tile size. Text symbols (`@`, `g`, `T`, etc.) are used as a fallback.
- **HUD:** A bottom panel draws the player's HP bar, gold, floor level, kills, and the last 4 log messages. Skill slots (1/2/3) are shown with cooldown overlays.

#### Swipe animation system
Attack animations are small arcs drawn over the attacker's tile. The `SwipeAnim` inner record holds `(fromX, fromY, toX, toY, isPlayer, progress)`. `triggerSwipe(...)` enqueues a new `SwipeAnim`; an `AnimationTimer` ticks progress each frame and calls `render()` until the animation completes.

#### Key handling (`handleKeyPress`)
All movement and skill keys are processed here. The method maps the pressed `KeyCode` to a `(dx, dy)` delta or a skill slot index and delegates to `GameState`.

---

## 4. Core — Game Logic

### 4.1 `GameState.java`

**Package:** `game.core`  
**~600 lines — central authority for all game rules.**

Holds the live game data for a single run:

| Field | Type | Purpose |
|---|---|---|
| `player` | `Player` | The active character |
| `map` | `DungeonMap` | Current dungeon floor tile grid |
| `enemies` | `List<Enemy>` | All living enemies on this floor |
| `items` | `List<Item>` | All uncollected items on this floor |
| `floor` | `int` | Current dungeon depth (starts at 1) |
| `visible[][]` | `boolean[][]` | Tiles in the player's current sight radius |
| `revealed[][]` | `boolean[][]` | Tiles ever seen (shown dimly) |
| `messages` | `List<String>` | Rolling log of last 4 events |

#### Initialisation (`init`)
Called by `Main` with the chosen `Player` after character selection. Resets all fields, posts a welcome message, and calls `generateLevel()`.

#### Level generation (`generateLevel`)
Instantiates a new `DungeonMap`, calls `generate()`, places the player in the first room, spawns enemies (type and count scale with floor depth), and scatters items. Updates the fog-of-war arrays.

#### Player movement (`movePlayer`)
The main gameplay method. For a given `(dx, dy)`:
1. Checks for a wall — if blocked, does nothing.
2. Checks for an enemy at the target tile — if present, resolves melee combat and fires `onAttack`.
3. Otherwise moves the player, collects any item on the new tile, checks for stairs, and calls `enemyTurns()`.

#### Enemy AI (`enemyTurns`)
Iterates all living enemies. Each enemy:
- If adjacent to the player → attacks (uses `HostileActions.atk()` for hit chance).
- If not adjacent → moves one step toward the player along the Manhattan-distance shortest path, avoiding walls and other enemies.

#### Fog of war (`updateFog`)
Recomputes `visible[][]` each turn by flood-filling outward from the player up to `VISION_RADIUS = 5` tiles, stopping at walls. Any newly visible tile is also added to `revealed[][]`.

#### Skill system
`pendingSkillIndex` holds the index of a skill waiting for a directional input. When a movement key is pressed while `pendingSkillIndex >= 0`, the `(dx, dy)` is passed to `Player.useSkillDirectional(...)` instead of triggering movement.

---

### 4.2 `ScoreManager.java`

**Package:** `game.core`

Manages a local `highscores.db` SQLite file via JDBC using the `sqlite-jdbc` driver bundled in `pom.xml`.

#### Schema
```sql
CREATE TABLE highscores (
    id     INTEGER PRIMARY KEY AUTOINCREMENT,
    name   TEXT    NOT NULL,
    floor  INTEGER NOT NULL,
    kills  INTEGER NOT NULL,
    gold   INTEGER NOT NULL,
    date   TEXT    NOT NULL DEFAULT (date('now'))
)
```

#### `saveIfHighScore(name, floor, kills, gold)`
Inserts the new record unconditionally, then immediately prunes the table to the top 10 rows by kills. This keeps the DB small and ensures the leaderboard never grows unbounded.

#### `load()`
Uses `GROUP BY (name, floor, kills, gold)` with `MAX(date)` so identical runs are de-duplicated. Returns a `List<String>` where each element is `"name,floor,kills,gold,date"` — the comma-delimited format parsed by `StartScreen.buildScoreBoard()`.

---

## 5. Entities — Characters

### 5.1 `Player.java`

**Package:** `game.entity.characters`

Base class for all playable characters. Contains all stats, position, the sprite resource path, and the skill list.

**Stats:** `hp`, `maxHp`, `attack`, `gold`, `kills`  
**Identity:** `name` (set from the name entry), `symbol` (text fallback), `spritePath`  
**Skills:** `List<Skill>` — capped at 3; populated by subclasses

Key methods:
- `takeDamage(int)` / `heal(int)` — HP modification clamped to [0, maxHp]
- `addGold(int)` / `spendGold(int)` — gold modification clamped to [0, ∞)
- `tickSkills()` — decrements all skill cooldowns by one turn; called at the end of each player action
- `useSkillInstant(index, ctx)` / `useSkillDirectional(index, ctx, dx, dy)` — delegate to `Skill.activateInstant/activateDirectional`

---

### 5.2 Warrior / Rogue / Mage

All three are trivial subclasses of `Player` that set stats via `super()` and register skills via `addSkill(new Skill(...))`.

| Class | HP | ATK | Flavour |
|---|---|---|---|
| `Warrior` | 20 | 4 | Tank; slow but durable |
| `Rogue` | 12 | 6 | Fast striker; glass cannon |
| `Mage` | 8 | 8 | Devastating but fragile |

Each also calls `setSpritePath("/sprites/<name>.png")` to point `GameView` to the correct sprite sheet.

---

### 5.3 `Skill.java`

**Package:** `game.entity.characters`  
**~250 lines**

Represents one of the three active skills each character can carry. A skill has:
- A `name` and `description` for display
- A `goldCost` paid once on first activation (unlocking the skill)
- A `cooldown` (turns) and current `cooldownRemaining`
- A boolean `unlocked` flag

Skills are either **instant** (fire immediately on key press) or **directional** (require a follow-up arrow/WASD key to choose a direction).

#### `SkillContext`
A simple value-object passed into activation methods so skills can read and modify game state (`player`, `enemies`, `map`, `messages`) without holding direct references to `GameState`.

#### Activation flow
1. Key [1/2/3] pressed → `GameState` checks `pendingSkillIndex`
2. If the skill is directional and awaiting a direction, `useSkillDirectional` is called with `(dx, dy)`
3. If instant, `useSkillInstant` fires immediately
4. On first use the gold cost is deducted and `unlocked = true`
5. `cooldownRemaining` is set to `cooldown`; the skill cannot fire again until it reaches 0 (decremented each turn by `tickSkills()`)

---

## 6. Entities — Enemies

### 6.1 `Enemy.java`

**Package:** `game.entity.hostile`

Base class for all dungeon enemies. Stores `(x, y)`, `symbol`, `name`, `hp`, `maxHp`, `attack`, `range` (attack range in tiles), and `acc` (accuracy 0–100).

Enemy AI logic is _not_ in this class — it lives entirely in `GameState.enemyTurns()`. `Enemy` is purely a data container.

---

### 6.2 `HostileActions.java`

**Package:** `game.entity.hostile`

A single-method interface implemented by `Enemy`. Provides:

```java
default boolean atk(int acc, Random rand) {
    return rand.nextInt() + 1 <= acc;
}
```

All enemy subclasses inherit this default. A hit occurs when the random roll falls within the accuracy ceiling. Accuracy of 100 virtually guarantees a hit; lower values introduce miss chances.

---

### 6.3 Goblin / ArcherGoblin / Troll

One-line subclasses that call `super(x, y, symbol, name, hp, atk, range, acc)` with fixed values:

| Enemy | HP | ATK | Range | Accuracy |
|---|---|---|---|---|
| `Goblin` | Low | Low | 1 (melee) | High |
| `ArcherGoblin` | Low | Medium | 3 (ranged) | Medium |
| `Troll` | High | High | 1 (melee) | High |

`GameState.generateLevel()` selects which enemies to spawn based on the current floor depth.

---

## 7. Items

### 7.1 `Item.java`

**Package:** `game.entity.item`

Immutable value class representing a collectible item at tile `(x, y)`.

```java
public enum Type { POTION, GOLD }
```

Items are collected automatically in `GameState.movePlayer()` when the player steps on their tile. The effect is applied in `GameState` based on `getType()`:
- `POTION` → `player.heal(2–5)`
- `GOLD` → `player.addGold(3–10)`

---

## 8. Map

### 8.1 `DungeonMap.java`

**Package:** `game.map`

Procedurally generates a grid of `Tile` values using a room-placement algorithm:

1. Fill the entire grid with `Tile.WALL`.
2. Attempt to place up to 30 rooms (random size, random position).
3. Each new room that does not overlap existing rooms is carved out (set to `Tile.FLOOR`).
4. Each new room is connected to the previous room via an L-shaped corridor: first a horizontal run, then a vertical run.
5. A `Tile.STAIR` is placed at the centre of the last room carved.

The generator uses a `Random` instance passed in from `GameState` so the sequence is deterministic given the same seed.

---

### 8.2 `Room.java`

**Package:** `game.map`

A simple rectangle `(x, y, width, height)` with:
- `contains(x, y)` — point-in-rectangle test used for overlap detection
- `getCenterX()` / `getCenterY()` — used to pick the corridor connection point and the staircase position

---

### 8.3 `Tile.java`

**Package:** `game.map`

An enum with three constants:

| Constant | Symbol | Meaning |
|---|---|---|
| `WALL` | `#` | Impassable solid tile |
| `FLOOR` | `.` | Walkable open tile |
| `STAIR` | `>` | Walkable tile that descends to the next floor |

Each constant stores a `char symbol` used for text-mode debug rendering.

---

## 9. Resources

```
src/main/resources/
├── fonts/
│   ├── Jacquard12-Regular.ttf    — bitmap pixel font used for the game title
│   ├── alagard.ttf               — secondary display font
│   └── Faith Collapsing.ttf      — decorative font (available for future use)
└── sprites/
    ├── background.png            — full-screen art used as StartScreen background
    ├── warrior2.png              — Warrior player sprite (16×16 px)
    ├── rogue2.png                — Rogue player sprite
    ├── mage.png                  — Mage player sprite
    ├── floor.png                 — Floor tile texture
    └── wall.png                  — Wall tile texture
```

All resources are loaded at runtime via `getClass().getResourceAsStream("/path")` (note the leading `/` for classpath-root resolution). Maven copies everything under `src/main/resources` into `target/classes`, which ends up on the classpath.

---

## 10. Build — `pom.xml`

The project uses **Maven** with the `javafx-maven-plugin` for packaging.

**Key dependencies:**
- `org.openjfx:javafx-controls` — JavaFX UI controls and layout
- `org.openjfx:javafx-graphics` — Canvas, GraphicsContext, animations
- `org.xerial:sqlite-jdbc` — embedded SQLite driver (no external DB server needed)

**Running the project:**
```bash
mvn javafx:run
```

**Packaging to a runnable JAR:**
```bash
mvn package
java -jar target/RogueGame-*.jar
```

---

## 11. Navigation Flow Diagram

```
Application launch
       │
       ▼
  Main.start()
       │
       ▼
 showSelect[0].run()
       │
       ▼
  StartScreen          ← background.png + leaderboard + name input
       │  (click Start or press Enter)
       ▼
 CharacterSelectScreen ← Warrior / Rogue / Mage cards
       │  (click Select)
       ▼
  GameState.init()     ← resets all run data, generates floor 1
       │
       ▼
   GameView            ← renders canvas, handles keys, shows HUD
       │
       ├── player moves / attacks ──► GameState.movePlayer()
       │                                   └── enemyTurns()
       │                                   └── updateFog()
       │
       ├── skill key pressed ───────► GameState skill activation path
       │
       ├── player dies / press R ───► showSelect[0].run()  (loop back)
       │
       └── descend stairs ──────────► GameState.generateLevel() (next floor)
```
