package game.entity.hostile;

public class Goblin extends Enemy {

    /**
     * Creates a Goblin at (x, y).
     * A basic melee enemy: moderate HP, low damage, 100% hit chance, melee range.
     */
    public Goblin(int x, int y) {
        super(x, y, "👺", "Goblin", 20, 1, 1, 100);
    }
}
