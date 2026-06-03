package game.entity.characters;


public class Mage extends Player {
    public Mage(int x, int y) {
        super(x, y, 8, 8);   // very fragile, very powerful
        setSymbol("🔮");
        setName("Mage");
    }
}
