package game.ui;

import game.entity.characters.*;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.*;

import java.util.function.Consumer;

/**
 * Screen displayed between the StartScreen and the game itself.
 * Shows three character cards (Warrior, Rogue, Mage) and invokes
 * {@code onSelect} with the chosen Player instance when the player clicks Select.
 */
public class CharacterSelectScreen extends VBox {

    /**
     * Builds the character selection screen.
     *
     * @param playerName the name entered on the StartScreen, set on the chosen player
     * @param onSelect   callback invoked with the created Player when a card is chosen
     */
    public CharacterSelectScreen(String playerName, Consumer<Player> onSelect) {
        setAlignment(Pos.CENTER);
        setSpacing(20);
        setBackground(new Background(new BackgroundFill(Color.web("#0d0b12"), null, null)));

        Text title = new Text("Choose Your Character");
        title.setFont(Font.font("Jacquard 12", FontWeight.BOLD, 60));
        title.setFill(Color.web("#e8e0f0"));

        HBox cards = new HBox(20);
        cards.setAlignment(Pos.CENTER);

        cards.getChildren().addAll(
            makeCard("⚔  Warrior", "HP: 20  ATK: 4\nTank — slow but sturdy",
                () -> { Player p = new Warrior(0, 0); p.setName(playerName); onSelect.accept(p); }),
            makeCard("🗡  Rogue",   "HP: 12  ATK: 6\nGlass cannon — fast striker",
                () -> { Player p = new Rogue(0, 0);   p.setName(playerName); onSelect.accept(p); }),
            makeCard("🔮  Mage",    "HP: 8   ATK: 8\nFragile but devastating",
                () -> { Player p = new Mage(0, 0);    p.setName(playerName); onSelect.accept(p); })
        );

        getChildren().addAll(title, cards);
    }

    /**
     * Builds a single styled character card containing the class name, a stat description,
     * and a Select button that triggers {@code onPick} when clicked.
     *
     * @param name   class name with emoji prefix (e.g. "⚔  Warrior")
     * @param desc   two-line stats and flavour text shown below the name
     * @param onPick runnable invoked when the Select button is pressed
     * @return the fully constructed card VBox
     */
    private VBox makeCard(String name, String desc, Runnable onPick) {
        VBox card = new VBox(12);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new javafx.geometry.Insets(20));
        card.setStyle("-fx-background-color:#1e1a2e; -fx-border-color:#3a3058;"
                    + "-fx-border-radius:8; -fx-background-radius:8;");
        card.setPrefWidth(180);

        Text nameText = new Text(name);
        nameText.setFont(Font.font("Monospaced", FontWeight.BOLD, 16));
        nameText.setFill(Color.web("#5ba4e0"));

        Text descText = new Text(desc);
        descText.setFont(Font.font("Monospaced", 12));
        descText.setFill(Color.web("#7a6e8a"));
        descText.setTextAlignment(TextAlignment.CENTER);

        Button btn = new Button("Select");
        btn.setStyle("-fx-background-color:#2e2845; -fx-text-fill:#e8e0f0;"
                   + "-fx-font-family:Monospaced;");
        btn.setOnAction(e -> onPick.run());

        card.getChildren().addAll(nameText, descText, btn);
        return card;
    }
}
