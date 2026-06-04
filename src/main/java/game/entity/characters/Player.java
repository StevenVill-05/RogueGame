package game.entity.characters;

public class Player {
    private int x, y;
    private int hp, maxHp;
    private int attack;
    private int gold;
    private int kills;
    private String symbol="@";
    private String name   = "Adventurer";

    //constructor
    public Player(int x, int y, int maxHp, int attack) {
        this.x = x;
        this.y = y;
        this.maxHp = maxHp;
        this.hp = maxHp;
        this.attack = attack;
        this.gold = 0;
        this.kills = 0;
    }


    //actions
    public void takeDamage(int dmg) { hp = Math.max(0, hp - dmg); }
    public void heal(int amount)    { hp = Math.min(maxHp, hp + amount); }
    public void addGold(int g)      { gold += g; }
    public void addKill()           { kills++; }

    //state
    public boolean isDead()         { return hp <= 0; }

    //getter
    public int getX()      { return x; }
    public int getY()      { return y; }
    public int getHp()     { return hp; }
    public int getMaxHp()  { return maxHp; }
    public int getAttack() { return attack; }
    public int getGold()   { return gold; }
    public int getKills()  { return kills; }
    public String getSymbol() { return symbol; }
    public String getName()   { return name; }

    //setter
    protected void setSymbol(String s) { this.symbol = s; }
    public void setName(String n)   { this.name = n; }
    public void setX(int x) { this.x = x; }
    public void setY(int y) { this.y = y; }
}
