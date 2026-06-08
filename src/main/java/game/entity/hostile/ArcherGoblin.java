package game.entity.hostile;

public class ArcherGoblin extends Enemy {

    /**
     * Creates an ArcherGoblin at (x, y).
     * A ranged enemy: moderate HP, low damage, 30% hit chance, 2-tile attack range.
     */
    public ArcherGoblin(int x, int y,int lv) {
        super(x, y, "\uD83C\uDFF9", "ArcherGoblin", 20+(20*lv*0.1), 1+lv*0.1, 2, 30+30*0.1*lv);
        setSpritePath("/sprites/archer_goblin.png");
    }
}
