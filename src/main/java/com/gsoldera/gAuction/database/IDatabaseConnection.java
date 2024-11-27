package com.gsoldera.gAuction.database;

import java.sql.Connection;
import java.sql.SQLException;

public interface IDatabaseConnection {
    Connection getConnection() throws SQLException;
    void closeConnection() throws SQLException;
    void initializeTables() throws SQLException;
}
