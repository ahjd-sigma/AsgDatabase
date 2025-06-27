package ahjd.asgDatabase.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.ServicePriority;

/**
 * Manages the configuration for the AsgDatabase plugin.
 */
public class ConfigManager {

    private final JavaPlugin plugin;
    private FileConfiguration config;

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    /**
     * Loads the configuration from the config.yml file.
     */
    public void loadConfig() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        config = plugin.getConfig();
    }

    /**
     * Gets the database filename from the configuration.
     * 
     * @return The database filename
     */
    public String getDatabaseFilename() {
        return config.getString("database.filename", "players");
    }

    /**
     * Checks if database backup on shutdown is enabled.
     * 
     * @return true if backup on shutdown is enabled, false otherwise
     */
    public boolean isBackupOnShutdownEnabled() {
        return config.getBoolean("database.backup-on-shutdown", true);
    }

    /**
     * Gets the maximum number of backups to keep.
     * 
     * @return The maximum number of backups
     */
    public int getMaxBackups() {
        return config.getInt("database.max-backups", 5);
    }

    /**
     * Checks if debug logging is enabled.
     * 
     * @return true if debug logging is enabled, false otherwise
     */
    public boolean isDebugEnabled() {
        return config.getBoolean("debug.enabled", false);
    }

    /**
     * Checks if query logging is enabled.
     * 
     * @return true if query logging is enabled, false otherwise
     */
    public boolean isQueryLoggingEnabled() {
        return config.getBoolean("debug.log-queries", false);
    }

    /**
     * Gets the maximum number of database connections.
     * 
     * @return The maximum number of connections
     */
    public int getMaxConnections() {
        return config.getInt("performance.max-connections", 10);
    }

    /**
     * Gets the database connection timeout in milliseconds.
     * 
     * @return The connection timeout
     */
    public int getConnectionTimeout() {
        return config.getInt("performance.connection-timeout", 30000);
    }

    /**
     * Checks if connection pooling is enabled.
     * 
     * @return true if connection pooling is enabled, false otherwise
     */
    public boolean isConnectionPoolingEnabled() {
        return config.getBoolean("performance.use-connection-pool", true);
    }

    /**
     * Checks if the API should be registered as a service.
     * 
     * @return true if the API should be registered, false otherwise
     */
    public boolean isApiServiceEnabled() {
        return config.getBoolean("api.register-service", true);
    }

    /**
     * Gets the service priority for the API.
     * 
     * @return The service priority
     */
    public ServicePriority getApiServicePriority() {
        String priority = config.getString("api.service-priority", "Normal");
        try {
            return ServicePriority.valueOf(priority.toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid service priority: " + priority + ". Using Normal instead.");
            return ServicePriority.Normal;
        }
    }
}