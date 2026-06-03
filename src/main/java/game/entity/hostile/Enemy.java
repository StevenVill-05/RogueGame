package game.entity.hostile;
import java.util.Random;

public class Enemy implements HostileActions {

    private int x, y;
    private final String symbol;
    private final String name;
    private int hp, maxHp;
    private int attack;
    private int range;
    private int acc;


    //constructor
    public Enemy(int x, int y, String symbol, String name, int maxHp, int attack, int range, int acc) {
        this.x = x;
        this.y = y;
        this.symbol = symbol;
        this.name = name;
        this.maxHp = maxHp;
        this.hp = maxHp;
        this.attack = attack;
        this.range = range;
        this.acc = acc;
    }

    //state
    public boolean isDead()         { return hp <= 0; }

    //action
    public void takeDamage(int dmg) { hp = Math.max(0, hp - dmg); }
    public boolean atk (int acc){
        Random random = new Random();
        int x = random.nextInt(100)+1;
        return x<=acc;
    }

    //getters
    public int getX()       { return x; }
    public int getY()       { return y; }
    public String getSymbol() { return symbol; }
    public String getName() { return name; }
    public int getHp()      { return hp; }
    public int getMaxHp()   { return maxHp; }
    public int getAttack()  { return attack; }
    public int getRange(){return range;}
    public int getAcc() {return acc;}

    //setters
    public void setX(int x) { this.x = x; }
    public void setY(int y) { this.y = y; }
}
