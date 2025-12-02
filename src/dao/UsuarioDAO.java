package dao;
import db.Conexion;
import models.Usuario;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
public class UsuarioDAO {
    public boolean registrar(Usuario u) {
        if (u == null || u.getUsername() == null || u.getUsername().trim().isEmpty()) {
            return false;
        }
        String username = u.getUsername().trim();
        try {
            if (existeUsuario(username)) {
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        String sql = "INSERT INTO usuarios (nombre, username, password, estado, intentos_login, bloqueado) VALUES (?, ?, ?, 0, 0, 0)";
        try (Connection con = Conexion.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, u.getNombre() != null ? u.getNombre().trim() : "");
            ps.setString(2, username);
            ps.setString(3, u.getPassword() != null ? u.getPassword() : "");
            int filas = ps.executeUpdate();
            return filas > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    public Usuario login(String username, String password) {
        String sql = "SELECT * FROM usuarios WHERE username = ? COLLATE utf8mb4_bin";
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
                if (passReal != null && passReal.equals(password)) {
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
        }
        return null;
    }
    private void manejarFallo(String username, int intentosActuales) {
        int nuevosIntentos = intentosActuales + 1;
        String sql;
        if (nuevosIntentos >= 3) {
            sql = "UPDATE usuarios SET intentos_login = ?, bloqueado = 1 WHERE username = ? COLLATE utf8mb4_bin";
        } else {
            sql = "UPDATE usuarios SET intentos_login = ? WHERE username = ? COLLATE utf8mb4_bin";
        }
        try (Connection con = Conexion.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, nuevosIntentos);
            ps.setString(2, username);
            ps.executeUpdate();
        } catch (SQLException e) {
        }
    }
    private void resetearIntentos(String username) {
        String sql = "UPDATE usuarios SET intentos_login = 0, bloqueado = 0 WHERE username = ?";
        try (Connection con = Conexion.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.executeUpdate();
        } catch (SQLException e) {
        }
    }
    public void actualizarEstado(String username, int estado) {
        String sql = "UPDATE usuarios SET estado = ? WHERE username = ?";
        try (Connection con = Conexion.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, estado);
            ps.setString(2, username);
            ps.executeUpdate();
        } catch (SQLException e) {
        }
    }
    public boolean estaBloqueado(String username) {
        String sql = "SELECT bloqueado FROM usuarios WHERE username = ? COLLATE utf8mb4_bin";
        try (Connection con = Conexion.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getBoolean("bloqueado");
        } catch (SQLException e) {
        }
        return false;
    }
    public int obtenerIntentos(String username) {
        String sql = "SELECT intentos_login FROM usuarios WHERE username = ? COLLATE utf8mb4_bin";
        try (Connection con = Conexion.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                int intentos = rs.getInt("intentos_login");
                return intentos;
            }
        } catch (SQLException e) {
        }
        return 0;
    }
    public boolean existeUsuario(String username) {
        if (username == null || username.trim().isEmpty()) {
            return false;
        }
        String usernameTrim = username.trim();
        String sql = "SELECT COUNT(*) FROM usuarios WHERE BINARY username = ?";
        try (Connection con = Conexion.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, usernameTrim);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                int count = rs.getInt(1);
                return count > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
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
        }
        return null;
    }
}
