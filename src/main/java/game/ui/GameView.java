package game.ui;

import game.core.GameState;
import game.entity.characters.Player;
import game.entity.hostile.Enemy;
import game.entity.item.Item;
import game.map.DungeonMap;
import game.map.Tile;
import javafx.animation.AnimationTimer;
import javafx.geometry.VPos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;

import java.util.List;

public class GameView {

    public static final int TILE      = 28;
    public static final int MAP_COLS  = 30;
    public static final int MAP_ROWS  = 22;
    public static final int HUD_HEIGHT = 60;
    public static final int MSG_HEIGHT = 36;


    public static final int HUD_HEIGHT_RATIO = 60; // base HUD height
    public static final int MSG_HEIGHT_RATIO = 36; // base message height
    public static final int BASE_TILE  = 28;
    public static final int BASE_WIDTH  = BASE_TILE * MAP_COLS;           // 840
    public static final int BASE_HEIGHT = 60 + BASE_TILE * MAP_ROWS + 36; // 712


    // Palette
    private static final Color BG          = Color.web("#0d0b12");
    private static final Color WALL        = Color.web("#2a2240");
    private static final Color WALL_EDGE   = Color.web("#3a3058");
    private static final Color FLOOR       = Color.web("#1e1a2e");
    private static final Color FLOOR_DARK  = Color.web("#17132a");
    private static final Color STAIR_COL   = Color.web("#e2b96a");
    private static final Color PLAYER_COL  = Color.web("#5ba4e0");
    private static final Color GOLD_COL    = Color.web("#f0c040");
    private static final Color POTION_COL  = Color.web("#5be08a");
    private static final Color HUD_BG      = Color.web("#110f1a");
    private static final Color HUD_BORDER  = Color.web("#2e2845");
    private static final Color HP_FULL     = Color.web("#4caf50");
    private static final Color HP_MID      = Color.web("#ff9800");
    private static final Color HP_LOW      = Color.web("#f44336");
    private static final Color TEXT_MAIN   = Color.web("#e8e0f0");
    private static final Color TEXT_DIM    = Color.web("#7a6e8a");
    private static final Color TEXT_GOLD   = Color.web("#f0c040");
    private static final Color TEXT_RED    = Color.web("#e05b5b");
    private static final Color TEXT_CYAN   = Color.web("#5be0d0");
    private static final Color FOG_SEEN    = Color.web("#0d0b12", 0.55);


    private final Stage stage;

    private final GameState state;
    private final Canvas canvas;
    private final BorderPane root;
    private final Font tileFont;
    private final Font hudFont;
    private final Font hudSmallFont;
    private final Font msgFont;
    private boolean needsRedraw = true;
    private Runnable onRestart;


    private int getTile() {
        double availH = stage.getScene().getHeight() - HUD_HEIGHT_RATIO - MSG_HEIGHT_RATIO;
        double availW = stage.getScene().getWidth();
        int tileByH = (int)(availH / MAP_ROWS);
        int tileByW = (int)(availW / MAP_COLS);
        return Math.max(8, Math.min(tileByH, tileByW));
    }



    //Constructor
    public GameView(GameState state, Runnable onRestart, Stage stage) {
        //loads font
        Font.loadFont(getClass().getResourceAsStream("/fonts/Jacquard12-Regular.ttf"), 16);
        //
        this.state = state;
        this.onRestart = onRestart;
        this.stage = stage;

        canvas = new Canvas(BASE_WIDTH, BASE_HEIGHT);
        canvas.setFocusTraversable(true);


        tileFont    = Font.font("Jacquard 12", FontWeight.BOLD, TILE - 6);
        hudFont     = Font.font("Monospaced", FontWeight.BOLD, 32);
        hudSmallFont= Font.font("Monospaced", FontWeight.NORMAL, 24);
        msgFont     = Font.font("Jacquard 12", FontWeight.NORMAL, 26);


        root = new BorderPane(canvas);
        root.setBackground(new Background(new BackgroundFill(BG, null, null)));

        // Resize canvas when the scene/window size changes
        if (stage != null && stage.getScene() != null) {
            stage.getScene().widthProperty().addListener((obs, oldVal, newVal) -> resizeCanvas());
            stage.getScene().heightProperty().addListener((obs, oldVal, newVal) -> resizeCanvas());
        }

        // Fonts are recreated on render based on tile size
        new AnimationTimer() {
            @Override public void handle(long now) {
                if (needsRedraw) { render(); needsRedraw = false; }
            }
        }.start();
    }

