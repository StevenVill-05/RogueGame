package game.entity.item;

public class Item {

    /** The two kinds of collectible items in the dungeon. */
    public enum Type { POTION, GOLD }

    private final int x, y;
    private final char symbol;
    private final String name;
    private final Type type;

    // ── Constructor ───────────────────────────────────────────────────────────

    /**
     * Creates an item at tile (x, y) with the given display symbol, name, and type.
     *
     * @param x      tile column
     * @param y      tile row
     * @param symbol character rendered on the map (e.g. '!' for potions, '$' for gold)
     * @param name   human-readable label shown in pick-up messages
     * @param type   POTION or GOLD, determines the effect when collected
     */
    public Item(int x, int y, char symbol, String name, Type type) {
        this.x      = x;
        this.y      = y;
        this.symbol = symbol;
        this.name   = name;
        this.type   = type;
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    /** Returns the item's X tile coordinate. */
    public int getX()       { return x; }

    /** Returns the item's Y tile coordinate. */
    public int getY()       { return y; }

    /** Returns the single character drawn on the map for this item. */
    public char getSymbol() { return symbol; }

    /** Returns the item's display name (used in log messages). */
    public String getName() { return name; }

    /** Returns the item type (POTION or GOLD), used to apply its effect. */
    public Type getType()   { return type; }
}
