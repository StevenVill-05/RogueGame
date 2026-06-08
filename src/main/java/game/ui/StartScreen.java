package game.ui;

import game.core.ScoreManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.*;

import java.util.List;
import java.util.function.Consumer;

/**
 * The initial title screen shown when the game launches or restarts.
 *
 * Layout (top-to-bottom inside a VBox):
 *   1. Game title
 *   2. High-score leaderboard  (built by buildScoreBoard)
 *   3. Name input field
 *   4. Row with "How to Play" button + Start button
 *
 * Clicking "How to Play" opens a modal overlay (buildTutorialOverlay) that
 * dims the screen and shows the tutorial panel; clicking anywhere on the
 * dim layer or the ✕ button dismisses it.
 *
 * Pressing Enter in the name field is equivalent to clicking Start.
 */
public class StartScreen extends StackPane {

    // ── Shared colour constants ────────────────────────────────────────────────

    /** Dark purple — used as the panel background for both sub-boxes. */
    private static final String BG_PANEL  = "#1e1a2e";

    /** Muted purple — used for borders and dim text. */
    private static final String BORDER    = "#3a3058";

    /** Light lavender — primary readable text colour. */
    private static final String TEXT_MAIN = "#e8e0f0";

    /** Soft blue — heading accents and HUD hints. */
    private static final String TEXT_BLUE = "#5ba4e0";

    /** Golden yellow — top-rank highlight and section headings. */
    private static final String TEXT_GOLD = "#f0c040";

    /** Muted grey-purple — secondary / placeholder text. */
    private static final String TEXT_DIM  = "#7a6e8a";

    // ── Constructor ───────────────────────────────────────────────────────────

    /**
     * Builds the entire start screen and registers all event handlers.
     *
     * The root is a StackPane so the tutorial overlay can be layered on top
     * of the main content without affecting layout flow.
     *
     * @param onStart callback invoked with the trimmed, sanitised player name
     *                when the user clicks Start or presses Enter
     */
    public StartScreen(Consumer<String> onStart) {
        // ── Main content layer ────────────────────────────────────────────────
        VBox content = new VBox(20);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(40));
        content.setBackground(new Background(new BackgroundFill(Color.web("#0d0b12"), null, null)));

        // ── Title ─────────────────────────────────────────────────────────────
        Text title = new Text("Dungeon Crawler");
        title.setFont(Font.font("Jacquard 12", FontWeight.BOLD, 120));
        title.setFill(Color.web(TEXT_MAIN));

        // ── Score board ───────────────────────────────────────────────────────
        VBox scoreBox = buildScoreBoard();

        // ── Name input label ──────────────────────────────────────────────────
        Text nameLabel = new Text("Enter your name:");
        nameLabel.setFont(Font.font("Monospaced", 14));
        nameLabel.setFill(Color.web(TEXT_DIM));

        // ── Name text field ───────────────────────────────────────────────────
        TextField nameField = new TextField();
        nameField.setPromptText("Adventurer");
        nameField.setMaxWidth(220);
        nameField.setStyle(
            "-fx-background-color:" + BG_PANEL + ";" +
            "-fx-text-fill:"        + TEXT_MAIN + ";" +
            "-fx-prompt-text-fill:" + BORDER    + ";" +
            "-fx-border-color:"     + BORDER    + ";" +
            "-fx-border-radius:4;"  +
            "-fx-background-radius:4;" +
            "-fx-font-family:Monospaced;" +
            "-fx-font-size:14;"
        );

        // ── How to Play button ────────────────────────────────────────────────
        // Opens the tutorial as a modal overlay instead of cluttering the screen.
        Button howToBtn = new Button("❓  HOW TO PLAY");
        howToBtn.setStyle(
            "-fx-background-color:" + BG_PANEL  + ";" +
            "-fx-text-fill:"        + TEXT_GOLD + ";" +
            "-fx-font-family:Monospaced;" +
            "-fx-font-size:14;" +
            "-fx-padding:10 20;" +
            "-fx-border-color:"     + TEXT_GOLD + ";" +
            "-fx-border-radius:4;"  +
            "-fx-background-radius:4;"
        );

        // ── Start button ──────────────────────────────────────────────────────
        Button startBtn = new Button("▶  START GAME");
        startBtn.setStyle(
            "-fx-background-color:" + BG_PANEL  + ";" +
            "-fx-text-fill:"        + TEXT_BLUE + ";" +
            "-fx-font-family:Monospaced;" +
            "-fx-font-size:14;" +
            "-fx-padding:10 20;" +
            "-fx-border-color:"     + TEXT_BLUE + ";" +
            "-fx-border-radius:4;"  +
            "-fx-background-radius:4;"
        );

        // ── Button row — How to Play sits left of Start ───────────────────────
        HBox btnRow = new HBox(16, howToBtn, startBtn);
        btnRow.setAlignment(Pos.CENTER);

