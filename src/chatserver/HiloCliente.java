package chatserver;

import common.Peticion;
import dao.*;
import models.*;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.List;
import java.util.ArrayList;

public class HiloCliente extends Thread {
    
    private Socket socket;
    private Servidor servidor;
    private ObjectOutputStream salida;
    private ObjectInputStream entrada;
    private Usuario usuarioConectado;
    private boolean conectado = true;

    public HiloCliente(Socket socket, Servidor servidor) {
        this.socket = socket;
        this.servidor = servidor;
    }
    
    public Usuario getUsuarioConectado() {
        return usuarioConectado;
    }

    @Override
    public void run() {
        try {
            salida = new ObjectOutputStream(socket.getOutputStream());
            entrada = new ObjectInputStream(socket.getInputStream());

            while (conectado) {
                Peticion peticion = (Peticion) entrada.readObject();
                procesarPeticion(peticion);
            }

        } catch (Exception e) {
            if (usuarioConectado != null) {
                servidor.log("Cliente desconectado: " + usuarioConectado.getUsername());
                // Actualizar estado en BD
                UsuarioDAO dao = new UsuarioDAO();
                dao.actualizarEstado(usuarioConectado.getUsername(), 0);
            } else {
                servidor.log("Cliente desconectado: " + socket.getInetAddress());
            }
            Servidor.clientesConectados.remove(this);
            cerrarConexion();
        }
    }

