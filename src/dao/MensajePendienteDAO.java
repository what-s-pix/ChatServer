package dao;
import db.Conexion;
import models.MensajePendiente;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
public class MensajePendienteDAO {
    public boolean guardarMensajePendiente(MensajePendiente mp) {
        String sql = "INSERT INTO mensajes_pendientes (fk_usuario, fk_grupo, fk_remitente, tipo, mensaje) VALUES (?, ?, ?, ?, ?)";
        try (Connection con = Conexion.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, mp.getFk_usuario());
            if (mp.getFk_grupo() != null) {
                ps.setInt(2, mp.getFk_grupo());
            } else {
                ps.setNull(2, java.sql.Types.INTEGER);
            }
            ps.setInt(3, mp.getFk_remitente());
            ps.setString(4, mp.getTipo());
            ps.setString(5, mp.getMensaje());
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            return false;
        }
    }
    public List<MensajePendiente> obtenerMensajesPendientes(int usuarioId) {
        List<MensajePendiente> mensajes = new ArrayList<>();
        String sql = "SELECT mp.*, u.nombre as nombre_remitente, g.titulo as titulo_grupo " +
                     "FROM mensajes_pendientes mp " +
                     "JOIN usuarios u ON mp.fk_remitente = u.pk_usuario " +
                     "LEFT JOIN grupos g ON mp.fk_grupo = g.pk_grupo " +
                     "WHERE mp.fk_usuario = ? " +
                     "ORDER BY mp.fecha_envio ASC";
        try (Connection con = Conexion.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, usuarioId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                MensajePendiente mp = new MensajePendiente();
                mp.setPk_pendiente(rs.getInt("pk_pendiente"));
                mp.setFk_usuario(rs.getInt("fk_usuario"));
                Integer fk_grupo = rs.getObject("fk_grupo", Integer.class);
                mp.setFk_grupo(fk_grupo);
                mp.setFk_remitente(rs.getInt("fk_remitente"));
                mp.setTipo(rs.getString("tipo"));
                mp.setMensaje(rs.getString("mensaje"));
                mp.setFecha_envio(rs.getTimestamp("fecha_envio"));
                mp.setNombreRemitente(rs.getString("nombre_remitente"));
                mp.setTituloGrupo(rs.getString("titulo_grupo"));
                mensajes.add(mp);
            }
        } catch (SQLException e) {
        }
        return mensajes;
    }
    public boolean eliminarMensajesPendientes(int usuarioId) {
        String sql = "DELETE FROM mensajes_pendientes WHERE fk_usuario = ?";
        try (Connection con = Conexion.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, usuarioId);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            return false;
        }
    }
}
