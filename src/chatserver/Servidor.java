package chatserver;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
public class Servidor extends JFrame {
    private JTextArea txtLog;
    private JLabel lblClientes;
    private JLabel lblEstado;
    public static ArrayList<HiloCliente> clientesConectados = new ArrayList<>();
    private SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
    public Servidor() {
        super("Visor del Servidor - What's Pix Chat");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        setLayout(new BorderLayout());
        JPanel panelInfo = new JPanel(new GridLayout(1, 3));
        lblEstado = new JLabel("Estado: Iniciando...");
        lblClientes = new JLabel("Clientes conectados: 0");
        JLabel lblFecha = new JLabel("Fecha: " + new SimpleDateFormat("dd/MM/yyyy").format(new Date()));
        panelInfo.add(lblEstado);
        panelInfo.add(lblClientes);
        panelInfo.add(lblFecha);
        txtLog = new JTextArea();
        txtLog.setEditable(false);
        txtLog.setFont(new java.awt.Font("Monospaced", java.awt.Font.PLAIN, 11));
        JScrollPane scrollLog = new JScrollPane(txtLog);
        add(panelInfo, BorderLayout.NORTH);
        add(scrollLog, BorderLayout.CENTER);
        setVisible(true);
        new Thread(() -> iniciarRed()).start();
    }
    private void iniciarRed() {
        int puerto = 5000;
        log("========================================");
        log("SERVIDOR WHAT'S PIX CHAT INICIADO");
        log("========================================");
        log("Puerto: " + puerto);
        log("Fecha/Hora: " + new Date());
        log("----------------------------------------");
        SwingUtilities.invokeLater(() -> {
            lblEstado.setText("Estado: Activo - Puerto " + puerto);
        });
        try (ServerSocket serverSocket = new ServerSocket(puerto)) {
            log("Servidor esperando conexiones en puerto " + puerto + "...");
            log("----------------------------------------");
            while (true) {
                Socket socketCliente = serverSocket.accept();
                log("[" + sdf.format(new Date()) + "] Nueva conexión: " + socketCliente.getInetAddress().getHostAddress());
                HiloCliente hilo = new HiloCliente(socketCliente, this);
                clientesConectados.add(hilo);
                actualizarContadorClientes();
                hilo.start();
            }
        } catch (IOException e) {
            log("ERROR CRÍTICO: " + e.getMessage());
            SwingUtilities.invokeLater(() -> {
                lblEstado.setText("Estado: ERROR - " + e.getMessage());
            });
        }
    }
    public synchronized void log(String mensaje) {
        SwingUtilities.invokeLater(() -> {
            String timestamp = "[" + sdf.format(new Date()) + "] ";
            txtLog.append(timestamp + mensaje + "\n");
            txtLog.setCaretPosition(txtLog.getDocument().getLength());
        });
    }
    public void actualizarContadorClientes() {
        SwingUtilities.invokeLater(() -> {
            lblClientes.setText("Clientes conectados: " + clientesConectados.size());
        });
    }
}
