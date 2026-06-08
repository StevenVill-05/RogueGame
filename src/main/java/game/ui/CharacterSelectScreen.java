package game.ui;

import game.entity.characters.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Button;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.*;

import java.util.function.Consumer;

/**
 * Screen displayed between the StartScreen and the game itself.
 *
 * Shows three character cards (Warrior, Rogue, Mage) and invokes
 * {@code onSelect} with the chosen Player instance when the player clicks Select.
 *
 * Background:
 *   A {@link DungeonBackground#warm(Canvas)} instance paints an animated amber-
 *   torchlit stone chamber onto a Canvas bound to the StackPane's dimensions.
 *   The warmer palette distinguishes this screen from the cool-blue StartScreen.
 *
 *   All card panels use semi-transparent fills so the animated background
 *   glows through behind them.
 */
public class CharacterSelectScreen extends StackPane {

    /** Reference kept so the caller can halt the AnimationTimer after navigation. */
    private final DungeonBackground bg;

    // ── Colour palette (consistent with StartScreen) ───────────────────────────
    private static final String BG_PANEL  = "rgba(22,14,8,0.82)";
    private static final String BORDER    = "#4a3828";
    private static final String TEXT_MAIN = "#e8e0f0";
    private static final String TEXT_BLUE = "#5ba4e0";
    private static final String TEXT_GOLD = "#f0c040";
    private static final String TEXT_DIM  = "#7a6e5a";

    /**
     * Builds the character selection screen.
     *
     * Root is a StackPane (back → front):
     *   1. Animated Canvas — DungeonBackground.warm() renders torch-lit stone.
     *   2. Content VBox — title + three character cards.
     *
     * @param playerName the name entered on the StartScreen, set on the chosen player
     * @param onSelect   callback invoked with the created Player when a card is chosen
     */
    public CharacterSelectScreen(String playerName, Consumer<Player> onSelect) {

        // ── Background canvas ──────────────────────────────────────────────────
        Canvas bgCanvas = new Canvas();
        bgCanvas.widthProperty().bind(widthProperty());
        bgCanvas.heightProperty().bind(heightProperty());
        bg = DungeonBackground.warm(bgCanvas);

        // ── Content layer ─────────────────────────────────────────────────────
        VBox content = new VBox(28);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(48));
        content.setBackground(Background.EMPTY);

        // ── Screen title ───────────────────────────────────────────────────────
        Text shadowTitle = new Text("Choose Your Character");
        shadowTitle.setFont(Font.font("Jacquard 12", FontWeight.BOLD, 60));
        shadowTitle.setFill(Color.color(0, 0, 0, 0.75));

        Text title = new Text("Choose Your Character");
        title.setFont(Font.font("Jacquard 12", FontWeight.BOLD, 60));
        title.setFill(Color.web(TEXT_MAIN));

        StackPane titleStack = new StackPane();
        StackPane.setMargin(shadowTitle, new Insets(4, 0, 0, 4));
        titleStack.getChildren().addAll(shadowTitle, title);
        titleStack.setAlignment(Pos.CENTER);

        // ── Character cards ────────────────────────────────────────────────────
        HBox cards = new HBox(24);
        cards.setAlignment(Pos.CENTER);

        cards.getChildren().addAll(
            makeCard(
                "⚔  Warrior",
                "HP: 20  ATK: 4",
                "Tank — slow but sturdy.\nHigh survivability,\nlow burst damage.",
                "#c87040",   // warm copper accent
                () -> { Player p = new Warrior(0, 0); p.setName(playerName); onSelect.accept(p); }
            ),
            makeCard(
                "🗡  Rogue",
                "HP: 12  ATK: 6",
                "Glass cannon — fast\nstriker. High risk,\nhigh reward.",
                "#60b878",   // green accent
                () -> { Player p = new Rogue(0, 0); p.setName(playerName); onSelect.accept(p); }
            ),
            makeCard(
                "🔮  Mage",
                "HP: 8   ATK: 8",
                "Fragile but devastating.\nMassive spell damage,\npaper-thin defences.",
                "#8860d8",   // purple accent
                () -> { Player p = new Mage(0, 0); p.setName(playerName); onSelect.accept(p); }
            )
        );

        content.getChildren().addAll(titleStack, cards);

        // StackPane layers: canvas → content
        getChildren().addAll(bgCanvas, content);
    }

    /** Stops the background AnimationTimer — call when this screen is removed. */
    public void stopBackground() {
        bg.stop();
    }

    /**
     * Builds a single styled character card.
     *
     * Each card is a VBox containing:
     *   • Class name + emoji (in the class accent colour)
     *   • One-line stat summary (gold)
     *   • Two/three-line flavour description (dim)
     *   • A "Select" button styled with the class accent colour
     *
     * The card panel uses a semi-transparent fill so torch glow from the
     * background can be faintly seen behind it.
     *
     * @param name      class name with emoji prefix
     * @param stats     short stat line shown below the name
     * @param desc      multi-line flavour / description text
     * @param accent    hex colour string for the card's accent (name, button border)
     * @param onPick    runnable invoked when the Select button is pressed
     * @return the fully constructed card VBox
     */
    private VBox makeCard(String name, String stats, String desc, String accent, Runnable onPick) {
        VBox card = new VBox(12);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(24, 20, 24, 20));
        card.setStyle(
            "-fx-background-color:" + BG_PANEL + ";" +
            "-fx-border-color:" + accent + ";" +
            "-fx-border-width:1.5;" +
            "-fx-border-radius:10;" +
            "-fx-background-radius:10;" +
            "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.75),14,0,0,4);"
        );
        card.setPrefWidth(200);
        card.setMinWidth(200);

        // Class name
        Text nameText = new Text(name);
        nameText.setFont(Font.font("Monospaced", FontWeight.BOLD, 17));
        nameText.setFill(Color.web(accent));

        // Divider line
        Region divider = new Region();
        divider.setPrefHeight(1);
        divider.setPrefWidth(160);
        divider.setStyle("-fx-background-color:" + BORDER + ";");

        // Stats line
        Text statsText = new Text(stats);
        statsText.setFont(Font.font("Monospaced", FontWeight.BOLD, 12));
        statsText.setFill(Color.web(TEXT_GOLD));
        statsText.setTextAlignment(TextAlignment.CENTER);

        // Flavour description
        Text descText = new Text(desc);
        descText.setFont(Font.font("Monospaced", 11));
        descText.setFill(Color.web(TEXT_DIM));
        descText.setTextAlignment(TextAlignment.CENTER);

        // Select button styled with the class accent
        Button btn = new Button("▶  Select");
        btn.setStyle(
            "-fx-background-color:rgba(10,7,4,0.75);" +
            "-fx-text-fill:" + accent + ";" +
            "-fx-font-family:Monospaced;" +
            "-fx-font-size:13;" +
            "-fx-padding:8 20;" +
            "-fx-border-color:" + accent + ";" +
            "-fx-border-radius:4;" +
            "-fx-background-radius:4;" +
            "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.6),6,0,0,2);"
        );
        btn.setOnAction(e -> onPick.run());

        card.getChildren().addAll(nameText, divider, statsText, descText, btn);
        return card;
    }
}
