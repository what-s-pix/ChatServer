package dao;

import db.Conexion;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

public class AmistadDAO {

    // 1. Enviar solicitud de amistad
    public boolean enviarSolicitud(String remitente, String destinatario) {
        // Primero validamos que no sean ya amigos ni haya solicitud pendiente
        if (sonAmigos(remitente, destinatario) || existeSolicitudPendiente(remitente, destinatario)) {
            return false;
        }

        String sql = "INSERT INTO solicitudes (remitente, destinatario, estado) VALUES (?, ?, 0)";
        try (Connection con = Conexion.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, remitente);
            ps.setString(2, destinatario);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // 2. Aceptar solicitud (Crea la amistad y actualiza la solicitud)
    public boolean aceptarSolicitud(int idSolicitud) {
        String sqlGet = "SELECT remitente, destinatario FROM solicitudes WHERE pk_solicitud = ?";
        String sqlInsert = "INSERT INTO amigos (usuario_a, usuario_b) VALUES (?, ?)";
        String sqlUpdate = "UPDATE solicitudes SET estado = 1 WHERE pk_solicitud = ?";

        try (Connection con = Conexion.getConnection()) {
            // A. Obtener datos de la solicitud
            PreparedStatement psGet = con.prepareStatement(sqlGet);
            psGet.setInt(1, idSolicitud);
            ResultSet rs = psGet.executeQuery();

            if (rs.next()) {
                String u1 = rs.getString("remitente");
                String u2 = rs.getString("destinatario");

                // B. Crear amistad (doble registro no es necesario si buscamos bien, pero un registro único basta)
                PreparedStatement psInsert = con.prepareStatement(sqlInsert);
                psInsert.setString(1, u1);
                psInsert.setString(2, u2);
                psInsert.executeUpdate();

                // C. Marcar solicitud como aceptada
                PreparedStatement psUpdate = con.prepareStatement(sqlUpdate);
                psUpdate.setInt(1, idSolicitud);
                psUpdate.executeUpdate();
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // 3. Verificar si son amigos (CRÍTICO PARA EL HISTORIAL)
    public boolean sonAmigos(String user1, String user2) {
        String sql = "SELECT * FROM amigos WHERE (usuario_a = ? AND usuario_b = ?) OR (usuario_a = ? AND usuario_b = ?)";
        try (Connection con = Conexion.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, user1);
            ps.setString(2, user2);
            ps.setString(3, user2);
            ps.setString(4, user1);
            ResultSet rs = ps.executeQuery();
            return rs.next(); // Si devuelve true, son amigos
        } catch (SQLException e) {
            return false;
        }
    }

    // Auxiliar: Verificar solicitud pendiente
    private boolean existeSolicitudPendiente(String remitente, String destinatario) {
        String sql = "SELECT * FROM solicitudes WHERE remitente = ? AND destinatario = ? AND estado = 0";
        try (Connection con = Conexion.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            
            ps.setString(1, remitente);
            ps.setString(2, destinatario);
            
            // CORRECCIÓN: Primero ejecutamos la query para obtener el ResultSet
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next(); // Ahora sí, verificamos si hay resultados en el ResultSet
            }
            
        } catch (SQLException e) { 
            return false; 
        }
    }
    
    // 4. Listar Solicitudes Pendientes (Para que el usuario las vea)
    public ArrayList<String> getSolicitudesPendientes(String miUsuario) {
        ArrayList<String> lista = new ArrayList<>();
        String sql = "SELECT remitente, pk_solicitud FROM solicitudes WHERE destinatario = ? AND estado = 0";
        try (Connection con = Conexion.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, miUsuario);
            ResultSet rs = ps.executeQuery();
            while(rs.next()) {
                // Formato simple: "usuario_id" (luego lo parseamos)
                lista.add(rs.getString("remitente") + ":" + rs.getInt("pk_solicitud"));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return lista;
    }
    
    // 5. Obtener lista de amigos confirmados
    // Hace un JOIN con la tabla usuarios para obtener los datos completos (nombre, estado, etc.)
    public ArrayList<models.Usuario> obtenerAmigos(String miUser) {
        ArrayList<models.Usuario> lista = new ArrayList<>();
        
        // Esta consulta busca en la tabla 'amigos' donde yo sea 'usuario_a' O 'usuario_b'
        // y se trae los datos del OTRO usuario (u.*) cruzando con la tabla usuarios.
        String sql = "SELECT u.* FROM usuarios u " +
                     "JOIN amigos a ON (u.username = a.usuario_a OR u.username = a.usuario_b) " +
                     "WHERE (a.usuario_a = ? OR a.usuario_b = ?) " +
                     "AND u.username != ?"; // Excluirme a mí mismo
                     
        try (Connection con = Conexion.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            
            ps.setString(1, miUser);
            ps.setString(2, miUser);
            ps.setString(3, miUser); // El tercer ? es para el !=
            
            ResultSet rs = ps.executeQuery();
            
            while(rs.next()) {
                models.Usuario u = new models.Usuario();
                u.setPk_usuario(rs.getInt("pk_usuario"));
                u.setNombre(rs.getString("nombre"));
                u.setUsername(rs.getString("username"));
                u.setEstado(rs.getInt("estado")); // Importante para ver si está online
                // No traemos el password por seguridad
                lista.add(u);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return lista;
    }
}