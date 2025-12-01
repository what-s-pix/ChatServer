package models;

import java.io.Serializable;

public class Grupo implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id;
    private String titulo;
    private String creador;
    
    public Grupo() {}

    public Grupo(int id, String titulo, String creador) {
        this.id = id;
        this.titulo = titulo;
        this.creador = creador;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public String getCreador() { return creador; }
    public void setCreador(String creador) { this.creador = creador; }
    
    @Override
    public String toString() {
        return titulo; // Importante para que se vea el nombre en la lista visual
    }
}