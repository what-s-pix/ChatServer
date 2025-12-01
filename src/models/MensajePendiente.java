package models;
import java.io.Serializable;
import java.sql.Timestamp;
public class MensajePendiente implements Serializable {
    private static final long serialVersionUID = 1L;
    private int pk_pendiente;
    private int fk_usuario;
    private Integer fk_grupo;
    private int fk_remitente;
    private String tipo;
    private String mensaje;
    private Timestamp fecha_envio;
    private String nombreRemitente;
    private String tituloGrupo;
    public MensajePendiente() {}
    public MensajePendiente(int fk_usuario, int fk_remitente, String tipo, String mensaje) {
        this.fk_usuario = fk_usuario;
        this.fk_remitente = fk_remitente;
        this.tipo = tipo;
        this.mensaje = mensaje;
    }
    public int getPk_pendiente() { return pk_pendiente; }
    public void setPk_pendiente(int pk_pendiente) { this.pk_pendiente = pk_pendiente; }
    public int getFk_usuario() { return fk_usuario; }
    public void setFk_usuario(int fk_usuario) { this.fk_usuario = fk_usuario; }
    public Integer getFk_grupo() { return fk_grupo; }
    public void setFk_grupo(Integer fk_grupo) { this.fk_grupo = fk_grupo; }
    public int getFk_remitente() { return fk_remitente; }
    public void setFk_remitente(int fk_remitente) { this.fk_remitente = fk_remitente; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public String getMensaje() { return mensaje; }
    public void setMensaje(String mensaje) { this.mensaje = mensaje; }
    public Timestamp getFecha_envio() { return fecha_envio; }
    public void setFecha_envio(Timestamp fecha_envio) { this.fecha_envio = fecha_envio; }
    public String getNombreRemitente() { return nombreRemitente; }
    public void setNombreRemitente(String nombreRemitente) { this.nombreRemitente = nombreRemitente; }
    public String getTituloGrupo() { return tituloGrupo; }
    public void setTituloGrupo(String tituloGrupo) { this.tituloGrupo = tituloGrupo; }
}
