package models;

import java.io.Serializable;
import java.sql.Timestamp;

public class Amistad implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private int pk_amistad;
    private int fk_usuario1;
    private int fk_usuario2;
    private String estado; // pendiente, aceptada, rechazada
    private Timestamp fecha_solicitud;
    private Timestamp fecha_aceptacion;
    private String nombreUsuario; // Para mostrar en la UI
    
    public Amistad() {}
    
    public Amistad(int fk_usuario1, int fk_usuario2) {
        this.fk_usuario1 = fk_usuario1;
        this.fk_usuario2 = fk_usuario2;
        this.estado = "pendiente";
    }
    
    // Getters y Setters
    public int getPk_amistad() { return pk_amistad; }
    public void setPk_amistad(int pk_amistad) { this.pk_amistad = pk_amistad; }
    
    public int getFk_usuario1() { return fk_usuario1; }
    public void setFk_usuario1(int fk_usuario1) { this.fk_usuario1 = fk_usuario1; }
    
    public int getFk_usuario2() { return fk_usuario2; }
    public void setFk_usuario2(int fk_usuario2) { this.fk_usuario2 = fk_usuario2; }
    
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    
    public Timestamp getFecha_solicitud() { return fecha_solicitud; }
    public void setFecha_solicitud(Timestamp fecha_solicitud) { this.fecha_solicitud = fecha_solicitud; }
    
    public Timestamp getFecha_aceptacion() { return fecha_aceptacion; }
    public void setFecha_aceptacion(Timestamp fecha_aceptacion) { this.fecha_aceptacion = fecha_aceptacion; }
    
    public String getNombreUsuario() { return nombreUsuario; }
    public void setNombreUsuario(String nombreUsuario) { this.nombreUsuario = nombreUsuario; }
}