        // ── Shared start action ───────────────────────────────────────────────
        // Trims the entered name, falls back to "Adventurer" if blank,
        // strips commas (they would corrupt the CSV score file), then fires onStart.
        Runnable doStart = () -> {
            String name = nameField.getText().trim();
            if (name.isEmpty()) name = "Adventurer";
            name = name.replace(",", ""); // commas break the CSV score format
            onStart.accept(name);
        };

        startBtn.setOnAction(e -> doStart.run());
        nameField.setOnAction(e -> doStart.run()); // Enter key in field = click Start

        // ── How to Play action — build and layer the overlay ──────────────────
        howToBtn.setOnAction(e -> showTutorialOverlay());

        // ── Assemble main content ─────────────────────────────────────────────
        content.getChildren().addAll(title, scoreBox, nameLabel, nameField, btnRow);

        // StackPane base layer = main content
        getChildren().add(content);
        setBackground(new Background(new BackgroundFill(Color.web("#0d0b12"), null, null)));
    }

    // ── Tutorial overlay ──────────────────────────────────────────────────────

    /**
     * Builds and adds a full-screen modal overlay containing the tutorial panel.
     *
     * The overlay consists of two layers pushed onto the StackPane:
     *   1. A semi-transparent dim pane that blocks interaction with the content below.
     *      Clicking it dismisses the overlay.
     *   2. The tutorial card (built by {@link #buildTutorialBox}) centred on screen,
     *      with a ✕ close button at its top-right corner.
     *
     * Both layers are removed together when the user closes the overlay.
     */
    private void showTutorialOverlay() {
        // ── Dim layer ─────────────────────────────────────────────────────────
        // Covers the entire StackPane; clicking it closes the overlay.
        Pane dimLayer = new Pane();
        dimLayer.setBackground(new Background(new BackgroundFill(
            Color.rgb(0, 0, 0, 0.65), null, null)));

        // ── Tutorial card ─────────────────────────────────────────────────────
        VBox card = buildTutorialBox();

        // ✕ close button — positioned at the top-right of the card
        Button closeBtn = new Button("✕");
        closeBtn.setStyle(
            "-fx-background-color:transparent;" +
            "-fx-text-fill:" + TEXT_DIM + ";" +
            "-fx-font-size:16;" +
            "-fx-cursor:hand;" +
            "-fx-padding:0 4 4 4;"
        );

        // Stack the card and the close button in a StackPane so the button
        // floats in the top-right corner without disrupting the card's layout.
        StackPane cardWrapper = new StackPane(card, closeBtn);
        StackPane.setAlignment(closeBtn, Pos.TOP_RIGHT);
        StackPane.setMargin(closeBtn, new Insets(8, 8, 0, 0));

        // Centre the card wrapper in the dim layer
        StackPane overlay = new StackPane(dimLayer, cardWrapper);
        overlay.setAlignment(Pos.CENTER);

        // ── Dismiss action ────────────────────────────────────────────────────
        Runnable dismiss = () -> getChildren().remove(overlay);
        closeBtn.setOnAction(e -> dismiss.run());
        // Clicking the dim area (not the card itself) also dismisses
        dimLayer.setOnMouseClicked(e -> dismiss.run());

        // Push the overlay on top of the existing StackPane layers
        getChildren().add(overlay);
    }

    // ── Score board ───────────────────────────────────────────────────────────

    /**
     * Builds the high-score table widget.
     *
     * Loads up to 10 entries from {@link ScoreManager}, formats them as
     * fixed-width columns (rank · name · floor · kills · gold · date), and
     * returns the assembled VBox.  The first-place row is highlighted in gold.
     * If no scores exist yet, a placeholder message is shown instead.
     *
     * @return a styled VBox ready to be inserted into the screen layout
     */
    private VBox buildScoreBoard() {
        VBox box = new VBox(6);
        box.setAlignment(Pos.CENTER);
        box.setStyle(
            "-fx-background-color:" + BG_PANEL + ";" +
            "-fx-border-color:"     + BORDER   + ";" +
            "-fx-border-radius:8;"  +
            "-fx-background-radius:8;"
        );
        box.setPadding(new Insets(16, 30, 16, 30));
        box.setMaxWidth(600);

        // Section heading
        Text heading = new Text("⚔  HIGH SCORES  ⚔");
        heading.setFont(Font.font("Monospaced", FontWeight.BOLD, 15));
        heading.setFill(Color.web(TEXT_GOLD));

        // Column-header row — uses monospaced fixed-width formatting
        Text header = new Text(String.format("%-16s %6s %6s %6s  %10s",
                "NAME", "FLOOR", "KILLS", "GOLD", "DATE"));
        header.setFont(Font.font("Monospaced", 12));
        header.setFill(Color.web(TEXT_BLUE));

        box.getChildren().addAll(heading, header);

        List<String> scores = ScoreManager.load();
        if (scores.isEmpty()) {
            // No runs recorded yet — show a friendly nudge
            Text empty = new Text("No scores yet — be the first!");
            empty.setFont(Font.font("Monospaced", 12));
            empty.setFill(Color.web(TEXT_DIM));
            box.getChildren().add(empty);
        } else {
            // Render each score entry as a single formatted text node
            for (int i = 0; i < scores.size(); i++) {
                String[] parts = scores.get(i).split(",", 5);
                if (parts.length < 4) continue; // skip lines that are malformed

                String rank = (i + 1) + ".";
                String date = parts.length >= 5 ? parts[4] : "";
                String line = String.format("%-3s %-14s %5s  %5s  %5s  %10s",
                        rank, parts[0], parts[1], parts[2], parts[3], date);

                Text row = new Text(line);
                row.setFont(Font.font("Monospaced", 12));
                // Gold highlight for first place; normal colour for the rest
                row.setFill(i == 0 ? Color.web(TEXT_GOLD) : Color.web(TEXT_MAIN));
                box.getChildren().add(row);
            }
        }

        return box;
    }

    // ── Tutorial box ──────────────────────────────────────────────────────────

    /**
     * Builds the "How to Play" tutorial card shown inside the modal overlay.
     *
     * The card is divided into three thematic columns:
     *   • Movement &amp; combat
     *   • Items &amp; map features
     *   • Skills &amp; hotkeys
     *
     * Each column is itself a VBox built by {@link #buildTutorialColumn}.
     * All content mirrors the actual key bindings wired in GameView.
     *
     * @return a styled VBox ready to be layered inside the overlay StackPane
     */
    private VBox buildTutorialBox() {
        VBox box = new VBox(14);
        box.setAlignment(Pos.CENTER);
        box.setStyle(
            "-fx-background-color:" + BG_PANEL + ";" +
            "-fx-border-color:"     + TEXT_GOLD + ";" +
            "-fx-border-width:2;"   +
            "-fx-border-radius:10;" +
            "-fx-background-radius:10;"
        );
        box.setPadding(new Insets(28, 36, 28, 36));
        box.setMaxWidth(720);

        // ── Section heading ────────────────────────────────────────────────────
        Text heading = new Text("📖  HOW TO PLAY");
        heading.setFont(Font.font("Monospaced", FontWeight.BOLD, 16));
        heading.setFill(Color.web(TEXT_GOLD));

        // ── Three columns ──────────────────────────────────────────────────────
        VBox colMovement = buildTutorialColumn("Movement & Combat",
            new String[]{
                "WASD / Arrows / hjkl", "Move in four directions",
                "Bump into enemy",      "Attack that enemy",
                "Enemies move each",    "turn you do",
            }
        );

        VBox colItems = buildTutorialColumn("Items & Map",
            new String[]{
                "💰  Gold coin",  "Walk over to collect",
                "🧪  Potion",     "Walk over to heal HP",
                ">  Stairs",      "Walk over to go deeper",
                "Fog of war",     "Unexplored tiles are dark",
            }
        );

        VBox colSkills = buildTutorialColumn("Skills & Keys",
            new String[]{
                "[1] [2] [3]",   "Activate skill slot",
                "First press",   "Unlocks skill (costs gold)",
                "Directional →", "Some skills need a direction",
                "R",             "Restart run",
                "F11",           "Toggle fullscreen",
            }
        );

        // ── Row to hold the three columns side-by-side ─────────────────────────
        HBox columns = new HBox(32, colMovement, colItems, colSkills);
        columns.setAlignment(Pos.TOP_CENTER);

        box.getChildren().addAll(heading, columns);
        return box;
    }

    /**
     * Builds a single labelled column for the tutorial panel.
     *
     * The {@code rows} array must contain an even number of strings:
     * alternating "key/concept" (highlighted in gold) and "description"
     * (shown in normal text).  Each pair becomes one line in the column.
     *
     * @param title the column heading text
     * @param rows  flat array of [key, description, key, description, …]
     * @return a VBox representing the column, sized for a fixed-width mono font
     */
    private VBox buildTutorialColumn(String title, String[] rows) {
        VBox col = new VBox(5);
        col.setAlignment(Pos.TOP_LEFT);
        col.setMinWidth(190);

        // Column title in soft blue
        Text titleText = new Text(title);
        titleText.setFont(Font.font("Monospaced", FontWeight.BOLD, 12));
        titleText.setFill(Color.web(TEXT_BLUE));
        col.getChildren().add(titleText);

        // Each pair of strings becomes one "key  description" text node
        for (int i = 0; i + 1 < rows.length; i += 2) {
            String key  = rows[i];
            String desc = rows[i + 1];

            // TextFlow lets key and description have different colours on one line
            Text keyText  = new Text(String.format("%-22s", key));
            keyText.setFont(Font.font("Monospaced", FontWeight.BOLD, 11));
            keyText.setFill(Color.web(TEXT_GOLD));

            Text descText = new Text(desc);
            descText.setFont(Font.font("Monospaced", 11));
            descText.setFill(Color.web(TEXT_MAIN));

            TextFlow line = new TextFlow(keyText, descText);
            col.getChildren().add(line);
        }

        return col;
    }
}
