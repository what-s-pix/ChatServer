package db;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
public class Conexion {
    private static final String URL = "jdbc:mysql://localhost:3306/what's pix?serverTimezone=UTC";
    private static final String USER = "root";
    private static final String PASSWORD = "";
    private static Connection instance;
    public static Connection getConnection() throws SQLException {
        if (instance == null || instance.isClosed()) {
            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
                instance = DriverManager.getConnection(URL, USER, PASSWORD);
            } catch (ClassNotFoundException e) {
                throw new SQLException(e);
            }
        }
        return instance;
    }
    public static void cerrar() {
        if (instance != null) {
            try {
                instance.close();
            } catch (SQLException e) {
            }
        }
    }
}
