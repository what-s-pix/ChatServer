
package common;


import java.io.Serializable;

public class Peticion implements Serializable {
    private static final long serialVersionUID = 1L;

    private String accion;  // Ej: "LOGIN", "REGISTRO", "MENSAJE"
    private Object datos;   // Aquí guardamos el Usuario, el Mensaje, etc.
    
    // Constructores
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
