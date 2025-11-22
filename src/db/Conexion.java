
package db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Conexion {
    // Ajusta estos datos según tu configuración local
    private static final String URL = "jdbc:mysql://localhost:3307/what's pix?serverTimezone=UTC";
    private static final String USER = "root"; 
    private static final String PASSWORD = ""; // <--- Pon tu contraseña aquí

    // Variable estática para el patrón Singleton (una sola instancia)
    private static Connection instance;

    public static Connection getConnection() throws SQLException {
        if (instance == null || instance.isClosed()) {
            try {
                // Esto carga el driver de MySQL (asegúrate de añadir el .jar a Libraries)
                Class.forName("com.mysql.cj.jdbc.Driver"); 
                instance = DriverManager.getConnection(URL, USER, PASSWORD);
                System.out.println(">>> Conexión a BD exitosa.");
            } catch (ClassNotFoundException e) {
                System.err.println("ERROR: No se encontró el Driver de MySQL.");
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
                e.printStackTrace();
            }
        }
    }
}
