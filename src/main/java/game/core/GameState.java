package game.core;

import game.entity.characters.Player;
import game.entity.hostile.ArcherGoblin;
import game.entity.hostile.Enemy;
import game.entity.hostile.Goblin;
import game.entity.hostile.Troll;
import game.entity.item.Item;
import game.map.DungeonMap;
import game.map.Tile;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Central game state — owns the player, map, enemies, items,
 * fog-of-war arrays, message log, and floor counter.
 * All gameplay logic (movement, combat, item pick-up, enemy AI) lives here.
 */
public class GameState {

    private Player player;
    private DungeonMap map;
    private List<Enemy> enemies;
    private List<Item>  items;
    private int floor;
    private boolean gameOver;

    /** Rolling log of the last 4 combat/event messages shown in the HUD. */
    private final List<String> messages = new ArrayList<>();

    /**
     * Optional callback fired after any attack.
     * Receives int[]{fromX, fromY, toX, toY, isPlayer} so the
     * view can play a swipe animation without knowing game internals.
     */
    private java.util.function.Consumer<int[]> onAttack;

    // ── Fog-of-war ────────────────────────────────────────────────────────────

    /** true for tiles currently within the player's line-of-sight radius. */
    private boolean[][] visible;

    /** true for tiles the player has seen at least once (shown dimly). */
    private boolean[][] revealed;

    private static final int VISION_RADIUS = 5;

    private final Random random = new Random();

    // ── Initialisation ────────────────────────────────────────────────────────

    /**
     * Registers a callback that fires whenever an attack occurs.
     * The callback receives {fromX, fromY, toX, toY, isPlayer(1=yes, 0=no)}.
     * Used by GameView to trigger swipe animations on the JavaFX thread.
     */
    public void setOnAttack(java.util.function.Consumer<int[]> callback) {
        this.onAttack = callback;
    }

    /**
     * Initialises a new game run with the chosen player character.
     * Resets the floor counter, clears entity lists, posts a welcome message,
     * and generates the first dungeon level.
     *
     * @param selectedPlayer the Player subclass chosen on the character-select screen
     */
    public void init(Player selectedPlayer) {
        floor    = 1;
        player   = selectedPlayer;
        enemies  = new ArrayList<>();
        items    = new ArrayList<>();
        gameOver = false;
        pendingSkillIndex = -1;
        addMessage("Welcome to the dungeon! Find the stairs (>) to descend.");
        generateLevel();
    }

    /**
     * Generates a fresh dungeon level: creates a new map, resets fog-of-war,
     * places the player at the start room, and spawns enemies and items.
     * Called on init and each time the player descends a staircase.
     */
    public void generateLevel() {
        map = new DungeonMap(80, 30, random);
        map.generate();

        visible  = new boolean[map.getHeight()][map.getWidth()];
        revealed = new boolean[map.getHeight()][map.getWidth()];

        int[] start = map.getStartPosition();
        player.setX(start[0]);
        player.setY(start[1]);

        enemies.clear();
        items.clear();
        spawnEnemies();
        spawnItems();
        updateFog();
    }

    // ── Spawning ──────────────────────────────────────────────────────────────

    /**
     * Instantiates a single enemy of type {@code i} at tile (x, y) and adds it
     * to the enemy list.  The mapping is: 1 → Goblin, 2 → ArcherGoblin, 3 → Troll.
     * Unknown values are silently ignored.
     *
     * @param i enemy-type index derived from the current floor
     * @param x tile column for the new enemy
     * @param y tile row for the new enemy
     */
    private void spawnMob(int i, int x, int y) {
        Enemy mob = switch (i) {
            case 1 -> new Goblin(x, y,this.floor);
            case 2 -> new ArcherGoblin(x, y,this.floor);
            case 3 -> new Troll(x, y,this.floor);
            default -> null;
        };
        if (mob != null) enemies.add(mob);
    }

    /**
     * Populates each non-starting room with 1–2 enemies.
     * The enemy type is randomly chosen based on the current floor:
     * floors 1–2 → Goblins only, floors 3–4 → up to ArcherGoblin, floors 5+ → any.
     */
    private void spawnEnemies() {
        int lv = 1;
        if (floor >= 3) lv = 2;
        if (floor >= 5) lv = 3;

        int p = random.nextInt(lv) + 1;

        List<int[]> rooms = map.getRoomCenters();
        for (int i = 1; i < rooms.size(); i++) {
            int count = 1 + random.nextInt(2);
            for (int c = 0; c < count; c++) {
                int[] room = map.getRandomFloorInRoom(i);
                if (room != null) spawnMob(p, room[0], room[1]);
            }
        }
    }

