package game.map;

import java.util.Random;

public class Room {
    public final int x, y, width, height;

    public Room(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public int centerX() { return x + width / 2; }
    public int centerY() { return y + height / 2; }

    public boolean intersects(Room other) {
        return x < other.x + other.width + 1
            && x + width + 1 > other.x
            && y < other.y + other.height + 1
            && y + height + 1 > other.y;
    }

    public int[] randomFloor(Random random) {
        int rx = x + 1 + random.nextInt(Math.max(1, width - 2));
        int ry = y + 1 + random.nextInt(Math.max(1, height - 2));
        return new int[]{rx, ry};
    }
}