    private void procesarPeticion(Peticion p) {
        UsuarioDAO usuarioDAO = new UsuarioDAO();

        switch (p.getAccion()) {
            case "LOGIN":
                Usuario uLogin = (Usuario) p.getDatos();
                servidor.log("Intento de login: " + uLogin.getUsername());

                Usuario logueado = usuarioDAO.login(uLogin.getUsername(), uLogin.getPassword());

                if (logueado != null) {
                    this.usuarioConectado = logueado;
                    servidor.log("Usuario LOGUEADO: " + logueado.getUsername());
                    
                    // Enviar mensajes pendientes al conectarse
                    MensajePendienteDAO mpDAO = new MensajePendienteDAO();
                    List<MensajePendiente> pendientes = mpDAO.obtenerMensajesPendientes(logueado.getPk_usuario());
                    if (!pendientes.isEmpty()) {
                        enviar(new Peticion("MENSAJES_PENDIENTES", pendientes));
                        mpDAO.eliminarMensajesPendientes(logueado.getPk_usuario());
                    }
                    
                    enviar(new Peticion("LOGIN_OK", logueado));
                } else {
                    boolean bloqueado = usuarioDAO.estaBloqueado(uLogin.getUsername());
                    int intentos = usuarioDAO.obtenerIntentos(uLogin.getUsername());
                    
                    if (bloqueado) {
                        enviar(new Peticion("LOGIN_BLOQUEADO", "Tu cuenta ha sido bloqueada. Debes recuperar tu contraseña."));
                        servidor.log("Usuario BLOQUEADO: " + uLogin.getUsername());
                    } else if (intentos >= 3) {
                        enviar(new Peticion("LOGIN_BLOQUEADO", "Demasiados intentos fallidos. Debes recuperar tu contraseña."));
                    } else {
                        enviar(new Peticion("LOGIN_ERROR", "Credenciales incorrectas. Intentos: " + intentos + "/3"));
                        servidor.log("Fallo de credenciales: " + uLogin.getUsername() + " (Intentos: " + intentos + ")");
                    }
                }
                break;

            case "REGISTRO":
                Usuario uReg = (Usuario) p.getDatos();
                boolean registrado = usuarioDAO.registrar(uReg);
                if (registrado) {
                    enviar(new Peticion("REGISTRO_OK", "Usuario creado con éxito"));
                    servidor.log("Nuevo usuario registrado: " + uReg.getUsername());
                } else {
                    enviar(new Peticion("REGISTRO_ERROR", "El usuario ya existe"));
                }
                break;
                
            case "RECUPERAR_CONTRASENA":
                String[] datosRecuperacion = (String[]) p.getDatos();
                String usernameRec = datosRecuperacion[0];
                String nuevaPass = datosRecuperacion[1];
                
                if (usuarioDAO.existeUsuario(usernameRec)) {
                    if (usuarioDAO.recuperarContrasena(usernameRec, nuevaPass)) {
                        enviar(new Peticion("RECUPERAR_OK", "Contraseña recuperada exitosamente"));
                        servidor.log("Contraseña recuperada para: " + usernameRec);
                    } else {
                        enviar(new Peticion("RECUPERAR_ERROR", "Error al recuperar contraseña"));
                    }
                } else {
                    enviar(new Peticion("RECUPERAR_ERROR", "Usuario no encontrado"));
                }
                break;
                
            case "OBTENER_USUARIOS":
                if (usuarioConectado == null) break;
                List<Usuario> todosUsuarios = usuarioDAO.obtenerTodosUsuarios();
                enviar(new Peticion("LISTA_USUARIOS", todosUsuarios));
                servidor.log("Lista de usuarios enviada a: " + usuarioConectado.getUsername());
                break;
                
            case "OBTENER_AMIGOS":
                if (usuarioConectado == null) break;
                AmistadDAO amistadDAO = new AmistadDAO();
                List<Amistad> amigos = amistadDAO.obtenerAmigos(usuarioConectado.getPk_usuario());
                enviar(new Peticion("LISTA_AMIGOS", amigos));
                break;
                
            case "ENVIAR_SOLICITUD_AMISTAD":
                if (usuarioConectado == null) break;
                int idDestinatario = (Integer) p.getDatos();
                Amistad solicitud = new Amistad(usuarioConectado.getPk_usuario(), idDestinatario);
                AmistadDAO amistadDAO2 = new AmistadDAO();
                if (amistadDAO2.enviarSolicitud(solicitud)) {
                    enviar(new Peticion("SOLICITUD_ENVIADA", "Solicitud de amistad enviada"));
                    servidor.log(usuarioConectado.getUsername() + " envió solicitud a usuario ID: " + idDestinatario);
                    
                    // Notificar al destinatario si está conectado
                    notificarUsuario(idDestinatario, "NUEVA_SOLICITUD_AMISTAD", null);
                } else {
                    enviar(new Peticion("SOLICITUD_ERROR", "No se pudo enviar la solicitud"));
                }
                break;
                
            case "ACEPTAR_SOLICITUD_AMISTAD":
                if (usuarioConectado == null) break;
                int pkAmistad = (Integer) p.getDatos();
                AmistadDAO amistadDAO3 = new AmistadDAO();
                if (amistadDAO3.aceptarSolicitud(pkAmistad)) {
                    enviar(new Peticion("SOLICITUD_ACEPTADA", "Solicitud aceptada"));
                    servidor.log(usuarioConectado.getUsername() + " aceptó una solicitud de amistad");
                } else {
                    enviar(new Peticion("SOLICITUD_ERROR", "Error al aceptar solicitud"));
                }
                break;
                
            case "RECHAZAR_SOLICITUD_AMISTAD":
                if (usuarioConectado == null) break;
                int pkAmistadRech = (Integer) p.getDatos();
                AmistadDAO amistadDAO4 = new AmistadDAO();
                if (amistadDAO4.rechazarSolicitud(pkAmistadRech)) {
                    enviar(new Peticion("SOLICITUD_RECHAZADA", "Solicitud rechazada"));
                } else {
                    enviar(new Peticion("SOLICITUD_ERROR", "Error al rechazar solicitud"));
                }
                break;
                
            case "OBTENER_SOLICITUDES":
                if (usuarioConectado == null) break;
                AmistadDAO amistadDAO5 = new AmistadDAO();
                List<Amistad> solicitudes = amistadDAO5.obtenerSolicitudesPendientes(usuarioConectado.getPk_usuario());
                enviar(new Peticion("LISTA_SOLICITUDES", solicitudes));
                break;
                
            case "ENVIAR_MENSAJE_PRIVADO":
                if (usuarioConectado == null) break;
                Mensaje mensaje = (Mensaje) p.getDatos();
                mensaje.setFk_remitente(usuarioConectado.getPk_usuario());
                
                // Verificar que sean amigos
                AmistadDAO amistadDAO6 = new AmistadDAO();
                if (amistadDAO6.sonAmigos(usuarioConectado.getPk_usuario(), mensaje.getFk_destinatario())) {
                    // Guardar en historial (solo entre amigos)
                    MensajeDAO mensajeDAO = new MensajeDAO();
                    mensajeDAO.guardarMensaje(mensaje);
                    
                    // Enviar al destinatario si está conectado
                    boolean enviado = notificarUsuario(mensaje.getFk_destinatario(), "MENSAJE_PRIVADO", mensaje);
                    
                    if (!enviado) {
                        // Si no está conectado, guardar como pendiente
                        MensajePendienteDAO mpDAO2 = new MensajePendienteDAO();
                        MensajePendiente mp = new MensajePendiente();
                        mp.setFk_usuario(mensaje.getFk_destinatario());
                        mp.setFk_remitente(mensaje.getFk_remitente());
                        mp.setTipo("privado");
                        mp.setMensaje(mensaje.getMensaje());
                        mpDAO2.guardarMensajePendiente(mp);
                    }
                    
                    enviar(new Peticion("MENSAJE_ENVIADO", "Mensaje enviado"));
                    servidor.log(usuarioConectado.getUsername() + " envió mensaje a usuario ID: " + mensaje.getFk_destinatario());
                } else {
                    enviar(new Peticion("MENSAJE_ERROR", "Solo puedes enviar mensajes a tus amigos"));
                }
                break;
                
            case "OBTENER_HISTORIAL":
                if (usuarioConectado == null) break;
                int idOtroUsuario = (Integer) p.getDatos();
                MensajeDAO mensajeDAO2 = new MensajeDAO();
                List<Mensaje> historial = mensajeDAO2.obtenerHistorial(usuarioConectado.getPk_usuario(), idOtroUsuario);
                enviar(new Peticion("HISTORIAL_MENSAJES", historial));
                break;
                
            case "CREAR_GRUPO":
                if (usuarioConectado == null) break;
                Grupo grupo = (Grupo) p.getDatos();
                grupo.setFk_creador(usuarioConectado.getPk_usuario());
                GrupoDAO grupoDAO = new GrupoDAO();
                int pkGrupo = grupoDAO.crearGrupo(grupo);
                if (pkGrupo > 0) {
                    grupo.setPk_grupo(pkGrupo);
                    enviar(new Peticion("GRUPO_CREADO", grupo));
                    servidor.log(usuarioConectado.getUsername() + " creó el grupo: " + grupo.getTitulo());
                } else {
                    enviar(new Peticion("GRUPO_ERROR", "Error al crear grupo"));
                }
                break;
                
            case "INVITAR_A_GRUPO":
                if (usuarioConectado == null) break;
                Object[] datosInvitacion = (Object[]) p.getDatos();
                int idGrupo = (Integer) datosInvitacion[0];
                int idInvitado = (Integer) datosInvitacion[1];
                
                GrupoDAO grupoDAO2 = new GrupoDAO();
                if (!grupoDAO2.esCreador(idGrupo, usuarioConectado.getPk_usuario())) {
                    enviar(new Peticion("INVITACION_ERROR", "Solo el creador puede invitar"));
                    break;
                }
                
                InvitacionGrupoDAO invDAO = new InvitacionGrupoDAO();
                InvitacionGrupo invitacion = new InvitacionGrupo(idGrupo, idInvitado, usuarioConectado.getPk_usuario());
                if (invDAO.crearInvitacion(invitacion)) {
                    enviar(new Peticion("INVITACION_ENVIADA", "Invitación enviada"));
                    servidor.log(usuarioConectado.getUsername() + " invitó a usuario ID: " + idInvitado + " al grupo ID: " + idGrupo);
                    
                    // Notificar al invitado
                    notificarUsuario(idInvitado, "NUEVA_INVITACION_GRUPO", invitacion);
                } else {
                    enviar(new Peticion("INVITACION_ERROR", "Error al enviar invitación"));
                }
                break;
                
            case "ACEPTAR_INVITACION_GRUPO":
                if (usuarioConectado == null) break;
                int pkInvitacion = (Integer) p.getDatos();
                InvitacionGrupoDAO invDAO2 = new InvitacionGrupoDAO();
                List<InvitacionGrupo> invitacionesPend = invDAO2.obtenerInvitacionesPendientes(usuarioConectado.getPk_usuario());
                InvitacionGrupo inv = null;
                for (InvitacionGrupo i : invitacionesPend) {
                    if (i.getPk_invitacion() == pkInvitacion) {
                        inv = i;
                        break;
                    }
                }
                
                if (inv != null && invDAO2.aceptarInvitacion(pkInvitacion)) {
                    GrupoDAO grupoDAO3 = new GrupoDAO();
                    grupoDAO3.agregarMiembro(inv.getFk_grupo(), usuarioConectado.getPk_usuario());
                    
                    // Verificar si el grupo ahora es válido (3+ miembros)
                    int miembros = grupoDAO3.contarMiembros(inv.getFk_grupo());
                    int aceptadas = grupoDAO3.contarInvitacionesAceptadas(inv.getFk_grupo());
                    int pendientes = grupoDAO3.contarInvitacionesPendientes(inv.getFk_grupo());
                    
                    if (miembros + aceptadas + pendientes >= 3) {
                        enviar(new Peticion("INVITACION_ACEPTADA", "Invitación aceptada. Grupo válido."));
                        servidor.log(usuarioConectado.getUsername() + " aceptó invitación al grupo ID: " + inv.getFk_grupo());
                    } else {
                        // Si no alcanza 3, eliminar grupo
                        grupoDAO3.eliminarGrupo(inv.getFk_grupo());
                        enviar(new Peticion("GRUPO_ELIMINADO", "El grupo fue eliminado por no alcanzar el mínimo de miembros"));
                        servidor.log("Grupo ID: " + inv.getFk_grupo() + " eliminado por falta de miembros");
                    }
                } else {
                    enviar(new Peticion("INVITACION_ERROR", "Error al aceptar invitación"));
                }
                break;
                
            case "RECHAZAR_INVITACION_GRUPO":
                if (usuarioConectado == null) break;
                int pkInvitacionRech = (Integer) p.getDatos();
                InvitacionGrupoDAO invDAO3 = new InvitacionGrupoDAO();
                List<InvitacionGrupo> invitacionesPend2 = invDAO3.obtenerInvitacionesPendientes(usuarioConectado.getPk_usuario());
                InvitacionGrupo invRech = null;
                for (InvitacionGrupo i : invitacionesPend2) {
                    if (i.getPk_invitacion() == pkInvitacionRech) {
                        invRech = i;
                        break;
                    }
                }
                
                if (invRech != null && invDAO3.rechazarInvitacion(pkInvitacionRech)) {
                    // Verificar si era el 3er elemento y eliminar grupo si es necesario
                    GrupoDAO grupoDAO4 = new GrupoDAO();
                    int miembros = grupoDAO4.contarMiembros(invRech.getFk_grupo());
                    int aceptadas = grupoDAO4.contarInvitacionesAceptadas(invRech.getFk_grupo());
                    int pendientes = grupoDAO4.contarInvitacionesPendientes(invRech.getFk_grupo());
                    
                    if (miembros + aceptadas + pendientes < 3) {
                        grupoDAO4.eliminarGrupo(invRech.getFk_grupo());
                        enviar(new Peticion("GRUPO_ELIMINADO", "El grupo fue eliminado"));
                        servidor.log("Grupo ID: " + invRech.getFk_grupo() + " eliminado por rechazo de invitación");
                    } else {
                        enviar(new Peticion("INVITACION_RECHAZADA", "Invitación rechazada"));
                    }
                } else {
                    enviar(new Peticion("INVITACION_ERROR", "Error al rechazar invitación"));
                }
                break;
                
            case "OBTENER_INVITACIONES_GRUPO":
                if (usuarioConectado == null) break;
                InvitacionGrupoDAO invDAO4 = new InvitacionGrupoDAO();
                List<InvitacionGrupo> invitaciones = invDAO4.obtenerInvitacionesPendientes(usuarioConectado.getPk_usuario());
                enviar(new Peticion("LISTA_INVITACIONES_GRUPO", invitaciones));
                break;
                
            case "OBTENER_GRUPOS":
                if (usuarioConectado == null) break;
                GrupoDAO grupoDAO5 = new GrupoDAO();
                List<Grupo> grupos = grupoDAO5.obtenerGruposUsuario(usuarioConectado.getPk_usuario());
                enviar(new Peticion("LISTA_GRUPOS", grupos));
                break;
                
            case "ENVIAR_MENSAJE_GRUPO":
                if (usuarioConectado == null) break;
                MensajeGrupo mensajeGrupo = (MensajeGrupo) p.getDatos();
                mensajeGrupo.setFk_remitente(usuarioConectado.getPk_usuario());
                
                GrupoDAO grupoDAO6 = new GrupoDAO();
                if (grupoDAO6.esMiembro(mensajeGrupo.getFk_grupo(), usuarioConectado.getPk_usuario())) {
                    MensajeGrupoDAO mgDAO = new MensajeGrupoDAO();
                    mgDAO.guardarMensaje(mensajeGrupo);
                    
                    // Enviar a todos los miembros del grupo
                    List<Usuario> miembros = grupoDAO6.obtenerMiembros(mensajeGrupo.getFk_grupo());
                    for (Usuario miembro : miembros) {
                        if (miembro.getPk_usuario() != usuarioConectado.getPk_usuario()) {
                            if (miembro.getEstado() == 1) {
                                // Usuario conectado, enviar directamente
                                notificarUsuario(miembro.getPk_usuario(), "MENSAJE_GRUPO", mensajeGrupo);
                            } else {
                                // Usuario desconectado, guardar como pendiente
                                MensajePendienteDAO mpDAO3 = new MensajePendienteDAO();
                                MensajePendiente mp = new MensajePendiente();
                                mp.setFk_usuario(miembro.getPk_usuario());
                                mp.setFk_grupo(mensajeGrupo.getFk_grupo());
                                mp.setFk_remitente(mensajeGrupo.getFk_remitente());
                                mp.setTipo("grupo");
                                mp.setMensaje(mensajeGrupo.getMensaje());
                                mpDAO3.guardarMensajePendiente(mp);
                            }
                        }
                    }
                    
                    enviar(new Peticion("MENSAJE_ENVIADO", "Mensaje enviado al grupo"));
                    servidor.log(usuarioConectado.getUsername() + " envió mensaje al grupo ID: " + mensajeGrupo.getFk_grupo());
                } else {
                    enviar(new Peticion("MENSAJE_ERROR", "No eres miembro de este grupo"));
                }
                break;
                
            case "OBTENER_HISTORIAL_GRUPO":
                if (usuarioConectado == null) break;
                int idGrupoHistorial = (Integer) p.getDatos();
                MensajeGrupoDAO mgDAO2 = new MensajeGrupoDAO();
                List<MensajeGrupo> historialGrupo = mgDAO2.obtenerHistorial(idGrupoHistorial);
                enviar(new Peticion("HISTORIAL_GRUPO", historialGrupo));
                break;
                
            case "SALIR_GRUPO":
                if (usuarioConectado == null) break;
                int idGrupoSalir = (Integer) p.getDatos();
                GrupoDAO grupoDAO7 = new GrupoDAO();
                
                if (grupoDAO7.esCreador(idGrupoSalir, usuarioConectado.getPk_usuario())) {
                    // Si es el creador, eliminar el grupo
                    grupoDAO7.eliminarGrupo(idGrupoSalir);
                    enviar(new Peticion("GRUPO_ELIMINADO", "Grupo eliminado (creador salió)"));
                    servidor.log(usuarioConectado.getUsername() + " salió y eliminó el grupo ID: " + idGrupoSalir);
                } else {
                    // Si no es creador, solo salir
                    grupoDAO7.eliminarMiembro(idGrupoSalir, usuarioConectado.getPk_usuario());
                    enviar(new Peticion("SALIO_GRUPO", "Saliste del grupo"));
                    servidor.log(usuarioConectado.getUsername() + " salió del grupo ID: " + idGrupoSalir);
                }
                break;
                
            case "ELIMINAR_MIEMBRO_GRUPO":
                if (usuarioConectado == null) break;
                Object[] datosEliminar = (Object[]) p.getDatos();
                int idGrupoElim = (Integer) datosEliminar[0];
                int idMiembroElim = (Integer) datosEliminar[1];
                
                GrupoDAO grupoDAO8 = new GrupoDAO();
                if (grupoDAO8.esCreador(idGrupoElim, usuarioConectado.getPk_usuario())) {
                    grupoDAO8.eliminarMiembro(idGrupoElim, idMiembroElim);
                    enviar(new Peticion("MIEMBRO_ELIMINADO", "Miembro eliminado"));
                    servidor.log(usuarioConectado.getUsername() + " eliminó miembro ID: " + idMiembroElim + " del grupo ID: " + idGrupoElim);
                } else {
                    enviar(new Peticion("PERMISO_ERROR", "Solo el creador puede eliminar miembros"));
                }
                break;
                
            default:
                servidor.log("Acción desconocida: " + p.getAccion());
        }
    }
    
    private boolean notificarUsuario(int usuarioId, String accion, Object datos) {
        for (HiloCliente cliente : Servidor.clientesConectados) {
            if (cliente.getUsuarioConectado() != null && 
                cliente.getUsuarioConectado().getPk_usuario() == usuarioId) {
                cliente.enviar(new Peticion(accion, datos));
                return true;
            }
        }
        return false;
    }

    public void enviar(Peticion p) {
        try {
            salida.writeObject(p);
            salida.flush();
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
