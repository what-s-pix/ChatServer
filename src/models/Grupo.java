package models;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
public class Grupo implements Serializable {
    private static final long serialVersionUID = 1L;
    private int pk_grupo;
    private String titulo;
    private int fk_creador;
    private Timestamp fecha_creacion;
    private boolean activo;
    private String nombreCreador;
    private List<Usuario> miembros;
    public Grupo() {
        this.miembros = new ArrayList<>();
    }
    public Grupo(String titulo, int fk_creador) {
        this.titulo = titulo;
        this.fk_creador = fk_creador;
        this.activo = true;
        this.miembros = new ArrayList<>();
    }
    public int getPk_grupo() { return pk_grupo; }
    public void setPk_grupo(int pk_grupo) { this.pk_grupo = pk_grupo; }
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public int getFk_creador() { return fk_creador; }
    public void setFk_creador(int fk_creador) { this.fk_creador = fk_creador; }
    public Timestamp getFecha_creacion() { return fecha_creacion; }
    public void setFecha_creacion(Timestamp fecha_creacion) { this.fecha_creacion = fecha_creacion; }
    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }
    public String getNombreCreador() { return nombreCreador; }
    public void setNombreCreador(String nombreCreador) { this.nombreCreador = nombreCreador; }
    public List<Usuario> getMiembros() { return miembros; }
    public void setMiembros(List<Usuario> miembros) { this.miembros = miembros; }
}
