package common;
import java.io.Serializable;
public class Peticion implements Serializable {
    private static final long serialVersionUID = 1L;
    private String accion;
    private Object datos;
    public Peticion(String accion, Object datos) {
        this.accion = accion;
        this.datos = datos;
    }
    public String getAccion() {
        return accion;
    }
    public Object getDatos() {
        return datos;
    }
}
