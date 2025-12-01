package dao;

import db.Conexion;
import models.Grupo;
import models.Mensaje;
import java.sql.*;
import java.util.ArrayList;

public class GrupoDAO {

    // 1. Crear Grupo (El creador se añade automáticamente como aceptado)
    public int crearGrupo(String titulo, String creador) {
        String sqlGrupo = "INSERT INTO grupos (titulo, creador) VALUES (?, ?)";
        String sqlMiembro = "INSERT INTO miembros_grupo (id_grupo, usuario, estado) VALUES (?, ?, 1)";
        
        try (Connection con = Conexion.getConnection()) {
            // Insertar Grupo y pedir la ID generada
            PreparedStatement ps = con.prepareStatement(sqlGrupo, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, titulo);
            ps.setString(2, creador);
            ps.executeUpdate();
            
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) {
                int idGrupo = rs.getInt(1);
                
                // Insertar al Creador como miembro activo
                PreparedStatement psM = con.prepareStatement(sqlMiembro);
                psM.setInt(1, idGrupo);
                psM.setString(2, creador);
                psM.executeUpdate();
                
                return idGrupo;
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return -1;
    }

    // 2. Invitar Usuario (Estado 0 = Pendiente)
    public boolean invitarUsuario(int idGrupo, String usuario) {
        if (esMiembro(idGrupo, usuario)) return false;

        String sql = "INSERT INTO miembros_grupo (id_grupo, usuario, estado) VALUES (?, ?, 0)";
        try (Connection con = Conexion.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idGrupo);
            ps.setString(2, usuario);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) { return false; }
    }

    // 3. Aceptar Invitación
    public boolean aceptarInvitacion(int idGrupo, String usuario) {
        String sql = "UPDATE miembros_grupo SET estado = 1 WHERE id_grupo = ? AND usuario = ?";
        try (Connection con = Conexion.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idGrupo);
            ps.setString(2, usuario);
            
            // REGLA: Al aceptar, verificamos si el grupo ya es válido (mínimo 3 personas contadas)
            // Tu regla decía: "2 aceptan y 1 pendiente cuenta como 3". 
            // Aquí simplemente dejamos que exista. La validación de eliminar se hace al SALIR.
            
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { return false; }
    }
    
    // 4. Guardar Mensaje de Grupo
    public boolean guardarMensajeGrupo(Mensaje m) {
        // Guardamos con destinatario 'GRUPO' y llenamos id_grupo
        String sql = "INSERT INTO mensajes (remitente, destinatario, contenido, id_grupo, leido) VALUES (?, 'GRUPO', ?, ?, 1)";
        try (Connection con = Conexion.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, m.getRemitente());
            ps.setString(2, m.getContenido());
            ps.setInt(3, m.getId()); // Usamos el campo ID del objeto Mensaje para pasar el id_grupo
            ps.executeUpdate();
            return true;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }
    
    // 5. Obtener Historial de Grupo
    public ArrayList<Mensaje> getHistorialGrupo(int idGrupo) {
        ArrayList<Mensaje> lista = new ArrayList<>();
        String sql = "SELECT * FROM mensajes WHERE id_grupo = ? ORDER BY fecha ASC";
        try (Connection con = Conexion.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idGrupo);
            ResultSet rs = ps.executeQuery();
            while(rs.next()) {
                Mensaje m = new Mensaje(
                    rs.getInt("pk_mensaje"),
                    rs.getString("remitente"),
                    "GRUPO",
                    rs.getString("contenido"),
                    rs.getTimestamp("fecha")
                );
                m.setId(idGrupo); 
                lista.add(m);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return lista;
    }

    // --- MÉTODOS AUXILIARES DE CONSULTA ---

    public ArrayList<Grupo> getMisGrupos(String usuario) {
        ArrayList<Grupo> lista = new ArrayList<>();
        String sql = "SELECT g.* FROM grupos g JOIN miembros_grupo mg ON g.pk_grupo = mg.id_grupo WHERE mg.usuario = ? AND mg.estado = 1";
        try (Connection con = Conexion.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, usuario);
            ResultSet rs = ps.executeQuery();
            while(rs.next()) {
                lista.add(new Grupo(rs.getInt("pk_grupo"), rs.getString("titulo"), rs.getString("creador")));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return lista;
    }
    
    public ArrayList<Grupo> getInvitacionesGrupos(String usuario) {
        ArrayList<Grupo> lista = new ArrayList<>();
        String sql = "SELECT g.* FROM grupos g JOIN miembros_grupo mg ON g.pk_grupo = mg.id_grupo WHERE mg.usuario = ? AND mg.estado = 0";
        try (Connection con = Conexion.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, usuario);
            ResultSet rs = ps.executeQuery();
            while(rs.next()) {
                lista.add(new Grupo(rs.getInt("pk_grupo"), rs.getString("titulo"), rs.getString("creador")));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return lista;
    }

    public ArrayList<String> getMiembros(int idGrupo) {
        ArrayList<String> lista = new ArrayList<>();
        String sql = "SELECT usuario FROM miembros_grupo WHERE id_grupo = ?";
        try (Connection con = Conexion.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idGrupo);
            ResultSet rs = ps.executeQuery();
            while(rs.next()) lista.add(rs.getString("usuario"));
        } catch (SQLException e) {}
        return lista;
    }

    private boolean esMiembro(int idGrupo, String usuario) {
        String sql = "SELECT * FROM miembros_grupo WHERE id_grupo = ? AND usuario = ?";
        try (Connection con = Conexion.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idGrupo);
            ps.setString(2, usuario);
            return ps.executeQuery().next();
        } catch (SQLException e) { return false; }
    }
}