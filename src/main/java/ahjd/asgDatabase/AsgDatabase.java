package ahjd.asgDatabase;

import org.bukkit.plugin.java.JavaPlugin;

public class AsgDatabase extends JavaPlugin {

    private static AsgDatabase instance;
    private DatabaseConnection databaseConnection;
    private DatabaseAPI databaseAPI;

    @Override
    public void onEnable() {
        instance = this;

        // Initialize database connection
        databaseConnection = new DatabaseConnection(this);
        if (!databaseConnection.connect()) {
            getLogger().severe("Failed to connect to database! Disabling plugin...");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Initialize API
        databaseAPI = new DatabaseAPI(databaseConnection);

        getLogger().info("DatabaseManager has been enabled successfully!");
    }

    @Override
    public void onDisable() {
        if (databaseConnection != null) {
            databaseConnection.disconnect();
        }
        getLogger().info("DatabaseManager has been disabled!");
    }

    public static AsgDatabase getInstance() {
        return instance;
    }

    public DatabaseAPI getDatabaseAPI() {
        return databaseAPI;
    }

    public DatabaseConnection getDatabaseConnection() {
        return databaseConnection;
    }
}