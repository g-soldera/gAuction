package com.gsoldera.gAuction.database;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import org.slf4j.Logger;

import com.gsoldera.gAuction.GAuctionPlugin;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

/**
 * MySQL implementation of database connection
 * Uses HikariCP for connection pooling
 */
public final class MySQLDatabase implements IDatabaseConnection {
    private final Logger logger;
    private HikariDataSource dataSource;

    /**
     * Creates a new MySQL database connection pool
     * @param plugin Plugin instance for logging
     * @param host Database host address
     * @param port Database port
     * @param database Database name
     * @param username Database username
     * @param password Database password
     */
    public MySQLDatabase(GAuctionPlugin plugin, String host, int port,
                        String database, String username, String password) {
        this.logger = plugin.getPluginLogger();

        HikariConfig config = new HikariConfig();
        config.setPoolName("GAuction-MySQL");
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        config.setJdbcUrl(String.format("jdbc:mysql://%s:%d/%s?useSSL=false&serverTimezone=UTC", 
            host, port, database));
        config.setUsername(username);
        config.setPassword(password);

        // Connection pool settings
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(5);
        config.setConnectionTimeout(30000); // 30 seconds
        config.setIdleTimeout(600000); // 10 minutes
        config.setMaxLifetime(1800000); // 30 minutes
        config.setKeepaliveTime(0); // Disable keepalive
        config.setAutoCommit(true);

        // MySQL specific optimizations
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");
        config.addDataSourceProperty("useLocalSessionState", "true");
        config.addDataSourceProperty("rewriteBatchedStatements", "true");
        config.addDataSourceProperty("cacheResultSetMetadata", "true");
        config.addDataSourceProperty("cacheServerConfiguration", "true");
        config.addDataSourceProperty("elideSetAutoCommits", "true");
        config.addDataSourceProperty("maintainTimeStats", "false");
        config.addDataSourceProperty("useUnicode", "true");
        config.addDataSourceProperty("characterEncoding", "utf8mb4");

        try {
            dataSource = new HikariDataSource(config);
            testConnection(); // Validate connection on startup
        } catch (Exception e) {
            logger.error("Failed to initialize MySQL connection pool", e);
            throw new RuntimeException("Could not initialize database connection", e);
        }
    }

    @Override
    public Connection getConnection() throws SQLException {
        if (dataSource == null || dataSource.isClosed()) {
            throw new SQLException("Connection pool is not initialized or has been closed");
        }
        return dataSource.getConnection();
    }

    @Override
    public void closeConnection() throws SQLException {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    @Override
    public void initializeTables() throws SQLException {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            // Create auction history table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS auction_history (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    item_serialized TEXT NOT NULL,
                    seller_uuid VARCHAR(36) NOT NULL,
                    seller_name VARCHAR(16) NOT NULL,
                    buyer_uuid VARCHAR(36),
                    buyer_name VARCHAR(16),
                    start_time BIGINT NOT NULL,
                    end_time BIGINT NOT NULL,
                    min_bid DOUBLE NOT NULL,
                    final_bid DOUBLE,
                    status ENUM('ACTIVE', 'SOLD', 'EXPIRED', 'CANCELLED', 'COLLECTED') NOT NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    INDEX idx_seller_uuid (seller_uuid),
                    INDEX idx_buyer_uuid (buyer_uuid),
                    INDEX idx_status_end_time (status, end_time)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """);

            // Create auction queue table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS auction_queue (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    item_serialized TEXT NOT NULL,
                    seller_uuid VARCHAR(36) NOT NULL,
                    seller_name VARCHAR(16) NOT NULL,
                    min_bid DOUBLE NOT NULL,
                    step_value DOUBLE,
                    start_time BIGINT NOT NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    INDEX idx_seller_uuid (seller_uuid),
                    INDEX idx_start_time (start_time)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """);

            // Create banned items table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS banned_items (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    item_identifier VARCHAR(255) NOT NULL,
                    banned_by VARCHAR(36),
                    ban_reason TEXT,
                    ban_date BIGINT NOT NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    UNIQUE KEY uk_item_identifier (item_identifier)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """);

            logger.info("MySQL tables initialized successfully");
        } catch (SQLException e) {
            logger.error("Error creating MySQL tables", e);
            throw e;
        }
    }

    /**
     * Tests if the database connection is working
     * @return true if connection is valid, false otherwise
     */
    public boolean testConnection() {
        try (Connection conn = getConnection()) {
            if (!conn.isValid(5)) {
                throw new SQLException("Connection validation failed");
            }
            return true;
        } catch (SQLException e) {
            logger.error("MySQL connection test failed", e);
            return false;
        }
    }
}
