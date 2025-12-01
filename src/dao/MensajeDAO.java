package dao;

import db.Conexion;
import models.Mensaje;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

public class MensajeDAO {

    // 1. Guardar un mensaje en la BD
    public boolean guardar(Mensaje m) {
        String sql = "INSERT INTO mensajes (remitente, destinatario, contenido, leido) VALUES (?, ?, ?, 0)";
        try (Connection con = Conexion.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            
            ps.setString(1, m.getRemitente());
            ps.setString(2, m.getDestinatario());
            ps.setString(3, m.getContenido());
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("Error guardando mensaje: " + e.getMessage());
            return false;
        }
    }

    // 2. Obtener mensajes NO leídos de un usuario (cuando se conecta)
    public ArrayList<Mensaje> getMensajesPendientes(String usernameDestino) {
        ArrayList<Mensaje> lista = new ArrayList<>();
        String sql = "SELECT * FROM mensajes WHERE destinatario = ? AND leido = 0";
        
        try (Connection con = Conexion.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            
            ps.setString(1, usernameDestino);
            ResultSet rs = ps.executeQuery();
            
            while (rs.next()) {
                Mensaje m = new Mensaje(
                    rs.getInt("pk_mensaje"),
                    rs.getString("remitente"),
                    rs.getString("destinatario"),
                    rs.getString("contenido"),
                    rs.getTimestamp("fecha")
                );
                lista.add(m);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return lista;
    }
    
    // 3. Marcar mensajes como leídos (para que no se envíen dos veces)
    public void marcarLeidos(String usernameDestino) {
        String sql = "UPDATE mensajes SET leido = 1 WHERE destinatario = ?";
        try (Connection con = Conexion.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, usernameDestino);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    // 4. Obtener historial completo entre dos usuarios (ordenado por fecha)
    public ArrayList<Mensaje> obtenerHistorial(String user1, String user2) {
        ArrayList<Mensaje> historial = new ArrayList<>();
        
        // Buscamos mensajes donde (Yo envié a Él) O (Él me envió a Mí)
        String sql = "SELECT * FROM mensajes WHERE " +
                     "(remitente = ? AND destinatario = ?) OR " +
                     "(remitente = ? AND destinatario = ?) " +
                     "ORDER BY fecha ASC";
        
        try (Connection con = Conexion.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            
            ps.setString(1, user1); ps.setString(2, user2);
            ps.setString(3, user2); ps.setString(4, user1);
            
            ResultSet rs = ps.executeQuery();
            while(rs.next()) {
                Mensaje m = new Mensaje(
                    rs.getInt("pk_mensaje"),
                    rs.getString("remitente"),
                    rs.getString("destinatario"),
                    rs.getString("contenido"),
                    rs.getTimestamp("fecha")
                );
                historial.add(m);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return historial;
    }
}