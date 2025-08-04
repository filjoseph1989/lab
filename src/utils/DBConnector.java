package utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnector {
    // kani nga mga properties mao ni ang credentials sa pag connect sa database
    private static final String URL = "jdbc:mysql://localhost:3306/comlab";
    private static final String USER = "fil";
    private static final String PASSWORD = "password";

    private static Connection connection = null;

    // Private constructor to prevent instantiation
    public DBConnector() {}

    // kani nga method mao ni ang connection sa database
    public static Connection getConnection() throws SQLException {
        // kung ang variable connection kay null so wala pa naka connect sa database
        // or kung na disconnect na siya sa database mahimong true ang condition diri
        if (connection == null || connection.isClosed()) {
            try { // kani try kay i-try niya run ang line pag connect, kung naa error mo-adto sa catch
                // diri mag connect sa database gamit ang username og password
                connection = DriverManager.getConnection(URL, USER, PASSWORD);
            } catch (SQLException e) { // so kung naa error sa pag connect sa database kani ang mo run
                e.printStackTrace(); // i print ang mga line nga nag trace sa error
                throw new SQLException("Error connecting to the database."); // mao ni message sa reason sa error
            }
        }
        return connection; // kani mao mani ang variable sa line 13, dili na ni null once naka connect na sa database
    }
}
