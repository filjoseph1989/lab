package utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnector {

    private static final String URL = "jdbc:mysql://localhost:3306/comlab";
    private static final String USER = "fil";
    private static final String PASSWORD = "password";

    private static Connection connection = null; // ✅ Missing field added

    // Private constructor to prevent instantiation
    private DBConnector() {}

    // Get the single connection instance
    public static Connection getConnection() throws SQLException {
        // Double-checked locking for thread safety and performance
        if (connection == null || connection.isClosed()) { // First check (not synchronized)
            synchronized (DBConnector.class) {
                if (connection == null || connection.isClosed()) { // Second check (synchronized)
                    try {
                        connection = DriverManager.getConnection(URL, USER, PASSWORD);
                    } catch (SQLException e) {
                        // It's better to log the exception and re-throw a more specific one
                        // or handle it appropriately rather than just printing the stack trace.
                        throw new SQLException("Failed to connect to the database.", e);
                    }
                }
            }
        }
        return connection;
    }
}