    /**
     * Places items in non-starting rooms with a 60% chance per room.
     * Each eligible room gets either a Potion ('!') or a Gold coin ('$'),
     * chosen at random.
     */
    private void spawnItems() {
        List<int[]> rooms = map.getRoomCenters();
        for (int i = 1; i < rooms.size(); i++) {
            if (random.nextFloat() < 0.6f) {
                int[] pos = map.getRandomFloorInRoom(i);
                if (pos != null) {
                    boolean isPotion = random.nextBoolean();
                    items.add(isPotion
                        ? new Item(pos[0], pos[1], "🍶", "Potion", Item.Type.POTION)
                        : new Item(pos[0], pos[1], "💰", "Gold",   Item.Type.GOLD));
                }
            }
        }
    }

    // ── Fog of War ────────────────────────────────────────────────────────────

    /**
     * Recomputes the {@code visible} array using a circular FOV around the player.
     * Tiles within VISION_RADIUS that pass a line-of-sight check are marked visible
     * and permanently marked as revealed.
     */
    private void updateFog() {
        for (boolean[] row : visible) java.util.Arrays.fill(row, false);
        int px = player.getX(), py = player.getY();
        for (int dy = -VISION_RADIUS; dy <= VISION_RADIUS; dy++) {
            for (int dx = -VISION_RADIUS; dx <= VISION_RADIUS; dx++) {
                if (dx * dx + dy * dy > VISION_RADIUS * VISION_RADIUS) continue;
                int tx = px + dx, ty = py + dy;
                if (tx < 0 || ty < 0 || tx >= map.getWidth() || ty >= map.getHeight()) continue;
                if (hasLos(px, py, tx, ty)) {
                    visible[ty][tx]  = true;
                    revealed[ty][tx] = true;
                }
            }
        }
    }

    /**
     * Bresenham-style line-of-sight check between two tile positions.
     * Walks the line from (x0, y0) to (x1, y1) and returns false as soon
     * as a WALL tile is encountered before the destination.
     *
     * @return true if there is an unobstructed path between the two tiles
     */
    private boolean hasLos(int x0, int y0, int x1, int y1) {
        int dx = Math.abs(x1 - x0), dy = Math.abs(y1 - y0);
        int sx = x0 < x1 ? 1 : -1, sy = y0 < y1 ? 1 : -1;
        int err = dx - dy, cx = x0, cy = y0;
        while (true) {
            if (cx == x1 && cy == y1) return true;
            if (map.getTile(cx, cy) == Tile.WALL) return false;
            int e2 = 2 * err;
            if (e2 > -dy) { err -= dy; cx += sx; }
            if (e2 <  dx) { err += dx; cy += sy; }
        }
    }

    // ── Player Actions ────────────────────────────────────────────────────────

    /**
     * Attempts to move the player by (dx, dy).
     * If the target tile is a wall the move is cancelled.
     * If an enemy occupies the target tile the player attacks it instead of moving.
     * If the target tile is a staircase the player descends to the next floor.
     * After any valid action enemy turns are processed.
     *
     * @param dx horizontal movement delta (-1, 0, or 1)
     * @param dy vertical movement delta   (-1, 0, or 1)
     */
    public void movePlayer(int dx, int dy) {
        int nx = player.getX() + dx, ny = player.getY() + dy;
        if (!map.isWalkable(nx, ny)) return;

        Enemy target = getEnemyAt(nx, ny);
        if (target != null) {
            // ── Player attacks enemy ──
            int dmg = player.getAttack();
            target.takeDamage(dmg);
            if (onAttack != null)
                onAttack.accept(new int[]{player.getX(), player.getY(),
                                          target.getX(), target.getY(), 1});

            if (target.isDead()) {
                enemies.remove(target);
                player.addKill();
                addMessage("You killed the " + target.getName() + "!");
            } else {
                addMessage("Hit " + target.getName() + " for " + dmg + " dmg ("
                        + target.getHp() + "/" + target.getMaxHp() + " left)");
            }
        } else {
            // ── Player moves ──
            player.setX(nx);
            player.setY(ny);

            Item item = getItemAt(nx, ny);
            if (item != null) {
                applyItem(item);
                items.remove(item);
            } else if (map.getTile(nx, ny) == Tile.STAIR) {
                floor++;
                addMessage("You descend to floor " + floor + "...");
                player.heal(player.getMaxHp());
                generateLevel();
                return;
            }
            updateFog();
        }
        enemyTurns();
        player.tickSkills();
    }
    /*
     * POTION heals 2–5 HP; GOLD awards 3–10 gold coins.
     *
     * @param item the item being collected
     */
    private void applyItem(Item item) {
        switch (item.getType()) {
            case POTION -> { int h = 2 + random.nextInt(4); player.heal(h);    addMessage("Drank a potion, healed " + h + " HP!"); }
            case GOLD   -> { int g = 3 + random.nextInt(8); player.addGold(g); addMessage("Picked up " + g + " gold!"); }
        }
    }

