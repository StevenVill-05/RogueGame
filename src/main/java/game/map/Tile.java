package game.map;

/**
 * Represents the possible terrain types for a single map cell.
 * Each tile carries a text-mode symbol for debugging/fallback rendering.
 */
public enum Tile {
    WALL('#'),
    FLOOR('.'),
    STAIR('>');

    private final char symbol;

    /** Constructs a Tile enum constant with the given text symbol. */
    Tile(char symbol) {
        this.symbol = symbol;
    }

    /** Returns the single ASCII character representing this tile type. */
    public char getSymbol() {
        return symbol;
    }
}
