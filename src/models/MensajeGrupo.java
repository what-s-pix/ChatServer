package models;
import java.io.Serializable;
import java.sql.Timestamp;
public class MensajeGrupo implements Serializable {
    private static final long serialVersionUID = 1L;
    private int pk_mensaje;
    private int fk_grupo;
    private int fk_remitente;
    private String mensaje;
    private Timestamp fecha_envio;
    private String nombreRemitente;
    private String tituloGrupo;
    public MensajeGrupo() {}
    public MensajeGrupo(int fk_grupo, int fk_remitente, String mensaje) {
        this.fk_grupo = fk_grupo;
        this.fk_remitente = fk_remitente;
        this.mensaje = mensaje;
    }
    public int getPk_mensaje() { return pk_mensaje; }
    public void setPk_mensaje(int pk_mensaje) { this.pk_mensaje = pk_mensaje; }
    public int getFk_grupo() { return fk_grupo; }
    public void setFk_grupo(int fk_grupo) { this.fk_grupo = fk_grupo; }
    public int getFk_remitente() { return fk_remitente; }
    public void setFk_remitente(int fk_remitente) { this.fk_remitente = fk_remitente; }
    public String getMensaje() { return mensaje; }
    public void setMensaje(String mensaje) { this.mensaje = mensaje; }
    public Timestamp getFecha_envio() { return fecha_envio; }
    public void setFecha_envio(Timestamp fecha_envio) { this.fecha_envio = fecha_envio; }
    public String getNombreRemitente() { return nombreRemitente; }
    public void setNombreRemitente(String nombreRemitente) { this.nombreRemitente = nombreRemitente; }
    public String getTituloGrupo() { return tituloGrupo; }
    public void setTituloGrupo(String tituloGrupo) { this.tituloGrupo = tituloGrupo; }
}
