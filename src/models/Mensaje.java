package models;

import java.io.Serializable;
import java.sql.Timestamp;

public class Mensaje implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id;
    private String remitente;     // Username de quien envía
    private String destinatario;  // Username de quien recibe
    private String contenido;
    private Timestamp fecha;
    
    // Constructor vacío
    public Mensaje() {}

    // Constructor para crear mensaje nuevo (sin fecha ni id, se ponen al guardar)
    public Mensaje(String remitente, String destinatario, String contenido) {
        this.remitente = remitente;
        this.destinatario = destinatario;
        this.contenido = contenido;
    }
    
    // Constructor completo (desde BD)
    public Mensaje(int id, String remitente, String destinatario, String contenido, Timestamp fecha) {
        this.id = id;
        this.remitente = remitente;
        this.destinatario = destinatario;
        this.contenido = contenido;
        this.fecha = fecha;
    }

    // Getters y Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getRemitente() { return remitente; }
    public void setRemitente(String remitente) { this.remitente = remitente; }
    public String getDestinatario() { return destinatario; }
    public void setDestinatario(String destinatario) { this.destinatario = destinatario; }
    public String getContenido() { return contenido; }
    public void setContenido(String contenido) { this.contenido = contenido; }
    public Timestamp getFecha() { return fecha; }
    public void setFecha(Timestamp fecha) { this.fecha = fecha; }
}