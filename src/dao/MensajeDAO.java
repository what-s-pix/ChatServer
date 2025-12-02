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
        System.out.println("[MENSAJE_DAO] Guardando mensaje: remitente=" + m.getFk_remitente() + 
                          ", destinatario=" + m.getFk_destinatario() + ", mensaje=" + m.getMensaje());
        try (Connection con = Conexion.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, m.getFk_remitente());
            ps.setInt(2, m.getFk_destinatario());
            ps.setString(3, m.getMensaje());
            int filas = ps.executeUpdate();
            System.out.println("[MENSAJE_DAO] Mensaje guardado exitosamente. Filas afectadas: " + filas);
            return filas > 0;
        } catch (SQLException e) {
            System.err.println("[MENSAJE_DAO] ERROR guardando mensaje: " + e.getMessage());
            e.printStackTrace();
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
        }
        return mensajes;
    }
}
