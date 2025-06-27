package ahjd.asgDatabase;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.sql.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.lang.reflect.Type;

public class DatabaseAPI {

    private final DatabaseConnection dbConnection;
    private final Logger logger;
    private final Gson gson;

    public DatabaseAPI(DatabaseConnection dbConnection) {
        this.dbConnection = dbConnection;
        this.logger = Logger.getLogger("DatabaseAPI");
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .serializeNulls()
                .create();
    }

    // ==================== UNIVERSAL DATA STORAGE ====================

    /**
     * Store any data with automatic type detection
     */
    public boolean storeData(String dataType, String identifier, String key, Object value) {
        return storeData(dataType, identifier, key, value, null);
    }

    /**
     * Store data with metadata
     */
    public boolean storeData(String dataType, String identifier, String key, Object value, String metadata) {
        String sql = """
            INSERT OR REPLACE INTO data_storage (data_type, identifier, data_key, data_value, value_type, metadata, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
        """;

        try (PreparedStatement stmt = dbConnection.getConnection().prepareStatement(sql)) {
            stmt.setString(1, dataType);
            stmt.setString(2, identifier);
            stmt.setString(3, key);
            stmt.setString(4, serializeValue(value));
            stmt.setString(5, getValueType(value));
            stmt.setString(6, metadata);

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to store data", e);
            return false;
        }
    }

    /**
     * Store multiple data points at once (batch operation)
     */
    public boolean storeDataBatch(String dataType, String identifier, Map<String, Object> data) {
        return storeDataBatch(dataType, identifier, data, null);
    }

