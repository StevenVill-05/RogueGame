package game.map;

public enum Tile {
    WALL('#'),
    FLOOR('.'),
    STAIR('>');

    private final char symbol;

    Tile(char symbol) {
        this.symbol = symbol;
    }

    public char getSymbol() {
        return symbol;
    }
}
