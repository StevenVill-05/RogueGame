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
import javafx.scene.image.Image;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * JavaFX rendering layer for the rogue-like game.
 * Owns the Canvas and the AnimationTimer loop; receives state from GameState
 * and translates it into pixel output on every frame that needs a redraw.
 *
 * Sprites are NOT loaded here. Each Player subclass declares its own
 * sprite resource path via {@link Player#getSpritePath()}, and this class
 * lazily loads and caches those images on first use.
 */
public class GameView {

    // ── Layout constants ──────────────────────────────────────────────────────

    public static final int MAP_COLS  = 30;
    public static final int MAP_ROWS  = 22;

    /** Initial tile size; actual size is computed dynamically from window dimensions. */
    public static final int BASE_TILE        = 28;
    public static final int HUD_HEIGHT_RATIO = 60;
    public static final int MSG_HEIGHT_RATIO = 80;
    public static final int BASE_WIDTH       = BASE_TILE * MAP_COLS;
    public static final int BASE_HEIGHT      = HUD_HEIGHT_RATIO + BASE_TILE * MAP_ROWS + MSG_HEIGHT_RATIO;

    // Aliases kept for any external code that references them
    public static final int TILE       = BASE_TILE;
    public static final int HUD_HEIGHT = HUD_HEIGHT_RATIO;
    public static final int MSG_HEIGHT = MSG_HEIGHT_RATIO;

    // ── Sprites ───────────────────────────────────────────────────────────────

    /** Static tile sprites shared across all characters. */
    private final Image wallSprite;
    private final Image floorSprite;

    /**
     * Lazy cache: maps a sprite resource path (e.g. "/sprites/warrior2.png")
     * to its loaded Image.  Populated on demand by {@link #getPlayerSprite(Player)}.
     */
    private final Map<String, Image> spriteCache = new HashMap<>();

    // ── Colour palette ────────────────────────────────────────────────────────

    private static final Color BG         = Color.web("#0d0b12");
    private static final Color STAIR_COL  = Color.web("#e2b96a");
    private static final Color GOLD_COL   = Color.web("#f0c040");
    private static final Color POTION_COL = Color.web("#5be08a");
    private static final Color HUD_BG     = Color.web("#110f1a");
    private static final Color HUD_BORDER = Color.web("#2e2845");
    private static final Color HP_FULL    = Color.web("#4caf50");
    private static final Color HP_MID     = Color.web("#ff9800");
    private static final Color HP_LOW     = Color.web("#f44336");
    private static final Color TEXT_MAIN  = Color.web("#e8e0f0");
    private static final Color TEXT_DIM   = Color.web("#7a6e8a");
    private static final Color TEXT_GOLD  = Color.web("#f0c040");
    private static final Color TEXT_RED   = Color.web("#e05b5b");
    private static final Color TEXT_CYAN  = Color.web("#5be0d0");
    private static final Color FOG_SEEN   = Color.web("#0d0b12", 0.30);

    // ── State ─────────────────────────────────────────────────────────────────

    private final Stage stage;
    private final GameState state;
    private final Canvas canvas;
    private final StackPane root;
    private boolean needsRedraw = true;
    private Runnable onRestart;

    /** Prevents saving the score to the DB more than once per run. */
    private boolean scoreSaved = false;

    // ── Dynamic sizing ────────────────────────────────────────────────────────

    /**
     * Computes the largest integer tile size that fits the current scene dimensions
     * without overflowing either axis.  Falls back to BASE_TILE if no scene exists.
     */
    private int getTile() {
        if (stage == null || stage.getScene() == null) return BASE_TILE;
        double availH = stage.getScene().getHeight() - HUD_HEIGHT_RATIO - MSG_HEIGHT_RATIO;
        double availW = stage.getScene().getWidth();
        int tileByH = (int)(availH / MAP_ROWS);
        int tileByW = (int)(availW / MAP_COLS);
        return Math.max(8, Math.min(tileByH, tileByW));
    }

    /** Returns the fixed HUD bar height in pixels. */
    private int getHudHeight() { return HUD_HEIGHT_RATIO; }

    /** Returns the fixed message bar height in pixels. */
    private int getMsgHeight() { return MSG_HEIGHT_RATIO; }

    /** Returns the total pixel width of the map area (tile * MAP_COLS). */
    private int getWinWidth()  { return getTile() * MAP_COLS; }

    /** Returns the total pixel height needed for HUD + map + message bar. */
    private int getWinHeight() { return getHudHeight() + getTile() * MAP_ROWS + getMsgHeight(); }

    /**
     * Resizes the canvas to match the current window dimensions and requests a redraw.
     * Called whenever the scene's width or height property changes.
     */
    private void resizeCanvas() {
        int mapW   = getWinWidth();
        int sceneW = (stage != null && stage.getScene() != null)
                ? (int) stage.getScene().getWidth() : mapW;
        canvas.setWidth(Math.max(mapW, sceneW));
        canvas.setHeight(getWinHeight());
        needsRedraw = true;
    }

    // ── Constructor ───────────────────────────────────────────────────────────

    /**
     * Builds the GameView: loads the custom font, loads the shared tile sprites,
     * creates the Canvas, wires resize listeners, and starts the AnimationTimer loop.
     *
     * @param state     live game state to render each frame
     * @param onRestart callback invoked when the player presses R to restart
     * @param stage     primary JavaFX stage (needed for resize detection and fullscreen)
     */
    public GameView(GameState state, Runnable onRestart, Stage stage) {
        Font.loadFont(getClass().getResourceAsStream("/fonts/Jacquard12-Regular.ttf"), 16);

        // Load the two shared tile sprites (walls and floor)
        wallSprite  = new Image(getClass().getResourceAsStream("/sprites/wall.png"));
        floorSprite = new Image(getClass().getResourceAsStream("/sprites/floor.png"));

        this.state     = state;
        this.onRestart = onRestart;
        this.stage     = stage;

        canvas = new Canvas(BASE_WIDTH, BASE_HEIGHT);
        canvas.setFocusTraversable(true);

        root = new StackPane(canvas);
        root.setBackground(new Background(new BackgroundFill(BG, null, null)));
        javafx.scene.layout.StackPane.setAlignment(canvas, javafx.geometry.Pos.TOP_LEFT);

        // Wire resize listeners — we must handle the case where the scene
        // is set after construction, so we listen on the stage's sceneProperty.
        if (stage != null) {
            stage.sceneProperty().addListener((obs, oldScene, newScene) -> {
                if (newScene != null) {
                    newScene.widthProperty() .addListener((o, ov, nv) -> resizeCanvas());
                    newScene.heightProperty().addListener((o, ov, nv) -> resizeCanvas());
                    resizeCanvas();
                }
            });
            if (stage.getScene() != null) {
                stage.getScene().widthProperty() .addListener((o, ov, nv) -> resizeCanvas());
                stage.getScene().heightProperty().addListener((o, ov, nv) -> resizeCanvas());
            }
            // Force a post-layout resize so the canvas matches the real scene size
            javafx.application.Platform.runLater(this::resizeCanvas);
        }

        // Animation loop: advances swipe animations and triggers redraws
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

                if (needsRedraw || state.consumeSkillRedraw()) { render(); needsRedraw = false; }
            }
        }.start();
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /** Returns the StackPane that should be set as the scene root. */
    public StackPane getRoot() { return root; }

    /** Returns the Canvas on which the game is drawn (used to attach key listeners). */
    public Canvas getCanvas() { return canvas; }

    /**
     * Handles a key-press event from the scene.
     * F11 toggles fullscreen; R restarts the game; arrow/WASD/hjkl move the player.
     *
     * @param e the key event forwarded from the canvas's onKeyPressed handler
     */
    public void handleKeyPress(KeyEvent e) {
        if (e.getCode() == javafx.scene.input.KeyCode.F11) {
            if (stage != null) stage.setFullScreen(!stage.isFullScreen());
            return;
        }
        if (e.getCode().toString().equals("R")) {
            if (onRestart != null) onRestart.run();
            return;
        }
        if (state.isGameOver()) return;

        // Skill keys [1], [2], [3]
        switch (e.getCode()) {
            case DIGIT1 -> { state.onSkillKey(0); needsRedraw = true; return; }
            case DIGIT2 -> { state.onSkillKey(1); needsRedraw = true; return; }
            case DIGIT3 -> { state.onSkillKey(2); needsRedraw = true; return; }
            default -> {}
        }

        // Resolve movement direction
        int dx = 0, dy = 0;
        switch (e.getCode()) {
            case UP,    W, K -> dy = -1;
            case DOWN,  S, J -> dy =  1;
            case LEFT,  A, H -> dx = -1;
            case RIGHT, D, L -> dx =  1;
            default -> { return; }
        }

        // If a directional skill is pending, feed it the direction instead of moving
        if (state.getPendingSkillIndex() >= 0) {
            state.onDirectionForSkill(dx, dy);
            needsRedraw = true;
            return;
        }

        state.movePlayer(dx, dy);
        needsRedraw = true;
    }

    // ── Render ────────────────────────────────────────────────────────────────

    /**
     * Master render call: computes dynamic font and size values, clears the canvas,
     * then delegates to renderHUD, renderMap, renderMessage, and (if game over)
     * renderGameOver.
     */
    private void render() {
        int tile  = getTile();
        int hudH  = getHudHeight();
        int msgH  = getMsgHeight();
        int mapW  = getWinWidth();
        int winH  = getWinHeight();

        int sceneW = (stage != null && stage.getScene() != null)
                ? (int) stage.getScene().getWidth() : mapW;
        int hudW = Math.max(mapW, sceneW);

        // Font sizes clamped so they always fit within their respective bars
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

        if (state.isGameOver()) renderGameOver(gc, hudW, winH);
    }

    /**
     * Draws the top HUD bar containing HP bar, floor, gold, kills, and hint text.
     * All positions are expressed as fractions of {@code winW} and {@code hudH}
     * so the layout scales correctly with the window size.
     *
     * @param gc          graphics context to draw into
     * @param hudFont     bold font for stat values
     * @param hudSmallFont regular font for stat labels and hints
     * @param hudH        pixel height of the HUD bar
     * @param winW        pixel width of the window (may be wider than the map)
     */
    private void renderHUD(GraphicsContext gc, Font hudFont, Font hudSmallFont,
                           int hudH, int winW) {
        gc.setFill(HUD_BG);
        gc.fillRect(0, 0, winW, hudH);
        gc.setStroke(HUD_BORDER);
        gc.setLineWidth(1);
        gc.strokeLine(0, hudH - 1, winW, hudH - 1);

        Player p = state.getPlayer();

        double rowTop = hudH * 0.10;
        double rowBot = hudH * 0.45;
        double barY   = hudH * 0.30;
        double barH   = hudH * 0.22;

        double floorX = winW * 0.02;
        double hpX    = winW * 0.13;
        double hpBarW = winW * 0.18;
        double goldX  = winW * 0.37;
        double killsX = winW * 0.48;

        drawHudStat(gc, floorX, rowTop, rowBot, "FLOOR",
                String.valueOf(state.getFloor()), TEXT_CYAN, hudFont, hudSmallFont);

        // HP label and bar
        gc.setFont(hudSmallFont);
        gc.setTextAlign(TextAlignment.LEFT);
        gc.setTextBaseline(VPos.TOP);
        gc.setFill(TEXT_DIM);
        gc.fillText("HP", hpX, rowTop);

        double hpRatio = (double) p.getHp() / p.getMaxHp();
        Color hpColor  = hpRatio > 0.5 ? HP_FULL : hpRatio > 0.25 ? HP_MID : HP_LOW;
        gc.setFill(Color.web("#1a1625"));
        gc.fillRoundRect(hpX, barY, hpBarW, barH, 4, 4);
        gc.setFill(hpColor);
        gc.fillRoundRect(hpX, barY, hpBarW * hpRatio, barH, 4, 4);
        gc.setFill(hpColor);
        gc.fillText(p.getHp() + "/" + p.getMaxHp(), hpX + hpBarW + winW * 0.006, rowTop);

        drawHudStat(gc, goldX,  rowTop, rowBot, "GOLD",  String.valueOf(p.getGold()),  TEXT_GOLD, hudFont, hudSmallFont);
        drawHudStat(gc, killsX, rowTop, rowBot, "KILLS", String.valueOf(p.getKills()), TEXT_RED,  hudFont, hudSmallFont);

        // Keyboard hints — only shown when the window is wide enough
        if (winW > 500) {
            gc.setFont(hudSmallFont);
            gc.setFill(TEXT_DIM);
            gc.setTextAlign(TextAlignment.RIGHT);
            gc.setTextBaseline(VPos.TOP);
            gc.fillText("WASD / arrows to move  |  bump to attack  |  [1][2][3] skills", winW - winW * 0.01, rowTop);
            gc.fillText("> stairs to descend  |  R restart  |  F11 fullscreen", winW - winW * 0.01, rowBot);
        }
    }

    /**
     * Draws a single labelled HUD stat (label on top, value below) at position x.
     *
     * @param gc         graphics context
     * @param x          left edge of the stat column
     * @param labelY     vertical position of the dim label text
     * @param valueY     vertical position of the coloured value text
     * @param label      upper dim label string (e.g. "GOLD")
     * @param value      lower bright value string (e.g. "42")
     * @param valueColor colour applied to the value text
     * @param hudFont    bold font for the value
     * @param hudSmall   regular font for the label
     */
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

    /**
     * Draws the visible portion of the dungeon map centred on the player.
     * Renders wall/floor sprites, stair glyphs, items, enemy sprites with HP bars,
     * the player sprite, and all active swipe animations.
     * Tiles that are revealed but not currently visible are darkened with a fog overlay.
     *
     * @param gc       graphics context
     * @param tileSize pixel size of each tile square this frame
     * @param tileFont font used for stair and item glyphs
     * @param offsetY  vertical pixel offset below the HUD bar
     */
    private void renderMap(GraphicsContext gc, int tileSize, Font tileFont, int offsetY) {
        DungeonMap map    = state.getMap();
        Player     player = state.getPlayer();

        int vpCols = Math.min(MAP_COLS, map.getWidth());
        int vpRows = Math.min(MAP_ROWS, map.getHeight());
        int startX = Math.max(0, Math.min(player.getX() - vpCols / 2, map.getWidth()  - vpCols));
        int startY = Math.max(0, Math.min(player.getY() - vpRows / 2, map.getHeight() - vpRows));

        // ── Terrain ──
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

                // Fog overlay for seen-but-not-visible tiles
                if (!visible && revealed) {
                    gc.setFill(FOG_SEEN);
                    gc.fillRect(px, py, tileSize, tileSize);
                }
            }
        }

        // ── Items ──
        gc.setFont(tileFont);
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setTextBaseline(VPos.CENTER);
        for (Item item : state.getItems()) {
            int col = item.getX() - startX, row = item.getY() - startY;
            if (col < 0 || row < 0 || col >= vpCols || row >= vpRows) continue;
            if (!state.isVisible(item.getX(), item.getY())) continue;
            double px = col * tileSize + tileSize / 2.0;
            double py = offsetY + row * tileSize + tileSize / 2.0 + 1;
            gc.setFill(item.getType() == Item.Type.GOLD ? GOLD_COL : POTION_COL);
            gc.fillText(String.valueOf(item.getSymbol()), px, py);
        }

        // ── Enemies (glyph + HP bar) ──
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

            // Thin HP bar above the enemy glyph
            double bw = tileSize - 4, bh = 3;
            double bx = px + 2,       by = py + 1;
            gc.setFill(Color.web("#550000"));
            gc.fillRect(bx, by, bw, bh);
            gc.setFill(Color.web("#ff4444"));
            gc.fillRect(bx, by, bw * ((double) enemy.getHp() / enemy.getMaxHp()), bh);
        }

        // ── Player sprite ──
        int pcol = player.getX() - startX, prow = player.getY() - startY;
        if (pcol >= 0 && prow >= 0 && pcol < vpCols && prow < vpRows) {
            double px = pcol * tileSize, py = offsetY + prow * tileSize;
            gc.drawImage(getPlayerSprite(player), px, py, tileSize, tileSize);
        }

        // ── Swipe animations ──
        for (SwipeAnim s : swipes) {
            int col = s.tileX - startX, row = s.tileY - startY;
            if (col < 0 || row < 0 || col >= vpCols || row >= vpRows) continue;

            double px = col * tileSize, py = offsetY + row * tileSize;
            double cx = px + tileSize / 2.0, cy = py + tileSize / 2.0;

            double alpha      = 1.0 - (s.progress * s.progress);
            double radius     = tileSize * 0.35 * (0.5 + s.progress * 0.5);
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
     * Draws the message bar below the map with the last few combat log entries,
     * and the skill slots in the bottom-left corner next to the messages.
     * Slot visual states:
     *  • Locked     — dark, gold lock icon + cost shown
     *  • Pending    — bright gold outline (waiting for direction)
     *  • Cooldown   — grey overlay + red turn count
     *  • Ready      — blue outline, normal icon
     */
    private void renderMessage(GraphicsContext gc, Font msgFont,
                               int hudH, int tileSize, int msgH, int winW) {
        double barY = hudH + (double) tileSize * MAP_ROWS;

        gc.setFill(HUD_BG);
        gc.fillRect(0, barY, winW, msgH);
        gc.setStroke(HUD_BORDER);
        gc.strokeLine(0, barY, winW, barY);

        // ── Skill slots ────────────────────────────────────────────────────────
        java.util.List<game.entity.characters.Skill> skills = state.getPlayer().getSkills();
        int slotSize  = Math.min(msgH - 6, 56);
        int slotGap   = 5;
        int slotStart = 6;
        int pending   = state.getPendingSkillIndex();

        Font skillKeyFont   = Font.font("Monospaced",  FontWeight.BOLD,   Math.max(9,  slotSize / 6));
        Font skillIconFont  = Font.font("Jacquard 12", FontWeight.NORMAL, Math.max(14, slotSize / 2));
        Font cooldownFont   = Font.font("Monospaced",  FontWeight.BOLD,   Math.max(12, slotSize / 3));
        Font costFont       = Font.font("Monospaced",  FontWeight.BOLD,   Math.max(8,  slotSize / 6));

        for (int i = 0; i < skills.size(); i++) {
            game.entity.characters.Skill skill = skills.get(i);
            double sx = slotStart + i * (slotSize + slotGap);
            double sy = barY + (msgH - slotSize) / 2.0;

            boolean locked   = !skill.isUnlocked();
            boolean onCd     = !locked && !skill.isReady();
            boolean isPending = (pending == i);
            boolean ready    = !locked && !onCd;

            // ── Background ──
            if (locked) {
                gc.setFill(Color.web("#100e18"));
            } else if (isPending) {
                gc.setFill(Color.web("#2a2010"));
            } else {
                gc.setFill(ready ? Color.web("#1e1a2e") : Color.web("#0d0b12"));
            }
            gc.fillRoundRect(sx, sy, slotSize, slotSize, 7, 7);

            // ── Border ──
            gc.setLineWidth(isPending ? 2.5 : 1.5);
            if (isPending)     gc.setStroke(Color.web("#f0c040"));       // gold = "choose direction"
            else if (locked)   gc.setStroke(Color.web("#3a2810"));       // very dim brown
            else if (ready)    gc.setStroke(Color.web("#5ba4e0"));       // blue
            else               gc.setStroke(Color.web("#2e2845"));       // dim purple
            gc.strokeRoundRect(sx, sy, slotSize, slotSize, 7, 7);

            // ── Dark overlay for locked / cooldown ──
            if (locked || onCd) {
                gc.setFill(Color.web("#000000", locked ? 0.6 : 0.5));
                gc.fillRoundRect(sx, sy, slotSize, slotSize, 7, 7);
            }

            // ── Icon ──
            gc.setFont(skillIconFont);
            gc.setTextAlign(TextAlignment.CENTER);
            gc.setTextBaseline(VPos.CENTER);
            if (locked)       gc.setFill(Color.web("#3a3058"));
            else if (isPending) gc.setFill(Color.web("#f0c040"));
            else              gc.setFill(ready ? TEXT_MAIN : TEXT_DIM);
            gc.fillText(locked ? "🔒" : skill.getIcon(),
                        sx + slotSize / 2.0, sy + slotSize * 0.42);

            // ── Key number (top-left) ──
            gc.setFont(skillKeyFont);
            gc.setTextAlign(TextAlignment.LEFT);
            gc.setTextBaseline(VPos.TOP);
            gc.setFill(isPending ? Color.web("#f0c040")
                       : locked  ? Color.web("#3a2810")
                       : ready   ? Color.web("#5ba4e0")
                                 : Color.web("#3a3058"));
            gc.fillText(String.valueOf(i + 1), sx + 3, sy + 2);

            // ── Cooldown turns (centre-bottom) ──
            if (onCd) {
                gc.setFont(cooldownFont);
                gc.setTextAlign(TextAlignment.CENTER);
                gc.setTextBaseline(VPos.CENTER);
                gc.setFill(Color.web("#e05b5b"));
                gc.fillText(String.valueOf(skill.getCooldownLeft()),
                            sx + slotSize / 2.0, sy + slotSize * 0.75);
            }

            // ── Gold cost (bottom-right corner) when locked ──
            if (locked) {
                gc.setFont(costFont);
                gc.setTextAlign(TextAlignment.RIGHT);
                gc.setTextBaseline(VPos.BOTTOM);
                gc.setFill(TEXT_GOLD);
                gc.fillText(skill.getGoldCost() + "g",
                            sx + slotSize - 3, sy + slotSize - 2);
            }

            // ── "PICK DIR" label when pending ──
            if (isPending) {
                gc.setFont(costFont);
                gc.setTextAlign(TextAlignment.CENTER);
                gc.setTextBaseline(VPos.BOTTOM);
                gc.setFill(Color.web("#f0c040"));
                gc.fillText("DIR?", sx + slotSize / 2.0, sy + slotSize - 2);
            }
        }

        // ── Messages (right of skill slots) ───────────────────────────────────
        int msgOffsetX = slotStart + skills.size() * (slotSize + slotGap) + 8;

        gc.setFont(msgFont);
        gc.setTextAlign(TextAlignment.LEFT);
        gc.setTextBaseline(VPos.TOP);

        List<String> msgs = state.getMessages();
        for (int i = 0; i < msgs.size(); i++) {
            gc.setFill(i == msgs.size() - 1 ? TEXT_MAIN : TEXT_DIM);
            gc.fillText(msgs.get(i), msgOffsetX, barY + 4 + i * 14);
        }
    }

    /**
     * Draws the game-over overlay (semi-transparent dark curtain, title, stats, hint).
     * Also saves the score to the database the first time this is called per run.
     *
     * @param gc    graphics context
     * @param winW  full window width
     * @param winH  full window height
     */
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
        gc.fillText("Floor " + state.getFloor() + "  |  " + p.getKills()
                + " kills  |  " + p.getGold() + " gold",
                winW / 2.0, winH / 2.0 + 16);

        if (!scoreSaved) {
            ScoreManager.saveIfHighScore(p.getName(), state.getFloor(), p.getKills(), p.getGold());
            scoreSaved = true;
        }

        gc.setFill(TEXT_DIM);
        gc.setFont(Font.font("Monospaced", FontWeight.NORMAL, 13));
        gc.fillText("Press R to restart", winW / 2.0, winH / 2.0 + 50);
    }

    // ── Sprite helpers ────────────────────────────────────────────────────────

    /**
     * Returns the sprite Image for the given player by reading the sprite path
     * declared on the Player subclass.  Images are loaded on first access and
     * cached in {@link #spriteCache} for subsequent frames.
     *
     * If the player's sprite path is null or the resource cannot be found,
     * the wall sprite is returned as a visible fallback (avoids NullPointerException).
     *
     * @param player the player whose sprite should be retrieved
     * @return the Image to draw for this player character
     */
    private Image getPlayerSprite(Player player) {
        String path = player.getSpritePath();
        if (path == null) return wallSprite; // fallback — should not happen with proper subclasses

        return spriteCache.computeIfAbsent(path, p -> {
            var stream = getClass().getResourceAsStream(p);
            return stream != null ? new Image(stream) : wallSprite;
        });
    }

    // ── Swipe animation ───────────────────────────────────────────────────────

    private final java.util.List<SwipeAnim> swipes = new java.util.ArrayList<>();
    private long lastNanoTime = 0;

    /**
     * Immutable data record for a single arc-swipe animation.
     * Progress goes from 0.0 (just started) to 1.0 (finished and removed).
     */
    private static class SwipeAnim {
        int    tileX, tileY;
        double progress;
        boolean isPlayer;
        double  angleDeg;

        /**
         * Creates a new swipe animation aimed from (fromX, fromY) to (tileX, tileY).
         * The arc angle is derived from the direction of the attack.
         *
         * @param tileX    destination tile column (where the arc is drawn)
         * @param tileY    destination tile row
         * @param isPlayer true if the player is attacking (cyan arc), false for enemies (red)
         * @param fromX    attacker's tile column (used to compute the arc angle)
         * @param fromY    attacker's tile row
         */
        SwipeAnim(int tileX, int tileY, boolean isPlayer, int fromX, int fromY) {
            this.tileX    = tileX;
            this.tileY    = tileY;
            this.progress = 0.0;
            this.isPlayer = isPlayer;
            this.angleDeg = Math.toDegrees(Math.atan2(tileY - fromY, tileX - fromX));
        }
    }

    /**
     * Queues a new swipe animation for the next render frames.
     * Called by Main after wiring the onAttack callback on GameState.
     *
     * @param fromX    attacker X tile
     * @param fromY    attacker Y tile
     * @param toX      target X tile
     * @param toY      target Y tile
     * @param isPlayer true if the player is the attacker (determines arc colour)
     */
    public void triggerSwipe(int fromX, int fromY, int toX, int toY, boolean isPlayer) {
        swipes.add(new SwipeAnim(toX, toY, isPlayer, fromX, fromY));
        needsRedraw = true;
    }
}
