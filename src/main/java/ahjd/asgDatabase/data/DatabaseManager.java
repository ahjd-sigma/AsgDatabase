package ahjd.asgDatabase.data;

import ahjd.asgDatabase.AsgDatabase;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class DatabaseManager {

    private final JavaPlugin plugin;
    private Connection connection;

    public DatabaseManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void setupDatabase() {
        try {
            File folder = plugin.getDataFolder();
            if (!folder.exists()) folder.mkdirs();

            // Get database filename from config
            String dbFilename = ((AsgDatabase) plugin).getConfigManager().getDatabaseFilename();
            File dbFile = new File(folder, dbFilename + ".db");
            
            // Set up connection with timeout from config
            int timeout = ((AsgDatabase) plugin).getConfigManager().getConnectionTimeout();
            connection = DriverManager.getConnection(
                "jdbc:sqlite:" + dbFile.getAbsolutePath() + ";busy_timeout=" + timeout
            );
            
            // Enable foreign keys
            try (Statement pragmaStatement = connection.createStatement()) {
                pragmaStatement.execute("PRAGMA foreign_keys = ON");
            }

            try (Statement statement = connection.createStatement()) {
                // Create plugin_data table for player-specific plugin data
                statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS plugin_data (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        uuid TEXT NOT NULL,
                        plugin_name TEXT NOT NULL,
                        data_key TEXT NOT NULL,
                        data_value TEXT,
                        UNIQUE(uuid, plugin_name, data_key)
                    );
                """);
                
                // Create global_data table for plugin-wide data
                statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS global_data (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        plugin_name TEXT NOT NULL,
                        data_key TEXT NOT NULL,
                        data_value TEXT,
                        UNIQUE(plugin_name, data_key)
                    );
                """);
                
                // Create indices for faster lookups
                statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_plugin_data_uuid ON plugin_data(uuid);");
                statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_plugin_data_plugin ON plugin_data(plugin_name);");
                statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_global_data_plugin ON global_data(plugin_name);");
            }

            plugin.getLogger().info("SQLite database initialized with all required tables.");
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to setup database:");
            e.printStackTrace();
        }
    }

    public Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                // Get database filename from config
                String dbFilename = ((AsgDatabase) plugin).getConfigManager().getDatabaseFilename();
                File dbFile = new File(plugin.getDataFolder(), dbFilename + ".db");
                
                // Set up connection with timeout from config
                int timeout = ((AsgDatabase) plugin).getConfigManager().getConnectionTimeout();
                connection = DriverManager.getConnection(
                    "jdbc:sqlite:" + dbFile.getAbsolutePath() + ";busy_timeout=" + timeout
                );
                
                // Enable foreign keys
                try (Statement pragmaStatement = connection.createStatement()) {
                    pragmaStatement.execute("PRAGMA foreign_keys = ON");
                }
                
                // Log if debug is enabled
                if (((AsgDatabase) plugin).getConfigManager().isDebugEnabled()) {
                    plugin.getLogger().info("Database connection reopened.");
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to reopen database:");
            e.printStackTrace();
        }
        return connection;
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                plugin.getLogger().info("SQLite database connection closed.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Gets a list of all plugins that have data in the database.
     * 
     * @return A list of plugin names
     */
    public List<String> getPluginsWithData() {
        List<String> plugins = new ArrayList<>();
        try (Connection conn = getConnection()) {
            // Get plugins from plugin_data table
            String sql = "SELECT DISTINCT plugin_name FROM plugin_data";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                while (rs.next()) {
                    plugins.add(rs.getString("plugin_name"));
                }
            }
            
            // Get plugins from global_data table
            sql = "SELECT DISTINCT plugin_name FROM global_data";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                while (rs.next()) {
                    String pluginName = rs.getString("plugin_name");
                    if (!plugins.contains(pluginName)) {
                        plugins.add(pluginName);
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to get plugins with data:");
            e.printStackTrace();
        }
        return plugins;
    }
    
    /**
     * Gets a list of all player UUIDs that have data for a specific plugin.
     * 
     * @param pluginName The name of the plugin
     * @return A list of player UUIDs
     */
    public List<String> getPlayersWithDataForPlugin(String pluginName) {
        List<String> players = new ArrayList<>();
        try (Connection conn = getConnection()) {
            String sql = "SELECT DISTINCT uuid FROM plugin_data WHERE plugin_name = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, pluginName);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        players.add(rs.getString("uuid"));
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to get players with data for plugin " + pluginName + ":");
            e.printStackTrace();
        }
        return players;
    }
}
