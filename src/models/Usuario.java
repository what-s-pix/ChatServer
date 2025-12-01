
package models;


import java.io.Serializable;
import java.sql.Timestamp;

// IMPLEMENTS SERIALIZABLE es OBLIGATORIO para enviar objetos por red
public class Usuario implements Serializable {
    private static final long serialVersionUID = 1L; // Buena práctica para serialización

    // Campos idénticos a tu BD
    private int pk_usuario;
    private String nombre;
    private String username;
    private String password;
    private String ip;
    private int estado; // 0: desconectado, 1: conectado
    private Timestamp fecha_registro;
    private int intentos_login;
    private boolean bloqueado;

    // Constructor vacío (necesario para muchas librerías)
    public Usuario() {}

    // Constructor para Registro (lo que llenas en el formulario)
    public Usuario(String nombre, String username, String password) {
        this.nombre = nombre;
        this.username = username;
        this.password = password;
        this.intentos_login = 0;
        this.bloqueado = false;
    }

    // Constructor completo (para cuando lees de la BD)
    public Usuario(int pk_usuario, String nombre, String username, int estado, boolean bloqueado) {
        this.pk_usuario = pk_usuario;
        this.nombre = nombre;
        this.username = username;
        this.estado = estado;
        this.bloqueado = bloqueado;
    }

    // --- GETTERS Y SETTERS (Clic derecho > Insert Code > Getters and Setters) ---
    
    public int getPk_usuario() { 
        return pk_usuario; 
    }
    public void setPk_usuario(int pk_usuario) { 
        this.pk_usuario = pk_usuario; 
    }

    public String getNombre() { 
        return nombre; 
    }
    public void setNombre(String nombre) { 
        this.nombre = nombre; 
    }

    public String getUsername() { 
        return username; 
    }
    public void setUsername(String username) { 
        this.username = username; 
    }

    public String getPassword() { 
        return password; 
    }
    public void setPassword(String password) { 
        this.password = password; 
    }
    
    public int getIntentos_login() { 
        return intentos_login; 
    }
    public void setIntentos_login(int intentos_login) { 
        this.intentos_login = intentos_login; 
    }
    
    public boolean isBloqueado() { 
        return bloqueado; 
    }
    public void setBloqueado(boolean bloqueado) { 
        this.bloqueado = bloqueado; 
    }
    
    public int getEstado() {
        return estado;
    }

    public void setEstado(int estado) {
        this.estado = estado;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }
    
}
