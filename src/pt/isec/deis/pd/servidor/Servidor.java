package pt.isec.deis.pd.servidor;

import java.net.*;
import java.io.*;
import java.sql.*;
import java.util.Timer;
import java.util.TimerTask;

public class Servidor  {

    private static final int HEARTBEAT_INTERVAL = 10000; // Intervalo
    private static final int HEARTBEAT_PORT = 4444; //Porta de multicast
    private static final String HEARTBEAT_ADDRESS = "230.44.44.44"; //Address de multicast
    private static final int TIMEOUT = 60000;

    static double getVersion(String dbPath) {
    String url = "jdbc:sqlite:" + dbPath;
    String sql = "SELECT db_version FROM db_version";

    try (Connection connection = DriverManager.getConnection(url);
         PreparedStatement preparedStatement = connection.prepareStatement(sql);
         ResultSet resultSet = preparedStatement.executeQuery()) {

        if (resultSet.next()) {
            return resultSet.getDouble("version");
        }
    } catch (SQLException e) {
        e.printStackTrace();
    }

    return 0;
}

    static boolean loginUtilizador(String email, String password, String dbPath) {

        String url = "jdbc:sqlite:" + dbPath;

        String sql = "SELECT * FROM utilizador WHERE email = ? AND password = ?";

        try (Connection connection = DriverManager.getConnection(url);
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {


            preparedStatement.setString(1, email);
            preparedStatement.setString(2, password);


            ResultSet resultSet = preparedStatement.executeQuery();


            return resultSet.next();

        } catch (SQLException e) {

            System.out.println("Erro ao conectar à base de dados: " + e.getMessage());
            return false;
        }

    }

    static boolean registoUtilizador(String email, String password, String telefone, String dbPath) {

        String url = "jdbc:sqlite:" + dbPath;

         String sql = "INSERT INTO utilizador (email, password, telefone) VALUES (?, ?, ?)";

        try (Connection conn = DriverManager.getConnection(url)) {
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {


                pstmt.setString(1, email);
                pstmt.setString(2, password);
                pstmt.setString(3, telefone);


                int rowsInserted = pstmt.executeUpdate();
                return rowsInserted > 0;

            }
        } catch (SQLException e) {
            System.out.println("Erro ao inserir utilizador na base de dados: " + e.getMessage());
            return false;
        }


    }

    private static void startHeartBeat(int listeningPort, Double version){

     Timer timer = new Timer(true); // "true" para rodar como daemon

        TimerTask heartbeatTask = new TimerTask() {
            @Override
            public void run() {
                try {

                    String message = "Versão da base de dados: " + version
                            + ", Porto de escuta para backup: " + listeningPort;

                    // Envia a mensagem de heartbeat
                    sendHeartbeat(message);
                } catch (IOException e) {
                    System.out.println("Erro ao enviar heartbeat: " + e.getMessage());
                }
            }
        };


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

        if (args.length != 2) {
            System.out.println("Sintaxe: java Servidor [port] [fileDataBaseName]");
            return;
        }

        dbFilePath = args[1].trim();
        dbFile = new File(dbFilePath);

        Double version = getVersion(dbFilePath);

        if (!dbFile.exists()) {
            System.out.println("A diretoria " + dbFile + " não existe!");
            return;
        }
        if (!dbFile.canRead()) {
            System.out.println("Sem permissões de leitura na diretoria " + dbFile + "!");
            return;
        }

        try {

            listeningPort = Integer.parseInt(args[0]);

               try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + dbFilePath)) {
                System.out.println("Conectado à base de dados SQLite com sucesso.");

                // Iniciar o envio de heartbeats a cada 10 segundos
                startHeartBeat(listeningPort, version);

            try (ServerSocket serverSocket = new ServerSocket(listeningPort)) {

                System.out.println("Servidor iniciado na porta " + listeningPort + ". Aguardando conexões...");

                while (true) {
                    try {
                        Socket socket = serverSocket.accept();

                        socket.setSoTimeout(TIMEOUT);

                        System.out.println("Cliente conectado: " + socket.getInetAddress());

                        ClientHandler clientHandler = new ClientHandler(socket, connection, dbFilePath);
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
    private String dbFilePath;
    private boolean _islogged = false;

    public ClientHandler(Socket socket, Connection connection, String dbFilePath) {
        this.socket = socket;
        this.connection = connection;
        this.dbFilePath = dbFilePath;
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

            if (auth != null && auth.startsWith("AUTH:")) {
                String[] partes = auth.split(" ");
                if (partes.length == 3) {
                    String username = partes[1];
                    String password = partes[2];
                    if (Servidor.loginUtilizador(username, password, dbFilePath)) {
                        out.println("SUCCESS");
                        _islogged =true;
                        System.out.println("Autenticação bem sucedida para o cliente " + socket.getInetAddress());

                        //LOGICA GERAL DO PROGRAMA AQUI DENTRO
                        while(_islogged){


                        }



                    } else {
                        System.out.println("Autenticação falhou para o cliente " + socket.getInetAddress());

                    }
                } else {
                    System.out.println("Formato da autenticação errado");
                }
            } else {
                if(auth != null &&  auth.startsWith("REGIST:")){
                    String[] partes = auth.split(" ");
                    if (partes.length == 4) {
                        String username = partes[1];
                        String password = partes[2];
                        String telefone = partes[3];
                        if(Servidor.registoUtilizador(username,password,telefone, dbFilePath)){
                            out.println("SUCCESS");
                            System.out.println("Dados registados com sucesso na base de dados");
                        }else{
                            out.println("FAIL REGISTER");
                            System.out.println("O registo do utilizador na base de dados falhou");
                        }
                    }else{
                        System.out.println("Formato da registro errado");
                    }

                }else {
                    System.out.println("Nenhum dado para registo enviado");
                }
            }

        } catch (SocketTimeoutException ex) {
            System.out.println("O cliente não enviou qualquer dado (timeout).");
        } catch (IOException ex) {
            System.out.println("Problema de I/O ao atender o cliente: " + ex.getMessage());
        } finally {
            /*Este trecho de codigo não pode estar aqui porque asssim a comunicação com cada cliente
            vai ser finalizada de forma a que não possa acontecer uma comunicação continua*/
            try {
                socket.close();
                System.out.println("Socket do cliente foi encerrado");
            } catch (IOException e) {
                System.out.println("Erro ao fechar o socket: " + e.getMessage());
            }
        }
    }
}

