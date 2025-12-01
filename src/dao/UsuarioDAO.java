package dao;
import db.Conexion;
import models.Usuario;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
public class UsuarioDAO {
    public boolean registrar(Usuario u) {
        String sql = "INSERT INTO usuarios (nombre, username, password, ip, estado) VALUES (?, ?, ?, ?, 1)";
        try (Connection con = Conexion.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, u.getNombre());
            ps.setString(2, u.getUsername());
            ps.setString(3, u.getPassword());
            ps.setString(4, "127.0.0.1");
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("Error al registrar: " + e.getMessage());
            return false;
        }
    }
    public Usuario login(String username, String password) {
        String sql = "SELECT * FROM usuarios WHERE username = ?";
        try (Connection con = Conexion.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                boolean bloqueado = rs.getBoolean("bloqueado");
                if (bloqueado) {
                    return null;
                }
                String passReal = rs.getString("password");
                if (passReal.equals(password)) {
                    int id = rs.getInt("pk_usuario");
                    String nombre = rs.getString("nombre");
                    String user = rs.getString("username");
                    resetearIntentos(username);
                    actualizarEstado(username, 1);
                    return new Usuario(id, nombre, user, 1, false);
                } else {
                    int intentos = rs.getInt("intentos_login");
                    manejarFallo(username, intentos);
                    return null;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
    private void manejarFallo(String username, int intentosActuales) {
        int nuevosIntentos = intentosActuales + 1;
        String sql;
        if (nuevosIntentos >= 3) {
            sql = "UPDATE usuarios SET intentos_login = ?, bloqueado = 1 WHERE username = ?";
            System.out.println("¡ALERTA! Usuario " + username + " ha sido BLOQUEADO por intentos fallidos.");
        } else {
            sql = "UPDATE usuarios SET intentos_login = ? WHERE username = ?";
            System.out.println("Usuario " + username + " falló password. Intentos: " + nuevosIntentos);
        }
        try (Connection con = Conexion.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, nuevosIntentos);
            ps.setString(2, username);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    private void resetearIntentos(String username) {
        String sql = "UPDATE usuarios SET intentos_login = 0, bloqueado = 0 WHERE username = ?";
        try (Connection con = Conexion.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    public void actualizarEstado(String username, int estado) {
        String sql = "UPDATE usuarios SET estado = ? WHERE username = ?";
        try (Connection con = Conexion.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, estado);
            ps.setString(2, username);
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }
    public boolean estaBloqueado(String username) {
        String sql = "SELECT bloqueado FROM usuarios WHERE username = ?";
        try (Connection con = Conexion.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getBoolean("bloqueado");
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }
    public int obtenerIntentos(String username) {
        String sql = "SELECT intentos_login FROM usuarios WHERE username = ?";
        try (Connection con = Conexion.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt("intentos_login");
        } catch (SQLException e) { e.printStackTrace(); }
        return 0;
    }
    public boolean existeUsuario(String username) {
        String sql = "SELECT COUNT(*) FROM usuarios WHERE username = ?";
        try (Connection con = Conexion.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1) > 0;
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }
    public boolean recuperarContrasena(String username, String nuevaPassword) {
        String sql = "UPDATE usuarios SET password = ?, bloqueado = 0, intentos_login = 0 WHERE username = ?";
        try (Connection con = Conexion.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, nuevaPassword);
            ps.setString(2, username);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error al recuperar contraseña: " + e.getMessage());
            return false;
        }
    }
    public java.util.List<Usuario> obtenerUsuariosConectados() {
        java.util.List<Usuario> usuarios = new java.util.ArrayList<>();
        String sql = "SELECT pk_usuario, nombre, username, estado, bloqueado FROM usuarios WHERE estado = 1";
        try (Connection con = Conexion.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Usuario u = new Usuario();
                u.setPk_usuario(rs.getInt("pk_usuario"));
                u.setNombre(rs.getString("nombre"));
                u.setUsername(rs.getString("username"));
                u.setEstado(rs.getInt("estado"));
                u.setBloqueado(rs.getBoolean("bloqueado"));
                usuarios.add(u);
            }
        } catch (SQLException e) {
            System.err.println("Error al obtener usuarios conectados: " + e.getMessage());
        }
        return usuarios;
    }
    public java.util.List<Usuario> obtenerTodosUsuarios() {
        java.util.List<Usuario> usuarios = new java.util.ArrayList<>();
        String sql = "SELECT pk_usuario, nombre, username, estado, bloqueado FROM usuarios";
        try (Connection con = Conexion.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Usuario u = new Usuario();
                u.setPk_usuario(rs.getInt("pk_usuario"));
                u.setNombre(rs.getString("nombre"));
                u.setUsername(rs.getString("username"));
                u.setEstado(rs.getInt("estado"));
                u.setBloqueado(rs.getBoolean("bloqueado"));
                usuarios.add(u);
            }
        } catch (SQLException e) {
            System.err.println("Error al obtener usuarios: " + e.getMessage());
        }
        return usuarios;
    }
    public Usuario obtenerUsuarioPorId(int id) {
        String sql = "SELECT pk_usuario, nombre, username, estado, bloqueado FROM usuarios WHERE pk_usuario = ?";
        try (Connection con = Conexion.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                Usuario u = new Usuario();
                u.setPk_usuario(rs.getInt("pk_usuario"));
                u.setNombre(rs.getString("nombre"));
                u.setUsername(rs.getString("username"));
                u.setEstado(rs.getInt("estado"));
                u.setBloqueado(rs.getBoolean("bloqueado"));
                return u;
            }
        } catch (SQLException e) {
            System.err.println("Error al obtener usuario: " + e.getMessage());
        }
        return null;
    }
    public Usuario obtenerUsuarioPorUsername(String username) {
        String sql = "SELECT pk_usuario, nombre, username, estado, bloqueado FROM usuarios WHERE username = ?";
        try (Connection con = Conexion.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                Usuario u = new Usuario();
                u.setPk_usuario(rs.getInt("pk_usuario"));
                u.setNombre(rs.getString("nombre"));
                u.setUsername(rs.getString("username"));
                u.setEstado(rs.getInt("estado"));
                u.setBloqueado(rs.getBoolean("bloqueado"));
                return u;
            }
        } catch (SQLException e) {
            System.err.println("Error al obtener usuario por username: " + e.getMessage());
        }
        return null;
    }
}
