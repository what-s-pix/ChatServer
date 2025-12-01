package models;
import java.io.Serializable;
import java.sql.Timestamp;
public class InvitacionGrupo implements Serializable {
    private static final long serialVersionUID = 1L;
    private int pk_invitacion;
    private int fk_grupo;
    private int fk_invitado;
    private int fk_invitador;
    private String estado;
    private Timestamp fecha_invitacion;
    private Timestamp fecha_respuesta;
    private String tituloGrupo;
    private String nombreInvitador;
    public InvitacionGrupo() {}
    public InvitacionGrupo(int fk_grupo, int fk_invitado, int fk_invitador) {
        this.fk_grupo = fk_grupo;
        this.fk_invitado = fk_invitado;
        this.fk_invitador = fk_invitador;
        this.estado = "pendiente";
    }
    public int getPk_invitacion() { return pk_invitacion; }
    public void setPk_invitacion(int pk_invitacion) { this.pk_invitacion = pk_invitacion; }
    public int getFk_grupo() { return fk_grupo; }
    public void setFk_grupo(int fk_grupo) { this.fk_grupo = fk_grupo; }
    public int getFk_invitado() { return fk_invitado; }
    public void setFk_invitado(int fk_invitado) { this.fk_invitado = fk_invitado; }
    public int getFk_invitador() { return fk_invitador; }
    public void setFk_invitador(int fk_invitador) { this.fk_invitador = fk_invitador; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public Timestamp getFecha_invitacion() { return fecha_invitacion; }
    public void setFecha_invitacion(Timestamp fecha_invitacion) { this.fecha_invitacion = fecha_invitacion; }
    public Timestamp getFecha_respuesta() { return fecha_respuesta; }
    public void setFecha_respuesta(Timestamp fecha_respuesta) { this.fecha_respuesta = fecha_respuesta; }
    public String getTituloGrupo() { return tituloGrupo; }
    public void setTituloGrupo(String tituloGrupo) { this.tituloGrupo = tituloGrupo; }
    public String getNombreInvitador() { return nombreInvitador; }
    public void setNombreInvitador(String nombreInvitador) { this.nombreInvitador = nombreInvitador; }
}
