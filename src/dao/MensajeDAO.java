package dao;
import db.Conexion;
import models.Mensaje;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
public class MensajeDAO {
    public boolean guardarMensaje(Mensaje m) {
        String sql = "INSERT INTO mensajes_privados (fk_remitente, fk_destinatario, mensaje) VALUES (?, ?, ?)";
        try (Connection con = Conexion.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, m.getFk_remitente());
            ps.setInt(2, m.getFk_destinatario());
            ps.setString(3, m.getMensaje());
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("Error al guardar mensaje: " + e.getMessage());
            return false;
        }
    }
    public List<Mensaje> obtenerHistorial(int usuario1, int usuario2) {
        List<Mensaje> mensajes = new ArrayList<>();
        String sql = "SELECT m.*, u.nombre as nombre_remitente " +
                     "FROM mensajes_privados m " +
                     "JOIN usuarios u ON m.fk_remitente = u.pk_usuario " +
                     "WHERE (m.fk_remitente = ? AND m.fk_destinatario = ?) " +
                     "   OR (m.fk_remitente = ? AND m.fk_destinatario = ?) " +
                     "ORDER BY m.fecha_envio ASC";
        try (Connection con = Conexion.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, usuario1);
            ps.setInt(2, usuario2);
            ps.setInt(3, usuario2);
            ps.setInt(4, usuario1);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Mensaje m = new Mensaje();
                m.setPk_mensaje(rs.getInt("pk_mensaje"));
                m.setFk_remitente(rs.getInt("fk_remitente"));
                m.setFk_destinatario(rs.getInt("fk_destinatario"));
                m.setMensaje(rs.getString("mensaje"));
                m.setFecha_envio(rs.getTimestamp("fecha_envio"));
                m.setLeido(rs.getBoolean("leido"));
                m.setNombreRemitente(rs.getString("nombre_remitente"));
                mensajes.add(m);
            }
        } catch (SQLException e) {
            System.err.println("Error al obtener historial: " + e.getMessage());
        }
        return mensajes;
    }
}
