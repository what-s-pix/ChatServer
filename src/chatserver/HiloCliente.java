
package chatserver;

import common.Peticion;
import dao.AmistadDAO;
import dao.GrupoDAO;
import dao.MensajeDAO;
import dao.UsuarioDAO;
import models.Usuario;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import models.Mensaje;

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
        MensajeDAO mensajeDao = new MensajeDAO(); // Instanciamos el DAO de mensajes

        switch (p.getAccion()) {
            case "LOGIN":
                Usuario uLogin = (Usuario) p.getDatos();
                servidor.log("Intento de login: " + uLogin.getUsername());
                Usuario logueado = dao.login(uLogin.getUsername(), uLogin.getPassword());

                if (logueado != null) {
                    this.usuarioConectado = logueado;
                    enviar(new Peticion("LOGIN_OK", logueado));
                    servidor.log("Usuario LOGUEADO: " + logueado.getUsername());
                    
                    // --- NUEVO: REVISAR MENSAJES PENDIENTES ---
                    // Si alguien le escribió mientras estaba desconectado, se los enviamos ahora
                    java.util.ArrayList<Mensaje> pendientes = mensajeDao.getMensajesPendientes(logueado.getUsername());
                    if (!pendientes.isEmpty()) {
                        servidor.log("Enviando " + pendientes.size() + " mensajes pendientes a " + logueado.getUsername());
                        for (Mensaje m : pendientes) {
                            enviar(new Peticion("RECIBIR_MENSAJE", m));
                        }
                        // Marcamos como entregados en BD
                        mensajeDao.marcarLeidos(logueado.getUsername());
                    }
                    // -------------------------------------------

                } else {
                     boolean bloqueado = dao.estaBloqueado(uLogin.getUsername());
                     int intentos = dao.obtenerIntentos(uLogin.getUsername());
                     
                     if (bloqueado) {
                         enviar(new Peticion("LOGIN_BLOQUEADO", "Tu cuenta ha sido bloqueada. Debes recuperar tu contraseña."));
                     } else if (intentos >= 3) {
                         enviar(new Peticion("LOGIN_BLOQUEADO", "Demasiados intentos fallidos. Debes recuperar tu contraseña."));
                     } else {
                         enviar(new Peticion("LOGIN_ERROR", "Credenciales incorrectas. Intentos: " + intentos + "/3"));
                     }
                }
                break;

            case "REGISTRO":
                Usuario uReg = (Usuario) p.getDatos();
                boolean registrado = dao.registrar(uReg);
                if (registrado) {
                    enviar(new Peticion("REGISTRO_OK", "Usuario creado con éxito"));
                    servidor.log("Nuevo registro: " + uReg.getUsername());
                } else {
                    enviar(new Peticion("REGISTRO_ERROR", "El usuario ya existe"));
                }
                break;
                
            case "RECUPERAR_CONTRASENA":
                String[] datosRecuperacion = (String[]) p.getDatos();
                String usernameRec = datosRecuperacion[0];
                String nuevaPass = datosRecuperacion[1];
                
                if (dao.existeUsuario(usernameRec)) {
                    if (dao.recuperarContrasena(usernameRec, nuevaPass)) {
                        enviar(new Peticion("RECUPERAR_OK", "Contraseña recuperada exitosamente"));
                        servidor.log("Contraseña recuperada para: " + usernameRec);
                    } else {
                        enviar(new Peticion("RECUPERAR_ERROR", "Error al recuperar contraseña"));
                    }
                } else {
                    enviar(new Peticion("RECUPERAR_ERROR", "Usuario no encontrado"));
                }
                break;

            // --- NUEVO CASO: ENVIAR MENSAJE ---
            case "ENVIAR_MENSAJE":
                Mensaje m = (Mensaje) p.getDatos();
                if (this.usuarioConectado != null) {
                    m.setRemitente(this.usuarioConectado.getUsername());
                }

                servidor.log("Mensaje de " + m.getRemitente() + " para " + m.getDestinatario());

                // --- CAMBIO IMPORTANTE: VALIDAR AMISTAD ---
                AmistadDAO amistadDao = new AmistadDAO();
                boolean sonAmigos = amistadDao.sonAmigos(m.getRemitente(), m.getDestinatario());
                
                boolean guardado = false;
                if (sonAmigos) {
                    // SOLO guardamos en BD si son amigos (Regla del proyecto)
                    guardado = mensajeDao.guardar(m);
                    servidor.log(" -> Son amigos. Mensaje guardado en historial.");
                } else {
                    servidor.log(" -> NO son amigos. Mensaje efímero (no guardado).");
                }
                // ------------------------------------------
                
                // Intentar entregar en vivo (siempre se intenta, sean amigos o no)
                boolean entregado = false;
                synchronized(Servidor.clientesConectados) {
                    for (HiloCliente cliente : Servidor.clientesConectados) {
                        if (cliente.getUsuarioConectado() != null && 
                            cliente.getUsuarioConectado().getUsername().equals(m.getDestinatario())) {
                            
                            cliente.enviar(new Peticion("RECIBIR_MENSAJE", m));
                            entregado = true;
                            break;
                        }
                    }
                }
                // Si son amigos pero NO se entregó (estaba desconectado), 
                // ya se guardó en el IF de arriba, así que lo recibirá al loguearse.
                // Si NO son amigos y NO se entregó, el mensaje se pierde (comportamiento correcto para "no historial").
                break;
                
            case "LISTAR_AMIGOS":
                if (this.usuarioConectado == null) break;
                // Llamamos al DAO y devolvemos la lista de objetos Usuario
                java.util.ArrayList<models.Usuario> misAmigos = new AmistadDAO().obtenerAmigos(usuarioConectado.getUsername());
                enviar(new Peticion("LISTA_AMIGOS_OK", misAmigos));
                break;

            case "LISTAR_SOLICITUDES":
                if (this.usuarioConectado == null) break;
                // Devolvemos la lista de Strings "usuario:id_solicitud"
                java.util.ArrayList<String> sols = new AmistadDAO().getSolicitudesPendientes(usuarioConectado.getUsername());
                enviar(new Peticion("LISTA_SOLICITUDES_OK", sols));
                break;
                
            case "ENVIAR_SOLICITUD": // (Mejoramos el que tenías o agregamos este)
                String destinoSol = (String) p.getDatos();
                if (new AmistadDAO().enviarSolicitud(usuarioConectado.getUsername(), destinoSol)) {
                    enviar(new Peticion("SOLICITUD_ENVIADA_OK", "Solicitud enviada a " + destinoSol));
                } else {
                    enviar(new Peticion("SOLICITUD_ERROR", "No se pudo enviar (ya son amigos o pendiente)."));
                }
                break;

            case "ACEPTAR_SOLICITUD":
                int idSol = (int) p.getDatos();
                if (new AmistadDAO().aceptarSolicitud(idSol)) {
                    enviar(new Peticion("ACEPTAR_SOLICITUD_OK", "Solicitud aceptada."));
                }
                break;
                
            case "PEDIR_HISTORIAL":
                String otroUsuario = (String) p.getDatos();
                if (usuarioConectado != null) {
                    java.util.ArrayList<Mensaje> historial = mensajeDao.obtenerHistorial(usuarioConectado.getUsername(), otroUsuario);
                    
                    // TRUCO: Enviamos un arreglo de objetos: [0] = nombre del amigo, [1] = la lista de mensajes
                    // Esto es necesario para que el Cliente sepa a qué ventana pertenece este historial
                    Object[] respuesta = { otroUsuario, historial };
                    enviar(new Peticion("HISTORIAL_OK", respuesta));
                }
                break;    

            // --- ZONA DE GRUPOS ---
            
            case "CREAR_GRUPO":
                // Formato de datos esperado: "TituloGrupo:usuario1,usuario2,usuario3"
                String rawData = (String) p.getDatos();
                String[] partes = rawData.split(":");
                String titulo = partes[0];
                String[] invitados = partes[1].split(",");
                
                GrupoDAO gDao = new GrupoDAO();
                int idNuevoGrupo = gDao.crearGrupo(titulo, usuarioConectado.getUsername());
                
                if (idNuevoGrupo != -1) {
                    // Invitamos a los usuarios indicados
                    for (String invitado : invitados) {
                        if (!invitado.trim().isEmpty()) {
                            gDao.invitarUsuario(idNuevoGrupo, invitado.trim());
                        }
                    }
                    enviar(new Peticion("CREAR_GRUPO_OK", "Grupo '" + titulo + "' creado. Esperando aceptación."));
                } else {
                    enviar(new Peticion("ERROR_GRUPO", "Error al crear el grupo."));
                }
                break;

            case "LISTAR_MIS_GRUPOS":
                enviar(new Peticion("LISTA_GRUPOS_OK", new GrupoDAO().getMisGrupos(usuarioConectado.getUsername())));
                break;
                
            case "LISTAR_INVITACIONES_GRUPO":
                enviar(new Peticion("INVITACIONES_GRUPO_OK", new GrupoDAO().getInvitacionesGrupos(usuarioConectado.getUsername())));
                break;
                
            case "ACEPTAR_GRUPO":
                int idG = (int) p.getDatos();
                if (new GrupoDAO().aceptarInvitacion(idG, usuarioConectado.getUsername())) {
                    enviar(new Peticion("ACEPTAR_GRUPO_OK", "Te has unido al grupo."));
                }
                break;
                
            case "ENVIAR_MENSAJE_GRUPO":
                Mensaje mGrupo = (Mensaje) p.getDatos(); 
                // Aseguramos quién lo envía
                mGrupo.setRemitente(usuarioConectado.getUsername());
                
                GrupoDAO grupoDao = new GrupoDAO();
                // 1. Guardar en BD
                grupoDao.guardarMensajeGrupo(mGrupo);
                
                // 2. Reenviar a todos los miembros conectados
                int idGrupoDestino = mGrupo.getId(); // Aquí viene el ID del grupo
                java.util.ArrayList<String> miembros = grupoDao.getMiembros(idGrupoDestino);
                
                synchronized(Servidor.clientesConectados) {
                    for (HiloCliente cliente : Servidor.clientesConectados) {
                        // Verificar si el cliente conectado es miembro del grupo y NO soy yo
                        if (cliente.getUsuarioConectado() != null && 
                            miembros.contains(cliente.getUsuarioConectado().getUsername()) &&
                            !cliente.getUsuarioConectado().getUsername().equals(usuarioConectado.getUsername())) {
                            
                            cliente.enviar(new Peticion("RECIBIR_MENSAJE_GRUPO", mGrupo));
                        }
                    }
                }
                break;
                
            case "PEDIR_HISTORIAL_GRUPO":
                int idGrupoHist = (int) p.getDatos();
                java.util.ArrayList<Mensaje> histGrupo = new GrupoDAO().getHistorialGrupo(idGrupoHist);
                // Enviamos un objeto compuesto: { ID_GRUPO, LISTA_MENSAJES }
                Object[] respGrupo = { idGrupoHist, histGrupo };
                enviar(new Peticion("HISTORIAL_GRUPO_OK", respGrupo));
                break;
                
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
    
    // Necesitamos esto para buscar destinatarios en la lista de conectados
    public Usuario getUsuarioConectado() {
        return usuarioConectado;
    }
}