package com.gsoldera.gAuction.database;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import org.slf4j.Logger;

import com.gsoldera.gAuction.GAuctionPlugin;

/**
 * SQLite implementation of database connection
 * Handles local file-based database operations
 */
public class SQLiteDatabase implements IDatabaseConnection {
    private final Logger logger;
    private final File databaseFile;
    private Connection connection;

    /**
     * Creates a new SQLite database connection
     * @param plugin Plugin instance for logging
     * @param databaseFile File where database will be stored
     */
    public SQLiteDatabase(GAuctionPlugin plugin, File databaseFile) {
        this.logger = plugin.getPluginLogger();
        this.databaseFile = databaseFile;

        // Ensure parent directory exists
        File parentDir = databaseFile.getParentFile();
        if (parentDir != null && !parentDir.exists() && !parentDir.mkdirs()) {
            throw new RuntimeException("Failed to create database directory: " + parentDir);
        }
    }

    @Override
    public Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            try {
                // Ensure SQLite JDBC driver is loaded
                Class.forName("org.sqlite.JDBC");
                connection = DriverManager.getConnection("jdbc:sqlite:" + databaseFile.getAbsolutePath());
                
                // Enable foreign keys
                try (Statement stmt = connection.createStatement()) {
                    stmt.execute("PRAGMA foreign_keys = ON");
                }
                
                // Set useful pragmas
                try (Statement stmt = connection.createStatement()) {
                    stmt.execute("PRAGMA journal_mode = WAL");  // Write-Ahead Logging
                    stmt.execute("PRAGMA synchronous = NORMAL"); // Better performance with reasonable safety
                    stmt.execute("PRAGMA temp_store = MEMORY"); // Store temp tables in memory
                }
            } catch (ClassNotFoundException e) {
                throw new SQLException("SQLite JDBC driver not found", e);
            }
        }
        return connection;
    }

    @Override
    public void closeConnection() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
            connection = null;
        }
    }

    @Override
    public void initializeTables() throws SQLException {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            // Create auction history table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS auction_history (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    item_serialized TEXT NOT NULL,
                    seller_uuid TEXT NOT NULL,
                    seller_name TEXT NOT NULL,
                    buyer_uuid TEXT,
                    buyer_name TEXT,
                    start_time INTEGER NOT NULL,
                    end_time INTEGER NOT NULL,
                    min_bid REAL NOT NULL,
                    final_bid REAL,
                    status TEXT NOT NULL,
                    created_at INTEGER NOT NULL DEFAULT (strftime('%s','now')),
                    updated_at INTEGER NOT NULL DEFAULT (strftime('%s','now'))
                )
            """);

            // Create auction queue table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS auction_queue (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    item_serialized TEXT NOT NULL,
                    seller_uuid TEXT NOT NULL,
                    seller_name TEXT NOT NULL,
                    min_bid REAL NOT NULL,
                    step_value REAL,
                    start_time INTEGER NOT NULL,
                    created_at INTEGER NOT NULL DEFAULT (strftime('%s','now'))
                )
            """);

            // Create indexes for better performance
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_history_seller ON auction_history(seller_uuid)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_history_buyer ON auction_history(buyer_uuid)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_history_status ON auction_history(status)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_queue_seller ON auction_queue(seller_uuid)");

            logger.info("SQLite tables initialized successfully");
        } catch (SQLException e) {
            logger.error("Error creating SQLite tables", e);
            throw e;
        }
    }
}