    private int getHudHeight() { return HUD_HEIGHT_RATIO; }
    private int getMsgHeight() { return MSG_HEIGHT_RATIO; }
    private int getWinWidth()  { return getTile() * MAP_COLS; }
    private int getWinHeight() { return getHudHeight() + getTile() * MAP_ROWS + getMsgHeight(); }

    private void resizeCanvas() {
        canvas.setWidth(getWinWidth());
        canvas.setHeight(getWinHeight());
        needsRedraw = true;
    }



    public Pane getRoot()   { return root; }
    public Canvas getCanvas() { return canvas; }

    public void handleKeyPress(KeyEvent e) {
        if (e.getCode().toString().equals("R")) {
            if (onRestart != null) onRestart.run();
            return;
        }
        if (state.isGameOver()) {
            if (e.getCode().toString().equals("R")) {
                if (onRestart != null) onRestart.run();
                return;
            }
            return;
        }
        int dx = 0, dy = 0;
        switch (e.getCode()) {
            case UP: case W: case K: dy = -1; break;
            case DOWN: case S: case J: dy = 1; break;
            case LEFT: case A: case H: dx = -1; break;
            case RIGHT: case D: case L: dx = 1; break;
            default: return;
        }
        state.movePlayer(dx, dy);
        needsRedraw = true;
    }

    private void render() {
        int tile = getTile();
        int winW = getWinWidth();
        int winH = getWinHeight();
        Font dynTileFont    = Font.font("Jacquard 12", FontWeight.BOLD,    tile - 6);
        Font dynHudFont     = Font.font("Monospaced", FontWeight.BOLD,    Math.max(20, tile / 2));
        Font dynHudSmall    = Font.font("Monospaced", FontWeight.NORMAL,  Math.max(16,  tile / 2 - 2));
        Font dynMsgFont     = Font.font("Jacquard 12", FontWeight.NORMAL,  Math.max(18,  tile / 2 - 1));

        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setFill(BG);
        gc.fillRect(0, 0, winW, winH);

        renderHUD(gc, dynHudFont, dynHudSmall);
        renderMap(gc, tile, dynTileFont);
        renderMessage(gc, dynMsgFont);

        if (state.isGameOver()) renderGameOver(gc);
    }


    private void renderHUD(GraphicsContext gc, Font hudFont, Font hudSmallFont) {
        // HUD background
        gc.setFill(HUD_BG);
        gc.fillRect(0, 0, getWinWidth(), getWinHeight());
        gc.setStroke(HUD_BORDER);
        gc.setLineWidth(1);
        gc.strokeLine(0, HUD_HEIGHT - 1, getWinWidth(), HUD_HEIGHT - 1);

        Player p = state.getPlayer();

        // Floor
        drawHudStat(gc, 16, "FLOOR", String.valueOf(state.getFloor()), TEXT_CYAN,hudFont,hudSmallFont);

        // HP bar
        int hpBarX = 120, hpBarY = 18, hpBarW = 160, hpBarH = 14;
        double hpRatio = (double) p.getHp() / p.getMaxHp();
        Color hpColor = hpRatio > 0.5 ? HP_FULL : hpRatio > 0.25 ? HP_MID : HP_LOW;
        gc.setFill(Color.web("#1a1625"));
        gc.fillRoundRect(hpBarX, hpBarY, hpBarW, hpBarH, 4, 4);
        gc.setFill(hpColor);
        gc.fillRoundRect(hpBarX, hpBarY, (int)(hpBarW * hpRatio), hpBarH, 4, 4);
        gc.setFont(hudSmallFont);
        gc.setFill(TEXT_MAIN);
        gc.setTextAlign(TextAlignment.LEFT);
        gc.setTextBaseline(VPos.TOP);
        gc.fillText("HP", hpBarX, 6);
        gc.setFill(hpColor);
        gc.fillText(p.getHp() + "/" + p.getMaxHp(), hpBarX + hpBarW + 6, 6);

        // Gold
        drawHudStat(gc, 320, "GOLD", String.valueOf(p.getGold()), TEXT_GOLD, hudFont,hudSmallFont);

        // Kills
        drawHudStat(gc, 430, "KILLS", String.valueOf(p.getKills()), TEXT_RED, hudFont,hudSmallFont);

        // Controls hint
        gc.setFont(hudSmallFont);
        gc.setFill(TEXT_DIM);
        gc.setTextAlign(TextAlignment.RIGHT);
        gc.fillText("WASD / arrows to move  |  bump to attack", getWinWidth() - 12, 6);
        gc.fillText("> stairs to descend   |   R to restart", getWinWidth() - 12, 22);
    }

