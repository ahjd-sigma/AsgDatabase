package ahjd.asgDatabase;

import java.io.File;
import java.sql.*;
import java.util.logging.Level;
import org.bukkit.plugin.java.JavaPlugin;

public class DatabaseConnection {

    private final JavaPlugin plugin;
    private Connection connection;
    private final String databasePath;

    public DatabaseConnection(JavaPlugin plugin) {
        this.plugin = plugin;
        this.databasePath = plugin.getDataFolder().getAbsolutePath() + File.separator + "Database.db";
    }

    public boolean connect() {
        try {
            // Create plugin data folder if it doesn't exist
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdirs();
            }

            // Load SQLite JDBC driver
            Class.forName("org.sqlite.JDBC");

            // Create connection
            connection = DriverManager.getConnection("jdbc:sqlite:" + databasePath);

            // Enable foreign keys and other optimizations
            Statement stmt = connection.createStatement();
            stmt.execute("PRAGMA foreign_keys = ON");
            stmt.execute("PRAGMA journal_mode = WAL");
            stmt.execute("PRAGMA synchronous = NORMAL");
            stmt.execute("PRAGMA cache_size = 1000");
            stmt.execute("PRAGMA temp_store = MEMORY");
            stmt.close();

            // Create the universal table structure
            createUniversalTables();

            plugin.getLogger().info("Connected to SQLite database: " + databasePath);
            return true;

        } catch (ClassNotFoundException e) {
            plugin.getLogger().log(Level.SEVERE, "SQLite JDBC driver not found!", e);
            return false;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to connect to database!", e);
            return false;
        }
    }

    public void disconnect() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                plugin.getLogger().info("Database connection closed.");
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Error closing database connection", e);
        }
    }

    public Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                connect();
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to check connection status", e);
        }
        return connection;
    }

    private void createUniversalTables() throws SQLException {
        Statement stmt = connection.createStatement();

        // Universal data storage table - can store ANY type of data
        stmt.execute("""
            CREATE TABLE IF NOT EXISTS data_storage (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                data_type TEXT NOT NULL,
                identifier TEXT NOT NULL,
                data_key TEXT NOT NULL,
                data_value TEXT,
                value_type TEXT NOT NULL DEFAULT 'STRING',
                metadata TEXT,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                UNIQUE(data_type, identifier, data_key)
            )
        """);

        // Object collections table - for storing complex objects as JSON or serialized data
        stmt.execute("""
            CREATE TABLE IF NOT EXISTS object_storage (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                object_type TEXT NOT NULL,
                object_id TEXT NOT NULL,
                object_data TEXT NOT NULL,
                data_format TEXT NOT NULL DEFAULT 'JSON',
                version INTEGER DEFAULT 1,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                UNIQUE(object_type, object_id)
            )
        """);

        // Relationships table - for linking different data pieces together
        stmt.execute("""
            CREATE TABLE IF NOT EXISTS data_relationships (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                parent_type TEXT NOT NULL,
                parent_id TEXT NOT NULL,
                child_type TEXT NOT NULL,
                child_id TEXT NOT NULL,
                relationship_type TEXT NOT NULL DEFAULT 'OWNS',
                metadata TEXT,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                UNIQUE(parent_type, parent_id, child_type, child_id, relationship_type)
            )
        """);

        // Tags table - for flexible categorization and searching
        stmt.execute("""
            CREATE TABLE IF NOT EXISTS data_tags (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                target_type TEXT NOT NULL,
                target_id TEXT NOT NULL,
                tag_name TEXT NOT NULL,
                tag_value TEXT,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                UNIQUE(target_type, target_id, tag_name)
            )
        """);

        // Create indexes for optimal performance
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_data_type_identifier ON data_storage(data_type, identifier)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_data_key ON data_storage(data_key)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_object_type ON object_storage(object_type)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_object_id ON object_storage(object_id)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_parent_relation ON data_relationships(parent_type, parent_id)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_child_relation ON data_relationships(child_type, child_id)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_tags ON data_tags(target_type, target_id)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_tag_name ON data_tags(tag_name)");

        stmt.close();
        plugin.getLogger().info("Universal database tables created/verified successfully!");
    }

    public boolean isConnected() {
        try {
            return connection != null && !connection.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * Create a custom table dynamically if needed for specific use cases
     */
    public boolean createCustomTable(String tableName, String tableDefinition) {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS " + tableName + " (" + tableDefinition + ")");
            plugin.getLogger().info("Custom table '" + tableName + "' created successfully!");
            return true;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to create custom table: " + tableName, e);
            return false;
        }
    }
}