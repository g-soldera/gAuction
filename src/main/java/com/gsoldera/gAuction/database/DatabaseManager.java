package com.gsoldera.gAuction.database;

import java.sql.SQLException;

import org.slf4j.Logger;

import com.gsoldera.gAuction.GAuctionPlugin;
import com.gsoldera.gAuction.config.ConfigManager;
import com.gsoldera.gAuction.config.ConfigManager.DatabaseCredentials;

/**
 * Manages database connections and operations
 * Supports both MySQL and SQLite databases
 */
public class DatabaseManager {
    private final GAuctionPlugin plugin;
    private final Logger logger;
    private final ConfigManager configManager;
    private IDatabaseConnection databaseConnection;

    public DatabaseManager(GAuctionPlugin plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.logger = plugin.getPluginLogger();
        this.configManager = configManager;
    }

    /**
     * Initializes database connection and tables
     * @throws RuntimeException if database initialization fails
     */
    public void initialize() {
        try {
            DatabaseCredentials credentials = configManager.getDatabaseCredentials();
            
            databaseConnection = switch (configManager.getDatabaseType()) {
                case SQLITE -> new SQLiteDatabase(plugin, credentials.getSqliteFile());
                case MYSQL -> new MySQLDatabase(
                    plugin,
                    credentials.getHost(),
                    credentials.getPort(),
                    credentials.getDatabase(),
                    credentials.getUsername(),
                    credentials.getPassword()
                );
            };

            databaseConnection.initializeTables();
            logger.info("Database connection established successfully");
        } catch (SQLException e) {
            logger.error("Error initializing database", e);
            throw new RuntimeException("Failed to initialize database", e);
        }
    }

    /**
     * Closes database connection
     */
    public void shutdown() {
        if (databaseConnection != null) {
            try {
                databaseConnection.closeConnection();
                logger.info("Database connection closed");
            } catch (SQLException e) {
                logger.error("Error closing database connection", e);
            }
        }
    }

    /**
     * Gets the current database connection
     * @return The database connection interface
     * @throws IllegalStateException if database is not initialized
     */
    public IDatabaseConnection getDatabaseConnection() {
        if (databaseConnection == null) {
            throw new IllegalStateException("Database connection not initialized");
        }
        return databaseConnection;
    }
}
