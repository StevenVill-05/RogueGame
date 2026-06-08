package game.map;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Procedurally generated dungeon map.
 * Builds a grid of Tiles by placing non-overlapping rectangular Rooms
 * and connecting them with L-shaped corridors.
 */
public class DungeonMap {
    private final int width, height;
    private final Tile[][] tiles;
    private final List<Room> rooms = new ArrayList<>();
    private final Random random;

    /**
     * Allocates the tile grid. Call {@link #generate()} afterwards
     * to populate it with rooms and corridors.
     *
     * @param width  number of tile columns
     * @param height number of tile rows
     * @param random shared Random instance for reproducible generation
     */
    public DungeonMap(int width, int height, Random random) {
        this.width  = width;
        this.height = height;
        this.random = random;
        this.tiles  = new Tile[height][width];
    }

    // ── Generation ────────────────────────────────────────────────────────────

    /**
     * Fills the entire grid with walls, then carves up to 30 rooms.
     * Each new room is connected to the previous one via an L-shaped corridor.
     * A staircase tile is placed at the centre of the last room.
     */
    public void generate() {
        for (int y = 0; y < height; y++)
            for (int x = 0; x < width; x++)
                tiles[y][x] = Tile.WALL;

        rooms.clear();
        int attempts = 30;

        for (int i = 0; i < attempts; i++) {
            int w  = 5 + random.nextInt(8);
            int h  = 3 + random.nextInt(5);
            int rx = 1 + random.nextInt(width  - w - 2);
            int ry = 1 + random.nextInt(height - h - 2);
            Room room = new Room(rx, ry, w, h);

            boolean overlap = rooms.stream().anyMatch(r -> r.intersects(room));
            if (overlap) continue;

            carveRoom(room);
            if (!rooms.isEmpty()) {
                Room prev = rooms.get(rooms.size() - 1);
                carveCorridor(prev.centerX(), prev.centerY(),
                              room.centerX(), room.centerY());
            }
            rooms.add(room);
        }

        if (!rooms.isEmpty()) {
            Room last = rooms.get(rooms.size() - 1);
            tiles[last.centerY()][last.centerX()] = Tile.STAIR;
        }
    }

    /**
     * Sets every tile inside {@code room} to FLOOR.
     *
     * @param room the room whose interior should be carved out
     */
    private void carveRoom(Room room) {
        for (int y = room.y; y < room.y + room.height; y++)
            for (int x = room.x; x < room.x + room.width; x++)
                tiles[y][x] = Tile.FLOOR;
    }

    /**
     * Carves an L-shaped corridor of FLOOR tiles between two tile positions.
     * Moves horizontally first, then vertically.
     *
     * @param x1 starting column
     * @param y1 starting row
     * @param x2 destination column
     * @param y2 destination row
     */
    private void carveCorridor(int x1, int y1, int x2, int y2) {
        int cx = x1, cy = y1;
        while (cx != x2) {
            if (cx >= 0 && cx < width && cy >= 0 && cy < height)
                tiles[cy][cx] = Tile.FLOOR;
            cx += cx < x2 ? 1 : -1;
        }
        while (cy != y2) {
            if (cx >= 0 && cx < width && cy >= 0 && cy < height)
                tiles[cy][cx] = Tile.FLOOR;
            cy += cy < y2 ? 1 : -1;
        }
        tiles[cy][cx] = Tile.FLOOR;
    }

    // ── Queries ───────────────────────────────────────────────────────────────

    /**
     * Returns true if the tile at (x, y) is not a wall and is inside the map bounds.
     * Used by game logic to validate movement and pathfinding targets.
     */
    public boolean isWalkable(int x, int y) {
        if (x < 0 || y < 0 || x >= width || y >= height) return false;
        return tiles[y][x] != Tile.WALL;
    }

    /**
     * Returns the Tile at (x, y), or WALL if the position is out-of-bounds.
     * Safe to call with any coordinates without a bounds check.
     */
    public Tile getTile(int x, int y) {
        if (x < 0 || y < 0 || x >= width || y >= height) return Tile.WALL;
        return tiles[y][x];
    }

    /**
     * Returns the centre position of the first generated room as {x, y}.
     * Used to place the player at the start of a new level.
     * Falls back to {1, 1} if no rooms were generated.
     */
    public int[] getStartPosition() {
        if (rooms.isEmpty()) return new int[]{1, 1};
        Room first = rooms.get(0);
        return new int[]{first.centerX(), first.centerY()};
    }

    /**
     * Returns a list of {x, y} centre-points for every room in the dungeon.
     * Useful for deciding where to place enemies and items.
     */
    public List<int[]> getRoomCenters() {
        return rooms.stream().map(r -> new int[]{r.centerX(), r.centerY()}).toList();
    }

    /**
     * Returns a random walkable floor position inside the room at the given index,
     * or null if the index is out of range.
     *
     * @param index 0-based index into the generated room list
     */
    public int[] getRandomFloorInRoom(int index) {
        if (index >= rooms.size()) return null;
        return rooms.get(index).randomFloor(random);
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    /** Returns the total number of tile columns in this map. */
    public int getWidth()  { return width; }

    /** Returns the total number of tile rows in this map. */
    public int getHeight() { return height; }

    /** Returns the raw 2D tile array (row-major: tiles[y][x]). */
    public Tile[][] getTiles() { return tiles; }

    /** Returns the list of all rooms generated during {@link #generate()}. */
    public List<Room> getRooms() { return rooms; }
}
