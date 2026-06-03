package game.core;

import game.entity.characters.Player;
import game.entity.hostile.ArcherGoblin;
import game.entity.hostile.Enemy;
import game.entity.hostile.Goblin;
import game.entity.hostile.Troll;
import game.entity.item.Item;
import game.map.DungeonMap;
import game.map.Tile;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GameState {
    private Player player;
    private DungeonMap map;
    private List<Enemy> enemies;
    private List<Item> items;
    private int floor;
    private boolean gameOver;
    private final List<String> messages = new ArrayList<>();

    //map vision variables
    private boolean[][] visible;
    private boolean[][] revealed;
    private static final int VISION_RADIUS = 5;

    //random generator
    private final Random random = new Random();

    //player selected
    public void init(Player selectedPlayer) {
        floor = 1;
        player = selectedPlayer;
        enemies = new ArrayList<>();
        items = new ArrayList<>();
        addMessage("Welcome to the dungeon! Find the stairs (>) to descend.");
        gameOver = false;
        generateLevel();
    }

    //level generated, spawns player in it
    public void generateLevel() {
        map = new DungeonMap(80, 30, random);
        map.generate();

        visible  = new boolean[map.getHeight()][map.getWidth()];
        revealed = new boolean[map.getHeight()][map.getWidth()];

        int[] start = map.getStartPosition();
        player.setX(start[0]);
        player.setY(start[1]);

        enemies.clear();
        items.clear();
        spawnEnemies();
        spawnItems();
        updateFog();
    }

    //Mob generator **needs to change the selectin algo to be more efficient
    //i:  variable that decides which mob to generate, based on a list where each mob has a number assigned to them
    private void spawnMob(int i, int x, int y){
        Enemy mob =
                switch(i) {
                    case 1 -> new Goblin(x, y);
                    case 2 -> new ArcherGoblin(x, y);
                    case 3 -> new Troll(x, y);
                    default -> null;
                };
        if (mob!=null) enemies.add(mob);
    }

    //enemy spawner: spawns enemy in a room, enemy type is decided by spawnMob
    private void spawnEnemies() {
        /*possible enemies*/
        int lv = 1;
        if (floor>=3) lv = 2;
        if(floor>=5) lv = 3;
        Random random = new Random();
        int p = random.nextInt(lv)+1;

        // ########################### //
        List<int[]> rooms = map.getRoomCenters();
        for (int i = 1; i < rooms.size(); i++) {
            int count = 1 + random.nextInt(2);
            for (int c = 0; c < count; c++) {
                int[] room = map.getRandomFloorInRoom(i);
                if (room != null) {
                    spawnMob(p,room[0],room[1]);
                }
            }
        }
    }

    //item spawner
    private void spawnItems() {
        List<int[]> rooms = map.getRoomCenters();
        for (int i = 1; i < rooms.size(); i++) {
            if (random.nextFloat() < 0.6f) {
                int[] pos = map.getRandomFloorInRoom(i);
                if (pos != null) {
                    boolean isPotion = random.nextBoolean();
                    items.add(isPotion
                        ? new Item(pos[0], pos[1], '!', "Potion", Item.Type.POTION)
                        : new Item(pos[0], pos[1], '$', "Gold",   Item.Type.GOLD));
                }
            }
        }
    }

    //clears the fog around the player based on radius of vision
    private void updateFog() {
        for (boolean[] row : visible) java.util.Arrays.fill(row, false);
        int px = player.getX(), py = player.getY();
        for (int dy = -VISION_RADIUS; dy <= VISION_RADIUS; dy++) {
            for (int dx = -VISION_RADIUS; dx <= VISION_RADIUS; dx++) {
                if (dx * dx + dy * dy > VISION_RADIUS * VISION_RADIUS) continue;
                int tx = px + dx, ty = py + dy;
                if (tx < 0 || ty < 0 || tx >= map.getWidth() || ty >= map.getHeight()) continue;
                if (hasLos(px, py, tx, ty)) {
                    visible[ty][tx]  = true;
                    revealed[ty][tx] = true;
                }
            }
        }
    }

    //Line of sight
    private boolean hasLos(int x0, int y0, int x1, int y1) {
        int dx = Math.abs(x1 - x0), dy = Math.abs(y1 - y0);
        int sx = x0 < x1 ? 1 : -1, sy = y0 < y1 ? 1 : -1;
        int err = dx - dy, cx = x0, cy = y0;
        while (true) {
            if (cx == x1 && cy == y1) return true;
            if (map.getTile(cx, cy) == Tile.WALL) return false;
            int e2 =  2*err;
            if (e2 > -dy) { err -= dy; cx += sx; }
            if (e2 <  dx) { err += dx; cy += sy; }
        }
    }

    //Moves player, depending on the direction it can become an attack or get stopped by a wall
    public void movePlayer(int dx, int dy) {
        int nx = player.getX() + dx, ny = player.getY() + dy;
        if (!map.isWalkable(nx, ny)) return;

        Enemy target = getEnemyAt(nx, ny);
        if (target != null) {
            int dmg = player.getAttack();
            target.takeDamage(dmg);
            if (target.isDead()) {
                enemies.remove(target);
                player.addKill();
                addMessage("You killed the " + target.getName() + "!");
            } else {
                addMessage("Hit " + target.getName() + " for " + dmg + " dmg (" + target.getHp() + "/" + target.getMaxHp() + " left)");
            }
        } else {
            player.setX(nx);
            player.setY(ny);

            Item item = getItemAt(nx, ny);
            if (item != null) { applyItem(item); items.remove(item); }
            else if (map.getTile(nx, ny) == Tile.STAIR) {
                floor++;
                addMessage("You descend to floor " + floor + "...");
                player.heal(player.getMaxHp());
                generateLevel();

                return;
            }
            updateFog();
        }
        enemyTurns();
    }

    //applies item (money or potion)
    private void applyItem(Item item) {
        switch (item.getType()) {
            case POTION: { int h = 2 + random.nextInt(4); player.heal(h);    addMessage("Drank a potion, healed " + h + " HP!"); break; }
            case GOLD:   { int g = 3 + random.nextInt(8); player.addGold(g); addMessage( "Picked up " + g + " gold!"); break; }
        }
    }

    //enemy takes action
    private void enemyTurns() {
        for (Enemy e : enemies) {
            if (!visible[e.getY()][e.getX()]) continue;
            int ddx = player.getX() - e.getX(), ddy = player.getY() - e.getY();
            double dist = Math.sqrt(Math.pow(ddx,2)+Math.pow(ddy,2));
            if (dist > 10) continue;
            int mx = 0, my = 0;
            if (Math.abs(ddx) >= Math.abs(ddy)) mx = ddx > 0 ? 1 : -1;
            else my = ddy > 0 ? 1 : -1;

            int ex = e.getX() + mx, ey = e.getY() + my;

            if (dist<=e.getRange()) {
                if(e.atk(e.getAcc())){
                    int dmg = e.getAttack();
                    player.takeDamage(dmg);
                    addMessage(e.getName() + " hits you for " + dmg + "! (" + player.getHp() + " HP left)");
                }

                if (player.isDead()) { gameOver = true; }
            } else if (map.isWalkable(ex, ey) && getEnemyAt(ex, ey) == null) {
                e.setX(ex); e.setY(ey);
            }
        }
    }

    //gets enemy in [x,y]
    private Enemy getEnemyAt(int x, int y) {
        return enemies.stream().filter(e -> e.getX() == x && e.getY() == y).findFirst().orElse(null);
    }
    //gets item in [x,y]
    private Item getItemAt(int x, int y) {
        return items.stream().filter(i -> i.getX() == x && i.getY() == y).findFirst().orElse(null);
    }

    //checks if coords [x,y] is visible
    public boolean isVisible(int x, int y) {
        if (x < 0 || y < 0 || x >= map.getWidth() || y >= map.getHeight()) return false;
        return visible[y][x];
    }

    //checks if [x,y] are revealed tiles
    public boolean isRevealed(int x, int y) {
        if (x < 0 || y < 0 || x >= map.getWidth() || y >= map.getHeight()) return false;
        return revealed[y][x];
    }

    //message
    public void addMessage(String msg) {
        messages.add(msg);
        if (messages.size() > 4) messages.remove(0); // keep last 4
    }

    //getter
    public Player getPlayer()       { return player; }
    public DungeonMap getMap()      { return map; }
    public List<Enemy> getEnemies() { return enemies; }
    public List<Item> getItems()    { return items; }
    public int getFloor()           { return floor; }
    public List<String> getMessages() { return messages; }

    //state
    public boolean isGameOver()     { return gameOver; }
    public void setGameOver(boolean v) { gameOver = v; }
}
