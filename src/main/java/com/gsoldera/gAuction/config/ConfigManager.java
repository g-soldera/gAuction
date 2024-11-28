package com.gsoldera.gAuction.config;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.configuration.file.FileConfiguration;
import org.slf4j.Logger;

import com.gsoldera.gAuction.GAuctionPlugin;

/**
 * Manages plugin configuration including database settings, auction parameters,
 * and customizable messages
 */
public class ConfigManager {
    private final GAuctionPlugin plugin;
    private final Logger logger;
    private FileConfiguration config;
    
    // Auction settings
    private int auctionDuration;
    private int maxQueueSize;
    private boolean stepEnabled;
    private double stepPercentage;
    private double publicationFee;
    private double bidFee;
    private List<String> bannedItems;

    // Database settings
    private DatabaseType databaseType;
    private DatabaseCredentials databaseCredentials;

    // Language settings
    private String language;

    /**
     * Represents supported database types
     */
    public enum DatabaseType {
        MYSQL,
        SQLITE
    }

    /**
     * Holds database connection credentials
     */
    public static class DatabaseCredentials {
        private final String host;
        private final int port;
        private final String database;
        private final String username;
        private final String password;
        private final File sqliteFile;

        private DatabaseCredentials(String host, int port, String database, String username, String password, File sqliteFile) {
            this.host = host;
            this.port = port;
            this.database = database;
            this.username = username;
            this.password = password;
            this.sqliteFile = sqliteFile;
        }

        public String getHost() { return host; }
        public int getPort() { return port; }
        public String getDatabase() { return database; }
        public String getUsername() { return username; }
        public String getPassword() { return password; }
        public File getSqliteFile() { return sqliteFile; }

        /**
         * Creates MySQL credentials
         */
        public static DatabaseCredentials forMySQL(String host, int port, String database, String username, String password) {
            return new DatabaseCredentials(host, port, database, username, password, null);
        }

        /**
         * Creates SQLite credentials
         */
        public static DatabaseCredentials forSQLite(File sqliteFile) {
            return new DatabaseCredentials(null, 0, null, null, null, sqliteFile);
        }
    }

    public ConfigManager(GAuctionPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getPluginLogger();
    }

    /**
     * Reloads configuration from disk
     */
    public void reloadConfig() {
        plugin.reloadConfig();
        this.config = plugin.getConfig();
        loadConfig();
    }

    /**
     * Loads or reloads all configuration settings from config.yml
     */
    public void loadConfig() {
        try {
            // Save default config if it doesn't exist
            plugin.saveDefaultConfig();
            // Reload the config from disk
            plugin.reloadConfig();
            // Get the new config instance
            this.config = plugin.getConfig();
            
            loadAuctionSettings();
            loadLanguageSettings();
            loadDatabaseSettings();
            getBannedItems();
            
            logger.info("Configuration loaded successfully");
        } catch (Exception e) {
            logger.error("Error loading configuration", e);
            throw e;
        }
    }

    private void loadAuctionSettings() {
        if (config == null) {
            throw new IllegalStateException("Configuration not loaded");
        }
        auctionDuration = config.getInt("auction.duration", 300);
        maxQueueSize = config.getInt("auction.max_queue_size", 10);
        stepEnabled = config.getBoolean("auction.step.enabled", true);
        stepPercentage = config.getDouble("auction.step.percentage", 10.0);
        publicationFee = config.getDouble("auction.fees.publication", 0.0);
        bidFee = config.getDouble("auction.fees.bid", 0.0);
        bannedItems = new ArrayList<>(config.getStringList("auction.banned_items"));
    }

    /**
     * Loads language settings from config
     */
    private void loadLanguageSettings() {
        language = config.getString("language", "en-US");
        
        // Validate language
        if (!language.equals("en-US") && !language.equals("pt-BR")) {
            logger.warn("Invalid language '{}' specified, defaulting to en-US", language);
            language = "en-US";
            config.set("language", language);
        }
    }

