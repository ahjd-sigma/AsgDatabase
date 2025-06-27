package ahjd.asgDatabase.api;

import java.util.Map;
import java.util.UUID;

/**
 * Interface for other plugins to interact with the AsgDatabase plugin.
 * This API allows other plugins to save and retrieve data from the database.
 */
public interface DatabaseAPI {
    
    /**
     * Saves data for a player in the database.
     * 
     * @param uuid The UUID of the player
     * @param pluginName The name of the plugin saving the data
     * @param data A map containing the data to save (key-value pairs)
     * @return true if the data was saved successfully, false otherwise
     */
    boolean savePlayerData(UUID uuid, String pluginName, Map<String, Object> data);
    
    /**
     * Retrieves data for a player from the database.
     * 
     * @param uuid The UUID of the player
     * @param pluginName The name of the plugin retrieving the data
     * @return A map containing the retrieved data (key-value pairs), or null if no data exists
     */
    Map<String, Object> getPlayerData(UUID uuid, String pluginName);
    
    /**
     * Checks if data exists for a player from a specific plugin.
     * 
     * @param uuid The UUID of the player
     * @param pluginName The name of the plugin
     * @return true if data exists, false otherwise
     */
    boolean hasPlayerData(UUID uuid, String pluginName);
    
    /**
     * Deletes all data for a player from a specific plugin.
     * 
     * @param uuid The UUID of the player
     * @param pluginName The name of the plugin
     * @return true if the data was deleted successfully, false otherwise
     */
    boolean deletePlayerData(UUID uuid, String pluginName);
    
    /**
     * Saves global plugin data (not associated with a specific player).
     * 
     * @param pluginName The name of the plugin saving the data
     * @param key The key to store the data under
     * @param value The value to store
     * @return true if the data was saved successfully, false otherwise
     */
    boolean saveGlobalData(String pluginName, String key, Object value);
    
    /**
     * Retrieves global plugin data (not associated with a specific player).
     * 
     * @param pluginName The name of the plugin retrieving the data
     * @param key The key of the data to retrieve
     * @return The retrieved data, or null if no data exists
     */
    Object getGlobalData(String pluginName, String key);
}