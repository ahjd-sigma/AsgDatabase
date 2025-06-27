package ahjd.asgDatabase;

import ahjd.asgDatabase.api.DatabaseAPI;
import ahjd.asgDatabase.api.DatabaseAPIImpl;
import ahjd.asgDatabase.commands.DatabaseCommand;
import ahjd.asgDatabase.config.ConfigManager;
import ahjd.asgDatabase.data.DatabaseManager;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;

public final class AsgDatabase extends JavaPlugin {

    private ConfigManager configManager;
    private DatabaseManager databaseManager;
    private DatabaseAPI databaseAPI;

    @Override
    public void onEnable() {
        // Load configuration
        configManager = new ConfigManager(this);
        
        // Initialize database
        databaseManager = new DatabaseManager(this);
        databaseManager.setupDatabase();
        
        // Initialize API
        databaseAPI = new DatabaseAPIImpl(databaseManager, this);
        
        // Register API as a service for other plugins to use if enabled in config
        if (configManager.isApiServiceEnabled()) {
            getServer().getServicesManager().register(
                DatabaseAPI.class,
                databaseAPI,
                this,
                configManager.getApiServicePriority()
            );
            getLogger().info("API service registered with priority: " + configManager.getApiServicePriority());
        }
        
        // Register commands
        DatabaseCommand databaseCommand = new DatabaseCommand(this);
        getCommand("asgdb").setExecutor(databaseCommand);
        getCommand("asgdb").setTabCompleter(databaseCommand);
        
        getLogger().info("AsgDatabase has been enabled. API is available for other plugins.");
    }

    @Override
    public void onDisable() {
        // Create a backup if enabled in config
        if (configManager.isBackupOnShutdownEnabled()) {
            createBackup();
        }
        
        // Close database connection
        databaseManager.close();
        
        getLogger().info("AsgDatabase has been disabled.");
    }
    
    /**
     * Creates a backup of the database file.
     */
    private void createBackup() {
        try {
            // Get the database file
            File dataFolder = getDataFolder();
            File dbFile = new File(dataFolder, configManager.getDatabaseFilename() + ".db");
            
            if (!dbFile.exists()) {
                getLogger().warning("Database file not found, skipping backup.");
                return;
            }
            
            // Create backups folder if it doesn't exist
            File backupsFolder = new File(dataFolder, "backups");
            if (!backupsFolder.exists()) {
                backupsFolder.mkdirs();
            }
            
            // Create backup file with timestamp
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
            String timestamp = dateFormat.format(new Date());
            File backupFile = new File(backupsFolder, configManager.getDatabaseFilename() + "_" + timestamp + ".db");
            
            // Copy database file to backup file
            Files.copy(dbFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            
            getLogger().info("Database backup created: " + backupFile.getName());
            
            // Clean up old backups if needed
            cleanupOldBackups(backupsFolder);
            
        } catch (IOException e) {
            getLogger().severe("Failed to create database backup:");
            e.printStackTrace();
        }
    }
    
    /**
     * Cleans up old backups if there are more than the configured maximum.
     * 
     * @param backupsFolder The folder containing the backups
     */
    private void cleanupOldBackups(File backupsFolder) {
        File[] backupFiles = backupsFolder.listFiles((dir, name) -> name.startsWith(configManager.getDatabaseFilename()) && name.endsWith(".db"));
        
        if (backupFiles == null || backupFiles.length <= configManager.getMaxBackups()) {
            return;
        }
        
        // Sort backups by last modified time (oldest first)
        Arrays.sort(backupFiles, Comparator.comparingLong(File::lastModified));
        
        // Delete oldest backups until we're at the maximum
        int numToDelete = backupFiles.length - configManager.getMaxBackups();
        for (int i = 0; i < numToDelete; i++) {
            if (backupFiles[i].delete()) {
                getLogger().info("Deleted old backup: " + backupFiles[i].getName());
            } else {
                getLogger().warning("Failed to delete old backup: " + backupFiles[i].getName());
            }
        }
    }

    /**
     * Gets the database API instance.
     * Other plugins can also access this API through the Bukkit ServicesManager.
     * 
     * @return The database API instance
     */
    public DatabaseAPI getDatabaseAPI() {
        return databaseAPI;
    }
    
    /**
     * Gets the database manager instance.
     * This is primarily for internal use by the plugin.
     * 
     * @return The database manager instance
     */
    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }
    
    /**
     * Gets the configuration manager instance.
     * 
     * @return The configuration manager instance
     */
    public ConfigManager getConfigManager() {
        return configManager;
    }
}
