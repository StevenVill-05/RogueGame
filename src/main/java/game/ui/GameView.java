package game.ui;

import game.core.GameState;
import game.core.ScoreManager;
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
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import java.util.List;
import javafx.scene.image.Image;



public class GameView {
    private boolean scoreSaved = false;


    public static final int MAP_COLS  = 30;
    public static final int MAP_ROWS  = 22;

    // These are only used as the initial window size; layout is fully dynamic
    public static final int BASE_TILE   = 28;
    public static final int HUD_HEIGHT_RATIO = 60;
    public static final int MSG_HEIGHT_RATIO = 70;
    public static final int BASE_WIDTH  = BASE_TILE * MAP_COLS;           // 840
    public static final int BASE_HEIGHT = HUD_HEIGHT_RATIO + BASE_TILE * MAP_ROWS + MSG_HEIGHT_RATIO; // 712

    // Keep these for backward-compat constants that other code might reference
    public static final int TILE       = BASE_TILE;
    public static final int HUD_HEIGHT = HUD_HEIGHT_RATIO;
    public static final int MSG_HEIGHT = MSG_HEIGHT_RATIO;

    //sprites
    private final Image wallSprite;
    private final Image floorSprite;
    private final Image warriorSprite;
    private final Image mageSprite;
    private final Image rogueSprite;


    // Palette
    private static final Color BG          = Color.web("#0d0b12");
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
    private static final Color FOG_SEEN    = Color.web("#0d0b12", 0.30);


    private final Stage stage;
    private final GameState state;
    private final Canvas canvas;
    private final StackPane root;
    private boolean needsRedraw = true;
    private Runnable onRestart;


    // ── Dynamic sizing ────────────────────────────────────────────────────────

    /** Tile size that fits the current scene dimensions. */
    private int getTile() {
        if (stage == null || stage.getScene() == null) return BASE_TILE;
        double availH = stage.getScene().getHeight() - HUD_HEIGHT_RATIO - MSG_HEIGHT_RATIO;
        double availW = stage.getScene().getWidth();
        int tileByH = (int)(availH / MAP_ROWS);
        int tileByW = (int)(availW / MAP_COLS);
        return Math.max(8, Math.min(tileByH, tileByW));
    }

    private int getHudHeight() { return HUD_HEIGHT_RATIO; }
    private int getMsgHeight() { return MSG_HEIGHT_RATIO; }
    private int getWinWidth()  { return getTile() * MAP_COLS; }
    private int getWinHeight() { return getHudHeight() + getTile() * MAP_ROWS + getMsgHeight(); }

    private void resizeCanvas() {
        int mapW   = getWinWidth();
        int sceneW = (stage != null && stage.getScene() != null)
                ? (int) stage.getScene().getWidth() : mapW;
        canvas.setWidth(Math.max(mapW, sceneW));
        canvas.setHeight(getWinHeight());
        needsRedraw = true;
    }

    // ── Constructor ───────────────────────────────────────────────────────────

    public GameView(GameState state, Runnable onRestart, Stage stage) {
        Font.loadFont(getClass().getResourceAsStream("/fonts/Jacquard12-Regular.ttf"), 16);
        wallSprite  = new Image(getClass().getResourceAsStream("/sprites/wall.png"));
        floorSprite = new Image(getClass().getResourceAsStream("/sprites/floor.png"));
        warriorSprite = new Image(getClass().getResourceAsStream("/sprites/warrior2.png"));
        mageSprite    = new Image(getClass().getResourceAsStream("/sprites/mage.png"));
        rogueSprite   = new Image(getClass().getResourceAsStream("/sprites/rogue2.png"));


        this.state     = state;
        this.onRestart = onRestart;
        this.stage     = stage;

        canvas = new Canvas(BASE_WIDTH, BASE_HEIGHT);
        canvas.setFocusTraversable(true);

        root = new StackPane(canvas);
        root.setBackground(new Background(new BackgroundFill(BG, null, null)));
        javafx.scene.layout.StackPane.setAlignment(canvas, javafx.geometry.Pos.TOP_LEFT);

        // Attach resize listeners — use a scene-listener so we catch the scene
        // even if it hasn't been set yet at construction time.
        if (stage != null) {
            stage.sceneProperty().addListener((obs, oldScene, newScene) -> {
                if (newScene != null) {
                    newScene.widthProperty() .addListener((o, ov, nv) -> resizeCanvas());
                    newScene.heightProperty().addListener((o, ov, nv) -> resizeCanvas());
                    resizeCanvas();
                }
            });
            // Also attach immediately if scene already exists
            if (stage.getScene() != null) {
                stage.getScene().widthProperty() .addListener((o, ov, nv) -> resizeCanvas());
                stage.getScene().heightProperty().addListener((o, ov, nv) -> resizeCanvas());
            }
            // Force a resize after the JavaFX pulse so the canvas matches the
            // current scene size on first render (including after restart).
            javafx.application.Platform.runLater(this::resizeCanvas);
        }

        new AnimationTimer() {
            @Override public void handle(long now) {
                if (lastNanoTime == 0) lastNanoTime = now;
                double deltaSec = (now - lastNanoTime) / 1_000_000_000.0;
                lastNanoTime = now;

                if (!swipes.isEmpty()) {
                    swipes.forEach(s -> s.progress += deltaSec / 0.25);
                    swipes.removeIf(s -> s.progress >= 1.0);
                    needsRedraw = true;
                }

                if (needsRedraw) { render(); needsRedraw = false; }
            }
        }.start();
    }