    // ── Skill activation ──────────────────────────────────────────────────────

    /**
     * Index of the skill the player has queued up with [1/2/3], waiting for a direction.
     * -1 means no skill is pending.
     */
    private int pendingSkillIndex = -1;

    /** Returns the currently pending skill index (-1 if none). */
    public int getPendingSkillIndex() { return pendingSkillIndex; }

    /** Clears any pending directional skill (e.g. on restart or game-over). */
    public void clearPendingSkill() { pendingSkillIndex = -1; }

    /**
     * Builds a SkillContext wired to current game state.
     */
    private game.entity.characters.Skill.SkillContext buildSkillContext() {
        return new game.entity.characters.Skill.SkillContext(
            player, enemies,
            (x, y) -> isVisible(x, y),
            (x, y) -> map.isWalkable(x, y),
            this::addMessage,
            random);
    }

    /**
     * Called when the player presses [1], [2], or [3].
     * <ul>
     *   <li>If the skill is locked → attempt to unlock with gold.</li>
     *   <li>If the skill is instant → fire immediately.</li>
     *   <li>If the skill is directional → enter pending state (waits for next move key).</li>
     *   <li>If the skill is on cooldown → post a message, do nothing.</li>
     * </ul>
     */
    public void onSkillKey(int index) {
        if (gameOver) return;
        if (index < 0 || index >= player.getSkills().size()) return;

        game.entity.characters.Skill skill = player.getSkills().get(index);
        game.entity.characters.Skill.SkillContext ctx = buildSkillContext();

        // If still locked, try to buy
        if (!skill.isUnlocked()) {
            skill.tryUnlock(ctx);
            needsSkillRedraw = true;
            return;
        }

        if (!skill.isReady()) {
            addMessage(skill.getName() + " is on cooldown (" + skill.getCooldownLeft() + " turns).");
            needsSkillRedraw = true;
            return;
        }

        if (skill.requiresDirection()) {
            // Park it and wait for the next direction key
            pendingSkillIndex = index;
            addMessage(skill.getName() + ": choose a direction (WASD/arrows).");
            needsSkillRedraw = true;
        } else {
            // Fire instantly
            player.useSkillInstant(index, ctx);
            enemies.removeIf(e -> {
                if (e.isDead()) { player.addKill(); addMessage("You killed the " + e.getName() + "!"); return true; }
                return false;
            });
            updateFog();
            enemyTurns();
            player.tickSkills();
            needsSkillRedraw = true;
        }
    }

    /**
     * Called when the player presses a move key while a directional skill is pending.
     * Fires the skill in the given direction, validates it, and either executes or cancels.
     * @return true if the direction was consumed by a skill (do NOT also move the player)
     */
    public boolean onDirectionForSkill(int dx, int dy) {
        if (pendingSkillIndex < 0) return false;
        int idx = pendingSkillIndex;
        pendingSkillIndex = -1;

        game.entity.characters.Skill.SkillContext ctx = buildSkillContext();
        boolean fired = player.useSkillDirectional(idx, ctx, dx, dy);
        if (fired) {
            enemies.removeIf(e -> {
                if (e.isDead()) { player.addKill(); addMessage("You killed the " + e.getName() + "!"); return true; }
                return false;
            });
            updateFog();
            enemyTurns();
            player.tickSkills();
        }
        // If not fired, the validator already posted a message; skill stays ready
        return true; // direction was consumed regardless (cancel or fire)
    }

    // Flag for the view to know it needs to redraw (skills changed without a full move)
    private boolean needsSkillRedraw = false;
    public boolean consumeSkillRedraw() {
        boolean v = needsSkillRedraw;
        needsSkillRedraw = false;
        return v;
    }

