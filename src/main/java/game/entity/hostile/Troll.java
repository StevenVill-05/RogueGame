package game.entity.hostile;

public class Troll extends Enemy {

    /**
     * Creates a Troll at (x, y).
     * A heavy melee enemy: high HP, low damage, 100% hit chance, melee range.
     */
    public Troll(int x, int y) {
        super(x, y, "\uD83D\uDC79", "Troll", 20, 1, 1, 100);
    }
}
