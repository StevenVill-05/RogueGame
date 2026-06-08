package game.entity.characters;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Represents an active skill a player character can use.
 * <p>
 * Skills have two modes:
 * <ul>
 *   <li><b>Instant</b> ({@code requiresDirection = false}) — fires on the key-press
 *       of [1], [2], or [3].</li>
 *   <li><b>Directional</b> ({@code requiresDirection = true}) — pressing [1/2/3]
 *       enters a "pending" state; the next movement key supplies (dx, dy), which is
 *       then validated and passed to the effect. If the direction is blocked the skill
 *       is cancelled.</li>
 * </ul>
 * Skills are initially locked. Pressing [1/2/3] on a locked skill deducts the gold
 * cost and unlocks it permanently for the run.
 */
public class Skill {

    // ── Identity ──────────────────────────────────────────────────────────────

    private final String name;
    private final String description;
    /** Single character/emoji shown in the HUD slot. */
    private final String icon;

    // ── Unlock ────────────────────────────────────────────────────────────────

    /** Gold cost to unlock; 0 means the skill starts unlocked. */
    private final int goldCost;
    private boolean unlocked;

    // ── Cooldown ──────────────────────────────────────────────────────────────

    private final int maxCooldown;
    private int cooldownLeft;

    // ── Targeting ─────────────────────────────────────────────────────────────

    /**
     * If true, the skill waits for a direction key after [1/2/3] is pressed.
     * The effect receives the validated (dx, dy); invalid directions cancel the skill.
     */
    private final boolean requiresDirection;

    /**
     * Validator called before the effect when the skill requires a direction.
     * Receives the SkillContext and (dx, dy); returns true if the direction is valid.
     * If null, any non-zero direction is accepted.
     */
    private final java.util.function.BiFunction<SkillContext, int[], Boolean> directionValidator;

    /**
     * Instant effect: receives SkillContext only (used when requiresDirection = false).
     */
    private final Consumer<SkillContext> instantEffect;

    /**
     * Directional effect: receives SkillContext and int[]{dx, dy}
     * (used when requiresDirection = true).
     */
    private final BiConsumer<SkillContext, int[]> directionalEffect;

    // ── Constructors ──────────────────────────────────────────────────────────

    /** Builds an instant skill (no direction needed). */
    public Skill(String name, String description, String icon,
                 int goldCost, int maxCooldown,
                 Consumer<SkillContext> instantEffect) {
        this.name               = name;
        this.description        = description;
        this.icon               = icon;
        this.goldCost           = goldCost;
        this.unlocked           = (goldCost == 0);
        this.maxCooldown        = maxCooldown;
        this.cooldownLeft       = 0;
        this.requiresDirection  = false;
        this.directionValidator = null;
        this.instantEffect      = instantEffect;
        this.directionalEffect  = null;
    }

    /** Builds a directional skill with an optional direction validator. */
    public Skill(String name, String description, String icon,
                 int goldCost, int maxCooldown,
                 java.util.function.BiFunction<SkillContext, int[], Boolean> directionValidator,
                 BiConsumer<SkillContext, int[]> directionalEffect) {
        this.name               = name;
        this.description        = description;
        this.icon               = icon;
        this.goldCost           = goldCost;
        this.unlocked           = (goldCost == 0);
        this.maxCooldown        = maxCooldown;
        this.cooldownLeft       = 0;
        this.requiresDirection  = true;
        this.directionValidator = directionValidator;
        this.instantEffect      = null;
        this.directionalEffect  = directionalEffect;
    }

    // ── Unlock ────────────────────────────────────────────────────────────────

    public boolean isUnlocked()  { return unlocked; }
    public int     getGoldCost() { return goldCost; }

    /**
     * Attempts to unlock this skill by spending gold from the player in ctx.
     * @return true if unlocked successfully, false if not enough gold or already unlocked
     */
    public boolean tryUnlock(SkillContext ctx) {
        if (unlocked) return false;
        if (ctx.getPlayer().getGold() < goldCost) {
            ctx.addMessage("Not enough gold! Need " + goldCost + "g to unlock " + name + ".");
            return false;
        }
        ctx.getPlayer().spendGold(goldCost);
        unlocked = true;
        ctx.addMessage(name + " unlocked for " + goldCost + "g!");
        return true;
    }

    // ── Cooldown ──────────────────────────────────────────────────────────────

    public boolean isReady()       { return cooldownLeft <= 0; }
    public int     getCooldownLeft() { return cooldownLeft; }
    public int     getMaxCooldown()  { return maxCooldown; }

    /** Ticks the cooldown down by one turn. */
    public void tick() { if (cooldownLeft > 0) cooldownLeft--; }

    // ── Targeting ─────────────────────────────────────────────────────────────

    public boolean requiresDirection() { return requiresDirection; }

    /**
     * Activates this instant skill.
     * Returns false if locked, on cooldown, or this is a directional skill.
     */
    public boolean activateInstant(SkillContext ctx) {
        if (!unlocked || !isReady() || requiresDirection) return false;
        instantEffect.accept(ctx);
        cooldownLeft = maxCooldown;
        return true;
    }

    /**
     * Activates this directional skill with the given (dx, dy).
     * Runs the validator first; if invalid, cancels without consuming the cooldown.
     * @return true if activated, false if direction invalid or skill not ready
     */
    public boolean activateDirectional(SkillContext ctx, int dx, int dy) {
        if (!unlocked || !isReady() || !requiresDirection) return false;
        int[] dir = {dx, dy};
        if (directionValidator != null && !directionValidator.apply(ctx, dir)) {
            // Validator already posted a message
            return false;
        }
        directionalEffect.accept(ctx, dir);
        cooldownLeft = maxCooldown;
        return true;
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public String getName()        { return name; }
    public String getDescription() { return description; }
    public String getIcon()        { return icon; }

    // ── SkillContext ──────────────────────────────────────────────────────────

    /**
     * Bundles the game-state data skill effects need, without a hard dependency
     * on the {@code game.core} package.
     */
    public static class SkillContext {
        private final game.entity.characters.Player player;
        private final java.util.List<game.entity.hostile.Enemy> enemies;
        private final java.util.function.BiFunction<Integer, Integer, Boolean> isVisible;
        private final java.util.function.BiFunction<Integer, Integer, Boolean> isWalkable;
        private final java.util.function.Consumer<String> addMessage;
        private final java.util.Random random;

        public SkillContext(game.entity.characters.Player player,
                            java.util.List<game.entity.hostile.Enemy> enemies,
                            java.util.function.BiFunction<Integer, Integer, Boolean> isVisible,
                            java.util.function.BiFunction<Integer, Integer, Boolean> isWalkable,
                            java.util.function.Consumer<String> addMessage,
                            java.util.Random random) {
            this.player     = player;
            this.enemies    = enemies;
            this.isVisible  = isVisible;
            this.isWalkable = isWalkable;
            this.addMessage = addMessage;
            this.random     = random;
        }

        public game.entity.characters.Player getPlayer() { return player; }
        public java.util.List<game.entity.hostile.Enemy> getEnemies() { return enemies; }
        public boolean isVisible(int x, int y)  { return isVisible.apply(x, y); }
        public boolean isWalkable(int x, int y) { return isWalkable.apply(x, y); }
        public void addMessage(String msg)       { addMessage.accept(msg); }
        public java.util.Random getRandom()      { return random; }
    }
}
