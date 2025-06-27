package ahjd.asgDatabase.example;

import ahjd.asgDatabase.api.DatabaseAPI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * This is an example of a player stats plugin that uses the AsgDatabase API.
 * It demonstrates how to store and retrieve player statistics using the database.
 * This example focuses purely on database communication without player listeners.
 */
public class PlayerStatsExample extends JavaPlugin {

    private DatabaseAPI databaseAPI;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    
    /**
     * Updates a player's play time in the database.
     * 
     * @param uuid The UUID of the player
     * @param additionalTime Additional time to add in milliseconds
     */
    public void updatePlayTime(UUID uuid, long additionalTime) {
        if (databaseAPI.hasPlayerData(uuid, getName())) {
            Map<String, Object> data = databaseAPI.getPlayerData(uuid, getName());
            
            // Update play time
            long playTime = Long.parseLong(data.getOrDefault("playTime", "0").toString());
            data.put("playTime", playTime + additionalTime);
            data.put("lastUpdate", dateFormat.format(new Date()));
            
            databaseAPI.savePlayerData(uuid, getName(), data);
        }
    }

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
        
        // Register commands
        getCommand("stats").setExecutor(new StatsCommand(this));
        
        // Save plugin startup time as global data
        databaseAPI.saveGlobalData(getName(), "last_startup", System.currentTimeMillis());
        
