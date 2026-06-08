package game.entity.characters;

/**
 * The Rogue is a high-risk, high-reward striker that excels at burst damage
 * and gold accumulation. Lower HP than the Warrior but higher base attack,
 * with skills that punish clustered enemies and reward careful positioning.
 *
 * <pre>
 *   HP: 12  |  ATK: 6  (per hit)
 * </pre>
 *
 * Skills (unlocked with gold):
 * <ul>
 *   <li>[1] Backstab (directional, 15g, CD 5) — deal 3× attack damage to one adjacent enemy.</li>
 *   <li>[2] Smoke Bomb (instant, 25g, CD 6) — instantly recover 4 HP.</li>
 *   <li>[3] Pickpocket (directional, 35g, CD 4) — steal 8–18 gold from an adjacent enemy.</li>
 * </ul>
 */
public class Rogue extends Player {

    /**
     * Rogue skills:
     *  [1] Backstab   — cost 15g, directional triple-damage strike (must be enemy in that tile)
     *  [2] Smoke Bomb — cost 25g, instant: heal 4 HP and vanish
     *  [3] Pickpocket — cost 35g, directional: steal gold from enemy in chosen direction
     */
    public Rogue(int x, int y) {
        super(x, y, 12, 6);
        setSymbol("🗡");
        setName("Rogue");
        setSpritePath("/sprites/rogue2.png");

        // ── Skill 1: Backstab (directional) ───────────────────────────────────
        // Must have an enemy in the adjacent tile in the chosen direction
        addSkill(new Skill("Backstab", "Triple damage to adjacent enemy in chosen direction", "🗡",
            15, 5,
            // validator: enemy must be in the adjacent tile
            (ctx, dir) -> {
                int tx = ctx.getPlayer().getX() + dir[0];
                int ty = ctx.getPlayer().getY() + dir[1];
                boolean hasEnemy = ctx.getEnemies().stream()
                    .anyMatch(e -> e.getX() == tx && e.getY() == ty);
                if (!hasEnemy) {
                    ctx.addMessage("Backstab! No enemy in that direction.");
                    return false;
                }
                return true;
            },
            (ctx, dir) -> {
                int tx = ctx.getPlayer().getX() + dir[0];
                int ty = ctx.getPlayer().getY() + dir[1];
                game.entity.hostile.Enemy target = ctx.getEnemies().stream()
                    .filter(e -> e.getX() == tx && e.getY() == ty)
                    .findFirst().orElse(null);
                if (target != null) {
                    int dmg = ctx.getPlayer().getAttack() * 3;
                    target.takeDamage(dmg);
                    ctx.addMessage("Backstab! Dealt " + dmg + " dmg to " + target.getName() + "!");
                }
            }));

        // ── Skill 2: Smoke Bomb (instant) ─────────────────────────────────────
        addSkill(new Skill("Smoke Bomb", "Vanish in smoke and recover 4 HP", "💨",
            25, 6,
            ctx -> {
                ctx.getPlayer().heal(4);
                ctx.addMessage("Smoke Bomb! You vanish and recover 4 HP.");
            }));

        // ── Skill 3: Pickpocket (directional) ─────────────────────────────────
        // Must have an enemy in adjacent tile; steals 8–18 gold from it
        addSkill(new Skill("Pickpocket", "Steal 8-18 gold from adjacent enemy", "💰",
            35, 4,
            // validator: enemy must be adjacent in that direction
            (ctx, dir) -> {
                int tx = ctx.getPlayer().getX() + dir[0];
                int ty = ctx.getPlayer().getY() + dir[1];
                boolean hasEnemy = ctx.getEnemies().stream()
                    .anyMatch(e -> e.getX() == tx && e.getY() == ty);
                if (!hasEnemy) {
                    ctx.addMessage("Pickpocket! No one to steal from that way.");
                    return false;
                }
                return true;
            },
            (ctx, dir) -> {
                int gold = 8 + ctx.getRandom().nextInt(11);
                ctx.getPlayer().addGold(gold);
                ctx.addMessage("Pickpocket! Swiped " + gold + "g!");
            }));
    }
}
