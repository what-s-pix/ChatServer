package dao;
import db.Conexion;
import models.InvitacionGrupo;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
public class InvitacionGrupoDAO {
    public boolean crearInvitacion(InvitacionGrupo inv) {
        String sql = "INSERT INTO invitaciones_grupo (fk_grupo, fk_invitado, fk_invitador, estado) VALUES (?, ?, ?, 'pendiente')";
        try (Connection con = Conexion.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, inv.getFk_grupo());
            ps.setInt(2, inv.getFk_invitado());
            ps.setInt(3, inv.getFk_invitador());
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("Error al crear invitación: " + e.getMessage());
            return false;
        }
    }
    public boolean aceptarInvitacion(int pk_invitacion) {
        String sql = "UPDATE invitaciones_grupo SET estado = 'aceptada', fecha_respuesta = CURRENT_TIMESTAMP WHERE pk_invitacion = ?";
        try (Connection con = Conexion.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, pk_invitacion);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error al aceptar invitación: " + e.getMessage());
            return false;
        }
    }
    public boolean rechazarInvitacion(int pk_invitacion) {
        String sql = "UPDATE invitaciones_grupo SET estado = 'rechazada', fecha_respuesta = CURRENT_TIMESTAMP WHERE pk_invitacion = ?";
        try (Connection con = Conexion.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, pk_invitacion);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error al rechazar invitación: " + e.getMessage());
            return false;
        }
    }
    public List<InvitacionGrupo> obtenerInvitacionesPendientes(int usuarioId) {
        List<InvitacionGrupo> invitaciones = new ArrayList<>();
        String sql = "SELECT i.*, g.titulo as titulo_grupo, u.nombre as nombre_invitador " +
                     "FROM invitaciones_grupo i " +
                     "JOIN grupos g ON i.fk_grupo = g.pk_grupo " +
                     "JOIN usuarios u ON i.fk_invitador = u.pk_usuario " +
                     "WHERE i.fk_invitado = ? AND i.estado = 'pendiente'";
        try (Connection con = Conexion.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, usuarioId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                InvitacionGrupo inv = new InvitacionGrupo();
                inv.setPk_invitacion(rs.getInt("pk_invitacion"));
                inv.setFk_grupo(rs.getInt("fk_grupo"));
                inv.setFk_invitado(rs.getInt("fk_invitado"));
                inv.setFk_invitador(rs.getInt("fk_invitador"));
                inv.setEstado(rs.getString("estado"));
                inv.setFecha_invitacion(rs.getTimestamp("fecha_invitacion"));
                inv.setTituloGrupo(rs.getString("titulo_grupo"));
                inv.setNombreInvitador(rs.getString("nombre_invitador"));
                invitaciones.add(inv);
            }
        } catch (SQLException e) {
            System.err.println("Error al obtener invitaciones: " + e.getMessage());
        }
        return invitaciones;
    }
    public List<InvitacionGrupo> obtenerInvitacionesGrupo(int fk_grupo) {
        List<InvitacionGrupo> invitaciones = new ArrayList<>();
        String sql = "SELECT i.*, g.titulo as titulo_grupo, u.nombre as nombre_invitador " +
                     "FROM invitaciones_grupo i " +
                     "JOIN grupos g ON i.fk_grupo = g.pk_grupo " +
                     "JOIN usuarios u ON i.fk_invitador = u.pk_usuario " +
                     "WHERE i.fk_grupo = ?";
        try (Connection con = Conexion.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, fk_grupo);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                InvitacionGrupo inv = new InvitacionGrupo();
                inv.setPk_invitacion(rs.getInt("pk_invitacion"));
                inv.setFk_grupo(rs.getInt("fk_grupo"));
                inv.setFk_invitado(rs.getInt("fk_invitado"));
                inv.setFk_invitador(rs.getInt("fk_invitador"));
                inv.setEstado(rs.getString("estado"));
                inv.setFecha_invitacion(rs.getTimestamp("fecha_invitacion"));
                inv.setFecha_respuesta(rs.getTimestamp("fecha_respuesta"));
                inv.setTituloGrupo(rs.getString("titulo_grupo"));
                inv.setNombreInvitador(rs.getString("nombre_invitador"));
                invitaciones.add(inv);
            }
        } catch (SQLException e) {
            System.err.println("Error al obtener invitaciones del grupo: " + e.getMessage());
        }
        return invitaciones;
    }
}
