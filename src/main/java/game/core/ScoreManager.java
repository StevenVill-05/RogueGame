package game.core;

import java.sql.*;
import java.util.*;

/**
 * Manages persistent high-score storage using an embedded SQLite database.
 * The {@code highscores} table is created automatically on first class load.
 * Only the top 10 entries (by kills) are retained after each save.
 * Each record stores the date it was achieved; when loading, scores are
 * de-duplicated on (name, floor, kills, gold) and the most recent date wins.
 */
public class ScoreManager {

    private static final String DB_URL = "jdbc:sqlite:highscores.db";

    static {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt  = conn.createStatement()) {
            // Create table with date column (TEXT in ISO-8601 format)
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS highscores (
                    id     INTEGER PRIMARY KEY AUTOINCREMENT,
                    name   TEXT    NOT NULL,
                    floor  INTEGER NOT NULL,
                    kills  INTEGER NOT NULL,
                    gold   INTEGER NOT NULL,
                    date   TEXT    NOT NULL DEFAULT (date('now'))
                )
            """);
            // Add date column to existing DBs that were created before this version
            try {
                stmt.executeUpdate("ALTER TABLE highscores ADD COLUMN date TEXT NOT NULL DEFAULT (date('now'))");
            } catch (SQLException ignored) {
                // Column already exists — safe to ignore
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Inserts a new score record (including today's date) and then prunes the
     * table to keep only the top 10 entries ordered by kills descending.
     */
    public static void saveIfHighScore(String name, int floor, int kills, int gold) {
        String insert = "INSERT INTO highscores (name, floor, kills, gold, date) VALUES (?, ?, ?, ?, date('now'))";
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

        // Prune to top 10 by kills
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
     * Scores are de-duplicated on (name, floor, kills, gold) — duplicates
     * with the same stats only appear once, using the most recent date.
     * Each entry is returned as: {@code "name,floor,kills,gold,date"}.
     */
    public static List<String> load() {
        List<String> scores = new ArrayList<>();
        // GROUP BY the stat columns so identical runs collapse to one row;
        // MAX(date) picks the most recent occurrence.
        String query = """
            SELECT name, floor, kills, gold, MAX(date) AS date
            FROM highscores
            GROUP BY name, floor, kills, gold
            ORDER BY kills DESC
            LIMIT 10
        """;
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement ps = conn.prepareStatement(query);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                scores.add(rs.getString("name") + "," +
                           rs.getInt("floor")   + "," +
                           rs.getInt("kills")   + "," +
                           rs.getInt("gold")    + "," +
                           rs.getString("date"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return scores;
    }
}
