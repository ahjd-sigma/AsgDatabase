# AsgDatabase

This project provides a robust and flexible database API for Bukkit/Spigot plugins, built on SQLite. It offers functionalities for universal data storage, object serialization, and a tagging system, along with convenience methods for common Minecraft-related data.

## API Usage

### Accessing the API

To use the `DatabaseAPI` in your plugin, you can access it via Bukkit's ServicesManager:

```java
import ahjd.asgDatabase.DatabaseAPI;
import org.bukkit.Bukkit;

// In your plugin's onEnable() method or where you need to access the API
DatabaseAPI databaseAPI = Bukkit.getServer().getServicesManager().getRegistration(DatabaseAPI.class).getProvider();

if (databaseAPI == null) {
    // Handle case where API is not available
    getLogger().severe("AsgDatabase API not found!");
    return;
}
```

### Universal Data Storage

The API allows you to store and retrieve various types of data using a key-value pair system, with automatic type detection and conversion.

**Storing Data:**

```java
// Store a simple value
databaseAPI.storeData("player_data", player.getUniqueId().toString(), "coins", 1000);

// Store data with metadata
databaseAPI.storeData("player_data", player.getUniqueId().toString(), "last_login", System.currentTimeMillis(), "timestamp");

// Store multiple data points in a batch
Map<String, Object> playerStats = new HashMap<>();
playerStats.put("kills", 50);
playerStats.put("deaths", 10);
databaseAPI.storeDataBatch("player_stats", player.getUniqueId().toString(), playerStats);
```

**Retrieving Data:**

```java
// Retrieve a specific data point with type conversion
Integer coins = databaseAPI.getData("player_data", player.getUniqueId().toString(), "coins", Integer.class);

// Retrieve all data for a specific type and identifier
Map<String, Object> stats = databaseAPI.getAllData("player_stats", player.getUniqueId().toString());
```

### Object Storage

Store and retrieve complex Java objects, which are automatically serialized to JSON.

**Storing Objects:**

```java
public class PlayerSettings {
    public boolean chatEnabled = true;
    public String theme = "dark";
}

PlayerSettings settings = new PlayerSettings();
databaseAPI.storeObject("player_settings", player.getUniqueId().toString(), settings);
```

**Retrieving Objects:**

```java
PlayerSettings retrievedSettings = databaseAPI.getObject("player_settings", player.getUniqueId().toString(), PlayerSettings.class);

// Get object as a generic Map for dynamic access
Map<String, Object> settingsMap = databaseAPI.getObjectAsMap("player_settings", player.getUniqueId().toString());
```

### Tagging System

Apply tags to any data for flexible categorization and searching.

**Adding Tags:**

```java
// Add a tag with a value
databaseAPI.addTag("item", "sword_of_power", "rarity", "legendary");

// Add a simple tag without a value
databaseAPI.addTag("player", player.getUniqueId().toString(), "admin");
```

**Retrieving and Finding by Tags:**

```java
// Get all tags for a target
Map<String, String> itemTags = databaseAPI.getTags("item", "sword_of_power");

// Find all targets with a specific tag
List<String> legendaryItems = databaseAPI.findByTag("item", "rarity", "legendary");
```

### Convenience Methods

The API includes specific methods for common Minecraft data types:

```java
// Store player data
databaseAPI.storePlayerData(player, "economy", Map.of("balance", 500.0));

// Store entity data (e.g., custom mobs)
databaseAPI.storeEntityData(entity, Map.of("health", 100, "level", 5));

// Store hologram data
List<String> lines = Arrays.asList("Hello World", "This is a hologram");
Map<String, Object> properties = Map.of("display_time", 60);
databaseAPI.storeHologramData("welcome_hologram", player.getLocation(), lines, properties);
```

### Deleting Data

```java
// Delete all data for a specific type and identifier
databaseAPI.deleteData("player_data", player.getUniqueId().toString());

// Delete a specific data key
databaseAPI.deleteDataKey("player_data", player.getUniqueId().toString(), "coins");
```

### Custom SQL Execution

For advanced use cases, you can execute raw SQL queries:

```java
import java.sql.ResultSet;
import java.sql.SQLException;

try {
    ResultSet rs = databaseAPI.executeQuery("SELECT * FROM data_storage WHERE data_type = ?", "player_data");
    while (rs.next()) {
        System.out.println(rs.getString("data_key") + ": " + rs.getString("data_value"));
    }
} catch (SQLException e) {
    e.printStackTrace();
}

try {
    int affectedRows = databaseAPI.executeUpdate("DELETE FROM data_storage WHERE data_type = ?", "old_data");
    System.out.println("Rows affected: " + affectedRows);
} catch (SQLException e) {
    e.printStackTrace();
}
```

## Installation

1. Add the plugin as a dependency to your project.
2. Ensure your `plugin.yml` includes a `depend` or `softdepend` entry for `AsgDatabase` if it's a separate plugin.

## Building from Source

To build the project, navigate to the root directory and run:

```bash
mvn clean install
```

This will generate a JAR file in the `target/` directory.