package game.entity.characters;

public class Mage extends Player {

    /**
     * Creates a Mage at (x, y).
     * Very low HP, very high attack — fragile but devastating.
     * Loads the mage sprite for rendering.
     */
    public Mage(int x, int y) {
        super(x, y, 8, 8);
        setSymbol("🔮");
        setName("Mage");
        setSpritePath("/sprites/mage.png");
    }
}
