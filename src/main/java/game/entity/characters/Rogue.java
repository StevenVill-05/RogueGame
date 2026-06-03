package game.entity.characters;

public class Rogue extends Player {
    public Rogue(int x, int y) {
        super(x, y, 12, 6);  // low HP, high attack
        setSymbol("🗡");
        setName("Rogue");
    }
}
