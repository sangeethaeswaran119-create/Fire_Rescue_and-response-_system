import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Database configuration settings.
 *
 * NOTE FOR COLLEGE REVIEW:
 * Update the PASSWORD constant below with your local MySQL root password.
 * Your local MySQL service is currently running on port 3307.
 * If you run on a standard installation, switch port to 3306.
 */
public final class Config {

    // MySQL JDBC URL. Port 3307 matches your running MySQL80 service.
    // If your MySQL runs on default port, use: "jdbc:mysql://localhost:3306/smart_fire_rescue"
    public static final String URL = "jdbc:mysql://127.0.0.1:3307/smart_fire_rescue?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";

    // MySQL username
    public static final String USER = "root";

    // <<< ENTER YOUR MYSQL ROOT PASSWORD HERE >>>
    public static final String PASSWORD = "root";

    // Application web server port
    public static final int HTTP_PORT = 8080;

    private Config() {}

    /**
     * Attempts to open a JDBC connection.
     * Throws SQLException if connection fails.
     */
    public static Connection getConnection() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("Warning: MySQL JDBC Driver class not found: " + e.getMessage());
        }
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
