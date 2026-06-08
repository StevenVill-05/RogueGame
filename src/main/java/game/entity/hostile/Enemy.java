package game.entity.hostile;

import java.util.Random;

/**
 * Base class for all dungeon enemies (Goblin, ArcherGoblin, Troll).
 * Stores stats, position, and display symbol; provides takeDamage and isDead logic.
 * Enemy AI (movement and attack decisions) is handled in {@link game.core.GameState#enemyTurns()}.
 */
public class Enemy implements HostileActions {

    private int x, y;
    private final String symbol;
    private final String name;
    private int hp, maxHp;
    private int attack;
    private int range;
    private int acc;

    // ── Constructor ───────────────────────────────────────────────────────────

    /**
     * Creates an enemy at (x, y) with the given display symbol, name,
     * max HP, attack damage, attack range (in tiles), and accuracy (0–100).
     */
    public Enemy(int x, int y, String symbol, String name,
                 int maxHp, int attack, int range, int acc) {
        this.x      = x;
        this.y      = y;
        this.symbol = symbol;
        this.name   = name;
        this.maxHp  = maxHp;
        this.hp     = maxHp;
        this.attack = attack;
        this.range  = range;
        this.acc    = acc;
    }

    // ── State ─────────────────────────────────────────────────────────────────

    /** Returns true if the enemy's HP has reached zero. */
    public boolean isDead() { return hp <= 0; }

    // ── Actions ───────────────────────────────────────────────────────────────

    /** Reduces the enemy's HP by dmg, clamped to a minimum of 0. */
    public void takeDamage(int dmg) { hp = Math.max(0, hp - dmg); }

    // ── Getters ───────────────────────────────────────────────────────────────

    /** Returns the enemy's current X tile coordinate. */
    public int getX()       { return x; }

    /** Returns the enemy's current Y tile coordinate. */
    public int getY()       { return y; }

    /** Returns the emoji/text symbol drawn on the map for this enemy. */
    public String getSymbol() { return symbol; }

    /** Returns the enemy's display name shown in combat messages. */
    public String getName() { return name; }

    /** Returns the enemy's current HP. */
    public int getHp()      { return hp; }

    /** Returns the enemy's maximum HP. */
    public int getMaxHp()   { return maxHp; }

    /** Returns the enemy's base attack damage per hit. */
    public int getAttack()  { return attack; }

    /** Returns the tile range within which this enemy can attack. */
    public int getRange()   { return range; }

    /** Returns the enemy's accuracy value used in the hit-chance roll. */
    public int getAcc()     { return acc; }

    // ── Setters ───────────────────────────────────────────────────────────────

    /** Sets the enemy's X tile coordinate (used for movement). */
    public void setX(int x) { this.x = x; }

    /** Sets the enemy's Y tile coordinate (used for movement). */
    public void setY(int y) { this.y = y; }
}
