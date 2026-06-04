package game.core;

import game.ui.CharacterSelectScreen;
import game.ui.GameView;
import game.ui.StartScreen;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
public class Main extends Application {


    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        GameState state = new GameState();
        Scene[] sceneHolder = new Scene[1];
        Runnable[] showSelect = new Runnable[1];


        //
        showSelect[0] = () -> {
            StartScreen startScreen = new StartScreen(playerName -> {
                CharacterSelectScreen selectScreen = new CharacterSelectScreen(playerName, chosenPlayer -> {
                    state.init(chosenPlayer);
                    GameView gameView = new GameView(state, showSelect[0], stage);
                    sceneHolder[0].setRoot(gameView.getRoot());
                    gameView.getCanvas().setOnKeyPressed(gameView::handleKeyPress);
                    gameView.getCanvas().requestFocus();
                });
                sceneHolder[0].setRoot(selectScreen);
            });
            if (sceneHolder[0] == null) {
                // First launch
                sceneHolder[0] = new Scene(startScreen, GameView.BASE_WIDTH, GameView.BASE_HEIGHT);
            } else {
                // Returns to game
                sceneHolder[0].setRoot(startScreen);
            }
        };

        // Run it once to set everything up for first launch
        showSelect[0].run();
        stage.setTitle("Dungeon Crawler");
        stage.setScene(sceneHolder[0]);
        stage.setResizable(true);
        stage.show();
    }
}