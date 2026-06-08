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
    private double hp, maxHp;
    private double attack;
    private int range;
    private double acc;

    /**
     * Optional classpath resource path to this enemy's sprite PNG.
     * Set by subclasses via {@link #setSpritePath(String)}.
     * Null means fall back to the text {@link #symbol}.
     */
    private String spritePath = null;

    // ── Constructor ───────────────────────────────────────────────────────────

    /**
     * Creates an enemy at (x, y) with the given display symbol, name,
     * max HP, attack damage, attack range (in tiles), and accuracy (0–100).
     */
    public Enemy(int x, int y, String symbol, String name,
                 double maxHp, double attack, int range, double acc) {
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

    /**
     * Returns the classpath resource path to this enemy's sprite image,
     * or null if no sprite has been set (falls back to text glyph rendering).
     */
    public String getSpritePath() { return spritePath; }

    /**
     * Sets the classpath resource path to this enemy's sprite PNG.
     * Should be called by subclasses in their constructor, e.g. "/sprites/goblin.png".
     */
    protected void setSpritePath(String path) { this.spritePath = path; }

    /** Returns the enemy's display name shown in combat messages. */
    public String getName() { return name; }

    /** Returns the enemy's current HP. */
    public double getHp()      { return hp; }

    /** Returns the enemy's maximum HP. */
    public double getMaxHp()   { return maxHp; }

    /** Returns the enemy's base attack damage per hit. */
    public double getAttack()  { return attack; }

    /** Returns the tile range within which this enemy can attack. */
    public int getRange()   { return range; }

    /** Returns the enemy's accuracy value used in the hit-chance roll. */
    public double getAcc()     { return acc; }

    // ── Setters ───────────────────────────────────────────────────────────────

    /** Sets the enemy's X tile coordinate (used for movement). */
    public void setX(int x) { this.x = x; }

    /** Sets the enemy's Y tile coordinate (used for movement). */
    public void setY(int y) { this.y = y; }
}
