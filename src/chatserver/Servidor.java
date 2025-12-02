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
        super("Servidor");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        setLayout(new BorderLayout());
        JPanel panelInfo = new JPanel(new GridLayout(1, 2));
        lblEstado = new JLabel("Estado: Iniciando...");
        lblClientes = new JLabel("Clientes conectados: 0");
        panelInfo.add(lblEstado);
        panelInfo.add(lblClientes);
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
        System.out.println("[SERVIDOR] Iniciando servidor en puerto " + puerto);
        SwingUtilities.invokeLater(() -> {
            lblEstado.setText("puerto:" + puerto);
        });
        try (ServerSocket serverSocket = new ServerSocket(puerto)) {
            System.out.println("[SERVIDOR] Servidor escuchando en puerto " + puerto);
            log("Servidor iniciado y escuchando en puerto " + puerto);
            while (true) {
                System.out.println("[SERVIDOR] Esperando conexiones...");
                Socket socketCliente = serverSocket.accept();
                System.out.println("[SERVIDOR] Nueva conexión recibida de: " + socketCliente.getInetAddress().getHostAddress());
                log("[" + sdf.format(new Date()) + "] Nueva conexión: " + socketCliente.getInetAddress().getHostAddress());
                HiloCliente hilo = new HiloCliente(socketCliente, this);
                clientesConectados.add(hilo);
                actualizarContadorClientes();
                hilo.start();
                System.out.println("[SERVIDOR] Hilo de cliente iniciado. Total clientes: " + clientesConectados.size());
            }
        } catch (IOException e) {
            System.err.println("[SERVIDOR] ERROR CRÍTICO: " + e.getMessage());
            e.printStackTrace();
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
