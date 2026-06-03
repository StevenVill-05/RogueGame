package game.core;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class ScoreManager {

    private static final String FILE = "highscores.json";

    public static void saveIfHighScore(int floor, int kills, int gold) {
        List<String> scores = load();

        // Format: "floor,kills,gold"
        scores.add(floor + "," + kills + "," + gold);

        // Sort by kills descending, keep top 10
        scores.sort((a, b) -> {
            int ka = Integer.parseInt(a.split(",")[1]);
            int kb = Integer.parseInt(b.split(",")[1]);
            return kb - ka;
        });
        if (scores.size() > 10) scores = scores.subList(0, 10);

        try {
            Files.write(Paths.get(FILE), scores);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static List<String> load() {
        try {
            File f = new File(FILE);
            if (!f.exists()) return new ArrayList<>();
            return new ArrayList<>(Files.readAllLines(Paths.get(FILE)));
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }
}