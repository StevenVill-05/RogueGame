package game.entity.characters;

public class Rogue extends Player {

    /**
     * Creates a Rogue at (x, y).
     * Low HP, high attack — a glass-cannon striker.
     * Loads the rogue2 sprite for rendering.
     */
    public Rogue(int x, int y) {
        super(x, y, 12, 6);
        setSymbol("🗡");
        setName("Rogue");
        setSpritePath("/sprites/rogue2.png");
    }
}
