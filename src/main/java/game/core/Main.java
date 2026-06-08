package game.core;

import game.ui.CharacterSelectScreen;
import game.ui.GameView;
import game.ui.StartScreen;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Application entry point.
 * Owns the primary Stage and the scene-navigation flow:
 *   StartScreen → CharacterSelectScreen → GameView
 *
 * A single shared {@link GameState} is reused across restarts; only the
 * scene root changes. The {@code showSelect} runnable encapsulates the
 * full "go back to start" logic and is passed into GameView as {@code onRestart}.
 */
public class Main extends Application {

    /**
     * Standard JavaFX launch entry point — delegates to {@link #start(Stage)}.
     */
    public static void main(String[] args) {
        launch(args);
    }

    /**
     * Builds the initial scene and wires the three screens together.
     *
     * Flow:
     * 1. {@code showSelect} creates a {@link StartScreen}; the player enters a name.
     * 2. A {@link CharacterSelectScreen} is shown; the player picks a class.
     * 3. A {@link GameView} is created with the initialised {@link GameState},
     *    and the attack-animation callback is wired so GameView can play
     *    swipe arcs without depending on GameState internals.
     * 4. Pressing R calls {@code showSelect} again, restarting the loop.
     *
     * @param stage the primary JavaFX window provided by the platform
     */
    @Override
    public void start(Stage stage) {
        GameState state = new GameState();

        // One-element arrays used as mutable references inside lambdas
        Scene[]    sceneHolder = new Scene[1];
        Runnable[] showSelect  = new Runnable[1];

        showSelect[0] = () -> {
            StartScreen startScreen = new StartScreen(playerName -> {
                // Player has entered a name — show character selection
                CharacterSelectScreen selectScreen = new CharacterSelectScreen(playerName, chosenPlayer -> {
                    // Character chosen — initialise game and switch to GameView
                    state.init(chosenPlayer);
                    GameView gameView = new GameView(state, showSelect[0], stage);

                    // Wire attack callback: fires on the JavaFX thread so the view
                    // can safely enqueue swipe animations
                    state.setOnAttack(coords ->
                            javafx.application.Platform.runLater(() ->
                                    gameView.triggerSwipe(coords[0], coords[1],
                                                          coords[2], coords[3],
                                                          coords[4] == 1)));

                    sceneHolder[0].setRoot(gameView.getRoot());
                    gameView.getCanvas().setOnKeyPressed(gameView::handleKeyPress);
                    gameView.getCanvas().requestFocus();
                });
                sceneHolder[0].setRoot(selectScreen);
            });

            if (sceneHolder[0] == null) {
                // First launch — create the Scene
                sceneHolder[0] = new Scene(startScreen, GameView.BASE_WIDTH, GameView.BASE_HEIGHT);
            } else {
                // Restart — reuse the existing Scene, just swap the root
                sceneHolder[0].setRoot(startScreen);
            }
        };

        showSelect[0].run();

        stage.setTitle("Dungeon Crawler");
        stage.setScene(sceneHolder[0]);
        stage.setResizable(true);
        stage.show();
    }
}
