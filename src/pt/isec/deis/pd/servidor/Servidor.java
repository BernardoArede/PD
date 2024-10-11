package pt.isec.deis.pd.servidor;

import java.net.*;
import java.io.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Timer;
import java.util.TimerTask;

public class Servidor  {

    private static final int HEARTBEAT_INTERVAL = 10000; // Intervalo
    private static final int HEARTBEAT_PORT = 4444; //Porta de multicast
    private static final String HEARTBEAT_ADDRESS = "230.44.44.44"; //Address de multicast
    private static final String DB_VERSION = "1.0"; //Versão da base de dados

    private static final int TIMEOUT = 60000;



    static boolean autenticarUtilizador(String username, String password) {
        return username.equals("admin") && password.equals("admin");
    }

    private static void startHeartBeat(int listeningPort){

     Timer timer = new Timer(true); // "true" para rodar como daemon

        TimerTask heartbeatTask = new TimerTask() {
            @Override
            public void run() {
                try {

                    String message = "Versão da base de dados: " + DB_VERSION
                            + ", Porto de escuta para backup: " + listeningPort;

                    // Envia a mensagem de heartbeat
                    sendHeartbeat(message);
                } catch (IOException e) {
                    System.out.println("Erro ao enviar heartbeat: " + e.getMessage());
                }
            }
        };

        // Agendar a tarefa para rodar a cada 10 segundos
        timer.scheduleAtFixedRate(heartbeatTask, 0, HEARTBEAT_INTERVAL);
    }

    private static void sendHeartbeat(String message) throws IOException {
        InetAddress group = InetAddress.getByName(HEARTBEAT_ADDRESS);
        try (MulticastSocket multicastSocket = new MulticastSocket()) {
            byte[] buffer = message.getBytes();
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, group, HEARTBEAT_PORT);
            multicastSocket.send(packet);
            System.out.println("Heartbeat enviado: " + message);
        }
    }

    public static void main(String[] args) throws IOException {

        int listeningPort;
        String dbFilePath;
        File dbFile;

        // Verificar se foram passados os argumentos necessários
        if (args.length != 2) {
            System.out.println("Sintaxe: java Servidor [port] [fileDataBaseName]");
            return;
        }

        // Obter o ficheiro da base de dados
        dbFilePath = args[1].trim();
        dbFile = new File(dbFilePath);

        // Verificar se o ficheiro da base de dados existe
        if (!dbFile.exists()) {
            System.out.println("A diretoria " + dbFile + " não existe!");
            return;
        }

        // Verificar permissões de leitura
        if (!dbFile.canRead()) {
            System.out.println("Sem permissões de leitura na diretoria " + dbFile + "!");
            return;
        }

        try {

            listeningPort = Integer.parseInt(args[0]);

               try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + dbFilePath)) {
                System.out.println("Conectado à base de dados SQLite com sucesso.");

                // Iniciar o envio de heartbeats a cada 10 segundos
                startHeartBeat(listeningPort);

            try (ServerSocket serverSocket = new ServerSocket(listeningPort)) {

                System.out.println("Servidor iniciado na porta " + listeningPort + ". Aguardando conexões...");

                while (true) {
                    try {
                        Socket socket = serverSocket.accept();

                        socket.setSoTimeout(TIMEOUT);

                        System.out.println("Cliente conectado: " + socket.getInetAddress());

                        ClientHandler clientHandler = new ClientHandler(socket, connection);
                        Thread clientThread = new Thread(clientHandler);
                        clientThread.start();

                    } catch (SocketTimeoutException ex) {
                        System.out.println("O cliente não enviou qualquer dado (timeout).");
                    } catch (IOException ex) {
                        System.out.println("Problema de I/O ao atender o cliente: " + ex.getMessage());
                    }
                }

            } catch (SocketException e) {
                System.out.println("Ocorreu uma exceção no socket: " + e.getMessage());
            } catch (IOException e) {
                System.out.println("Ocorreu um erro de E/S: " + e.getMessage());
            }
             } catch (SQLException e) {
                System.out.println("Erro ao conectar à base de dados SQLite: " + e.getMessage());
            }

        } catch (NumberFormatException e) {
            System.out.println("O porto de escuta deve ser um número inteiro válido: " + e.getMessage());
        }
    }
}


class ClientHandler implements Runnable {
    private Socket socket;
    private Connection connection;

    public ClientHandler(Socket socket, Connection connection) {
        this.socket = socket;
        this.connection = connection;
    }

    @Override
    public void run() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            String auth;
            try {
                auth = in.readLine();
            } catch (SocketTimeoutException e) {
                System.out.println("Cliente não enviou as credenciais no tempo limite");
                return;
            }

            if (auth != null && auth.startsWith("AUTH")) {
                String[] partes = auth.split(" ");
                if (partes.length == 3) {
                    String username = partes[1];
                    String password = partes[2];
                    if (Servidor.autenticarUtilizador(username, password)) {
                        System.out.println("Autenticação bem sucedida para o cliente " + socket.getInetAddress());

                        //LOGICA GERAL DO PROGRAMA AQUI DENTRO



                    } else {
                        System.out.println("Autenticação falhou para o cliente " + socket.getInetAddress());
                    }
                } else {
                    System.out.println("Formato da autenticação errado");
                }
            } else {
                System.out.println("Nenhuma credencial enviada. Conexão encerrada");
            }

        } catch (SocketTimeoutException ex) {
            System.out.println("O cliente não enviou qualquer dado (timeout).");
        } catch (IOException ex) {
            System.out.println("Problema de I/O ao atender o cliente: " + ex.getMessage());
        } finally {
            try {
                socket.close();
                System.out.println("Socket do cliente foi encerrado");
            } catch (IOException e) {
                System.out.println("Erro ao fechar o socket: " + e.getMessage());
            }
        }
    }
}