    /**
     * Loads database-related settings
     */
    private void loadDatabaseSettings() {
        String dbTypeStr = config.getString("database.type");
        if (dbTypeStr == null || dbTypeStr.isEmpty()) {
            logger.warn("No database type specified, defaulting to SQLITE");
            databaseType = DatabaseType.SQLITE;
            return;
        }

        try {
            databaseType = DatabaseType.valueOf(dbTypeStr.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid database type '{}', defaulting to SQLITE", dbTypeStr);
            databaseType = DatabaseType.SQLITE;
        }

        if (databaseType == DatabaseType.MYSQL) {
            String host = config.getString("database.host", "localhost");
            int port = config.getInt("database.port", 3306);
            String database = config.getString("database.name", "gauction");
            String username = config.getString("database.user", "root");
            String password = config.getString("database.password", "");

            if (host == null || host.isEmpty()) {
                logger.warn("No MySQL host specified, defaulting to localhost");
                host = "localhost";
            }

            if (database == null || database.isEmpty()) {
                logger.warn("No MySQL database name specified, defaulting to gauction");
                database = "gauction";
            }

            if (username == null || username.isEmpty()) {
                logger.warn("No MySQL username specified, defaulting to root");
                username = "root";
            }

            databaseCredentials = DatabaseCredentials.forMySQL(host, port, database, username, password);
        } else {
            File dataFolder = plugin.getDataFolder();
            if (!dataFolder.exists() && !dataFolder.mkdirs()) {
                throw new RuntimeException("Could not create data folder");
            }

            File sqliteFile = new File(dataFolder, "auctions.db");
            databaseCredentials = DatabaseCredentials.forSQLite(sqliteFile);
        }
    }
        /**
     * Saves current configuration to file
     */
    public void saveConfig() {
        try {
            config.set("auction.duration", auctionDuration);
            config.set("auction.max_queue_size", maxQueueSize);
            config.set("auction.step.enabled", stepEnabled);
            config.set("auction.step.percentage", stepPercentage);
            config.set("auction.fees.publication", publicationFee);
            config.set("auction.fees.bid", bidFee);
            config.set("auction.banned_items", bannedItems);
            
            if (databaseType == DatabaseType.MYSQL) {
                config.set("database.type", "MYSQL");
                config.set("database.host", databaseCredentials.getHost());
                config.set("database.port", databaseCredentials.getPort());
                config.set("database.name", databaseCredentials.getDatabase());
                config.set("database.user", databaseCredentials.getUsername());
                config.set("database.password", databaseCredentials.getPassword());
            } else {
                config.set("database.type", "SQLITE");
            }
            
            plugin.saveConfig();
        } catch (Exception e) {
            logger.error("Error saving configuration", e);
        }
    }

    /**
     * Adds an item to the banned items list
     * @param itemId The item identifier to ban
     */
    public void addBannedItem(String itemId) {
        if (itemId != null && !bannedItems.contains(itemId)) {
            bannedItems.add(itemId);
            config.set("auction.banned_items", bannedItems);
            plugin.saveConfig();
            logger.info("Added {} to banned items list", itemId);
        }
    }

    // Getters
    public int getAuctionDuration() { return auctionDuration; }
    public int getMaxQueueSize() { return maxQueueSize; }
    public boolean isStepEnabled() { return config.getBoolean("auction.step.enabled", true); }
    public double getStepPercentage() { return config.getDouble("auction.step.percentage", 10.0); }
    public double getPublicationFee() { return publicationFee; }
    public double getBidFee() { return bidFee; }
    public List<String> getBannedItems() { return bannedItems; }
    public DatabaseType getDatabaseType() { return databaseType; }
    public DatabaseCredentials getDatabaseCredentials() { return databaseCredentials; }
    public String getLanguage() {
        return language;
    }

    // Setters
    public void setAuctionDuration(int duration) { this.auctionDuration = duration; }
    public void setMaxQueueSize(int size) { this.maxQueueSize = size; }
    public void setStepEnabled(boolean enabled) { this.stepEnabled = enabled; }
    public void setStepPercentage(double percentage) { this.stepPercentage = percentage; }
    public void setPublicationFee(double fee) { this.publicationFee = fee; }
    public void setBidFee(double fee) { this.bidFee = fee; }
    public void setBannedItems(List<String> items) { this.bannedItems = items; }
    public void setLanguage(String language) {
        this.language = language;
        config.set("language", language);
        saveConfig();
    }
}
