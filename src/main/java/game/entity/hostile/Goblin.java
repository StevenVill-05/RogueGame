package game.entity.hostile;

public class Goblin extends Enemy {

    /**
     * Creates a Goblin at (x, y).
     * A basic melee enemy: moderate HP, low damage, 100% hit chance, melee range.
     */
    public Goblin(int x, int y,int lv) {
        super(x, y, "👺", "Goblin", 20+(20*lv*0.1), 1+1*0.1*lv, 1, 50+50*0.1*lv);
        setSpritePath("/sprites/goblin.png");
    }
}
