
package chatserver;

import common.Peticion;
import dao.UsuarioDAO;
import models.Usuario;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class HiloCliente extends Thread {
    
    private Socket socket;
    private Servidor servidor; // Referencia al servidor principal para usar el log
    private ObjectOutputStream salida;
    private ObjectInputStream entrada;
    private Usuario usuarioConectado; // Aquí guardamos quién es este cliente tras el login
    private boolean conectado = true;

    public HiloCliente(Socket socket, Servidor servidor) {
        this.socket = socket;
        this.servidor = servidor;
    }

    @Override
    public void run() {
        try {
            // 1. Crear los canales de comunicación
            // IMPORTANTE: Primero crear el Output, luego el Input (Regla de Java Sockets)
            salida = new ObjectOutputStream(socket.getOutputStream());
            entrada = new ObjectInputStream(socket.getInputStream());

            // 2. Bucle infinito escuchando peticiones de ESTE cliente
            while (conectado) {
                // Se queda pausado aquí hasta que el cliente envíe algo
                Peticion peticion = (Peticion) entrada.readObject();
                
                // Procesamos según la "accion"
                procesarPeticion(peticion);
            }

        } catch (Exception e) {
            servidor.log("Cliente desconectado: " + socket.getInetAddress());
            // Aquí deberíamos manejar la desconexión (quitar de la lista, cambiar estado en BD...)
            cerrarConexion();
        }
    }

    private void procesarPeticion(Peticion p) {
        UsuarioDAO dao = new UsuarioDAO();

        switch (p.getAccion()) {
            case "LOGIN":
                // El objeto 'datos' debería ser un Usuario con username y pass
                Usuario uLogin = (Usuario) p.getDatos();
                servidor.log("Intento de login: " + uLogin.getUsername());

                // Verificamos en BD
                Usuario logueado = dao.login(uLogin.getUsername(), uLogin.getPassword());

                if (logueado != null) {
                    // Login exitoso
                    this.usuarioConectado = logueado;
                    enviar(new Peticion("LOGIN_OK", logueado));
                    servidor.log("Usuario LOGUEADO: " + logueado.getUsername());
                } else {
                    // Login fallido
                    // Verificamos si se bloqueó por el intento reciente
                    boolean bloqueado = dao.estaBloqueado(uLogin.getUsername());
                    if (bloqueado) {
                         enviar(new Peticion("LOGIN_BLOQUEADO", "Tu cuenta ha sido bloqueada por excesivos intentos."));
                         servidor.log("Usuario BLOQUEADO: " + uLogin.getUsername());
                    } else {
                         enviar(new Peticion("LOGIN_ERROR", "Credenciales incorrectas."));
                         servidor.log("Fallo de credenciales: " + uLogin.getUsername());
                    }
                }
                break;

            case "REGISTRO":
                Usuario uReg = (Usuario) p.getDatos();
                boolean registrado = dao.registrar(uReg);
                if (registrado) {
                    enviar(new Peticion("REGISTRO_OK", "Usuario creado con éxito"));
                    servidor.log("Nuevo usuario registrado: " + uReg.getUsername());
                } else {
                    enviar(new Peticion("REGISTRO_ERROR", "El usuario ya existe"));
                }
                break;
                
            // Aquí añadiremos más casos luego: "MENSAJE", "CREAR_GRUPO", etc.
            
            default:
                servidor.log("Acción desconocida: " + p.getAccion());
        }
    }

    // Método auxiliar para enviar respuestas al cliente
    public void enviar(Peticion p) {
        try {
            salida.writeObject(p);
            salida.flush(); // Fuerza el envío inmediato
        } catch (IOException e) {
            servidor.log("Error enviando mensaje: " + e.getMessage());
        }
    }
    
    private void cerrarConexion() {
        conectado = false;
        try {
            if (socket != null) socket.close();
        } catch (IOException e) {}
    }
}