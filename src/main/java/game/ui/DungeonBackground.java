package game.ui;

import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;

import java.util.Random;

/**
 * Procedurally generated dungeon atmospheric background painted on a Canvas.
 *
 * Renders a believable underground stone chamber using layered draw calls —
 * no external image files required.  The visual is built from:
 *
 *   1. Base stone floor — a grid of slightly varied dark-grey rectangles
 *      with thin mortar lines, giving depth via subtle per-tile colour noise.
 *   2. Wall / ceiling band — a thick strip along the top with crude stone-block
 *      outlines, suggesting the room's architecture.
 *   3. Torch glows — two or four radial-gradient "light pools" placed near the
 *      walls; their radius and brightness are animated with a low-frequency
 *      sine wave to simulate torch flicker.
 *   4. Corner shadows — four large radial gradients fading to near-black at
 *      each corner, creating a convincing sense of enclosed depth.
 *   5. Vignette — a full-screen soft dark overlay from the canvas edges inward,
 *      giving the classic dungeon-crawler "looking through a dark tunnel" feel.
 *   6. Dust motes — small, slowly drifting semi-transparent white circles that
 *      drift upward and loop.  Their opacity pulses with an individual phase
 *      offset so they feel organic rather than uniform.
 *
 * Two static factory methods choose colour temperature:
 *   {@link #cool(Canvas)}   — blue-tinted torchlight; used on the StartScreen.
 *   {@link #warm(Canvas)}   — orange-amber torchlight; used on CharacterSelectScreen.
 *
 * The returned instance is already started ({@link #start()} is called internally).
 * Call {@link #stop()} when the screen is removed from the scene graph to avoid
 * leaking the AnimationTimer.
 */
public class DungeonBackground {

    // ── Torch colour variants ─────────────────────────────────────────────────

    /** Blue-tinted torchlight — eerie, ancient feel for the title screen. */
    public static DungeonBackground cool(Canvas canvas) {
        return new DungeonBackground(canvas,
            Color.color(0.18, 0.30, 0.55, 0.55),  // torch inner glow
            Color.color(0.08, 0.14, 0.30, 0.0),   // torch outer (transparent)
            Color.color(0.05, 0.06, 0.14)          // base stone colour
        );
    }

    /** Warm amber torchlight — welcoming but foreboding for character selection. */
    public static DungeonBackground warm(Canvas canvas) {
        return new DungeonBackground(canvas,
            Color.color(0.60, 0.32, 0.05, 0.55),  // torch inner glow (amber)
            Color.color(0.20, 0.10, 0.02, 0.0),   // torch outer (transparent)
            Color.color(0.07, 0.05, 0.03)          // base stone (warm dark)
        );
    }

    // ── Internal state ────────────────────────────────────────────────────────

    private final Canvas           canvas;
    private final GraphicsContext  gc;
    private final AnimationTimer   timer;
    private final Random           rng = new Random(42); // fixed seed = stable stone layout

    /** Colour of the bright torch core. */
    private final Color torchInner;
    /** Colour at the edge of the torch gradient (should be transparent). */
    private final Color torchOuter;
    /** Darkest base colour used for stone tiles. */
    private final Color stoneBase;

    // Pre-computed per-tile colour noise so the floor doesn't flicker on redraw
    private float[] tileNoise;

    // Dust mote state — positions in [0,1] normalised coords, plus phase offset
    private static final int  MOTE_COUNT = 40;
    private final double[] moteX    = new double[MOTE_COUNT];
    private final double[] moteY    = new double[MOTE_COUNT];
    private final double[] motePhase = new double[MOTE_COUNT];
    private final double[] moteSpeed = new double[MOTE_COUNT];
    private final double[] moteSize  = new double[MOTE_COUNT];

    // Tile grid constants for the stone floor and ceiling
    private static final int TILE_W    = 42;
    private static final int TILE_H    = 38;
    private static final int CEIL_H    = 80; // height of the ceiling / wall band

