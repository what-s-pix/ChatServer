package models;
import java.io.Serializable;
import java.sql.Timestamp;
public class Mensaje implements Serializable {
    private static final long serialVersionUID = 1L;
    private int pk_mensaje;
    private int fk_remitente;
    private int fk_destinatario;
    private String mensaje;
    private Timestamp fecha_envio;
    private boolean leido;
    private String nombreRemitente;
    private String nombreDestinatario;
    public Mensaje() {}
    public Mensaje(int fk_remitente, int fk_destinatario, String mensaje) {
        this.fk_remitente = fk_remitente;
        this.fk_destinatario = fk_destinatario;
        this.mensaje = mensaje;
        this.leido = false;
    }
    public int getPk_mensaje() { return pk_mensaje; }
    public void setPk_mensaje(int pk_mensaje) { this.pk_mensaje = pk_mensaje; }
    public int getFk_remitente() { return fk_remitente; }
    public void setFk_remitente(int fk_remitente) { this.fk_remitente = fk_remitente; }
    public int getFk_destinatario() { return fk_destinatario; }
    public void setFk_destinatario(int fk_destinatario) { this.fk_destinatario = fk_destinatario; }
    public String getMensaje() { return mensaje; }
    public void setMensaje(String mensaje) { this.mensaje = mensaje; }
    public Timestamp getFecha_envio() { return fecha_envio; }
    public void setFecha_envio(Timestamp fecha_envio) { this.fecha_envio = fecha_envio; }
    public boolean isLeido() { return leido; }
    public void setLeido(boolean leido) { this.leido = leido; }
    public String getNombreRemitente() { return nombreRemitente; }
    public void setNombreRemitente(String nombreRemitente) { this.nombreRemitente = nombreRemitente; }
    public String getNombreDestinatario() { return nombreDestinatario; }
    public void setNombreDestinatario(String nombreDestinatario) { this.nombreDestinatario = nombreDestinatario; }
}
