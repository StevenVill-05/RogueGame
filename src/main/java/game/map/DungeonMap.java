package game.map;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class DungeonMap {
    private final int width, height;
    private final Tile[][] tiles;
    private final List<Room> rooms = new ArrayList<>();
    private final Random random;
    public DungeonMap(int width, int height, Random random) {
        this.width = width;
        this.height = height;
        this.random = random;
        this.tiles = new Tile[height][width];
    }

    //generates map
    public void generate() {
        // Fill map with walls
        for (int y = 0; y < height; y++)
            for (int x = 0; x < width; x++)
                tiles[y][x] = Tile.WALL;

        rooms.clear();
        int attempts = 30;

        for (int i = 0; i < attempts; i++) {
            int w = 5 + random.nextInt(8);
            int h = 3 + random.nextInt(5);
            int rx = 1 + random.nextInt(width - w - 2);
            int ry = 1 + random.nextInt(height - h - 2);
            Room room = new Room(rx, ry, w, h);

            boolean overlap = rooms.stream().anyMatch(r -> r.intersects(room));
            if (overlap) continue;

            carveRoom(room);
            if (!rooms.isEmpty()) {
                Room prev = rooms.get(rooms.size() - 1);
                carveCorridor(prev.centerX(), prev.centerY(), room.centerX(), room.centerY());
            }
            rooms.add(room);
        }

        // Place stairs in last room
        if (!rooms.isEmpty()) {
            Room last = rooms.get(rooms.size() - 1);
            tiles[last.centerY()][last.centerX()] = Tile.STAIR;
        }
    }

    //create rooms
    private void carveRoom(Room room) {
        for (int y = room.y; y < room.y + room.height; y++)
            for (int x = room.x; x < room.x + room.width; x++)
                tiles[y][x] = Tile.FLOOR;
    }

    //connect rooms
    private void carveCorridor(int x1, int y1, int x2, int y2) {
        int cx = x1, cy = y1;
        // Horizontal then vertical
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

    public boolean isWalkable(int x, int y) {
        if (x < 0 || y < 0 || x >= width || y >= height) return false;
        return tiles[y][x] != Tile.WALL;
    }

    //getters
    public Tile getTile(int x, int y) {
        if (x < 0 || y < 0 || x >= width || y >= height) return Tile.WALL;
        return tiles[y][x];
    }

    public int[] getStartPosition() {
        if (rooms.isEmpty()) return new int[]{1, 1};
        Room first = rooms.get(0);
        return new int[]{first.centerX(), first.centerY()};
    }

    public List<int[]> getRoomCenters() {
        return rooms.stream().map(r -> new int[]{r.centerX(), r.centerY()}).toList();
    }

    public int[] getRandomFloorInRoom(int index) {
        if (index >= rooms.size()) return null;
        return rooms.get(index).randomFloor(random);
    }

    public int getWidth()  { return width; }
    public int getHeight() { return height; }
    public Tile[][] getTiles() { return tiles; }
    public List<Room> getRooms() { return rooms; }
}
