package game.core;

import java.sql.*;
import java.util.*;

/**
 * Manages persistent high-score storage using an embedded SQLite database.
 * The {@code highscores} table is created automatically on first class load.
 * Only the top 10 entries (by kills) are retained after each save.
 */
public class ScoreManager {

    private static final String DB_URL = "jdbc:sqlite:highscores.db";

    // Static initialiser: ensure the table exists before any method is called
    static {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt  = conn.createStatement()) {
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS highscores (
                    id     INTEGER PRIMARY KEY AUTOINCREMENT,
                    name   TEXT    NOT NULL,
                    floor  INTEGER NOT NULL,
                    kills  INTEGER NOT NULL,
                    gold   INTEGER NOT NULL
                )
            """);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Inserts a new score record and then prunes the table to keep only the
     * top 10 entries ordered by kills descending.
     *
     * @param name  player's display name
     * @param floor dungeon floor reached
     * @param kills total enemies killed
     * @param gold  total gold collected
     */
    public static void saveIfHighScore(String name, int floor, int kills, int gold) {
        String insert = "INSERT INTO highscores (name, floor, kills, gold) VALUES (?, ?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement ps = conn.prepareStatement(insert)) {
            ps.setString(1, name);
            ps.setInt(2, floor);
            ps.setInt(3, kills);
            ps.setInt(4, gold);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // Delete all rows outside the top 10 by kills
        String cleanup = """
            DELETE FROM highscores
            WHERE id NOT IN (
                SELECT id FROM highscores
                ORDER BY kills DESC
                LIMIT 10
            )
        """;
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(cleanup);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Loads the top 10 scores from the database, ordered by kills descending.
     * Each entry is returned as a comma-separated string: {@code "name,floor,kills,gold"}.
     * This format matches what the StartScreen's score table parser expects.
     *
     * @return a list of up to 10 score strings, or an empty list if none exist
     */
    public static List<String> load() {
        List<String> scores = new ArrayList<>();
        String query = "SELECT DISTINCT name, floor, kills, gold FROM highscores ORDER BY kills DESC LIMIT 10";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement ps = conn.prepareStatement(query);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                scores.add(rs.getString("name") + "," +
                           rs.getInt("floor")   + "," +
                           rs.getInt("kills")   + "," +
                           rs.getInt("gold"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return scores;
    }
}