    /**
     * Store multiple data points with metadata
     */
    public boolean storeDataBatch(String dataType, String identifier, Map<String, Object> data, String metadata) {
        if (data == null || data.isEmpty()) return true;

        String sql = """
            INSERT OR REPLACE INTO data_storage (data_type, identifier, data_key, data_value, value_type, metadata, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
        """;

        Connection conn = dbConnection.getConnection();
        try {
            conn.setAutoCommit(false);

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                for (Map.Entry<String, Object> entry : data.entrySet()) {
                    stmt.setString(1, dataType);
                    stmt.setString(2, identifier);
                    stmt.setString(3, entry.getKey());
                    stmt.setString(4, serializeValue(entry.getValue()));
                    stmt.setString(5, getValueType(entry.getValue()));
                    stmt.setString(6, metadata);
                    stmt.addBatch();
                }
                stmt.executeBatch();
            }

            conn.commit();
            return true;

        } catch (SQLException e) {
            try {
                conn.rollback();
            } catch (SQLException rollbackEx) {
                logger.log(Level.SEVERE, "Failed to rollback batch operation", rollbackEx);
            }
            logger.log(Level.SEVERE, "Failed to store data batch", e);
            return false;
        } finally {
            try {
                conn.setAutoCommit(true);
            } catch (SQLException e) {
                logger.log(Level.WARNING, "Failed to reset auto-commit", e);
            }
        }
    }

    /**
     * Get data with automatic type conversion
     */
    public <T> T getData(String dataType, String identifier, String key, Class<T> expectedType) {
        String sql = "SELECT data_value, value_type FROM data_storage WHERE data_type = ? AND identifier = ? AND data_key = ?";

        try (PreparedStatement stmt = dbConnection.getConnection().prepareStatement(sql)) {
            stmt.setString(1, dataType);
            stmt.setString(2, identifier);
            stmt.setString(3, key);

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                String value = rs.getString("data_value");
                String valueType = rs.getString("value_type");
                return deserializeValue(value, valueType, expectedType);
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to retrieve data", e);
        }
        return null;
    }

    /**
     * Get data as string (backward compatibility)
     */
    public String getData(String dataType, String identifier, String key) {
        return getData(dataType, identifier, key, String.class);
    }

    /**
     * Get all data for a specific type and identifier
     */
    public Map<String, Object> getAllData(String dataType, String identifier) {
        Map<String, Object> data = new HashMap<>();
        String sql = "SELECT data_key, data_value, value_type FROM data_storage WHERE data_type = ? AND identifier = ?";

        try (PreparedStatement stmt = dbConnection.getConnection().prepareStatement(sql)) {
            stmt.setString(1, dataType);
            stmt.setString(2, identifier);

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                String key = rs.getString("data_key");
                String value = rs.getString("data_value");
                String valueType = rs.getString("value_type");
                data.put(key, deserializeValue(value, valueType, Object.class));
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to retrieve all data", e);
        }
        return data;
    }

    // ==================== OBJECT STORAGE ====================

    /**
     * Store any complex object as JSON
     */
    public boolean storeObject(String objectType, String objectId, Object object) {
        return storeObject(objectType, objectId, object, "JSON");
    }

    /**
     * Store object with custom format
     */
    public boolean storeObject(String objectType, String objectId, Object object, String format) {
        String sql = """
            INSERT OR REPLACE INTO object_storage (object_type, object_id, object_data, data_format, updated_at)
            VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP)
        """;

        try (PreparedStatement stmt = dbConnection.getConnection().prepareStatement(sql)) {
            stmt.setString(1, objectType);
            stmt.setString(2, objectId);

            String serializedData;
            if ("JSON".equals(format)) {
                serializedData = gson.toJson(object);
            } else {
                serializedData = object.toString();
            }

            stmt.setString(3, serializedData);
            stmt.setString(4, format);

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to store object", e);
            return false;
        }
    }

    /**
     * Get object with type safety
     */
    public <T> T getObject(String objectType, String objectId, Class<T> expectedType) {
        String sql = "SELECT object_data, data_format FROM object_storage WHERE object_type = ? AND object_id = ?";

        try (PreparedStatement stmt = dbConnection.getConnection().prepareStatement(sql)) {
            stmt.setString(1, objectType);
            stmt.setString(2, objectId);

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                String data = rs.getString("object_data");
                String format = rs.getString("data_format");

                if ("JSON".equals(format)) {
                    return gson.fromJson(data, expectedType);
                } else {
                    // For non-JSON formats, return as string and let caller handle conversion
                    return expectedType.cast(data);
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to retrieve object", e);
        }
        return null;
    }

    /**
     * Get object as Map (for dynamic access)
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getObjectAsMap(String objectType, String objectId) {
        Type mapType = new TypeToken<Map<String, Object>>(){}.getType();
        String sql = "SELECT object_data, data_format FROM object_storage WHERE object_type = ? AND object_id = ?";

        try (PreparedStatement stmt = dbConnection.getConnection().prepareStatement(sql)) {
            stmt.setString(1, objectType);
            stmt.setString(2, objectId);

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                String data = rs.getString("object_data");
                String format = rs.getString("data_format");

                if ("JSON".equals(format)) {
                    return gson.fromJson(data, mapType);
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to retrieve object as map", e);
        }
        return new HashMap<>();
    }

    /**
     * Get all objects of a specific type
     */
    public List<String> getObjectIds(String objectType) {
        List<String> ids = new ArrayList<>();
        String sql = "SELECT object_id FROM object_storage WHERE object_type = ?";

        try (PreparedStatement stmt = dbConnection.getConnection().prepareStatement(sql)) {
            stmt.setString(1, objectType);

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                ids.add(rs.getString("object_id"));
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to retrieve object IDs", e);
        }
        return ids;
    }

    // ==================== TAGGING SYSTEM ====================

    /**
     * Add a tag to any data
     */
    public boolean addTag(String targetType, String targetId, String tagName, String tagValue) {
        String sql = """
            INSERT OR REPLACE INTO data_tags (target_type, target_id, tag_name, tag_value)
            VALUES (?, ?, ?, ?)
        """;

        try (PreparedStatement stmt = dbConnection.getConnection().prepareStatement(sql)) {
            stmt.setString(1, targetType);
            stmt.setString(2, targetId);
            stmt.setString(3, tagName);
            stmt.setString(4, tagValue);

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to add tag", e);
            return false;
        }
    }

    /**
     * Add a simple tag without value
     */
    public boolean addTag(String targetType, String targetId, String tagName) {
        return addTag(targetType, targetId, tagName, null);
    }

    /**
     * Get all tags for a target
     */
    public Map<String, String> getTags(String targetType, String targetId) {
        Map<String, String> tags = new HashMap<>();
        String sql = "SELECT tag_name, tag_value FROM data_tags WHERE target_type = ? AND target_id = ?";

        try (PreparedStatement stmt = dbConnection.getConnection().prepareStatement(sql)) {
            stmt.setString(1, targetType);
            stmt.setString(2, targetId);

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                tags.put(rs.getString("tag_name"), rs.getString("tag_value"));
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to retrieve tags", e);
        }
        return tags;
    }

    /**
     * Find all targets with a specific tag
     */
    public List<String> findByTag(String targetType, String tagName, String tagValue) {
        List<String> targets = new ArrayList<>();
        String sql = "SELECT target_id FROM data_tags WHERE target_type = ? AND tag_name = ? AND tag_value = ?";

        try (PreparedStatement stmt = dbConnection.getConnection().prepareStatement(sql)) {
            stmt.setString(1, targetType);
            stmt.setString(2, tagName);
            stmt.setString(3, tagValue);

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                targets.add(rs.getString("target_id"));
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to find by tag", e);
        }
        return targets;
    }

    // ==================== CONVENIENCE METHODS ====================

    /**
     * Store player data dynamically
     */
    public boolean storePlayerData(Player player, String category, Map<String, Object> data) {
        return storeDataBatch("player_" + category, player.getUniqueId().toString(), data);
    }

    /**
     * Get player data dynamically
     */
    public Map<String, Object> getPlayerData(Player player, String category) {
        return getAllData("player_" + category, player.getUniqueId().toString());
    }

    /**
     * Store entity data dynamically
     */
    public boolean storeEntityData(Entity entity, Map<String, Object> data) {
        // Store basic location data
        Location loc = entity.getLocation();
        Map<String, Object> entityData = new HashMap<>(data);
        entityData.put("world", loc.getWorld().getName());
        entityData.put("x", loc.getX());
        entityData.put("y", loc.getY());
        entityData.put("z", loc.getZ());
        entityData.put("type", entity.getType().name());

        return storeDataBatch("entity", entity.getUniqueId().toString(), entityData);
    }

    /**
     * Store hologram data dynamically
     */
    public boolean storeHologramData(String hologramId, Location location, List<String> lines, Map<String, Object> properties) {
        Map<String, Object> hologramData = new HashMap<>();
        hologramData.put("world", location.getWorld().getName());
        hologramData.put("x", location.getX());
        hologramData.put("y", location.getY());
        hologramData.put("z", location.getZ());
        hologramData.put("lines", lines);

        if (properties != null) {
            hologramData.putAll(properties);
        }

        return storeDataBatch("hologram", hologramId, hologramData);
    }

    // ==================== UTILITY METHODS ====================

    /**
     * Delete all data for a specific type and identifier
     */
    public boolean deleteData(String dataType, String identifier) {
        String sql = "DELETE FROM data_storage WHERE data_type = ? AND identifier = ?";

        try (PreparedStatement stmt = dbConnection.getConnection().prepareStatement(sql)) {
            stmt.setString(1, dataType);
            stmt.setString(2, identifier);

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to delete data", e);
            return false;
        }
    }

    /**
     * Delete specific data key
     */
    public boolean deleteDataKey(String dataType, String identifier, String key) {
        String sql = "DELETE FROM data_storage WHERE data_type = ? AND identifier = ? AND data_key = ?";

        try (PreparedStatement stmt = dbConnection.getConnection().prepareStatement(sql)) {
            stmt.setString(1, dataType);
            stmt.setString(2, identifier);
            stmt.setString(3, key);

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to delete data key", e);
            return false;
        }
    }

    /**
     * Execute custom SQL query
     */
    public ResultSet executeQuery(String sql, Object... params) throws SQLException {
        PreparedStatement stmt = dbConnection.getConnection().prepareStatement(sql);

        for (int i = 0; i < params.length; i++) {
            stmt.setObject(i + 1, params[i]);
        }

        return stmt.executeQuery();
    }

    /**
     * Execute custom SQL update
     */
    public int executeUpdate(String sql, Object... params) throws SQLException {
        try (PreparedStatement stmt = dbConnection.getConnection().prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }

            return stmt.executeUpdate();
        }
    }

    // ==================== PRIVATE HELPER METHODS ====================

    private String serializeValue(Object value) {
        if (value == null) return null;

        if (value instanceof String || value instanceof Number || value instanceof Boolean) {
            return value.toString();
        } else if (value instanceof Collection || value instanceof Map || value.getClass().isArray()) {
            return gson.toJson(value);
        } else {
            return gson.toJson(value);
        }
    }

    private String getValueType(Object value) {
        if (value == null) return "NULL";
        if (value instanceof String) return "STRING";
        if (value instanceof Integer) return "INTEGER";
        if (value instanceof Double || value instanceof Float) return "DOUBLE";
        if (value instanceof Boolean) return "BOOLEAN";
        if (value instanceof Long) return "LONG";
        if (value instanceof Collection) return "LIST";
        if (value instanceof Map) return "MAP";
        return "OBJECT";
    }

    @SuppressWarnings("unchecked")
    private <T> T deserializeValue(String value, String valueType, Class<T> expectedType) {
        if (value == null) return null;

        try {
            switch (valueType) {
                case "STRING":
                    return expectedType.cast(value);
                case "INTEGER":
                    if (expectedType == Integer.class || expectedType == int.class) {
                        return expectedType.cast(Integer.parseInt(value));
                    }
                    break;
                case "DOUBLE":
                    if (expectedType == Double.class || expectedType == double.class) {
                        return expectedType.cast(Double.parseDouble(value));
                    }
                    if (expectedType == Float.class || expectedType == float.class) {
                        return expectedType.cast(Float.parseFloat(value));
                    }
                    break;
                case "BOOLEAN":
                    if (expectedType == Boolean.class || expectedType == boolean.class) {
                        return expectedType.cast(Boolean.parseBoolean(value));
                    }
                    break;
                case "LONG":
                    if (expectedType == Long.class || expectedType == long.class) {
                        return expectedType.cast(Long.parseLong(value));
                    }
                    break;
                case "LIST":
                case "MAP":
                case "OBJECT":
                    return gson.fromJson(value, expectedType);
            }

            // Fallback: try to deserialize as JSON if expected type is not primitive
            if (!expectedType.isPrimitive() && expectedType != String.class) {
                return gson.fromJson(value, expectedType);
            }

            return expectedType.cast(value);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to deserialize value: " + value + " to type: " + expectedType, e);
            return null;
        }
    }
}