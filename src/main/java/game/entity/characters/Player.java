package game.entity.characters;

import javafx.scene.image.Image;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Player {
    private int x, y;
    private int hp, maxHp;
    private int attack;
    private int gold;
    private int kills;
    private String symbol = "@";
    private String name   = "Adventurer";

    // Each subclass sets this to its own sprite resource path.
    // GameView reads it via getSpritePath() to load the image.
    private String spritePath = null;

    /** The three active skills for this character (index 0 = key 1, etc.). */
    private final List<Skill> skills = new ArrayList<>();

    // ── Constructor ───────────────────────────────────────────────────────────

    /** Creates a player at (x, y) with the given max HP and attack power. */
    public Player(int x, int y, int maxHp, int attack) {
        this.x = x;
        this.y = y;
        this.maxHp = maxHp;
        this.hp = maxHp;
        this.attack = attack;
        this.gold = 0;
        this.kills = 0;
    }

    // ── Actions ───────────────────────────────────────────────────────────────

    /** Reduces the player's HP by dmg, clamped to a minimum of 0. */
    public void takeDamage(int dmg) { hp = Math.max(0, hp - dmg); }

    /** Heals the player by amount, clamped to maxHp. */
    public void heal(int amount)    { hp = Math.min(maxHp, hp + amount); }

    /** Adds g gold coins to the player's wallet. */
    public void addGold(int g)      { gold += g; }

    /** Deducts g gold coins from the player's wallet (clamped to 0). */
    public void spendGold(int g)    { gold = Math.max(0, gold - g); }

    /** Increments the player's kill counter by one. */
    public void addKill()           { kills++; }

    // ── State ─────────────────────────────────────────────────────────────────

    /** Returns true if the player's HP has reached zero. */
    public boolean isDead() { return hp <= 0; }

    // ── Getters ───────────────────────────────────────────────────────────────

    /** Returns the player's current X tile coordinate. */
    public int getX()      { return x; }

    /** Returns the player's current Y tile coordinate. */
    public int getY()      { return y; }

    /** Returns the player's current HP. */
    public int getHp()     { return hp; }

    /** Returns the player's maximum HP. */
    public int getMaxHp()  { return maxHp; }

    /** Returns the player's base attack damage. */
    public int getAttack() { return attack; }

    /** Returns the player's accumulated gold. */
    public int getGold()   { return gold; }

    /** Returns the number of enemies the player has killed. */
    public int getKills()  { return kills; }

    /** Returns the character symbol used as a text-mode fallback. */
    public String getSymbol() { return symbol; }

    /** Returns the player's display name. */
    public String getName()   { return name; }

    /**
     * Returns the classpath resource path to this character's sprite image,
     * or null if no sprite has been set (falls back to text rendering).
     */
    public String getSpritePath() { return spritePath; }

    // ── Setters ───────────────────────────────────────────────────────────────

    /** Sets the text-mode fallback symbol for this character. */
    protected void setSymbol(String s) { this.symbol = s; }

    /** Sets the player's display name. */
    public void setName(String n)   { this.name = n; }

    /** Sets the player's X tile coordinate. */
    public void setX(int x) { this.x = x; }

    /** Sets the player's Y tile coordinate. */
    public void setY(int y) { this.y = y; }

    /**
     * Sets the classpath resource path to this character's sprite PNG.
     * Should be called by subclasses in their constructor, e.g. "/sprites/warrior2.png".
     */
    protected void setSpritePath(String path) { this.spritePath = path; }

    /**
     * Adds a skill to this character's skill list.
     * Should be called by subclasses in their constructor; up to 3 skills.
     */
    protected void addSkill(Skill skill) {
        if (skills.size() < 3) skills.add(skill);
    }

    /** Returns an unmodifiable view of this character's skill list. */
    public List<Skill> getSkills() { return Collections.unmodifiableList(skills); }

    /**
     * Ticks all skill cooldowns down by one turn.
     * Call this at the end of each player action.
     */
    public void tickSkills() {
        for (Skill s : skills) s.tick();
    }

    /**
     * Activates an instant skill at the given 0-based index.
     * @return true if activated, false otherwise
     */
    public boolean useSkillInstant(int index, Skill.SkillContext ctx) {
        if (index < 0 || index >= skills.size()) return false;
        return skills.get(index).activateInstant(ctx);
    }

    /**
     * Activates a directional skill at the given 0-based index with (dx, dy).
     * @return true if activated, false otherwise
     */
    public boolean useSkillDirectional(int index, Skill.SkillContext ctx, int dx, int dy) {
        if (index < 0 || index >= skills.size()) return false;
        return skills.get(index).activateDirectional(ctx, dx, dy);
    }
}
