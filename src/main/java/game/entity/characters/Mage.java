package game.entity.characters;

public class Mage extends Player {

    /**
     * Mage skills:
     *  [1] Fireball   — cost 15g, instant blast on all visible enemies for 5 dmg
     *  [2] Blink      — cost 30g, directional teleport 3 tiles (blocked by walls)
     *  [3] Drain Life — cost 45g, directional drain: steal HP from enemy in that direction
     */
    public Mage(int x, int y) {
        super(x, y, 8, 8);
        setSymbol("🔮");
        setName("Mage");
        setSpritePath("/sprites/mage.png");

        // ── Skill 1: Fireball (instant) ───────────────────────────────────────
        addSkill(new Skill("Fireball", "Blast all visible enemies for 5 dmg", "🔥",
            15, 6,
            ctx -> {
                int hit = 0;
                for (game.entity.hostile.Enemy e : ctx.getEnemies()) {
                    if (ctx.isVisible(e.getX(), e.getY())) {
                        e.takeDamage(5);
                        hit++;
                    }
                }
                ctx.addMessage("Fireball! Scorched " + hit + " enemies for 5 dmg each.");
            }));

        // ── Skill 2: Blink (directional) ──────────────────────────────────────
        // Teleports 3 tiles in chosen direction; each intermediate tile must be walkable
        addSkill(new Skill("Blink", "Teleport 3 tiles in chosen direction", "✨",
            30, 8,
            // validator: the destination 3 tiles away must be walkable (all steps checked)
            (ctx, dir) -> {
                int px = ctx.getPlayer().getX(), py = ctx.getPlayer().getY();
                for (int step = 1; step <= 3; step++) {
                    int tx = px + dir[0] * step, ty = py + dir[1] * step;
                    if (!ctx.isWalkable(tx, ty)) {
                        ctx.addMessage("Blink! A wall blocks that direction.");
                        return false;
                    }
                }
                return true;
            },
            (ctx, dir) -> {
                int px = ctx.getPlayer().getX(), py = ctx.getPlayer().getY();
                ctx.getPlayer().setX(px + dir[0] * 3);
                ctx.getPlayer().setY(py + dir[1] * 3);
                ctx.addMessage("Blink! You teleport 3 tiles!");
            }));

        // ── Skill 3: Drain Life (directional) ────────────────────────────────
        // Drains an enemy in the chosen direction (must be adjacent)
        addSkill(new Skill("Drain Life", "Steal 4 HP from adjacent enemy in chosen direction", "💜",
            45, 5,
            // validator: there must be an enemy in that adjacent tile
            (ctx, dir) -> {
                int tx = ctx.getPlayer().getX() + dir[0];
                int ty = ctx.getPlayer().getY() + dir[1];
                boolean hasEnemy = ctx.getEnemies().stream()
                    .anyMatch(e -> e.getX() == tx && e.getY() == ty);
                if (!hasEnemy) {
                    ctx.addMessage("Drain Life! No enemy in that direction.");
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
                    target.takeDamage(4);
                    ctx.getPlayer().heal(4);
                    ctx.addMessage("Drain Life! Stole 4 HP from " + target.getName() + ".");
                }
            }));
    }
}
