package dao;
import db.Conexion;
import models.Amistad;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
public class AmistadDAO {
    public boolean enviarSolicitud(Amistad a) {
        if (existeAmistad(a.getFk_usuario1(), a.getFk_usuario2())) {
            return false;
        }
        String sql = "INSERT INTO amistades (fk_usuario1, fk_usuario2, estado) VALUES (?, ?, 'pendiente')";
        try (Connection con = Conexion.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, a.getFk_usuario1());
            ps.setInt(2, a.getFk_usuario2());
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("Error al enviar solicitud: " + e.getMessage());
            return false;
        }
    }
    public boolean aceptarSolicitud(int pk_amistad) {
        String sql = "UPDATE amistades SET estado = 'aceptada', fecha_aceptacion = CURRENT_TIMESTAMP WHERE pk_amistad = ?";
        try (Connection con = Conexion.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, pk_amistad);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error al aceptar solicitud: " + e.getMessage());
            return false;
        }
    }
    public boolean rechazarSolicitud(int pk_amistad) {
        String sql = "UPDATE amistades SET estado = 'rechazada' WHERE pk_amistad = ?";
        try (Connection con = Conexion.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, pk_amistad);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error al rechazar solicitud: " + e.getMessage());
            return false;
        }
    }
    public boolean existeAmistad(int usuario1, int usuario2) {
        String sql = "SELECT COUNT(*) FROM amistades " +
                     "WHERE (fk_usuario1 = ? AND fk_usuario2 = ?) " +
                     "   OR (fk_usuario1 = ? AND fk_usuario2 = ?)";
        try (Connection con = Conexion.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, usuario1);
            ps.setInt(2, usuario2);
            ps.setInt(3, usuario2);
            ps.setInt(4, usuario1);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            System.err.println("Error al verificar amistad: " + e.getMessage());
        }
        return false;
    }
    public boolean sonAmigos(int usuario1, int usuario2) {
        String sql = "SELECT COUNT(*) FROM amistades " +
                     "WHERE estado = 'aceptada' " +
                     "  AND ((fk_usuario1 = ? AND fk_usuario2 = ?) " +
                     "   OR (fk_usuario1 = ? AND fk_usuario2 = ?))";
        try (Connection con = Conexion.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, usuario1);
            ps.setInt(2, usuario2);
            ps.setInt(3, usuario2);
            ps.setInt(4, usuario1);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            System.err.println("Error al verificar amistad: " + e.getMessage());
        }
        return false;
    }
    public List<Amistad> obtenerAmigos(int usuarioId) {
        List<Amistad> amigos = new ArrayList<>();
        String sql = "SELECT a.*, " +
                     "CASE WHEN a.fk_usuario1 = ? THEN u2.nombre ELSE u1.nombre END as nombre_usuario " +
                     "FROM amistades a " +
                     "LEFT JOIN usuarios u1 ON a.fk_usuario1 = u1.pk_usuario " +
                     "LEFT JOIN usuarios u2 ON a.fk_usuario2 = u2.pk_usuario " +
                     "WHERE a.estado = 'aceptada' " +
                     "  AND (a.fk_usuario1 = ? OR a.fk_usuario2 = ?)";
        try (Connection con = Conexion.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, usuarioId);
            ps.setInt(2, usuarioId);
            ps.setInt(3, usuarioId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Amistad a = new Amistad();
                a.setPk_amistad(rs.getInt("pk_amistad"));
                a.setFk_usuario1(rs.getInt("fk_usuario1"));
                a.setFk_usuario2(rs.getInt("fk_usuario2"));
                a.setEstado(rs.getString("estado"));
                a.setFecha_solicitud(rs.getTimestamp("fecha_solicitud"));
                a.setFecha_aceptacion(rs.getTimestamp("fecha_aceptacion"));
                a.setNombreUsuario(rs.getString("nombre_usuario"));
                amigos.add(a);
            }
        } catch (SQLException e) {
            System.err.println("Error al obtener amigos: " + e.getMessage());
        }
        return amigos;
    }
    public List<Amistad> obtenerSolicitudesPendientes(int usuarioId) {
        List<Amistad> solicitudes = new ArrayList<>();
        String sql = "SELECT a.*, u.nombre as nombre_usuario " +
                     "FROM amistades a " +
                     "JOIN usuarios u ON a.fk_usuario1 = u.pk_usuario " +
                     "WHERE a.fk_usuario2 = ? AND a.estado = 'pendiente'";
        try (Connection con = Conexion.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, usuarioId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Amistad a = new Amistad();
                a.setPk_amistad(rs.getInt("pk_amistad"));
                a.setFk_usuario1(rs.getInt("fk_usuario1"));
                a.setFk_usuario2(rs.getInt("fk_usuario2"));
                a.setEstado(rs.getString("estado"));
                a.setFecha_solicitud(rs.getTimestamp("fecha_solicitud"));
                a.setNombreUsuario(rs.getString("nombre_usuario"));
                solicitudes.add(a);
            }
        } catch (SQLException e) {
            System.err.println("Error al obtener solicitudes: " + e.getMessage());
        }
        return solicitudes;
    }
}
