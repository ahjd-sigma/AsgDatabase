package ahjd.asgDatabase.example;

import ahjd.asgDatabase.api.DatabaseAPI;
import org.bukkit.entity.Player;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * This is an example plugin that demonstrates how to use the AsgDatabase API.
 * This class is not meant to be used directly, but rather as a reference for
 * other plugin developers.
 * 
 * This example shows how to use the database without player listeners,
 * focusing purely on database communication.
 */
public class ExamplePlugin extends JavaPlugin {

    private DatabaseAPI databaseAPI;

    @Override
    public void onEnable() {
        // Get the API from the services manager
        RegisteredServiceProvider<DatabaseAPI> provider = getServer().getServicesManager().getRegistration(DatabaseAPI.class);
        
        if (provider == null) {
            getLogger().severe("AsgDatabase plugin not found! Disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        databaseAPI = provider.getProvider();
        
        // Register commands instead of event listeners
        getCommand("points").setExecutor(new PointsCommand());
        
        // Save plugin startup time as global data
        Map<String, Object> globalData = new HashMap<>();
        globalData.put("startupTime", System.currentTimeMillis());
        databaseAPI.saveGlobalData(getName(), "pluginInfo", globalData);
        
        getLogger().info("ExamplePlugin has been enabled!");
    }
    
    @Override
    public void onDisable() {
        // Save plugin shutdown time as global data
        Map<String, Object> globalData = new HashMap<>();
        globalData.put("shutdownTime", System.currentTimeMillis());
        databaseAPI.saveGlobalData(getName(), "pluginInfo", globalData);
        
        getLogger().info("ExamplePlugin has been disabled!");
    }
    
    /**
     * Example method to add points to a player.
     * 
     * @param player The player to add points to
     * @param points The number of points to add
     */
    public void addPoints(Player player, int points) {
        UUID uuid = player.getUniqueId();
        
        // Get existing data
        Map<String, Object> data = databaseAPI.getPlayerData(uuid, getName());
        if (data == null) {
            data = new HashMap<>();
            data.put("points", points);
            data.put("level", 1);
            data.put("lastUpdate", System.currentTimeMillis());
        } else {
            // Update points
            int currentPoints = Integer.parseInt(data.get("points").toString());
            data.put("points", currentPoints + points);
            data.put("lastUpdate", System.currentTimeMillis());
            
            // Check if player should level up (example: level up every 100 points)
            int currentLevel = Integer.parseInt(data.get("level").toString());
            int newLevel = (currentPoints + points) / 100 + 1;
            
            if (newLevel > currentLevel) {
                data.put("level", newLevel);
                player.sendMessage("Congratulations! You've reached level " + newLevel + "!");
            }
        }
        
        // Save the updated data
        databaseAPI.savePlayerData(uuid, getName(), data);
        player.sendMessage("You've earned " + points + " points!");
    }
    
    /**
     * Example method to get a player's points.
     * 
     * @param player The player to get points for
     * @return The player's points, or 0 if the player has no data
     */
    public int getPoints(Player player) {
        UUID uuid = player.getUniqueId();
        
        // Get existing data
        Map<String, Object> data = databaseAPI.getPlayerData(uuid, getName());
        if (data == null || !data.containsKey("points")) {
            return 0;
        }
        
        return Integer.parseInt(data.get("points").toString());
    }
    
    /**
     * Example method to get a player's level.
     * 
     * @param player The player to get level for
     * @return The player's level, or 1 if the player has no data
     */
    public int getLevel(Player player) {
        UUID uuid = player.getUniqueId();
        
        // Get existing data
        Map<String, Object> data = databaseAPI.getPlayerData(uuid, getName());
        if (data == null || !data.containsKey("level")) {
            return 1;
        }
        
        return Integer.parseInt(data.get("level").toString());
    }
    
    /**
     * Command executor for the /points command
     */
    private class PointsCommand implements CommandExecutor {
        
        @Override
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("This command can only be used by players.");
                return true;
            }
            
            Player player = (Player) sender;
            
            if (args.length == 0) {
                // Display player's points and level
                int points = getPoints(player);
                int level = getLevel(player);
                player.sendMessage("You have " + points + " points and are level " + level);
                return true;
            }
            
            if (args.length == 2 && args[0].equalsIgnoreCase("add")) {
                try {
                    int amount = Integer.parseInt(args[1]);
                    addPoints(player, amount);
                    player.sendMessage("Added " + amount + " points to your account.");
                } catch (NumberFormatException e) {
                    player.sendMessage("Invalid amount. Please enter a number.");
                }
                return true;
            }
            
            return false;
        }
    }
}