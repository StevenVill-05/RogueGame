package game.map;

import java.util.Random;

/**
 * Represents a rectangular room carved into the dungeon map.
 * Stores its top-left origin (x, y) and its dimensions (width, height).
 */
public class Room {
    public final int x, y, width, height;

    /**
     * Creates a room with the given top-left position and size.
     *
     * @param x      left-most tile column
     * @param y      top-most tile row
     * @param width  number of columns
     * @param height number of rows
     */
    public Room(int x, int y, int width, int height) {
        this.x      = x;
        this.y      = y;
        this.width  = width;
        this.height = height;
    }

    /** Returns the tile column at the horizontal center of this room. */
    public int centerX() { return x + width / 2; }

    /** Returns the tile row at the vertical center of this room. */
    public int centerY() { return y + height / 2; }

    /**
     * Returns true if this room's bounding box overlaps with {@code other},
     * including a 1-tile buffer to keep rooms separated by at least one wall.
     */
    public boolean intersects(Room other) {
        return x < other.x + other.width  + 1
            && x + width  + 1 > other.x
            && y < other.y + other.height + 1
            && y + height + 1 > other.y;
    }

    /**
     * Returns a random floor position inside this room (inset by 1 tile on each side).
     * The result is a two-element int array: {tileX, tileY}.
     */
    public int[] randomFloor(Random random) {
        int rx = x + 1 + random.nextInt(Math.max(1, width  - 2));
        int ry = y + 1 + random.nextInt(Math.max(1, height - 2));
        return new int[]{rx, ry};
    }
}
