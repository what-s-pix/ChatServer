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
                    MensajePendienteDAO mpDAO = new MensajePendienteDAO();
                    List<MensajePendiente> pendientes = mpDAO.obtenerMensajesPendientes(logueado.getPk_usuario());
                    if (!pendientes.isEmpty()) {
                        enviar(new Peticion("MENSAJES_PENDIENTES", pendientes));
                        mpDAO.eliminarMensajesPendientes(logueado.getPk_usuario());
                    }
                    enviar(new Peticion("LOGIN_OK", logueado));
                } else {
                    // Obtener intentos DESPUÉS de que login() los haya incrementado
                    int intentos = usuarioDAO.obtenerIntentos(uLogin.getUsername());
                    boolean bloqueado = usuarioDAO.estaBloqueado(uLogin.getUsername());
                    if (bloqueado) {
                        enviar(new Peticion("LOGIN_BLOQUEADO", "Tu cuenta ha sido bloqueada. Debes recuperar tu contraseña."));
                        servidor.log("Usuario BLOQUEADO: " + uLogin.getUsername() + " (Intentos: " + intentos + ")");
                    } else if (intentos >= 3) {
                        enviar(new Peticion("LOGIN_BLOQUEADO", "Has alcanzado el límite de intentos. Debes recuperar tu contraseña."));
                        servidor.log("Usuario BLOQUEADO por límite de intentos: " + uLogin.getUsername() + " (Intentos: " + intentos + ")");
                    } else {
                        enviar(new Peticion("LOGIN_ERROR", "Credenciales incorrectas. Intentos: " + intentos + "/3"));
                        servidor.log("Fallo de credenciales: " + uLogin.getUsername() + " (Intentos: " + intentos + ")");
                    }
                }
                break;
            case "REGISTRO":
                Usuario uReg = (Usuario) p.getDatos();
                if (uReg.getUsername() == null || uReg.getUsername().trim().isEmpty()) {
                    enviar(new Peticion("REGISTRO_ERROR", "El nombre de usuario no puede estar vacío"));
                    break;
                }
                if (uReg.getPassword() == null || uReg.getPassword().trim().isEmpty()) {
                    enviar(new Peticion("REGISTRO_ERROR", "La contraseña no puede estar vacía"));
                    break;
                }
                boolean registrado = usuarioDAO.registrar(uReg);
                if (registrado) {
                    enviar(new Peticion("REGISTRO_OK", "Usuario creado con éxito"));
                    servidor.log("Nuevo usuario registrado: " + uReg.getUsername());
                } else {
                    enviar(new Peticion("REGISTRO_ERROR", "El usuario '" + uReg.getUsername() + "' ya existe. Por favor, elige otro nombre de usuario."));
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
            case "LISTAR_AMIGOS":
                if (usuarioConectado == null) break;
                AmistadDAO amistadDAO = new AmistadDAO();
                List<Amistad> amigos = amistadDAO.obtenerAmigos(usuarioConectado.getPk_usuario());
                enviar(new Peticion("LISTA_AMIGOS_OK", amigos));
                break;
            case "ENVIAR_SOLICITUD_AMISTAD":
            case "ENVIAR_SOLICITUD":
                if (usuarioConectado == null) break;
                Object datosSolicitud = p.getDatos();
                int idDestinatario;
                if (datosSolicitud instanceof Integer) {
                    idDestinatario = (Integer) datosSolicitud;
                } else if (datosSolicitud instanceof String) {
                    // Si es un username, obtener el ID
                    UsuarioDAO usuarioDAO5 = new UsuarioDAO();
                    Usuario destinatario = usuarioDAO5.obtenerUsuarioPorUsername((String) datosSolicitud);
                    if (destinatario == null) {
                        enviar(new Peticion("SOLICITUD_ERROR", "Usuario no encontrado"));
                        break;
                    }
                    idDestinatario = destinatario.getPk_usuario();
                } else {
                    enviar(new Peticion("SOLICITUD_ERROR", "Datos inválidos"));
                    break;
                }
                Amistad solicitud = new Amistad(usuarioConectado.getPk_usuario(), idDestinatario);
                AmistadDAO amistadDAO2 = new AmistadDAO();
                if (amistadDAO2.enviarSolicitud(solicitud)) {
                    enviar(new Peticion("SOLICITUD_ENVIADA_OK", "Solicitud de amistad enviada"));
                    servidor.log(usuarioConectado.getUsername() + " envió solicitud a usuario ID: " + idDestinatario);
                    notificarUsuario(idDestinatario, "NUEVA_SOLICITUD_AMISTAD", null);
                } else {
                    enviar(new Peticion("SOLICITUD_ERROR", "No se pudo enviar la solicitud"));
                }
                break;
            case "ACEPTAR_SOLICITUD_AMISTAD":
            case "ACEPTAR_SOLICITUD":
                if (usuarioConectado == null) break;
                int pkAmistad = (Integer) p.getDatos();
                AmistadDAO amistadDAO3 = new AmistadDAO();
                if (amistadDAO3.aceptarSolicitud(pkAmistad)) {
                    enviar(new Peticion("ACEPTAR_SOLICITUD_OK", "Solicitud aceptada"));
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
            case "LISTAR_SOLICITUDES":
                if (usuarioConectado == null) break;
                AmistadDAO amistadDAO5 = new AmistadDAO();
                List<Amistad> solicitudes = amistadDAO5.obtenerSolicitudesPendientes(usuarioConectado.getPk_usuario());
                // Convertir a formato esperado por el cliente
                java.util.ArrayList<String> solicitudesFormato = new java.util.ArrayList<>();
                for (Amistad a : solicitudes) {
                    UsuarioDAO usuarioDAO4 = new UsuarioDAO();
                    Usuario solicitante = usuarioDAO4.obtenerUsuarioPorId(a.getFk_usuario1() == usuarioConectado.getPk_usuario() ? a.getFk_usuario2() : a.getFk_usuario1());
                    if (solicitante != null) {
                        solicitudesFormato.add(solicitante.getUsername() + ":" + a.getPk_amistad());
                    }
                }
                enviar(new Peticion("LISTA_SOLICITUDES_OK", solicitudesFormato));
                break;
            case "ENVIAR_MENSAJE_PRIVADO":
                if (usuarioConectado == null) break;
                Mensaje mensaje = (Mensaje) p.getDatos();
                mensaje.setFk_remitente(usuarioConectado.getPk_usuario());
                AmistadDAO amistadDAO6 = new AmistadDAO();
                if (amistadDAO6.sonAmigos(usuarioConectado.getPk_usuario(), mensaje.getFk_destinatario())) {
                    MensajeDAO mensajeDAO = new MensajeDAO();
                    mensajeDAO.guardarMensaje(mensaje);
                    mensaje.setNombreRemitente(usuarioConectado.getUsername());
                    boolean enviado = notificarUsuario(mensaje.getFk_destinatario(), "RECIBIR_MENSAJE", mensaje);
                    if (!enviado) {
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
            case "ENVIAR_MENSAJE":
                if (usuarioConectado == null) break;
                Mensaje mensajeSimple = (Mensaje) p.getDatos();
                mensajeSimple.setFk_remitente(usuarioConectado.getPk_usuario());
                mensajeSimple.setNombreRemitente(usuarioConectado.getUsername());
                if (mensajeSimple.getFk_destinatario() == 0 && mensajeSimple.getNombreDestinatario() != null) {
                    UsuarioDAO usuarioDAO2 = new UsuarioDAO();
                    Usuario destinatario = usuarioDAO2.obtenerUsuarioPorUsername(mensajeSimple.getNombreDestinatario());
                    if (destinatario != null) {
                        mensajeSimple.setFk_destinatario(destinatario.getPk_usuario());
                    } else {
                        enviar(new Peticion("MENSAJE_ERROR", "Usuario destinatario no encontrado: " + mensajeSimple.getNombreDestinatario()));
                        break;
                    }
                } else if (mensajeSimple.getFk_destinatario() == 0) {
                    enviar(new Peticion("MENSAJE_ERROR", "Destinatario no especificado"));
                    break;
                }
                AmistadDAO amistadDAO7 = new AmistadDAO();
                boolean sonAmigos2 = amistadDAO7.sonAmigos(usuarioConectado.getPk_usuario(), mensajeSimple.getFk_destinatario());
                if (sonAmigos2) {
                    MensajeDAO mensajeDAO3 = new MensajeDAO();
                    mensajeDAO3.guardarMensaje(mensajeSimple);
                    servidor.log(" -> Son amigos. Mensaje guardado en historial.");
                } else {
                    servidor.log(" -> NO son amigos. Mensaje efímero (no guardado).");
                }
                boolean entregado2 = notificarUsuario(mensajeSimple.getFk_destinatario(), "RECIBIR_MENSAJE", mensajeSimple);
                if (!entregado2 && sonAmigos2) {
                    MensajePendienteDAO mpDAO4 = new MensajePendienteDAO();
                    MensajePendiente mp2 = new MensajePendiente();
                    mp2.setFk_usuario(mensajeSimple.getFk_destinatario());
                    mp2.setFk_remitente(mensajeSimple.getFk_remitente());
                    mp2.setTipo("privado");
                    mp2.setMensaje(mensajeSimple.getMensaje());
                    mpDAO4.guardarMensajePendiente(mp2);
                }
                servidor.log(usuarioConectado.getUsername() + " envió mensaje para usuario ID: " + mensajeSimple.getFk_destinatario());
                break;
            case "OBTENER_HISTORIAL":
            case "PEDIR_HISTORIAL":
                if (usuarioConectado == null) break;
                Object datosHistorial = p.getDatos();
                int idOtroUsuario;
                if (datosHistorial instanceof Integer) {
                    idOtroUsuario = (Integer) datosHistorial;
                } else {
                    // Si es un String (username), obtener el ID
                    UsuarioDAO usuarioDAO3 = new UsuarioDAO();
                    Usuario otroUsuario = usuarioDAO3.obtenerUsuarioPorUsername((String) datosHistorial);
                    if (otroUsuario == null) break;
                    idOtroUsuario = otroUsuario.getPk_usuario();
                }
                MensajeDAO mensajeDAO2 = new MensajeDAO();
                List<Mensaje> historial = mensajeDAO2.obtenerHistorial(usuarioConectado.getPk_usuario(), idOtroUsuario);
                Object[] paquete = {(String) (datosHistorial instanceof String ? datosHistorial : null), historial};
                enviar(new Peticion("HISTORIAL_OK", paquete));
                break;
            case "CREAR_GRUPO":
                if (usuarioConectado == null) break;
                Object datosGrupo = p.getDatos();
                Grupo grupo;
                if (datosGrupo instanceof Grupo) {
                    grupo = (Grupo) datosGrupo;
                } else if (datosGrupo instanceof String) {
                    // Formato: "titulo:invitados"
                    String[] partes = ((String) datosGrupo).split(":", 2);
                    String titulo = partes[0];
                    String invitadosStr = partes.length > 1 ? partes[1] : "";
                    grupo = new Grupo(titulo, usuarioConectado.getPk_usuario());
                    // Procesar invitados si existen
                    if (!invitadosStr.trim().isEmpty()) {
                        String[] usernames = invitadosStr.split(",");
                        InvitacionGrupoDAO invDAO5 = new InvitacionGrupoDAO();
                        UsuarioDAO usuarioDAO6 = new UsuarioDAO();
                        for (String username : usernames) {
                            username = username.trim();
                            if (!username.isEmpty()) {
                                Usuario invitado = usuarioDAO6.obtenerUsuarioPorUsername(username);
                                if (invitado != null && invitado.getPk_usuario() != usuarioConectado.getPk_usuario()) {
                                    // Se enviará la invitación después de crear el grupo
                                }
                            }
                        }
                    }
                } else {
                    enviar(new Peticion("GRUPO_ERROR", "Datos inválidos"));
                    break;
                }
                grupo.setFk_creador(usuarioConectado.getPk_usuario());
                GrupoDAO grupoDAO = new GrupoDAO();
                int pkGrupo = grupoDAO.crearGrupo(grupo);
                if (pkGrupo > 0) {
                    grupo.setPk_grupo(pkGrupo);
                    // Enviar invitaciones si se proporcionaron
                    if (datosGrupo instanceof String) {
                        String[] partes = ((String) datosGrupo).split(":", 2);
                        String invitadosStr = partes.length > 1 ? partes[1] : "";
                        if (!invitadosStr.trim().isEmpty()) {
                            String[] usernames = invitadosStr.split(",");
                            InvitacionGrupoDAO invDAO6 = new InvitacionGrupoDAO();
                            UsuarioDAO usuarioDAO7 = new UsuarioDAO();
                            for (String username : usernames) {
                                username = username.trim();
                                if (!username.isEmpty()) {
                                    Usuario invitado = usuarioDAO7.obtenerUsuarioPorUsername(username);
                                    if (invitado != null && invitado.getPk_usuario() != usuarioConectado.getPk_usuario()) {
                                        InvitacionGrupo invitacion = new InvitacionGrupo(pkGrupo, invitado.getPk_usuario(), usuarioConectado.getPk_usuario());
                                        invDAO6.crearInvitacion(invitacion);
                                        notificarUsuario(invitado.getPk_usuario(), "NUEVA_INVITACION_GRUPO", invitacion);
                                    }
                                }
                            }
                        }
                    }
                    enviar(new Peticion("CREAR_GRUPO_OK", "Grupo creado exitosamente"));
                    servidor.log(usuarioConectado.getUsername() + " creó el grupo: " + grupo.getTitulo());
                } else {
                    enviar(new Peticion("ERROR_GRUPO", "Error al crear grupo"));
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
                    notificarUsuario(idInvitado, "NUEVA_INVITACION_GRUPO", invitacion);
                } else {
                    enviar(new Peticion("INVITACION_ERROR", "Error al enviar invitación"));
                }
                break;
            case "ACEPTAR_INVITACION_GRUPO":
            case "ACEPTAR_GRUPO":
                if (usuarioConectado == null) break;
                Object datosAceptar = p.getDatos();
                int pkInvitacion;
                if (datosAceptar instanceof Integer) {
                    pkInvitacion = (Integer) datosAceptar;
                } else {
                    // Si es un ID de grupo, buscar la invitación correspondiente
                    int idGrupoAceptar = (Integer) datosAceptar;
                    InvitacionGrupoDAO invDAO7 = new InvitacionGrupoDAO();
                    List<InvitacionGrupo> todasInvitaciones = invDAO7.obtenerInvitacionesPendientes(usuarioConectado.getPk_usuario());
                    InvitacionGrupo invEncontrada = null;
                    for (InvitacionGrupo i : todasInvitaciones) {
                        if (i.getFk_grupo() == idGrupoAceptar) {
                            invEncontrada = i;
                            break;
                        }
                    }
                    if (invEncontrada == null) {
                        enviar(new Peticion("INVITACION_ERROR", "Invitación no encontrada"));
                        break;
                    }
                    pkInvitacion = invEncontrada.getPk_invitacion();
                }
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
                    int miembros = grupoDAO3.contarMiembros(inv.getFk_grupo());
                    int aceptadas = grupoDAO3.contarInvitacionesAceptadas(inv.getFk_grupo());
                    int pendientes = grupoDAO3.contarInvitacionesPendientes(inv.getFk_grupo());
                    if (miembros + aceptadas + pendientes >= 3) {
                        enviar(new Peticion("ACEPTAR_GRUPO_OK", "Invitación aceptada. Grupo válido."));
                        servidor.log(usuarioConectado.getUsername() + " aceptó invitación al grupo ID: " + inv.getFk_grupo());
                    } else {
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
            case "LISTAR_INVITACIONES_GRUPO":
                if (usuarioConectado == null) break;
                InvitacionGrupoDAO invDAO4 = new InvitacionGrupoDAO();
                List<InvitacionGrupo> invitaciones = invDAO4.obtenerInvitacionesPendientes(usuarioConectado.getPk_usuario());
                // Convertir a formato esperado por el cliente
                java.util.ArrayList<Grupo> gruposInvitaciones = new java.util.ArrayList<>();
                GrupoDAO grupoDAO9 = new GrupoDAO();
                for (InvitacionGrupo invItem : invitaciones) {
                    Grupo g = grupoDAO9.obtenerGrupo(invItem.getFk_grupo());
                    if (g != null) {
                        gruposInvitaciones.add(g);
                    }
                }
                enviar(new Peticion("INVITACIONES_GRUPO_OK", gruposInvitaciones));
                break;
            case "OBTENER_GRUPOS":
            case "LISTAR_MIS_GRUPOS":
                if (usuarioConectado == null) break;
                GrupoDAO grupoDAO5 = new GrupoDAO();
                List<Grupo> grupos = grupoDAO5.obtenerGruposUsuario(usuarioConectado.getPk_usuario());
                enviar(new Peticion("LISTA_GRUPOS_OK", grupos));
                break;
            case "ENVIAR_MENSAJE_GRUPO":
                if (usuarioConectado == null) break;
                MensajeGrupo mensajeGrupo = (MensajeGrupo) p.getDatos();
                mensajeGrupo.setFk_remitente(usuarioConectado.getPk_usuario());
                mensajeGrupo.setNombreRemitente(usuarioConectado.getUsername());
                GrupoDAO grupoDAO6 = new GrupoDAO();
                if (grupoDAO6.esMiembro(mensajeGrupo.getFk_grupo(), usuarioConectado.getPk_usuario())) {
                    MensajeGrupoDAO mgDAO = new MensajeGrupoDAO();
                    mgDAO.guardarMensaje(mensajeGrupo);
                    List<Usuario> miembros = grupoDAO6.obtenerMiembros(mensajeGrupo.getFk_grupo());
                    for (Usuario miembro : miembros) {
                        if (miembro.getPk_usuario() != usuarioConectado.getPk_usuario()) {
                            if (miembro.getEstado() == 1) {
                                notificarUsuario(miembro.getPk_usuario(), "MENSAJE_GRUPO", mensajeGrupo);
                            } else {
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
            case "PEDIR_HISTORIAL_GRUPO":
                if (usuarioConectado == null) break;
                int idGrupoHistorial = (Integer) p.getDatos();
                MensajeGrupoDAO mgDAO2 = new MensajeGrupoDAO();
                List<MensajeGrupo> historialGrupo = mgDAO2.obtenerHistorial(idGrupoHistorial);
                Object[] paqueteG = {idGrupoHistorial, historialGrupo};
                enviar(new Peticion("HISTORIAL_GRUPO_OK", paqueteG));
                break;
            case "SALIR_GRUPO":
                if (usuarioConectado == null) break;
                int idGrupoSalir = (Integer) p.getDatos();
                GrupoDAO grupoDAO7 = new GrupoDAO();
                if (grupoDAO7.esCreador(idGrupoSalir, usuarioConectado.getPk_usuario())) {
                    grupoDAO7.eliminarGrupo(idGrupoSalir);
                    enviar(new Peticion("GRUPO_ELIMINADO", "Grupo eliminado (creador salió)"));
                    servidor.log(usuarioConectado.getUsername() + " salió y eliminó el grupo ID: " + idGrupoSalir);
                } else {
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