    // ── Constructor ───────────────────────────────────────────────────────────

    private DungeonBackground(Canvas canvas, Color torchInner, Color torchOuter, Color stoneBase) {
        this.canvas     = canvas;
        this.gc         = canvas.getGraphicsContext2D();
        this.torchInner = torchInner;
        this.torchOuter = torchOuter;
        this.stoneBase  = stoneBase;

        initMotes();

        // Lazy-initialise tileNoise on first frame when dimensions are known
        tileNoise = null;

        long[] startNanos = { 0 };

        timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (startNanos[0] == 0) startNanos[0] = now;
                double t = (now - startNanos[0]) / 1_000_000_000.0; // seconds
                drawFrame(t);
            }
        };

        timer.start();
    }

    /** Stops the animation timer.  Call this when the background is no longer visible. */
    public void stop() {
        timer.stop();
    }

    // ── Initialisation helpers ────────────────────────────────────────────────

    /**
     * Scatter dust motes at random positions across the canvas.
     * Y positions are biased toward the lower half of the screen so motes
     * appear to rise from the dungeon floor rather than fall from the ceiling.
     */
    private void initMotes() {
        Random r = new Random(99);
        for (int i = 0; i < MOTE_COUNT; i++) {
            moteX[i]     = r.nextDouble();
            moteY[i]     = r.nextDouble();
            motePhase[i] = r.nextDouble() * Math.PI * 2;
            moteSpeed[i] = 0.012 + r.nextDouble() * 0.018;
            moteSize[i]  = 1.0 + r.nextDouble() * 2.2;
        }
    }

    /**
     * Lazily pre-compute per-tile brightness noise when the canvas has a valid size.
     * Each tile gets a value in [-0.025, 0.025] added to its grey channel so the
     * floor looks like real stone rather than a uniform fill.
     */
    private void ensureTileNoise(int cols, int rows) {
        int needed = (cols + 1) * (rows + 2);
        if (tileNoise != null && tileNoise.length >= needed) return;
        tileNoise = new float[needed];
        for (int i = 0; i < tileNoise.length; i++) {
            tileNoise[i] = (float)(rng.nextDouble() * 0.05 - 0.025);
        }
    }

    // ── Draw ──────────────────────────────────────────────────────────────────

    /**
     * Redraws the entire background for one frame.
     *
     * Drawing order (back → front):
     *   1. Solid near-black base fill
     *   2. Stone floor tiles (below the ceiling band)
     *   3. Ceiling / wall band with stone-block outlines
     *   4. Torch glow pools (animated)
     *   5. Corner darkness gradients
     *   6. Full-screen vignette
     *   7. Dust motes (animated)
     *
     * @param t elapsed time in seconds, used to drive all animations
     */
    private void drawFrame(double t) {
        double W = canvas.getWidth();
        double H = canvas.getHeight();
        if (W <= 0 || H <= 0) return;

        int cols = (int)(W / TILE_W) + 2;
        int rows = (int)((H - CEIL_H) / TILE_H) + 2;
        ensureTileNoise(cols, rows);

        gc.save();

        // ── 1. Base fill ──────────────────────────────────────────────────────
        gc.setFill(stoneBase);
        gc.fillRect(0, 0, W, H);

        // ── 2. Stone floor tiles ──────────────────────────────────────────────
        double br = stoneBase.getRed();
        double bg = stoneBase.getGreen();
        double bb = stoneBase.getBlue();

        for (int row = 0; row < rows; row++) {
            double ty = CEIL_H + row * TILE_H;
            for (int col = 0; col < cols; col++) {
                double tx = col * TILE_W;
                int idx = row * (cols + 1) + col;
                if (idx >= tileNoise.length) continue;
                double noise = tileNoise[idx];

                // Perspective darkening — tiles near the top of the floor area (closer to
                // the ceiling) are darker, giving the impression of depth receding into shadow.
                double perspectiveFade = 0.55 + 0.45 * ((double)row / rows);

                gc.setFill(Color.color(
                    clamp(br + noise + 0.08, 0, 1) * perspectiveFade,
                    clamp(bg + noise + 0.07, 0, 1) * perspectiveFade,
                    clamp(bb + noise + 0.09, 0, 1) * perspectiveFade
                ));
                gc.fillRect(tx + 1, ty + 1, TILE_W - 2, TILE_H - 2);

                // Mortar lines — 1 px darker stroke between tiles
                gc.setStroke(Color.color(br * 0.4, bg * 0.4, bb * 0.4, 0.8));
                gc.setLineWidth(1.0);
                gc.strokeRect(tx + 0.5, ty + 0.5, TILE_W - 1, TILE_H - 1);
            }
        }

        // ── 3. Ceiling / wall band ─────────────────────────────────────────────
        // Large dark fill covers the top portion
        double ceilDark = 0.6;
        gc.setFill(Color.color(br * ceilDark, bg * ceilDark, bb * ceilDark));
        gc.fillRect(0, 0, W, CEIL_H);

        // Stone blocks drawn on the ceiling band
        int blockW = 60, blockH = 30;
        int ceilRows = (CEIL_H / blockH) + 1;
        for (int row = 0; row < ceilRows; row++) {
            // Offset every other row for a brick-like stagger
            double offsetX = (row % 2 == 0) ? 0 : blockW * 0.5;
            int blockCols = (int)(W / blockW) + 2;
            for (int col = 0; col < blockCols; col++) {
                double tx = offsetX + col * blockW - blockW;
                double ty = row * blockH;
                int idx = row * (blockCols + 1) + col;
                float noise = (idx < tileNoise.length) ? tileNoise[idx] : 0;

                double brightness = 0.18 + noise * 0.3 + 0.04 * (row / (double)ceilRows);
                gc.setFill(Color.color(
                    clamp(br + brightness, 0, 1) * ceilDark,
                    clamp(bg + brightness, 0, 1) * ceilDark,
                    clamp(bb + brightness + 0.015, 0, 1) * ceilDark
                ));
                gc.fillRect(tx + 1, ty + 1, blockW - 2, blockH - 2);

                // Block outline
                gc.setStroke(Color.color(0, 0, 0, 0.55));
                gc.setLineWidth(1.5);
                gc.strokeRect(tx + 0.5, ty + 0.5, blockW - 1, blockH - 1);
            }
        }

        // Thin bright edge line where ceiling meets floor — simulates the room's
        // back wall / skirting, a visual "ground line" for the far wall
        gc.setStroke(Color.color(
            clamp(br + 0.22, 0, 1),
            clamp(bg + 0.20, 0, 1),
            clamp(bb + 0.25, 0, 1),
            0.45));
        gc.setLineWidth(2.0);
        gc.strokeLine(0, CEIL_H, W, CEIL_H);

        // ── 4. Torch glows (animated) ──────────────────────────────────────────
        // Two torches placed 15% and 85% horizontally, near the bottom of the ceiling.
        drawTorch(W * 0.15, CEIL_H - 10, W * 0.38, t, 0.0);
        drawTorch(W * 0.85, CEIL_H - 10, W * 0.38, t, 1.3);

        // On wider canvases add two more torches toward the far wall
        if (W > 600) {
            drawTorch(W * 0.50, CEIL_H - 5, W * 0.28, t, 0.7);
        }

        // ── 5. Corner darkness ────────────────────────────────────────────────
        drawCornerShadow(0,   0,   W * 0.55, H * 0.55, 0.0); // top-left
        drawCornerShadow(W,   0,   W * 0.55, H * 0.55, 0.0); // top-right
        drawCornerShadow(0,   H,   W * 0.50, H * 0.50, 0.0); // bottom-left
        drawCornerShadow(W,   H,   W * 0.50, H * 0.50, 0.0); // bottom-right

        // ── 6. Vignette — soft full-screen dark edge ──────────────────────────
        RadialGradient vignette = new RadialGradient(
            0, 0,
            W / 2, H / 2,
            Math.max(W, H) * 0.72,
            false,
            CycleMethod.NO_CYCLE,
            new Stop(0.0, Color.TRANSPARENT),
            new Stop(0.65, Color.TRANSPARENT),
            new Stop(1.0, Color.color(0, 0, 0, 0.80))
        );
        gc.setFill(vignette);
        gc.fillRect(0, 0, W, H);

        // ── 7. Dust motes ─────────────────────────────────────────────────────
        for (int i = 0; i < MOTE_COUNT; i++) {
            // Move upward, wrapping when the mote exits the top
            moteY[i] -= moteSpeed[i] * 0.008;
            if (moteY[i] < -0.02) moteY[i] = 1.02;

            double px = moteX[i] * W;
            double py = moteY[i] * H;

            // Opacity pulses between ~10% and ~45%
            double alpha = 0.10 + 0.35 * (0.5 + 0.5 * Math.sin(t * 1.2 + motePhase[i]));
            double size  = moteSize[i] * (0.85 + 0.15 * Math.sin(t * 0.9 + motePhase[i]));

            gc.setFill(Color.color(1, 1, 1, alpha));
            gc.fillOval(px - size / 2, py - size / 2, size, size);
        }

        gc.restore();
    }

    /**
     * Draws a single radial torch glow centred at (cx, cy).
     *
     * The glow radius pulses with a low-frequency sine driven by {@code t} and
     * the per-torch {@code phase} offset, producing an independent flicker for
     * each light source.  The gradient runs from {@link #torchInner} at the
     * centre to fully transparent ({@link #torchOuter}) at the edge.
     *
     * @param cx     centre X of the glow pool
     * @param cy     centre Y of the glow pool
     * @param radius nominal radius — actual radius is animated ±8%
     * @param t      elapsed time in seconds
     * @param phase  per-torch phase offset so torches flicker independently
     */
    private void drawTorch(double cx, double cy, double radius, double t, double phase) {
        // Low-frequency flicker: 0.92 – 1.08 multiplier
        double flicker = 1.0 + 0.08 * Math.sin(t * 3.7 + phase)
                             + 0.04 * Math.sin(t * 7.1 + phase * 2.3);
        double r = radius * flicker;

        RadialGradient glow = new RadialGradient(
            0, 0,
            cx, cy,
            r,
            false,
            CycleMethod.NO_CYCLE,
            new Stop(0.0, torchInner),
            new Stop(0.55, Color.color(
                torchInner.getRed(),
                torchInner.getGreen(),
                torchInner.getBlue(),
                torchInner.getOpacity() * 0.3)),
            new Stop(1.0, torchOuter)
        );
        gc.setFill(glow);
        gc.fillRect(cx - r, cy - r, r * 2, r * 2);
    }

    /**
     * Paints a dark radial gradient from (cx, cy) outward, making the given
     * corner of the canvas appear to fade into deep shadow.
     *
     * @param cx, cy  corner pixel coordinates
     * @param rw, rh  gradient ellipse half-widths (to allow asymmetric coverage)
     * @param alpha   base opacity of the shadow (0 = no shadow)
     */
    private void drawCornerShadow(double cx, double cy, double rw, double rh, double alpha) {
        double r = Math.max(rw, rh);
        RadialGradient shadow = new RadialGradient(
            0, 0,
            cx, cy,
            r,
            false,
            CycleMethod.NO_CYCLE,
            new Stop(0.0, Color.color(0, 0, 0, 0.78)),
            new Stop(0.5, Color.color(0, 0, 0, 0.30)),
            new Stop(1.0, Color.TRANSPARENT)
        );
        gc.setFill(shadow);
        gc.fillRect(cx - r, cy - r, r * 2, r * 2);
    }

    // ── Utility ───────────────────────────────────────────────────────────────

    private static double clamp(double v, double lo, double hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }
}