    // ── Public API ────────────────────────────────────────────────────────────

    public StackPane getRoot() { return root; }
    public Canvas getCanvas() { return canvas; }

    public void handleKeyPress(KeyEvent e) {
        // F11 toggles fullscreen
        if (e.getCode() == javafx.scene.input.KeyCode.F11) {
            if (stage != null) stage.setFullScreen(!stage.isFullScreen());
            return;
        }
        if (e.getCode().toString().equals("R")) {
            if (onRestart != null) onRestart.run();
            return;
        }
        if (state.isGameOver()) return;

        int dx = 0, dy = 0;
        switch (e.getCode()) {
            case UP:    case W: case K: dy = -1; break;
            case DOWN:  case S: case J: dy =  1; break;
            case LEFT:  case A: case H: dx = -1; break;
            case RIGHT: case D: case L: dx =  1; break;
            default: return;
        }
        state.movePlayer(dx, dy);
        needsRedraw = true;
    }

    // ── Render ────────────────────────────────────────────────────────────────

    private void render() {
        int tile  = getTile();
        int hudH  = getHudHeight();
        int msgH  = getMsgHeight();
        int mapW  = getWinWidth();
        int winH  = getWinHeight();

        // HUD stretches to full scene width so stats don't get crushed
        int sceneW = (stage != null && stage.getScene() != null)
                ? (int) stage.getScene().getWidth() : mapW;
        int hudW = Math.max(mapW, sceneW);

        // Cap font sizes so they always fit inside the fixed hudH (60px)
        int hudFontSize  = (int) Math.min(hudH * 0.40, Math.max(14, tile / 2));
        int hudSmallSize = (int) Math.min(hudH * 0.28, Math.max(11, tile / 2 - 2));
        int msgFontSize  = Math.max(14, tile / 2 - 1);

        Font dynTileFont = Font.font("Jacquard 12", FontWeight.BOLD,   tile - 6);
        Font dynHudFont  = Font.font("Monospaced",  FontWeight.BOLD,   hudFontSize);
        Font dynHudSmall = Font.font("Monospaced",  FontWeight.NORMAL, hudSmallSize);
        Font dynMsgFont  = Font.font("Jacquard 12", FontWeight.NORMAL, msgFontSize);

        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setFill(BG);
        gc.fillRect(0, 0, hudW, winH);

        renderHUD(gc, dynHudFont, dynHudSmall, hudH, hudW);
        renderMap(gc, tile, dynTileFont, hudH);
        renderMessage(gc, dynMsgFont, hudH, tile, msgH, hudW);

        if (state.isGameOver()) {
            renderGameOver(gc, hudW, winH);
        }
    }