    private void drawHudStat(GraphicsContext gc, double x, String label, String value, Color valueColor, Font hudFont, Font hudSmall) {
        gc.setFont(hudSmall);
        gc.setTextAlign(TextAlignment.LEFT);
        gc.setTextBaseline(VPos.TOP);
        gc.setFill(TEXT_DIM);
        gc.fillText(label, x, 6);
        gc.setFont(hudFont);
        gc.setFill(valueColor);
        gc.fillText(value, x, 22);
    }

    private void renderMap(GraphicsContext gc, int tileSize, Font tileFont ) {
        DungeonMap map = state.getMap();
        Player player = state.getPlayer();
        int offsetY = HUD_HEIGHT;

        // Viewport: center on player
        int vpCols = Math.min(MAP_COLS, map.getWidth());
        int vpRows = Math.min(MAP_ROWS, map.getHeight());
        int startX = Math.max(0, Math.min(player.getX() - vpCols / 2, map.getWidth()  - vpCols));
        int startY = Math.max(0, Math.min(player.getY() - vpRows / 2, map.getHeight() - vpRows));

        for (int row = 0; row < vpRows; row++) {
            for (int col = 0; col < vpCols; col++) {
                int mx = startX + col, my = startY + row;
                boolean visible  = state.isVisible(mx, my);
                boolean revealed = state.isRevealed(mx, my);
                if (!visible && !revealed) continue;

                double px = col * tileSize, py = offsetY + row * tileSize;
                Tile tile = map.getTile(mx, my);

                // Draw tile background
                if (tile == Tile.WALL) {
                    gc.setFill(visible ? WALL : WALL.darker());
                    gc.fillRect(px, py, tileSize, tileSize);
                    if (visible) {
                        gc.setFill(WALL_EDGE);
                        gc.fillRect(px, py, tileSize, 2);
                        gc.fillRect(px, py, 2, tileSize);
                    }
                } else {
                    gc.setFill(visible ? FLOOR : FLOOR_DARK);
                    gc.fillRect(px, py, tileSize, tileSize);
                    // subtle grid line
                    if (visible) {
                        gc.setFill(Color.web("#ffffff", 0.025));
                        gc.fillRect(px, py, tileSize, 1);
                        gc.fillRect(px, py, 1, tileSize);
                    }
                    if (tile == Tile.STAIR && visible) {
                        gc.setFont(tileFont);
                        gc.setFill(STAIR_COL);
                        gc.setTextAlign(TextAlignment.CENTER);
                        gc.setTextBaseline(VPos.CENTER);
                        gc.fillText(">", px + tileSize / 2.0, py + tileSize / 2.0 + 1);
                    }
                }

                // Fog overlay for seen-but-not-visible
                if (!visible && revealed) {
                    gc.setFill(FOG_SEEN);
                    gc.fillRect(px, py, tileSize, tileSize);
                }
            }
        }

        // Items
        gc.setFont(tileFont);
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setTextBaseline(VPos.CENTER);
        for (Item item : state.getItems()) {
            int col = item.getX() - startX, row = item.getY() - startY;
            if (col < 0 || row < 0 || col >= vpCols || row >= vpRows) continue;
            if (!state.isVisible(item.getX(), item.getY())) continue;
            double px = col * tileSize + tileSize / 2.0, py = offsetY + row * tileSize + tileSize / 2.0 + 1;
            gc.setFill(item.getType() == Item.Type.GOLD ? GOLD_COL : POTION_COL);
            gc.fillText(String.valueOf(item.getSymbol()), px, py);
        }

        // Enemies
        for (Enemy enemy : state.getEnemies()) {int col = enemy.getX() - startX, row = enemy.getY() - startY;

            // 1. Only draw if on screen and visible to player
            if (col < 0 || row < 0 || col >= vpCols || row >= vpRows) continue;
            if (!state.isVisible(enemy.getX(), enemy.getY())) continue;

            double px = col * tileSize, py = offsetY + row * tileSize;

            // 2. Draw the Symbol (Emoji or Character)
            gc.setFont(tileFont);
            gc.setFill(Color.WHITE); // Emojis usually keep their own colors anyway
            gc.setTextAlign(TextAlignment.CENTER);
            gc.setTextBaseline(VPos.CENTER);

            // This dynamically draws whatever symbol the enemy has (e.g., the Goblin 👺)
            gc.fillText(enemy.getSymbol(), px + tileSize / 2.0, py + tileSize / 2.0 + 1);

            // 3. Draw the HP bar (Generic Red for all enemies)
            double bw = tileSize - 4, bh = 3;
            double bx = px + 2, by = py + 1;
            gc.setFill(Color.web("#550000")); // Dark background
            gc.fillRect(bx, by, bw, bh);

            gc.setFill(Color.web("#ff4444")); // Bright red health
            double healthWidth = bw * ((double) enemy.getHp() / enemy.getMaxHp());
            gc.fillRect(bx, by, healthWidth, bh);
        }

        // Player
        int pcol = player.getX() - startX, prow = player.getY() - startY;
        if (pcol >= 0 && prow >= 0 && pcol < vpCols && prow < vpRows) {
            double px = pcol * tileSize + tileSize / 2.0, py = offsetY + prow * tileSize + tileSize / 2.0 + 1;
            gc.setFont(tileFont);
            gc.setFill(PLAYER_COL);
            gc.setTextAlign(TextAlignment.CENTER);
            gc.setTextBaseline(VPos.CENTER);

            gc.fillText(player.getSymbol(), px, py);

        }
    }

