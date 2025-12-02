package dao;
import db.Conexion;
import models.Grupo;
import models.Usuario;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
public class GrupoDAO {
    public int crearGrupo(Grupo g) {
        String sql = "INSERT INTO grupos (titulo, fk_creador) VALUES (?, ?)";
        try (Connection con = Conexion.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, g.getTitulo());
            ps.setInt(2, g.getFk_creador());
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) {
                int pk_grupo = rs.getInt(1);
                agregarMiembro(pk_grupo, g.getFk_creador());
                return pk_grupo;
            }
        } catch (SQLException e) {
        }
        return -1;
    }
    public boolean agregarMiembro(int fk_grupo, int fk_usuario) {
        String sql = "INSERT INTO miembros_grupo (fk_grupo, fk_usuario) VALUES (?, ?)";
        try (Connection con = Conexion.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, fk_grupo);
            ps.setInt(2, fk_usuario);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            return false;
        }
    }
    public boolean eliminarMiembro(int fk_grupo, int fk_usuario) {
        String sql = "DELETE FROM miembros_grupo WHERE fk_grupo = ? AND fk_usuario = ?";
        try (Connection con = Conexion.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, fk_grupo);
            ps.setInt(2, fk_usuario);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            return false;
        }
    }
    public boolean eliminarGrupo(int pk_grupo) {
        String sql = "UPDATE grupos SET activo = FALSE WHERE pk_grupo = ?";
        try (Connection con = Conexion.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, pk_grupo);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            return false;
        }
    }
    public int contarMiembros(int fk_grupo) {
        String sql = "SELECT COUNT(*) FROM miembros_grupo WHERE fk_grupo = ?";
        try (Connection con = Conexion.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, fk_grupo);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
        }
        return 0;
    }
    public int contarInvitacionesAceptadas(int fk_grupo) {
        String sql = "SELECT COUNT(*) FROM invitaciones_grupo " +
                     "WHERE fk_grupo = ? AND estado = 'aceptada'";
        try (Connection con = Conexion.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, fk_grupo);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
        }
        return 0;
    }
    public int contarInvitacionesPendientes(int fk_grupo) {
        String sql = "SELECT COUNT(*) FROM invitaciones_grupo " +
                     "WHERE fk_grupo = ? AND estado = 'pendiente'";
        try (Connection con = Conexion.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, fk_grupo);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
        }
        return 0;
    }
    public boolean esCreador(int fk_grupo, int fk_usuario) {
        String sql = "SELECT COUNT(*) FROM grupos WHERE pk_grupo = ? AND fk_creador = ?";
        try (Connection con = Conexion.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, fk_grupo);
            ps.setInt(2, fk_usuario);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
        }
        return false;
    }
    public boolean esMiembro(int fk_grupo, int fk_usuario) {
        String sql = "SELECT COUNT(*) FROM miembros_grupo WHERE fk_grupo = ? AND fk_usuario = ?";
        try (Connection con = Conexion.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, fk_grupo);
            ps.setInt(2, fk_usuario);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
        }
        return false;
    }
    public List<Grupo> obtenerGruposUsuario(int usuarioId) {
        List<Grupo> grupos = new ArrayList<>();
        String sql = "SELECT g.*, u.nombre as nombre_creador " +
                     "FROM grupos g " +
                     "JOIN miembros_grupo mg ON g.pk_grupo = mg.fk_grupo " +
                     "JOIN usuarios u ON g.fk_creador = u.pk_usuario " +
                     "WHERE mg.fk_usuario = ? AND g.activo = TRUE";
        try (Connection con = Conexion.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, usuarioId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Grupo g = new Grupo();
                g.setPk_grupo(rs.getInt("pk_grupo"));
                g.setTitulo(rs.getString("titulo"));
                g.setFk_creador(rs.getInt("fk_creador"));
                g.setFecha_creacion(rs.getTimestamp("fecha_creacion"));
                g.setActivo(rs.getBoolean("activo"));
                g.setNombreCreador(rs.getString("nombre_creador"));
                grupos.add(g);
            }
        } catch (SQLException e) {
        }
        return grupos;
    }
    public List<Usuario> obtenerMiembros(int fk_grupo) {
        List<Usuario> miembros = new ArrayList<>();
        String sql = "SELECT u.* FROM usuarios u " +
                     "JOIN miembros_grupo mg ON u.pk_usuario = mg.fk_usuario " +
                     "WHERE mg.fk_grupo = ?";
        try (Connection con = Conexion.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, fk_grupo);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Usuario u = new Usuario();
                u.setPk_usuario(rs.getInt("pk_usuario"));
                u.setNombre(rs.getString("nombre"));
                u.setUsername(rs.getString("username"));
                u.setEstado(rs.getInt("estado"));
                miembros.add(u);
            }
        } catch (SQLException e) {
        }
        return miembros;
    }
    public Grupo obtenerGrupo(int pk_grupo) {
        String sql = "SELECT g.*, u.nombre as nombre_creador FROM grupos g " +
                     "JOIN usuarios u ON g.fk_creador = u.pk_usuario " +
                     "WHERE g.pk_grupo = ?";
        try (Connection con = Conexion.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, pk_grupo);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                Grupo g = new Grupo();
                g.setPk_grupo(rs.getInt("pk_grupo"));
                g.setTitulo(rs.getString("titulo"));
                g.setFk_creador(rs.getInt("fk_creador"));
                g.setFecha_creacion(rs.getTimestamp("fecha_creacion"));
                g.setActivo(rs.getBoolean("activo"));
                g.setNombreCreador(rs.getString("nombre_creador"));
                return g;
            }
        } catch (SQLException e) {
        }
        return null;
    }
}