    /**
     * Processes one turn for every enemy that is currently visible to the player.
     * Each enemy moves one step toward the player if they are within 10 tiles.
     * If the enemy is within attack range it rolls for a hit; on success it
     * deals damage and fires the attack callback for the swipe animation.
     * Sets gameOver if the player's HP reaches zero.
     */
    private void enemyTurns() {
        for (Enemy e : enemies) {
            if (!visible[e.getY()][e.getX()]) continue;

            int ddx = player.getX() - e.getX();
            int ddy = player.getY() - e.getY();
            double dist = Math.sqrt(ddx * ddx + ddy * ddy);
            if (dist > 10) continue;

            // Choose the primary axis to step along
            int mx = 0, my = 0;
            if (Math.abs(ddx) >= Math.abs(ddy)) mx = ddx > 0 ? 1 : -1;
            else                                 my = ddy > 0 ? 1 : -1;

            int ex = e.getX() + mx, ey = e.getY() + my;

            if (dist <= e.getRange()) {
                // ── Enemy attacks player ──
                if (e.atk(e.getAcc(), random)) {
                    int dmg = (int)e.getAttack();
                    player.takeDamage(dmg);
                    if (onAttack != null)
                        onAttack.accept(new int[]{e.getX(), e.getY(),
                                                  player.getX(), player.getY(), 0});
                    addMessage(e.getName() + " hits you for " + dmg + "! ("
                            + player.getHp() + " HP left)");
                }
                if (player.isDead()) gameOver = true;
            } else if (map.isWalkable(ex, ey) && getEnemyAt(ex, ey) == null) {
                // ── Enemy moves toward player ──
                e.setX(ex);
                e.setY(ey);
            }
        }
    }

    // ── Lookups ───────────────────────────────────────────────────────────────

    /**
     * Returns the enemy occupying tile (x, y), or null if the tile is empty.
     * Used to detect attack targets when the player moves.
     */
    private Enemy getEnemyAt(int x, int y) {
        return enemies.stream()
                      .filter(e -> e.getX() == x && e.getY() == y)
                      .findFirst().orElse(null);
    }

    /**
     * Returns the item sitting on tile (x, y), or null if none.
     * Used to auto-collect items when the player steps onto their tile.
     */
    private Item getItemAt(int x, int y) {
        return items.stream()
                    .filter(i -> i.getX() == x && i.getY() == y)
                    .findFirst().orElse(null);
    }

    // ── Visibility ────────────────────────────────────────────────────────────

    /**
     * Returns true if tile (x, y) is currently within the player's line of sight.
     * Out-of-bounds coordinates always return false.
     */
    public boolean isVisible(int x, int y) {
        if (x < 0 || y < 0 || x >= map.getWidth() || y >= map.getHeight()) return false;
        return visible[y][x];
    }

    /**
     * Returns true if tile (x, y) has been seen by the player at least once.
     * Revealed-but-not-visible tiles are drawn with a dark fog overlay.
     * Out-of-bounds coordinates always return false.
     */
    public boolean isRevealed(int x, int y) {
        if (x < 0 || y < 0 || x >= map.getWidth() || y >= map.getHeight()) return false;
        return revealed[y][x];
    }

    // ── Messaging ─────────────────────────────────────────────────────────────

    /**
     * Appends a message to the combat log, keeping only the last 4 entries.
     *
     * @param msg the line of text to display in the HUD message area
     */
    public void addMessage(String msg) {
        messages.add(msg);
        if (messages.size() > 4) messages.remove(0);
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    /** Returns the active player instance. */
    public Player    getPlayer()   { return player; }

    /** Returns the current dungeon map. */
    public DungeonMap getMap()     { return map; }

    /** Returns the live list of enemies on the current level. */
    public List<Enemy> getEnemies() { return enemies; }

    /** Returns the live list of items on the current level. */
    public List<Item>  getItems()   { return items; }

    /** Returns the current dungeon floor number (starts at 1). */
    public int getFloor()          { return floor; }

    /** Returns the rolling message log (at most 4 entries). */
    public List<String> getMessages() { return messages; }

    // ── State ─────────────────────────────────────────────────────────────────

    /** Returns true if the game has ended (player dead or escaped). */
    public boolean isGameOver()       { return gameOver; }

    /** Manually overrides the game-over flag (used by test code or restart logic). */
    public void setGameOver(boolean v) { gameOver = v; }
}