    private void renderMessage(GraphicsContext gc, Font msgFont) {


        double y = HUD_HEIGHT + MAP_ROWS * TILE;
        gc.setFill(HUD_BG);
        gc.fillRect(0, y, getWinWidth(), MSG_HEIGHT);
        gc.setStroke(HUD_BORDER);
        gc.strokeLine(0, y, getWinWidth(), y);

        gc.setFont(msgFont);
        gc.setFill(TEXT_MAIN);
        gc.setTextAlign(TextAlignment.LEFT);
        gc.setTextBaseline(VPos.TOP);

        List<String> msgs = state.getMessages();
        for (int i = 0; i < msgs.size(); i++) {
            gc.setFill(i == msgs.size() - 1 ? TEXT_MAIN : TEXT_DIM); // latest is brighter
            gc.fillText("  " + msgs.get(i), 0, y + 4 + i * 14);
        }
    }

    private void renderGameOver(GraphicsContext gc) {
        gc.setFill(Color.web("#000000", 0.75));
        gc.fillRect(0, 0, getWinWidth(), getWinHeight());

        Player p = state.getPlayer();
        String title = p.isDead() ? "YOU DIED" : "YOU ESCAPED!";
        Color titleColor = p.isDead() ? Color.web("#e05b5b") : Color.web("#e2b96a");

        gc.setFont(Font.font("Monospaced", FontWeight.BOLD, 42));
        gc.setFill(titleColor);
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setTextBaseline(VPos.CENTER);
        gc.fillText(title, getWinWidth() / 2.0, getWinHeight() / 2.0 - 30);

        gc.setFont(Font.font("Monospaced", FontWeight.NORMAL, 16));
        gc.setFill(TEXT_MAIN);
        gc.fillText(
            "Floor " + state.getFloor() + "  |  " + p.getKills() + " kills  |  " + p.getGold() + " gold",
            getWinWidth() / 2.0, getWinHeight() / 2.0 + 16
        );

        gc.setFill(TEXT_DIM);
        gc.setFont(Font.font("Monospaced", FontWeight.NORMAL, 13));
        gc.fillText("Press R to restart", getWinWidth() / 2.0, getWinHeight() / 2.0 + 50);
    }
}