        getLogger().info("PlayerStatsExample has been enabled!");
    }
    
    @Override
    public void onDisable() {
        // Save plugin shutdown time as global data
        databaseAPI.saveGlobalData(getName(), "last_shutdown", System.currentTimeMillis());
        
        getLogger().info("PlayerStatsExample has been disabled!");
    }
    
    /**
     * Initializes a new player's stats in the database.
     * This would be called manually when needed, not through event listeners.
     * 
     * @param uuid The UUID of the player
     * @param playerName The name of the player
     */
    public void initializePlayerStats(UUID uuid, String playerName) {
        if (!databaseAPI.hasPlayerData(uuid, getName())) {
            Map<String, Object> data = new HashMap<>();
            data.put("kills", 0);
            data.put("deaths", 0);
            data.put("mobKills", 0);
            data.put("damageDealt", 0.0);
            data.put("damageTaken", 0.0);
            data.put("firstJoin", dateFormat.format(new Date()));
            data.put("lastUpdate", dateFormat.format(new Date()));
            data.put("playTime", 0L);
            data.put("playerName", playerName);
            
            databaseAPI.savePlayerData(uuid, getName(), data);
        }
    }
    
    /**
     * Updates a player's kill count.
     * 
     * @param uuid The UUID of the player
     * @param playerKill Whether the kill was a player (true) or mob (false)
     */
    public void addKill(UUID uuid, boolean playerKill) {
        if (databaseAPI.hasPlayerData(uuid, getName())) {
            Map<String, Object> data = databaseAPI.getPlayerData(uuid, getName());
            
            if (playerKill) {
                // Update player kills
                int kills = Integer.parseInt(data.getOrDefault("kills", "0").toString());
                data.put("kills", kills + 1);
            } else {
                // Update mob kills
                int mobKills = Integer.parseInt(data.getOrDefault("mobKills", "0").toString());
                data.put("mobKills", mobKills + 1);
            }
            
            data.put("lastUpdate", dateFormat.format(new Date()));
            databaseAPI.savePlayerData(uuid, getName(), data);
        }
    }
    
    /**
     * Adds a death to a player's stats.
     * 
     * @param uuid The UUID of the player
     */
    public void addDeath(UUID uuid) {
        if (databaseAPI.hasPlayerData(uuid, getName())) {
            Map<String, Object> data = databaseAPI.getPlayerData(uuid, getName());
            
            // Update deaths
            int deaths = Integer.parseInt(data.getOrDefault("deaths", "0").toString());
            data.put("deaths", deaths + 1);
            data.put("lastUpdate", dateFormat.format(new Date()));
            
            databaseAPI.savePlayerData(uuid, getName(), data);
        }
    }
    
    /**
     * Updates a player's damage stats.
     * 
     * @param uuid The UUID of the player
     * @param damage The amount of damage
     * @param isDamageDealt Whether this is damage dealt (true) or taken (false)
     */
    public void updateDamage(UUID uuid, double damage, boolean isDamageDealt) {
        if (databaseAPI.hasPlayerData(uuid, getName())) {
            Map<String, Object> data = databaseAPI.getPlayerData(uuid, getName());
            
            if (isDamageDealt) {
                // Update damage dealt
                double damageDealt = Double.parseDouble(data.getOrDefault("damageDealt", "0.0").toString());
                data.put("damageDealt", damageDealt + damage);
            } else {
                // Update damage taken
                double damageTaken = Double.parseDouble(data.getOrDefault("damageTaken", "0.0").toString());
                data.put("damageTaken", damageTaken + damage);
            }
            
            data.put("lastUpdate", dateFormat.format(new Date()));
            databaseAPI.savePlayerData(uuid, getName(), data);
        }
    }
    
    /**
     * Gets the database API instance.
     * 
     * @return The database API instance
     */
    public DatabaseAPI getDatabaseAPI() {
        return databaseAPI;
    }
    
    /**
     * Formats a duration in milliseconds to a readable string.
     * 
     * @param millis The duration in milliseconds
     * @return A formatted string (e.g., "2h 30m 15s")
     */
    public String formatPlayTime(long millis) {
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        
        seconds %= 60;
        minutes %= 60;
        hours %= 24;
        
        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append("d ");
        if (hours > 0) sb.append(hours).append("h ");
        if (minutes > 0) sb.append(minutes).append("m ");
        sb.append(seconds).append("s");
        
        return sb.toString();
    }
    
    /**
     * Command handler for stats commands.
     */
    public class StatsCommand implements CommandExecutor {
        
        private final PlayerStatsExample plugin;
        
        public StatsCommand(PlayerStatsExample plugin) {
            this.plugin = plugin;
        }
        
        @Override
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            if (args.length == 0) {
                // Show own stats if player, or help if console
                if (sender instanceof Player) {
                    showStats((Player) sender, ((Player) sender).getUniqueId());
                } else {
                    sender.sendMessage(ChatColor.GOLD + "=== Stats Commands ===");
                    sender.sendMessage(ChatColor.YELLOW + "/stats" + ChatColor.WHITE + " - Show your stats");
                    sender.sendMessage(ChatColor.YELLOW + "/stats <player>" + ChatColor.WHITE + " - Show another player's stats");
                }
                return true;
            }
            
            // Look up another player's stats
            Player target = Bukkit.getPlayer(args[0]);
            if (target != null) {
                showStats(sender, target.getUniqueId());
            } else {
                // Try to find offline player
                sender.sendMessage(ChatColor.RED + "Player not found or not online: " + args[0]);
            }
            
            return true;
        }
        
        /**
         * Shows a player's stats to a command sender.
         * 
         * @param sender The command sender to show the stats to
         * @param uuid The UUID of the player whose stats to show
         */
        private void showStats(CommandSender sender, UUID uuid) {
            if (!plugin.getDatabaseAPI().hasPlayerData(uuid, plugin.getName())) {
                sender.sendMessage(ChatColor.RED + "No stats found for this player.");
                return;
            }
            
            Map<String, Object> data = plugin.getDatabaseAPI().getPlayerData(uuid, plugin.getName());
            
            // Get player name
            String playerName = "Unknown";
            if (sender instanceof Player && ((Player) sender).getUniqueId().equals(uuid)) {
                playerName = sender.getName();
            } else {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null) {
                    playerName = player.getName();
                }
            }
            
            // Display stats
            sender.sendMessage(ChatColor.GOLD + "=== Stats for " + playerName + " ===");
            
            // Combat stats
            int kills = Integer.parseInt(data.getOrDefault("kills", "0").toString());
            int deaths = Integer.parseInt(data.getOrDefault("deaths", "0").toString());
            int mobKills = Integer.parseInt(data.getOrDefault("mobKills", "0").toString());
            double damageDealt = Double.parseDouble(data.getOrDefault("damageDealt", "0.0").toString());
            double damageTaken = Double.parseDouble(data.getOrDefault("damageTaken", "0.0").toString());
            
            sender.sendMessage(ChatColor.YELLOW + "Kills: " + ChatColor.WHITE + kills);
            sender.sendMessage(ChatColor.YELLOW + "Deaths: " + ChatColor.WHITE + deaths);
            sender.sendMessage(ChatColor.YELLOW + "K/D Ratio: " + ChatColor.WHITE + 
                    (deaths > 0 ? String.format("%.2f", (double) kills / deaths) : kills));
            sender.sendMessage(ChatColor.YELLOW + "Mob Kills: " + ChatColor.WHITE + mobKills);
            sender.sendMessage(ChatColor.YELLOW + "Damage Dealt: " + ChatColor.WHITE + String.format("%.1f", damageDealt));
            sender.sendMessage(ChatColor.YELLOW + "Damage Taken: " + ChatColor.WHITE + String.format("%.1f", damageTaken));
            
            // Player info
            String firstJoin = data.getOrDefault("firstJoin", "Unknown").toString();
            String lastUpdate = data.getOrDefault("lastUpdate", "Unknown").toString();
            playerName = data.getOrDefault("playerName", "Unknown").toString();
            
            sender.sendMessage(ChatColor.YELLOW + "First Recorded: " + ChatColor.WHITE + firstJoin);
            sender.sendMessage(ChatColor.YELLOW + "Last Updated: " + ChatColor.WHITE + lastUpdate);
            
            if (Bukkit.getPlayer(uuid) != null) {
                sender.sendMessage(ChatColor.YELLOW + "Status: " + ChatColor.GREEN + "Online");
            } else {
                sender.sendMessage(ChatColor.YELLOW + "Status: " + ChatColor.RED + "Offline");
            }
            
            // Play time
            long playTime = Long.parseLong(data.getOrDefault("playTime", "0").toString());
            sender.sendMessage(ChatColor.YELLOW + "Total Play Time: " + ChatColor.WHITE + plugin.formatPlayTime(playTime));
        }
    }
}