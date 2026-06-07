package game.entity.characters;

public class Warrior extends Player {

    /**
     * Warrior skills (all directional — swing matters):
     *  [1] Shield Bash   — cost 15g, push+damage enemy in chosen direction
     *  [2] War Cry       — cost 25g, instant AoE taunt (no direction needed)
     *  [3] Cleave        — cost 40g, strike all enemies in a directional arc (3 tiles)
     */
    public Warrior(int x, int y) {
        super(x, y, 20, 2);
        setSymbol("⚜️");
        setName("Warrior");
        setSpritePath("/sprites/warrior2.png");

        // ── Skill 1: Shield Bash (directional) ────────────────────────────────
        // Direction must have a walkable tile (not a wall) to bash into
        addSkill(new Skill("Shield Bash", "Bash enemy in chosen direction, dealing 4 dmg", "🛡",
            15, 4,
            // validator: direction tile must be walkable or occupied by enemy
            (ctx, dir) -> {
                int tx = ctx.getPlayer().getX() + dir[0];
                int ty = ctx.getPlayer().getY() + dir[1];
                boolean hasTarget = ctx.getEnemies().stream()
                    .anyMatch(e -> e.getX() == tx && e.getY() == ty);
                if (!hasTarget && !ctx.isWalkable(tx, ty)) {
                    ctx.addMessage("Shield Bash! Nothing to bash that way.");
                    return false;
                }
                return true;
            },
            (ctx, dir) -> {
                int tx = ctx.getPlayer().getX() + dir[0];
                int ty = ctx.getPlayer().getY() + dir[1];
                game.entity.hostile.Enemy hit = ctx.getEnemies().stream()
                    .filter(e -> e.getX() == tx && e.getY() == ty)
                    .findFirst().orElse(null);
                if (hit != null) {
                    hit.takeDamage(4);
                    ctx.addMessage("Shield Bash! Hit " + hit.getName() + " for 4 dmg!");
                } else {
                    ctx.getPlayer().heal(3);
                    ctx.addMessage("Shield Bash! You brace and recover 3 HP.");
                }
            }));

        // ── Skill 2: War Cry (instant) ────────────────────────────────────────
        addSkill(new Skill("War Cry", "Terrify all visible enemies for 2 dmg each", "📣",
            25, 5,
            ctx -> {
                int hit = 0;
                for (game.entity.hostile.Enemy e : ctx.getEnemies()) {
                    if (ctx.isVisible(e.getX(), e.getY())) {
                        e.takeDamage(2);
                        hit++;
                    }
                }
                ctx.addMessage("War Cry! " + hit + " enemies cower, each taking 2 dmg.");
            }));

        // ── Skill 3: Cleave (directional) ─────────────────────────────────────
        // Strikes enemies in a 3-tile line in chosen direction
        addSkill(new Skill("Cleave", "Strike all enemies in a line (3 tiles) for 2x dmg", "⚔",
            40, 5,
            // validator: at least one walkable tile in that direction
            (ctx, dir) -> {
                if (dir[0] == 0 && dir[1] == 0) return false;
                int tx = ctx.getPlayer().getX() + dir[0];
                int ty = ctx.getPlayer().getY() + dir[1];
                if (!ctx.isWalkable(tx, ty)) {
                    ctx.addMessage("Cleave! A wall blocks your swing.");
                    return false;
                }
                return true;
            },
            (ctx, dir) -> {
                int px = ctx.getPlayer().getX(), py = ctx.getPlayer().getY();
                int dmg = ctx.getPlayer().getAttack() * 2;
                int hit = 0;
                for (int step = 1; step <= 3; step++) {
                    int tx = px + dir[0] * step, ty = py + dir[1] * step;
                    for (game.entity.hostile.Enemy e : ctx.getEnemies()) {
                        if (e.getX() == tx && e.getY() == ty) {
                            e.takeDamage(dmg);
                            hit++;
                        }
                    }
                }
                ctx.addMessage("Cleave! Hit " + hit + " enemies for " + dmg + " dmg each!");
            }));
    }
}
