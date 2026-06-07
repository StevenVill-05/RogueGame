package game.entity.characters;

public class Warrior extends Player {

    /**
     * Creates a Warrior at (x, y).
     * High HP, low attack — a durable frontline fighter.
     * Loads the warrior2 sprite for rendering.
     */
    public Warrior(int x, int y) {
        super(x, y, 20, 2);
        setSymbol("⚜️");
        setName("Warrior");
        setSpritePath("/sprites/warrior2.png");
    }
}
