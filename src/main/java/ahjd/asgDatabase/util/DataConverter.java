package ahjd.asgDatabase.util;

import java.util.UUID;

/**
 * Utility class for converting between different data types.
 * This is useful for handling data stored in the database.
 */
public class DataConverter {

    /**
     * Converts a string to an integer.
     * 
     * @param value The string value to convert
     * @param defaultValue The default value to return if conversion fails
     * @return The converted integer or the default value if conversion fails
     */
    public static int toInt(String value, int defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Converts a string to a long.
     * 
     * @param value The string value to convert
     * @param defaultValue The default value to return if conversion fails
     * @return The converted long or the default value if conversion fails
     */
    public static long toLong(String value, long defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Converts a string to a double.
     * 
     * @param value The string value to convert
     * @param defaultValue The default value to return if conversion fails
     * @return The converted double or the default value if conversion fails
     */
    public static double toDouble(String value, double defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Converts a string to a boolean.
     * 
     * @param value The string value to convert
     * @param defaultValue The default value to return if conversion fails
     * @return The converted boolean or the default value if conversion fails
     */
    public static boolean toBoolean(String value, boolean defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        return "true".equalsIgnoreCase(value) || "1".equals(value) || "yes".equalsIgnoreCase(value);
    }

    /**
     * Converts a string to a UUID.
     * 
     * @param value The string value to convert
     * @return The converted UUID or null if conversion fails
     */
    public static UUID toUUID(String value) {
        if (value == null) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Converts an object to a string.
     * 
     * @param value The object to convert
     * @return The string representation of the object or null if the object is null
     */
    public static String toString(Object value) {
        return value == null ? null : value.toString();
    }
}