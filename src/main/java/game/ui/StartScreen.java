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
 * Displays the game title, a high-score leaderboard, a name input field,
 * and a Start button. Pressing Enter in the text field is equivalent to
 * clicking Start.
 */
public class StartScreen extends VBox {

    /**
     * Builds the start screen.
     *
     * @param onStart callback invoked with the trimmed player name when the game is started
     */
    public StartScreen(Consumer<String> onStart) {
        setAlignment(Pos.CENTER);
        setSpacing(24);
        setPadding(new Insets(40));
        setBackground(new Background(new BackgroundFill(Color.web("#0d0b12"), null, null)));

        Text title = new Text("DUNGEON CRAWLER");
        title.setFont(Font.font("Jacquard 12", FontWeight.BOLD, 72));
        title.setFill(Color.web("#e8e0f0"));

        VBox scoreBox = buildScoreBoard();

        Text nameLabel = new Text("Enter your name:");
        nameLabel.setFont(Font.font("Monospaced", 14));
        nameLabel.setFill(Color.web("#7a6e8a"));

        TextField nameField = new TextField();
        nameField.setPromptText("Adventurer");
        nameField.setMaxWidth(220);
        nameField.setStyle(
            "-fx-background-color:#1e1a2e;" +
            "-fx-text-fill:#e8e0f0;" +
            "-fx-prompt-text-fill:#3a3058;" +
            "-fx-border-color:#3a3058;" +
            "-fx-border-radius:4;" +
            "-fx-background-radius:4;" +
            "-fx-font-family:Monospaced;" +
            "-fx-font-size:14;"
        );

        Button startBtn = new Button("▶  START GAME");
        startBtn.setStyle(
            "-fx-background-color:#2e2845;" +
            "-fx-text-fill:#5ba4e0;" +
            "-fx-font-family:Monospaced;" +
            "-fx-font-size:16;" +
            "-fx-padding:10 30;" +
            "-fx-border-color:#5ba4e0;" +
            "-fx-border-radius:4;" +
            "-fx-background-radius:4;"
        );

        // Shared start action: sanitises the name and fires the callback
        Runnable doStart = () -> {
            String name = nameField.getText().trim();
            if (name.isEmpty()) name = "Adventurer";
            name = name.replace(",", ""); // commas break the CSV score format
            onStart.accept(name);
        };

        startBtn.setOnAction(e -> doStart.run());
        nameField.setOnAction(e -> doStart.run());

        getChildren().addAll(title, scoreBox, nameLabel, nameField, startBtn);
    }

    /**
     * Builds the high-score table widget.
     * Loads up to 10 entries from {@link ScoreManager}, formats them as
     * fixed-width columns (rank, name, floor, kills, gold), and returns
     * the assembled VBox.  If no scores exist a placeholder message is shown.
     *
     * @return the styled score-board VBox ready to be added to the screen
     */
    private VBox buildScoreBoard() {
        VBox box = new VBox(6);
        box.setAlignment(Pos.CENTER);
        box.setStyle(
            "-fx-background-color:#1e1a2e;" +
            "-fx-border-color:#3a3058;" +
            "-fx-border-radius:8;" +
            "-fx-background-radius:8;"
        );
        box.setPadding(new Insets(16, 30, 16, 30));
        box.setMaxWidth(460);

        Text heading = new Text("⚔  HIGH SCORES  ⚔");
        heading.setFont(Font.font("Monospaced", FontWeight.BOLD, 15));
        heading.setFill(Color.web("#f0c040"));

        Text header = new Text(String.format("%-16s %6s %6s %6s", "NAME", "FLOOR", "KILLS", "GOLD"));
        header.setFont(Font.font("Monospaced", 12));
        header.setFill(Color.web("#5ba4e0"));

        box.getChildren().addAll(heading, header);

        List<String> scores = ScoreManager.load();
        if (scores.isEmpty()) {
            Text empty = new Text("No scores yet — be the first!");
            empty.setFont(Font.font("Monospaced", 12));
            empty.setFill(Color.web("#7a6e8a"));
            box.getChildren().add(empty);
        } else {
            for (int i = 0; i < scores.size(); i++) {
                String[] parts = scores.get(i).split(",", 4);
                if (parts.length < 4) continue; // skip malformed lines

                String rank = (i + 1) + ".";
                String line = String.format("%-3s %-14s %5s  %5s  %5s",
                        rank, parts[0], parts[1], parts[2], parts[3]);

                Text row = new Text(line);
                row.setFont(Font.font("Monospaced", 12));
                // Gold colour for #1, white for the rest
                row.setFill(i == 0 ? Color.web("#f0c040") : Color.web("#e8e0f0"));
                box.getChildren().add(row);
            }
        }

        return box;
    }
}