    private void renderHUD(GraphicsContext gc, Font hudFont, Font hudSmallFont, int hudH, int winW) {
        gc.setFill(HUD_BG);
        gc.fillRect(0, 0, winW, hudH);
        gc.setStroke(HUD_BORDER);
        gc.setLineWidth(1);
        gc.strokeLine(0, hudH - 1, winW, hudH - 1);

        Player p = state.getPlayer();

        // All vertical positions scale with hudH
        double rowTop = hudH * 0.10;   // label row  (~top 10%)
        double rowBot = hudH * 0.45;   // value row  (~middle)
        double barY   = hudH * 0.30;   // HP bar top
        double barH   = hudH * 0.22;   // HP bar height

        // Left-side stats: FLOOR | HP | GOLD | KILLS
        // Spread across the left 55% of the HUD, equally spaced
        double floorX = winW * 0.02;
        double hpX    = winW * 0.13;
        double hpBarX = hpX;
        double hpBarW = winW * 0.18;
        double goldX  = winW * 0.37;
        double killsX = winW * 0.48;

        // FLOOR
        drawHudStat(gc, floorX, rowTop, rowBot, "FLOOR",
                String.valueOf(state.getFloor()), TEXT_CYAN, hudFont, hudSmallFont);

        // HP label + bar
        gc.setFont(hudSmallFont);
        gc.setTextAlign(TextAlignment.LEFT);
        gc.setTextBaseline(VPos.TOP);
        gc.setFill(TEXT_DIM);
        gc.fillText("HP", hpX, rowTop);

        double hpRatio = (double) p.getHp() / p.getMaxHp();
        Color hpColor  = hpRatio > 0.5 ? HP_FULL : hpRatio > 0.25 ? HP_MID : HP_LOW;
        gc.setFill(Color.web("#1a1625"));
        gc.fillRoundRect(hpBarX, barY, hpBarW, barH, 4, 4);
        gc.setFill(hpColor);
        gc.fillRoundRect(hpBarX, barY, hpBarW * hpRatio, barH, 4, 4);
        gc.setFill(hpColor);
        gc.fillText(p.getHp() + "/" + p.getMaxHp(), hpBarX + hpBarW + winW * 0.006, rowTop);

        // GOLD / KILLS
        drawHudStat(gc, goldX,  rowTop, rowBot, "GOLD",  String.valueOf(p.getGold()),  TEXT_GOLD, hudFont, hudSmallFont);
        drawHudStat(gc, killsX, rowTop, rowBot, "KILLS", String.valueOf(p.getKills()), TEXT_RED,  hudFont, hudSmallFont);

        // Right-side hints — only show if there's room (winW > 500)
        if (winW > 500) {
            gc.setFont(hudSmallFont);
            gc.setFill(TEXT_DIM);
            gc.setTextAlign(TextAlignment.RIGHT);
            gc.setTextBaseline(VPos.TOP);
            gc.fillText("WASD / arrows to move  |  bump to attack", winW - winW * 0.01, rowTop);
            gc.fillText("> stairs to descend  |  R restart  |  F11 fullscreen", winW - winW * 0.01, rowBot);
        }
    }

    private void drawHudStat(GraphicsContext gc, double x, double labelY, double valueY,
                             String label, String value, Color valueColor,
                             Font hudFont, Font hudSmall) {
        gc.setFont(hudSmall);
        gc.setTextAlign(TextAlignment.LEFT);
        gc.setTextBaseline(VPos.TOP);
        gc.setFill(TEXT_DIM);
        gc.fillText(label, x, labelY);
        gc.setFont(hudFont);
        gc.setFill(valueColor);
        gc.fillText(value, x, valueY);
    }

    private void renderMap(GraphicsContext gc, int tileSize, Font tileFont, int offsetY) {
        DungeonMap map    = state.getMap();
        Player     player = state.getPlayer();

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

                if (tile == Tile.WALL) {
                    gc.setGlobalAlpha(visible ? 1.0 : 0.4);
                    gc.drawImage(wallSprite, px, py, tileSize, tileSize);
                    gc.setGlobalAlpha(1.0);
                } else {
                    gc.setGlobalAlpha(visible ? 1.0 : 0.4);
                    gc.drawImage(floorSprite, px, py, tileSize, tileSize);
                    gc.setGlobalAlpha(1.0);
                    if (tile == Tile.STAIR && visible) {
                        gc.setFont(tileFont);
                        gc.setFill(STAIR_COL);
                        gc.setTextAlign(TextAlignment.CENTER);
                        gc.setTextBaseline(VPos.CENTER);
                        gc.fillText(">", px + tileSize / 2.0, py + tileSize / 2.0 + 1);
                    }
                }

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
        for (Enemy enemy : state.getEnemies()) {
            int col = enemy.getX() - startX, row = enemy.getY() - startY;
            if (col < 0 || row < 0 || col >= vpCols || row >= vpRows) continue;
            if (!state.isVisible(enemy.getX(), enemy.getY())) continue;

            double px = col * tileSize, py = offsetY + row * tileSize;

            gc.setFont(tileFont);
            gc.setFill(Color.WHITE);
            gc.setTextAlign(TextAlignment.CENTER);
            gc.setTextBaseline(VPos.CENTER);
            gc.fillText(enemy.getSymbol(), px + tileSize / 2.0, py + tileSize / 2.0 + 1);

            double bw = tileSize - 4, bh = 3;
            double bx = px + 2, by = py + 1;
            gc.setFill(Color.web("#550000"));
            gc.fillRect(bx, by, bw, bh);
            gc.setFill(Color.web("#ff4444"));
            gc.fillRect(bx, by, bw * ((double) enemy.getHp() / enemy.getMaxHp()), bh);
        }

        // Player
        int pcol = player.getX() - startX, prow = player.getY() - startY;
        if (pcol >= 0 && prow >= 0 && pcol < vpCols && prow < vpRows) {
            double px = pcol * tileSize, py = offsetY + prow * tileSize;
            gc.drawImage(getPlayerSprite(player), px, py, tileSize, tileSize);
        }

        // Swipe animations
        for (SwipeAnim s : swipes) {
            int col = s.tileX - startX, row = s.tileY - startY;
            if (col < 0 || row < 0 || col >= vpCols || row >= vpRows) continue;

            double px = col * tileSize, py = offsetY + row * tileSize;
            double cx = px + tileSize / 2.0, cy = py + tileSize / 2.0;

            double alpha  = 1.0 - (s.progress * s.progress);
            double radius = tileSize * 0.35 * (0.5 + s.progress * 0.5);
            double startAngle = -s.angleDeg - 45;
            double arcLength  = 90 + s.progress * 30;

            gc.save();
            gc.setGlobalAlpha(alpha);
            gc.setStroke(s.isPlayer ? Color.web("#5be0ff") : Color.web("#ff5b5b"));
            gc.setLineWidth(tileSize * 0.12);
            gc.setLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
            gc.strokeArc(cx - radius, cy - radius, radius * 2, radius * 2,
                    startAngle, arcLength, javafx.scene.shape.ArcType.OPEN);
            gc.restore();
        }
    }

