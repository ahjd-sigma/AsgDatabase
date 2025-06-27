# AsgDatabase

A Spigot plugin that provides a centralized SQLite database for plugin-to-database communication, allowing plugins to store and retrieve data.

## Features

- SQLite database for persistent data storage
- API for other plugins to store and retrieve data
- Support for player-specific data (UUID-based)
- Support for global plugin data
- Simple and easy-to-use interface
- Pure database functionality with no player listeners

## Installation

1. Download the latest release from the releases page
2. Place the JAR file in your server's `plugins` folder
3. Restart your server

## API Usage

### Accessing the API

There are two ways to access the AsgDatabase API:

#### Method 1: Using Bukkit ServicesManager

```java
DatabaseAPI databaseAPI = getServer().getServicesManager().getRegistration(DatabaseAPI.class).getProvider();
```

#### Method 2: Direct Plugin Access

```java
AsgDatabase asgDatabase = (AsgDatabase) getServer().getPluginManager().getPlugin("AsgDatabase");
DatabaseAPI databaseAPI = asgDatabase.getDatabaseAPI();
```

### Saving Player Data

```java
UUID playerUUID = player.getUniqueId();
String yourPluginName = "YourPluginName";

Map<String, Object> data = new HashMap<>();
data.put("points", 100);
data.put("level", 5);
data.put("lastLogin", System.currentTimeMillis());

databaseAPI.savePlayerData(playerUUID, yourPluginName, data);
```

### Retrieving Player Data

```java
UUID playerUUID = player.getUniqueId();
String yourPluginName = "YourPluginName";

Map<String, Object> data = databaseAPI.getPlayerData(playerUUID, yourPluginName);
if (data != null) {
    int points = Integer.parseInt(data.get("points").toString());
    int level = Integer.parseInt(data.get("level").toString());
    long lastLogin = Long.parseLong(data.get("lastLogin").toString());
    
    // Use the data in your plugin
}
```

### Checking if Player Data Exists

```java
boolean hasData = databaseAPI.hasPlayerData(playerUUID, yourPluginName);
```

### Deleting Player Data

```java
databaseAPI.deletePlayerData(playerUUID, yourPluginName);
```

### Saving Global Plugin Data

```java
databaseAPI.saveGlobalData(yourPluginName, "serverName", "Awesome Server");
databaseAPI.saveGlobalData(yourPluginName, "maxPlayers", 100);
```

### Retrieving Global Plugin Data

```java
String serverName = (String) databaseAPI.getGlobalData(yourPluginName, "serverName");
int maxPlayers = Integer.parseInt(databaseAPI.getGlobalData(yourPluginName, "maxPlayers").toString());
```

## Example Plugin

Here's a simple example of a plugin that uses the AsgDatabase API:

```java
public class ExamplePlugin extends JavaPlugin {

    private DatabaseAPI databaseAPI;

    @Override
    public void onEnable() {
        // Get the API from the services manager
        databaseAPI = getServer().getServicesManager().getRegistration(DatabaseAPI.class).getProvider();
        
        if (databaseAPI == null) {
            getLogger().severe("AsgDatabase plugin not found! Disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        // Save some global plugin data
        databaseAPI.saveGlobalData(getName(), "enableTime", System.currentTimeMillis());
        
        getLogger().info("ExamplePlugin has been enabled!");
    }
    
    @Override
    public void onDisable() {
        // Save disable time
        if (databaseAPI != null) {
            databaseAPI.saveGlobalData(getName(), "disableTime", System.currentTimeMillis());
        }
    }
    
    // Example method to save player data when needed
    public void savePlayerStats(Player player, int points, int level) {
        UUID uuid = player.getUniqueId();
        
        Map<String, Object> data = new HashMap<>();
        data.put("points", points);
        data.put("level", level);
        data.put("lastUpdate", System.currentTimeMillis());
        
        databaseAPI.savePlayerData(uuid, getName(), data);
    }
    
    // Example method to get player data when needed
    public Map<String, Object> getPlayerStats(UUID uuid) {
        return databaseAPI.getPlayerData(uuid, getName());
    }
    
    public DatabaseAPI getDatabaseAPI() {
        return databaseAPI;
    }
}
```

## License

This project is licensed under the MIT License - see the LICENSE file for details.