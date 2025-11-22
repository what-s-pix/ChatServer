/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package chatserver;

import java.awt.BorderLayout;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

public class Servidor extends JFrame {

    // Componentes de la GUI
    private JTextArea txtLog;
    
    // Lista para guardar todos los hilos (útil para enviar mensajes a todos luego)
    public static ArrayList<HiloCliente> clientesConectados = new ArrayList<>();
    
    public Servidor() {
        // Configuración visual básica
        super("Visor del Servidor - Chat");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(500, 400);
        setLayout(new BorderLayout());

        txtLog = new JTextArea();
        txtLog.setEditable(false);
        add(new JScrollPane(txtLog), BorderLayout.CENTER);
        
        setVisible(true);
        
        // Iniciamos la escucha de red en un hilo aparte para no congelar la ventana
        new Thread(() -> iniciarRed()).start();
    }

    private void iniciarRed() {
        int puerto = 5000; // Puedes cambiarlo
        log("Iniciando servidor en puerto " + puerto + "...");
        
        try (ServerSocket serverSocket = new ServerSocket(puerto)) {
            log("Servidor esperando conexiones...");
            
            while (true) {
                // 1. Esperar conexión (se detiene aquí hasta que alguien entra)
                Socket socketCliente = serverSocket.accept();
                
                // 2. Crear un hilo para ese cliente
                log("Nueva conexión entrante: " + socketCliente.getInetAddress());
                HiloCliente hilo = new HiloCliente(socketCliente, this);
                
                // 3. Agregarlo a la lista y arrancarlo
                clientesConectados.add(hilo);
                hilo.start();
            }
        } catch (IOException e) {
            log("Error crítico en el servidor: " + e.getMessage());
        }
    }

    // Método sincronizado para que los hilos escriban en el log sin pelearse
    public synchronized void log(String mensaje) {
        SwingUtilities.invokeLater(() -> {
            txtLog.append(mensaje + "\n");
            // Auto-scroll hacia abajo
            txtLog.setCaretPosition(txtLog.getDocument().getLength());
        });
    }

    
}
