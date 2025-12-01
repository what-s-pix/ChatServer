
package dao;

import db.Conexion;
import models.Usuario;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UsuarioDAO {

    /**
     * Intenta registrar un nuevo usuario.
     * @return true si se registró, false si ya existe el username.
     */
    public boolean registrar(Usuario u) {
        String sql = "INSERT INTO usuarios (nombre, username, password, ip, estado) VALUES (?, ?, ?, ?, 1)";
        
        try (Connection con = Conexion.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            
            ps.setString(1, u.getNombre());
            ps.setString(2, u.getUsername());
            ps.setString(3, u.getPassword()); // En un caso real, aquí encriptaríamos
            ps.setString(4, "127.0.0.1"); // IP por defecto para pruebas
            
            ps.executeUpdate();
            return true;
            
        } catch (SQLException e) {
            System.err.println("Error al registrar: " + e.getMessage());
            return false; // Probablemente el username ya existe (Unique Key)
        }
    }

    /**
     * Valida el login. Maneja la lógica de intentos fallidos.
     * @return Usuario lleno si es correcto, null si falla o está bloqueado.
     */
    public Usuario login(String username, String password) {
        String sql = "SELECT * FROM usuarios WHERE username = ?";
        
        try (Connection con = Conexion.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            
            if (rs.next()) {
                // 1. Verificamos si está bloqueado
                boolean bloqueado = rs.getBoolean("bloqueado");
                if (bloqueado) {
                    return null; 
                }

                String passReal = rs.getString("password");
                
                // 2. Comparamos contraseñas
                if (passReal.equals(password)) {
                    // --- ÉXITO ---
                    
                    // PASO CRÍTICO: Guardamos los datos EN MEMORIA antes de llamar a otros métodos
                    // porque los otros métodos podrían cerrar la conexión.
                    int id = rs.getInt("pk_usuario");
                    String nombre = rs.getString("nombre");
                    String user = rs.getString("username");
                    
                    // Ya tenemos los datos, ahora sí podemos llamar a los updates
                    // (Si estos métodos cierran la conexión, ya no nos importa porque ya leímos los datos)
                    resetearIntentos(username);
                    actualizarEstado(username, 1); 
                    
                    // Devolvemos el objeto con los datos que guardamos
                    return new Usuario(id, nombre, user, 1, false);
                    
                } else {
                    // --- FALLO ---
                    // Obtenemos intentos ANTES de llamar a manejarFallo por seguridad
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
    
    // --- MÉTODOS AUXILIARES PARA LA REGLA DE 3 INTENTOS ---

    private void manejarFallo(String username, int intentosActuales) {
        int nuevosIntentos = intentosActuales + 1;
        String sql;
        
        if (nuevosIntentos >= 3) {
            // BLOQUEAR USUARIO
            sql = "UPDATE usuarios SET intentos_login = ?, bloqueado = 1 WHERE username = ?";
            System.out.println("¡ALERTA! Usuario " + username + " ha sido BLOQUEADO por intentos fallidos.");
        } else {
            // SOLO AUMENTAR CONTADOR
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
    
    // Método para saber si alguien está bloqueado sin hacer login completo (útil para mensajes de error precisos)
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
}
