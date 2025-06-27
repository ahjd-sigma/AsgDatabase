package ahjd.asgDatabase.example;

import ahjd.asgDatabase.api.DatabaseAPI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * This is an example of a hologram plugin that uses the AsgDatabase API.
 * It demonstrates how to store and retrieve hologram data from the database.
 * This example focuses purely on database communication without player listeners.
 */
public class HologramExample extends JavaPlugin {

    private DatabaseAPI databaseAPI;
    private Map<String, List<ArmorStand>> activeHolograms = new HashMap<>();

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
        getCommand("hologram").setExecutor(new HologramCommand(this));
        
        // Load all holograms from database
        loadAllHolograms();
        
        // Save plugin startup time as global data
        databaseAPI.saveGlobalData(getName(), "last_startup", System.currentTimeMillis());
        
        getLogger().info("HologramExample has been enabled!");
    }
    
    @Override
    public void onDisable() {
        // Remove all active holograms
        for (List<ArmorStand> stands : activeHolograms.values()) {
            for (ArmorStand stand : stands) {
                stand.remove();
            }
        }
        activeHolograms.clear();
        
        getLogger().info("HologramExample has been disabled!");
    }
    
    /**
     * Loads all holograms from the database.
     */
    private void loadAllHolograms() {
        // Get global data containing hologram count
        Object countObj = databaseAPI.getGlobalData(getName(), "hologram_count");
        if (countObj == null) {
            // No holograms exist yet
            return;
        }
        
        int count = Integer.parseInt(countObj.toString());
        for (int i = 1; i <= count; i++) {
            // Get hologram data
            Object holoDataObj = databaseAPI.getGlobalData(getName(), "hologram_" + i);
            if (holoDataObj != null) {
                String[] parts = holoDataObj.toString().split("\\|\\|");
                if (parts.length >= 5) {
                    String name = parts[0];
                    String worldName = parts[1];
                    double x = Double.parseDouble(parts[2]);
                    double y = Double.parseDouble(parts[3]);
                    double z = Double.parseDouble(parts[4]);
                    
                    // Get the lines for this hologram
                    Object linesObj = databaseAPI.getGlobalData(getName(), "hologram_" + i + "_lines");
                    if (linesObj != null) {
                        String[] lines = linesObj.toString().split("\\|\\|");
                        
                        // Create the hologram
                        World world = Bukkit.getWorld(worldName);
                        if (world != null) {
                            Location location = new Location(world, x, y, z);
                            createHologram(name, location, lines);
                            getLogger().info("Loaded hologram: " + name);
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Creates a hologram at the specified location with the given lines.
     * 
     * @param name The name of the hologram
     * @param location The location to create the hologram at
     * @param lines The lines of text to display
     * @return true if the hologram was created successfully, false otherwise
     */
    public boolean createHologram(String name, Location location, String[] lines) {
        // Remove existing hologram with the same name
        if (activeHolograms.containsKey(name)) {
            removeHologram(name);
        }
        
        List<ArmorStand> stands = new ArrayList<>();
        double currentY = location.getY();
        
        // Create an armor stand for each line
        for (String line : lines) {
            ArmorStand stand = (ArmorStand) location.getWorld().spawnEntity(new Location(
                    location.getWorld(), location.getX(), currentY, location.getZ()), EntityType.ARMOR_STAND);
            
            stand.setVisible(false);
            stand.setGravity(false);
            stand.setCustomName(ChatColor.translateAlternateColorCodes('&', line));
            stand.setCustomNameVisible(true);
            stand.setSmall(true);
            
            stands.add(stand);
            currentY -= 0.25; // Space between lines
        }
        
        activeHolograms.put(name, stands);
        
        // Save hologram to database
        saveHologram(name, location, lines);
        
        return true;
    }
    
    /**
     * Removes a hologram by name.
     * 
     * @param name The name of the hologram to remove
     * @return true if the hologram was removed, false if it doesn't exist
     */
    public boolean removeHologram(String name) {
        List<ArmorStand> stands = activeHolograms.get(name);
        if (stands == null) {
            return false;
        }
        
        // Remove all armor stands
        for (ArmorStand stand : stands) {
            stand.remove();
        }
        
        activeHolograms.remove(name);
        
        // Remove from database
        // First find the hologram ID
        Object countObj = databaseAPI.getGlobalData(getName(), "hologram_count");
        if (countObj != null) {
            int count = Integer.parseInt(countObj.toString());
            for (int i = 1; i <= count; i++) {
                Object holoDataObj = databaseAPI.getGlobalData(getName(), "hologram_" + i);
                if (holoDataObj != null) {
                    String[] parts = holoDataObj.toString().split("\\|\\|");
                    if (parts.length > 0 && parts[0].equals(name)) {
                        // Found the hologram, now delete its data
                        Map<String, Object> globalData = new HashMap<>();
                        globalData.put("hologram_" + i, null);
                        globalData.put("hologram_" + i + "_lines", null);
                        
                        // We're using a workaround since the API doesn't have a deleteGlobalData method
                        // Setting to null is our way of marking it as deleted
                        databaseAPI.saveGlobalData(getName(), "hologram_" + i, "DELETED");
                        databaseAPI.saveGlobalData(getName(), "hologram_" + i + "_lines", "DELETED");
                        return true;
                    }
                }
            }
        }
        
        return true;
    }
    
    /**
     * Saves a hologram to the database.
     * 
     * @param name The name of the hologram
     * @param location The location of the hologram
     * @param lines The lines of text in the hologram
     */
    private void saveHologram(String name, Location location, String[] lines) {
        // Get current hologram count
        int count = 1;
        Object countObj = databaseAPI.getGlobalData(getName(), "hologram_count");
        if (countObj != null) {
            count = Integer.parseInt(countObj.toString()) + 1;
        }
        
        // Save hologram location and metadata
        String locationData = name + "||"
                + location.getWorld().getName() + "||"
                + location.getX() + "||"
                + location.getY() + "||"
                + location.getZ();
        
        databaseAPI.saveGlobalData(getName(), "hologram_" + count, locationData);
        
        // Save hologram lines
        StringBuilder linesData = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            linesData.append(lines[i]);
            if (i < lines.length - 1) {
                linesData.append("||");
            }
        }
        
        databaseAPI.saveGlobalData(getName(), "hologram_" + count + "_lines", linesData.toString());
        
        // Update hologram count
        databaseAPI.saveGlobalData(getName(), "hologram_count", count);
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
     * Command handler for hologram commands.
     */
    public class HologramCommand implements CommandExecutor {
        
        private final HologramExample plugin;
        
        public HologramCommand(HologramExample plugin) {
            this.plugin = plugin;
        }
        
        @Override
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
                return true;
            }
            
            Player player = (Player) sender;
            
            if (args.length == 0) {
                // Show help
                player.sendMessage(ChatColor.GOLD + "=== Hologram Commands ===");
                player.sendMessage(ChatColor.YELLOW + "/hologram create <name> <text>" + ChatColor.WHITE + " - Create a hologram");
                player.sendMessage(ChatColor.YELLOW + "/hologram remove <name>" + ChatColor.WHITE + " - Remove a hologram");
                player.sendMessage(ChatColor.YELLOW + "/hologram list" + ChatColor.WHITE + " - List all holograms");
                return true;
            }
            
            switch (args[0].toLowerCase()) {
                case "create":
                    if (args.length < 3) {
                        player.sendMessage(ChatColor.RED + "Usage: /hologram create <name> <text>");
                        return true;
                    }
                    
                    String name = args[1];
                    StringBuilder text = new StringBuilder();
                    for (int i = 2; i < args.length; i++) {
                        text.append(args[i]);
                        if (i < args.length - 1) {
                            text.append(" ");
                        }
                    }
                    
                    // Split text by | for multiple lines
                    String[] lines = text.toString().split("\\|");
                    
                    // Create hologram at player's location
                    Location loc = player.getLocation().add(0, 2, 0); // 2 blocks above player
                    if (plugin.createHologram(name, loc, lines)) {
                        player.sendMessage(ChatColor.GREEN + "Hologram created: " + name);
                    } else {
                        player.sendMessage(ChatColor.RED + "Failed to create hologram.");
                    }
                    break;
                    
                case "remove":
                    if (args.length < 2) {
                        player.sendMessage(ChatColor.RED + "Usage: /hologram remove <name>");
                        return true;
                    }
                    
                    name = args[1];
                    if (plugin.removeHologram(name)) {
                        player.sendMessage(ChatColor.GREEN + "Hologram removed: " + name);
                    } else {
                        player.sendMessage(ChatColor.RED + "Hologram not found: " + name);
                    }
                    break;
                    
                case "list":
                    player.sendMessage(ChatColor.GOLD + "=== Holograms ===");
                    if (activeHolograms.isEmpty()) {
                        player.sendMessage(ChatColor.YELLOW + "No holograms exist.");
                    } else {
                        for (String holoName : activeHolograms.keySet()) {
                            player.sendMessage(ChatColor.YELLOW + "- " + holoName);
                        }
                    }
                    break;
                    
                default:
                    player.sendMessage(ChatColor.RED + "Unknown command. Type /hologram for help.");
                    break;
            }
            
            return true;
        }
    }
}