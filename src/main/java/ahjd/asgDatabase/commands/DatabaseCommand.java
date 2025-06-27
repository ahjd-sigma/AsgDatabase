package ahjd.asgDatabase.commands;

import ahjd.asgDatabase.AsgDatabase;
import ahjd.asgDatabase.api.DatabaseAPI;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Command handler for database management commands.
 */
public class DatabaseCommand implements CommandExecutor, TabCompleter {

    private final AsgDatabase plugin;
    private final DatabaseAPI databaseAPI;

    public DatabaseCommand(AsgDatabase plugin) {
        this.plugin = plugin;
        this.databaseAPI = plugin.getDatabaseAPI();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("asgdatabase.admin")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "help":
                sendHelp(sender);
                break;
            case "info":
                sendInfo(sender);
                break;
            case "list":
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Usage: /asgdb list <plugin>");
                    return true;
                }
                listPlayerData(sender, args[1]);
                break;
            case "view":
                if (args.length < 3) {
                    sender.sendMessage(ChatColor.RED + "Usage: /asgdb view <plugin> <player>");
                    return true;
                }
                viewPlayerData(sender, args[1], args[2]);
                break;
            case "delete":
                if (args.length < 3) {
                    sender.sendMessage(ChatColor.RED + "Usage: /asgdb delete <plugin> <player>");
                    return true;
                }
                deletePlayerData(sender, args[1], args[2]);
                break;
            default:
                sender.sendMessage(ChatColor.RED + "Unknown command. Type /asgdb help for help.");
                break;
        }

        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GREEN + "=== AsgDatabase Commands ===");
        sender.sendMessage(ChatColor.YELLOW + "/asgdb help" + ChatColor.WHITE + " - Show this help message");
        sender.sendMessage(ChatColor.YELLOW + "/asgdb info" + ChatColor.WHITE + " - Show plugin information");
        sender.sendMessage(ChatColor.YELLOW + "/asgdb list <plugin>" + ChatColor.WHITE + " - List players with data for a plugin");
        sender.sendMessage(ChatColor.YELLOW + "/asgdb view <plugin> <player>" + ChatColor.WHITE + " - View a player's data for a plugin");
        sender.sendMessage(ChatColor.YELLOW + "/asgdb delete <plugin> <player>" + ChatColor.WHITE + " - Delete a player's data for a plugin");
    }

    private void sendInfo(CommandSender sender) {
        sender.sendMessage(ChatColor.GREEN + "=== AsgDatabase Info ===");
        sender.sendMessage(ChatColor.YELLOW + "Version: " + ChatColor.WHITE + plugin.getDescription().getVersion());
        sender.sendMessage(ChatColor.YELLOW + "Author: " + ChatColor.WHITE + String.join(", ", plugin.getDescription().getAuthors()));
        sender.sendMessage(ChatColor.YELLOW + "Database: " + ChatColor.WHITE + "SQLite");
        sender.sendMessage(ChatColor.YELLOW + "API: " + ChatColor.WHITE + "Available for other plugins");
    }

    private void listPlayerData(CommandSender sender, String pluginName) {
        List<String> playerUUIDs = plugin.getDatabaseManager().getPlayersWithDataForPlugin(pluginName);
        
        if (playerUUIDs.isEmpty()) {
            sender.sendMessage(ChatColor.RED + "No players found with data for plugin " + pluginName);
            return;
        }
        
        sender.sendMessage(ChatColor.GREEN + "=== Players with data for " + pluginName + " ===");
        for (String uuidStr : playerUUIDs) {
            try {
                UUID uuid = UUID.fromString(uuidStr);
                // Try to get the player name from the server
                String playerName = plugin.getServer().getOfflinePlayer(uuid).getName();
                if (playerName == null) {
                    playerName = uuidStr; // Fall back to UUID if name not found
                }
                sender.sendMessage(ChatColor.YELLOW + playerName);
            } catch (IllegalArgumentException e) {
                // Invalid UUID, just show the string
                sender.sendMessage(ChatColor.YELLOW + uuidStr);
            }
        }
    }

    private void viewPlayerData(CommandSender sender, String pluginName, String playerName) {
        // Try to get the player's UUID from the server
        Player player = plugin.getServer().getPlayer(playerName);
        UUID uuid = null;
        
        if (player != null) {
            uuid = player.getUniqueId();
        } else {
            // Try to find the offline player
            var offlinePlayer = plugin.getServer().getOfflinePlayer(playerName);
            if (offlinePlayer.hasPlayedBefore()) {
                uuid = offlinePlayer.getUniqueId();
            }
        }

        if (uuid == null) {
            sender.sendMessage(ChatColor.RED + "Player not found: " + playerName);
            return;
        }

        // Get the player's data
        var data = databaseAPI.getPlayerData(uuid, pluginName);
        if (data == null || data.isEmpty()) {
            sender.sendMessage(ChatColor.RED + "No data found for player " + playerName + " in plugin " + pluginName);
            return;
        }

        sender.sendMessage(ChatColor.GREEN + "=== Data for " + playerName + " in " + pluginName + " ===");
        for (var entry : data.entrySet()) {
            sender.sendMessage(ChatColor.YELLOW + entry.getKey() + ": " + ChatColor.WHITE + entry.getValue());
        }
    }

    private void deletePlayerData(CommandSender sender, String pluginName, String playerName) {
        // Try to get the player's UUID from the server
        Player player = plugin.getServer().getPlayer(playerName);
        UUID uuid = null;
        
        if (player != null) {
            uuid = player.getUniqueId();
        } else {
            // Try to find the offline player
            var offlinePlayer = plugin.getServer().getOfflinePlayer(playerName);
            if (offlinePlayer.hasPlayedBefore()) {
                uuid = offlinePlayer.getUniqueId();
            }
        }

        if (uuid == null) {
            sender.sendMessage(ChatColor.RED + "Player not found: " + playerName);
            return;
        }

        // Delete the player's data
        boolean success = databaseAPI.deletePlayerData(uuid, pluginName);
        if (success) {
            sender.sendMessage(ChatColor.GREEN + "Successfully deleted data for player " + playerName + " in plugin " + pluginName);
        } else {
            sender.sendMessage(ChatColor.RED + "Failed to delete data for player " + playerName + " in plugin " + pluginName);
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("asgdatabase.admin")) {
            return new ArrayList<>();
        }

        if (args.length == 1) {
            return Arrays.asList("help", "info", "list", "view", "delete").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2 && (args[0].equalsIgnoreCase("list") || args[0].equalsIgnoreCase("view") || args[0].equalsIgnoreCase("delete"))) {
            // Return a list of plugins that have data in the database
            return plugin.getDatabaseManager().getPluginsWithData().stream()
                    .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 3 && (args[0].equalsIgnoreCase("view") || args[0].equalsIgnoreCase("delete"))) {
            return plugin.getServer().getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[2].toLowerCase()))
                    .collect(Collectors.toList());
        }

        return new ArrayList<>();
    }
}