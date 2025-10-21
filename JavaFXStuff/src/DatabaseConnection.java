import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;


/**
 * Stores database verification and connection information
 */
public class DatabaseConnection {
    /**
     * No-argument constructor required by JavaFX when instantiating the controller.
     * Providing an explicit constructor so the generated Javadoc includes a
     * documented constructor instead of a default undocumented one.
     */
    public DatabaseConnection() {}


    private static final String DB_URL = "jdbc:postgresql://csce-315-db.engr.tamu.edu/CSCE315Database";
    private static final String DB_USER = "compiler_errors";
    private static final String DB_PASSWORD = "root";
    
    private static Connection connection = null;
    
    /**
     * Connects to the database
     * @return Returns connection to database
     * @throws SQLException Thrown when driver not found
     */
    public static Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            try {
                Class.forName("org.postgresql.Driver");
                connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                System.out.println("Database connection established successfully.");
            } catch (ClassNotFoundException e) {
                System.err.println("PostgreSQL JDBC Driver not found.");
                e.printStackTrace();
                throw new SQLException("Driver not found", e);
            }
        }
        return connection;
    }
    
    /**
     * Closes connection to database
     */
    public static void closeConnection() {
        if (connection != null) {
            try {
                connection.close();
                System.out.println("Database connection closed.");
            } catch (SQLException e) {
                System.err.println("Error closing database connection.");
                e.printStackTrace();
            }
        }
    }
}