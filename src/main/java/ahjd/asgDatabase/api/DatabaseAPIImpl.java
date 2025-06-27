package ahjd.asgDatabase.api;

import ahjd.asgDatabase.data.DatabaseManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Implementation of the DatabaseAPI interface.
 */
public class DatabaseAPIImpl implements DatabaseAPI {

    private final DatabaseManager databaseManager;
    private final JavaPlugin plugin;

    public DatabaseAPIImpl(DatabaseManager databaseManager, JavaPlugin plugin) {
        this.databaseManager = databaseManager;
        this.plugin = plugin;
    }

    @Override
    public boolean savePlayerData(UUID uuid, String pluginName, Map<String, Object> data) {
        if (uuid == null || pluginName == null || data == null || data.isEmpty()) {
            return false;
        }

        try (Connection conn = databaseManager.getConnection()) {
            // First delete any existing data for this player and plugin
            String deleteSql = "DELETE FROM plugin_data WHERE uuid = ? AND plugin_name = ?";
            try (PreparedStatement ps = conn.prepareStatement(deleteSql)) {
                ps.setString(1, uuid.toString());
                ps.setString(2, pluginName);
                ps.executeUpdate();
            }

            // Now insert the new data
            String insertSql = "INSERT INTO plugin_data (uuid, plugin_name, data_key, data_value) VALUES (?, ?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                for (Map.Entry<String, Object> entry : data.entrySet()) {
                    ps.setString(1, uuid.toString());
                    ps.setString(2, pluginName);
                    ps.setString(3, entry.getKey());
                    ps.setString(4, entry.getValue().toString());
                    ps.executeUpdate();
                }
            }
            return true;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save player data", e);
            return false;
        }
    }

    @Override
    public Map<String, Object> getPlayerData(UUID uuid, String pluginName) {
        if (uuid == null || pluginName == null) {
            return null;
        }

        Map<String, Object> result = new HashMap<>();
        try (Connection conn = databaseManager.getConnection()) {
            String sql = "SELECT data_key, data_value FROM plugin_data WHERE uuid = ? AND plugin_name = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                ps.setString(2, pluginName);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        result.put(rs.getString("data_key"), rs.getString("data_value"));
                    }
                }
            }
            return result.isEmpty() ? null : result;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to get player data", e);
            return null;
        }
    }

    @Override
    public boolean hasPlayerData(UUID uuid, String pluginName) {
        if (uuid == null || pluginName == null) {
            return false;
        }

        try (Connection conn = databaseManager.getConnection()) {
            String sql = "SELECT COUNT(*) FROM plugin_data WHERE uuid = ? AND plugin_name = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                ps.setString(2, pluginName);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() && rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to check if player data exists", e);
            return false;
        }
    }

    @Override
    public boolean deletePlayerData(UUID uuid, String pluginName) {
        if (uuid == null || pluginName == null) {
            return false;
        }

        try (Connection conn = databaseManager.getConnection()) {
            String sql = "DELETE FROM plugin_data WHERE uuid = ? AND plugin_name = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                ps.setString(2, pluginName);
                return ps.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to delete player data", e);
            return false;
        }
    }

    @Override
    public boolean saveGlobalData(String pluginName, String key, Object value) {
        if (pluginName == null || key == null || value == null) {
            return false;
        }

        try (Connection conn = databaseManager.getConnection()) {
            String sql = "INSERT OR REPLACE INTO global_data (plugin_name, data_key, data_value) VALUES (?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, pluginName);
                ps.setString(2, key);
                ps.setString(3, value.toString());
                return ps.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save global data", e);
            return false;
        }
    }

    @Override
    public Object getGlobalData(String pluginName, String key) {
        if (pluginName == null || key == null) {
            return null;
        }

        try (Connection conn = databaseManager.getConnection()) {
            String sql = "SELECT data_value FROM global_data WHERE plugin_name = ? AND data_key = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, pluginName);
                ps.setString(2, key);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? rs.getString("data_value") : null;
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to get global data", e);
            return null;
        }
    }
}