    /**
     * Renders the message bar.
     * The bar sits at  hudH + tile*MAP_ROWS  — always derived from live tile size,
     * never from the compile-time TILE constant, so it can never overlap the map.
     */
    private void renderMessage(GraphicsContext gc, Font msgFont,
                               int hudH, int tileSize, int msgH, int winW) {
        // Position is fully dynamic: right below the map area
        double y = hudH + (double) tileSize * MAP_ROWS;

        gc.setFill(HUD_BG);
        gc.fillRect(0, y, winW, msgH);
        gc.setStroke(HUD_BORDER);
        gc.strokeLine(0, y, winW, y);

        gc.setFont(msgFont);
        gc.setFill(TEXT_MAIN);
        gc.setTextAlign(TextAlignment.LEFT);
        gc.setTextBaseline(VPos.TOP);

        List<String> msgs = state.getMessages();
        for (int i = 0; i < msgs.size(); i++) {
            gc.setFill(i == msgs.size() - 1 ? TEXT_MAIN : TEXT_DIM);
            gc.fillText("  " + msgs.get(i), 0, y + 4 + i * 14);
        }
    }

    private void renderGameOver(GraphicsContext gc, int winW, int winH) {
        gc.setFill(Color.web("#000000", 0.75));
        gc.fillRect(0, 0, winW, winH);

        Player p = state.getPlayer();
        String title      = p.isDead() ? "YOU DIED" : "YOU ESCAPED!";
        Color  titleColor = p.isDead() ? Color.web("#e05b5b") : Color.web("#e2b96a");

        gc.setFont(Font.font("Monospaced", FontWeight.BOLD, 42));
        gc.setFill(titleColor);
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setTextBaseline(VPos.CENTER);
        gc.fillText(title, winW / 2.0, winH / 2.0 - 30);

        gc.setFont(Font.font("Monospaced", FontWeight.NORMAL, 16));
        gc.setFill(TEXT_MAIN);
        gc.fillText(
                "Floor " + state.getFloor() + "  |  " + p.getKills() + " kills  |  " + p.getGold() + " gold",
                winW / 2.0, winH / 2.0 + 16
        );

        if (!scoreSaved) {
            ScoreManager.saveIfHighScore(p.getName(), state.getFloor(), p.getKills(), p.getGold());
            scoreSaved = true;
        }

        gc.setFill(TEXT_DIM);
        gc.setFont(Font.font("Monospaced", FontWeight.NORMAL, 13));
        gc.fillText("Press R to restart", winW / 2.0, winH / 2.0 + 50);
    }


    /** Returns the correct sprite for the chosen character class. */
    private Image getPlayerSprite(Player player) {
        String cls = player.getClass().getSimpleName();
        return switch (cls) {
            case "Mage"    -> mageSprite;
            case "Rogue"   -> rogueSprite;
            default        -> warriorSprite; // Warrior or any fallback
        };
    }

    // ── Swipe animation ───────────────────────────────────────────────────────

    private final java.util.List<SwipeAnim> swipes = new java.util.ArrayList<>();
    private long lastNanoTime = 0;

    private static class SwipeAnim {
        int tileX, tileY;
        double progress;
        boolean isPlayer;
        double angleDeg;
        SwipeAnim(int tileX, int tileY, boolean isPlayer, int fromX, int fromY) {
            this.tileX    = tileX; this.tileY  = tileY;
            this.progress = 0.0;   this.isPlayer = isPlayer;
            this.angleDeg = Math.toDegrees(Math.atan2(tileY - fromY, tileX - fromX));
        }
    }

    public void triggerSwipe(int fromX, int fromY, int toX, int toY, boolean isPlayer) {
        swipes.add(new SwipeAnim(toX, toY, isPlayer, fromX, fromY));
        needsRedraw = true;
    }
}