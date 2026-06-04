package game.core;

import java.sql.*;
import java.util.*;

public class ScoreManager {

    private static final String DB_URL = "jdbc:sqlite:highscores.db";

    // Called once at startup to ensure the table exists
    static {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {
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

    public static void saveIfHighScore(String name, int floor, int kills, int gold) {
        // Insert the new score
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

        // Keep only top 10 by kills — delete the rest
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

    // Returns list of strings in same format your UI already uses: "name,floor,kills,gold"
    public static List<String> load() {
        List<String> scores = new ArrayList<>();
        String query = "SELECT distinct name, floor, kills, gold FROM highscores ORDER BY kills DESC LIMIT 10";
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