package game.entity.item;

public class Item {
    public enum Type { POTION, GOLD }

    private final int x, y;
    private final char symbol;
    private final String name;
    private final Type type;

    public Item(int x, int y, char symbol, String name, Type type) {
        this.x = x;
        this.y = y;
        this.symbol = symbol;
        this.name = name;
        this.type = type;
    }

    public int getX()       { return x; }
    public int getY()       { return y; }
    public char getSymbol() { return symbol; }
    public String getName() { return name; }
    public Type getType()   { return type; }
}
