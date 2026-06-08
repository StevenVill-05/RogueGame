package game.entity.hostile;

public class ArcherGoblin extends Enemy {

    /**
     * Creates an ArcherGoblin at (x, y).
     * A ranged enemy: moderate HP, low damage, 30% hit chance, 2-tile attack range.
     */
    public ArcherGoblin(int x, int y) {
        super(x, y, "\uD83C\uDFF9", "ArcherGoblin", 20, 1, 2, 30);
    }
}
