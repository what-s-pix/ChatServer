package chatserver;
import common.Peticion;
import models.Usuario;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
public class TestCliente {
    public static void main(String[] args) {
        System.out.println("--- INICIANDO TEST DE CLIENTE ---");
        String host = "localhost";
        int puerto = 5000;
        try {
            System.out.println("1. Intentando conectar a " + host + ":" + puerto + "...");
            Socket socket = new Socket(host, puerto);
            ObjectOutputStream salida = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream entrada = new ObjectInputStream(socket.getInputStream());
            System.out.println("✅ Conectado al servidor.");
            String randomUser = "UsuarioTest_" + System.currentTimeMillis();
            Usuario nuevoUsuario = new Usuario("Tester", randomUser, "12345");
            System.out.println("\n--- ENVIANDO PETICIÓN: REGISTRO (" + randomUser + ") ---");
            salida.writeObject(new Peticion("REGISTRO", nuevoUsuario));
            Peticion respuestaReg = (Peticion) entrada.readObject();
            System.out.println("RESPUESTA SERVIDOR: " + respuestaReg.getAccion());
            System.out.println("DATOS: " + respuestaReg.getDatos());
            System.out.println("\n--- ENVIANDO PETICIÓN: LOGIN CORRECTO ---");
            Usuario credencialesOk = new Usuario(null, randomUser, "12345");
            salida.writeObject(new Peticion("LOGIN", credencialesOk));
            Peticion respuestaLogin = (Peticion) entrada.readObject();
            System.out.println("RESPUESTA SERVIDOR: " + respuestaLogin.getAccion());
            if (respuestaLogin.getDatos() instanceof Usuario) {
                Usuario uRecibido = (Usuario) respuestaLogin.getDatos();
                System.out.println("Usuario logueado: " + uRecibido.getNombre() + " (ID: " + uRecibido.getPk_usuario() + ")");
            }
            System.out.println("\n--- ENVIANDO PETICIÓN: LOGIN INCORRECTO ---");
            Usuario credencialesMal = new Usuario(null, randomUser, "badpass");
            salida.writeObject(new Peticion("LOGIN", credencialesMal));
            Peticion respuestaFail = (Peticion) entrada.readObject();
            System.out.println("RESPUESTA SERVIDOR: " + respuestaFail.getAccion());
            System.out.println("MENSAJE: " + respuestaFail.getDatos());
            socket.close();
            System.out.println("\n--- TEST FINALIZADO ---");
        } catch (Exception e) {
            System.err.println("❌ ERROR EN EL CLIENTE DE PRUEBA:");
            e.printStackTrace();
        }
    }
}
