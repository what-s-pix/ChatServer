package dao;
import db.Conexion;
import models.MensajeGrupo;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
public class MensajeGrupoDAO {
    public boolean guardarMensaje(MensajeGrupo m) {
        String sql = "INSERT INTO mensajes_grupo (fk_grupo, fk_remitente, mensaje) VALUES (?, ?, ?)";
        try (Connection con = Conexion.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, m.getFk_grupo());
            ps.setInt(2, m.getFk_remitente());
            ps.setString(3, m.getMensaje());
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("Error al guardar mensaje de grupo: " + e.getMessage());
            return false;
        }
    }
    public List<MensajeGrupo> obtenerHistorial(int fk_grupo) {
        List<MensajeGrupo> mensajes = new ArrayList<>();
        String sql = "SELECT m.*, u.nombre as nombre_remitente, g.titulo as titulo_grupo " +
                     "FROM mensajes_grupo m " +
                     "JOIN usuarios u ON m.fk_remitente = u.pk_usuario " +
                     "JOIN grupos g ON m.fk_grupo = g.pk_grupo " +
                     "WHERE m.fk_grupo = ? " +
                     "ORDER BY m.fecha_envio ASC";
        try (Connection con = Conexion.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, fk_grupo);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                MensajeGrupo m = new MensajeGrupo();
                m.setPk_mensaje(rs.getInt("pk_mensaje"));
                m.setFk_grupo(rs.getInt("fk_grupo"));
                m.setFk_remitente(rs.getInt("fk_remitente"));
                m.setMensaje(rs.getString("mensaje"));
                m.setFecha_envio(rs.getTimestamp("fecha_envio"));
                m.setNombreRemitente(rs.getString("nombre_remitente"));
                m.setTituloGrupo(rs.getString("titulo_grupo"));
                mensajes.add(m);
            }
        } catch (SQLException e) {
            System.err.println("Error al obtener historial de grupo: " + e.getMessage());
        }
        return mensajes;
    }
}
