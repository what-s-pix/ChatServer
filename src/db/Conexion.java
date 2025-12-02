package db;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
public class Conexion {
    private static final String URL = "jdbc:mysql://localhost:3306/what's pix?serverTimezone=UTC&autoReconnect=true&useSSL=false";
    private static final String USER = "root";
    private static final String PASSWORD = "";
    
    // Driver cargado una sola vez
    private static boolean driverLoaded = false;
    
    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            driverLoaded = true;
            System.out.println("[CONEXION] Driver MySQL cargado correctamente");
        } catch (ClassNotFoundException e) {
            System.err.println("[CONEXION] ERROR: No se pudo cargar el driver MySQL: " + e.getMessage());
        }
    }
    
    // Cada llamada retorna una nueva conexión para evitar problemas de concurrencia
    public static Connection getConnection() throws SQLException {
        if (!driverLoaded) {
            throw new SQLException("Driver MySQL no cargado");
        }
        Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
        System.out.println("[CONEXION] Nueva conexión creada");
        return conn;
    }
    
    // Método para cerrar una conexión de forma segura
    public static void cerrar(Connection conn) {
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                System.err.println("[CONEXION] Error cerrando conexión: " + e.getMessage());
            }
        }
    }
}
