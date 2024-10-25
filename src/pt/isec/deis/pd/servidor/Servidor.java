package pt.isec.deis.pd.servidor;

import java.net.*;
import java.io.*;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;

public class Servidor  {

    private static final int HEARTBEAT_INTERVAL = 10000; // Intervalo
    private static final int HEARTBEAT_PORT = 4444; //Porta de multicast
    private static final String HEARTBEAT_ADDRESS = "230.44.44.44"; //Address de multicast
    private static final int TIMEOUT = 500000;

    static double getVersion(String dbPath) {
    String url = "jdbc:sqlite:" + dbPath;
    String sql = "SELECT db_version FROM db_version";

    try (Connection connection = DriverManager.getConnection(url);
         PreparedStatement preparedStatement = connection.prepareStatement(sql);
         ResultSet resultSet = preparedStatement.executeQuery()) {

        if (resultSet.next()) {
            return resultSet.getDouble("db_version");
        }
    } catch (SQLException e) {
        System.out.println(e.getMessage());
    }

    return 0;
}

    static void upVersionDB(String dbPath) {
    String url = "jdbc:sqlite:" + dbPath;
    String selectSql = "SELECT db_version FROM db_version";
    String updateSql = "UPDATE db_version SET db_version = ?";

    try (Connection connection = DriverManager.getConnection(url);
         PreparedStatement selectStatement = connection.prepareStatement(selectSql);
         ResultSet resultSet = selectStatement.executeQuery()) {

        if (resultSet.next()) {
            double currentVersion = resultSet.getDouble("db_version");
            double newVersion = currentVersion + 0.1;

            try (PreparedStatement updateStatement = connection.prepareStatement(updateSql)) {
                updateStatement.setDouble(1, newVersion);
                updateStatement.executeUpdate();
                System.out.println("Versão atualizada para: " + newVersion);
            }
        }

    } catch (SQLException e) {
        e.printStackTrace();
    }
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

    private static void startHeartBeat(int listeningPort, String dbPath){

     Timer timer = new Timer(true); // "true" para rodar como daemon

        TimerTask heartbeatTask = new TimerTask() {
            @Override
            public void run() {
                try {
                        double version = getVersion(dbPath);
                        String message = String.format("Versão da base de dados: %.1f, Porto de escuta para backup: %d", version, listeningPort);

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

    public static boolean grupoExiste(String groupName, String dbFilePath) {
    String url = "jdbc:sqlite:" + dbFilePath;
    String sql = "SELECT COUNT(*) FROM grupo WHERE nome = ?";

    try (Connection connection = DriverManager.getConnection(url);
         PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
        preparedStatement.setString(1, groupName);
        ResultSet resultSet = preparedStatement.executeQuery();

        if (resultSet.next() && resultSet.getInt(1) > 0) {
            return true;  // Grupo já existe
        }
    } catch (SQLException e) {
        e.printStackTrace();
    }

    return false;  // Grupo não existe
}

    public static boolean criarGrupo(String groupName, String creatorEmail, String dbFilePath) {
    String url = "jdbc:sqlite:" + dbFilePath;
    String sqlInsertGroup = "INSERT INTO grupo(nome) VALUES(?)";
    String sqlGetUserId = "SELECT id_utilizador FROM utilizador WHERE email = ?";
    String sqlGetGroupId = "SELECT id_grupo FROM grupo WHERE nome = ?";
    String sqlAddMember = "INSERT INTO grupo_utilizador(id_grupo, id_utilizador) VALUES(?, ?)";

    try (Connection connection = DriverManager.getConnection(url)) {
        connection.setAutoCommit(false);  // Usar transações

        try (PreparedStatement insertGroupStmt = connection.prepareStatement(sqlInsertGroup);
             PreparedStatement getUserIdStmt = connection.prepareStatement(sqlGetUserId);
             PreparedStatement getGroupIdStmt = connection.prepareStatement(sqlGetGroupId);
             PreparedStatement addMemberStmt = connection.prepareStatement(sqlAddMember)) {

            // Inserir o grupo
            insertGroupStmt.setString(1, groupName);
            insertGroupStmt.executeUpdate();

            // Obter o ID do utilizador que criou o grupo
            getUserIdStmt.setString(1, creatorEmail);
            ResultSet userIdResult = getUserIdStmt.executeQuery();
            if (!userIdResult.next()) {
                connection.rollback();  // Se não encontrar o utilizador, desfaz a transação
                return false;
            }
            int userId = userIdResult.getInt("id_utilizador");

            // Obter o ID do grupo recém-criado
            getGroupIdStmt.setString(1, groupName);
            ResultSet groupIdResult = getGroupIdStmt.executeQuery();
            if (!groupIdResult.next()) {
                connection.rollback();  // Se não encontrar o grupo, desfaz a transação
                return false;
            }
            int groupId = groupIdResult.getInt("id_grupo");

            // Adicionar o criador como membro do grupo
            addMemberStmt.setInt(1, groupId);
            addMemberStmt.setInt(2, userId);
            addMemberStmt.executeUpdate();

            connection.commit();  // Confirma a transação
            return true;
        } catch (SQLException e) {
            connection.rollback();  // Desfaz a transação em caso de erro
            e.printStackTrace();
        }
    } catch (SQLException e) {
        e.printStackTrace();
    }

    return false;
}

    public static void imprimeGrupos(String username, String dbFilePath, PrintWriter out) {
    String url = "jdbc:sqlite:" + dbFilePath;
    String sql = """
        SELECT g.nome 
        FROM grupo g
        INNER JOIN grupo_utilizador gu ON g.id_grupo = gu.id_grupo
        INNER JOIN utilizador u ON gu.id_utilizador = u.id_utilizador
        WHERE u.email = ?
    """;

    try (Connection connection = DriverManager.getConnection(url);
         PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

        // Definir o parâmetro da consulta
        preparedStatement.setString(1, username);

        // Executar a consulta
        ResultSet resultSet = preparedStatement.executeQuery();

        // Verificar se o utilizador pertence a algum grupo
        boolean hasGroups = false;
        while (resultSet.next()) {
            hasGroups = true;
            String groupName = resultSet.getString("nome");
            out.println("Grupo: " + groupName);  // Enviar o nome do grupo para o cliente
        }

        if (!hasGroups) {
            out.println("Não pertence a nenhum grupo.");
        }

        // Enviar "FIM" para indicar o término da lista
        out.println("FIM");

    } catch (SQLException e) {
        e.printStackTrace();
        out.println("Erro ao recuperar grupos.");
        out.println("FIM");  // Enviar "FIM" mesmo em caso de erro
    }
}

    public static boolean enviarConvite(String usernameConvidado, String nomeGrupo, String dbFilePath) {
        String url = "jdbc:sqlite:" + dbFilePath;
        String sqlGetGroupId = "SELECT id_grupo FROM grupo WHERE nome = ?";
        String sqlGetUserId = "SELECT id_utilizador FROM utilizador WHERE email = ?";
        String sqlInsertConvite = "INSERT INTO convite(id_grupo, id_utilizador_convidado, estado) VALUES(?, ?, 'pendente')";

        try (Connection connection = DriverManager.getConnection(url);
             PreparedStatement getGroupIdStmt = connection.prepareStatement(sqlGetGroupId);
             PreparedStatement getUserIdStmt = connection.prepareStatement(sqlGetUserId);
             PreparedStatement insertConviteStmt = connection.prepareStatement(sqlInsertConvite)) {

            getGroupIdStmt.setString(1, nomeGrupo);
            ResultSet groupResultSet = getGroupIdStmt.executeQuery();
            if (!groupResultSet.next()) {
                return false;
            }
            int groupId = groupResultSet.getInt("id_grupo");


            getUserIdStmt.setString(1, usernameConvidado);
            ResultSet userResultSet = getUserIdStmt.executeQuery();
            if (!userResultSet.next()) {
                return false;  //Não encontrado o utilizador
            }
            int userId = userResultSet.getInt("id_utilizador");

            // Criar o convite
            insertConviteStmt.setInt(1, groupId);
            insertConviteStmt.setInt(2, userId);
            insertConviteStmt.executeUpdate();

            return true;
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            return false;
        }
    }

    public static List<String> listarConvitesPendentes(String username, String dbFilePath) {
    String url = "jdbc:sqlite:" + dbFilePath;
    String sqlGetUserId = "SELECT id_utilizador FROM utilizador WHERE email = ?";
    String sqlGetConvites = """
        SELECT g.nome 
        FROM convite c
        JOIN grupo g ON c.id_grupo = g.id_grupo
        WHERE c.id_utilizador_convidado = ? AND c.estado = 'pendente'
    """;

    List<String> gruposPendentes = new ArrayList<>();

    try (Connection connection = DriverManager.getConnection(url);
         PreparedStatement getUserIdStmt = connection.prepareStatement(sqlGetUserId);
         PreparedStatement getConvitesStmt = connection.prepareStatement(sqlGetConvites)) {

        // Obter o ID do utilizador
        getUserIdStmt.setString(1, username);
        ResultSet userResultSet = getUserIdStmt.executeQuery();
        if (!userResultSet.next()) {
            return gruposPendentes;  // Utilizador não encontrado
        }
        int userId = userResultSet.getInt("id_utilizador");

        // Obter convites pendentes
        getConvitesStmt.setInt(1, userId);
        ResultSet convitesResultSet = getConvitesStmt.executeQuery();

        // Preencher a lista com os nomes dos grupos
        while (convitesResultSet.next()) {
            gruposPendentes.add(convitesResultSet.getString("nome"));
        }
    } catch (SQLException e) {
        e.printStackTrace();
    }

    return gruposPendentes;
}

    public static boolean aceitarConvite(String username, String nomeGrupo, String dbFilePath) {
    String url = "jdbc:sqlite:" + dbFilePath;
    String sqlGetGroupId = "SELECT id_grupo FROM grupo WHERE nome = ?";
    String sqlGetUserId = "SELECT id_utilizador FROM utilizador WHERE email = ?";
    String sqlUpdateConvite = "UPDATE convite SET estado = 'aceite ' WHERE id_grupo = ? AND id_utilizador_convidado = ? AND estado = 'pendente'";
    String sqlAddUserToGroup = "INSERT INTO grupo_utilizador (id_grupo, id_utilizador) VALUES (?, ?)";

    try (Connection connection = DriverManager.getConnection(url);
         PreparedStatement getGroupIdStmt = connection.prepareStatement(sqlGetGroupId);
         PreparedStatement getUserIdStmt = connection.prepareStatement(sqlGetUserId);
         PreparedStatement updateConviteStmt = connection.prepareStatement(sqlUpdateConvite);
         PreparedStatement addUserToGroupStmt = connection.prepareStatement(sqlAddUserToGroup)) {

        // Obter o ID do grupo
        getGroupIdStmt.setString(1, nomeGrupo);
        ResultSet groupResultSet = getGroupIdStmt.executeQuery();
        if (!groupResultSet.next()) {
            return false;  // Grupo não encontrado
        }
        int groupId = groupResultSet.getInt("id_grupo");

        // Obter o ID do utilizador
        getUserIdStmt.setString(1, username);
        ResultSet userResultSet = getUserIdStmt.executeQuery();
        if (!userResultSet.next()) {
            return false;  // Utilizador não encontrado
        }
        int userId = userResultSet.getInt("id_utilizador");

        // Atualizar o estado do convite para "aceite"
        updateConviteStmt.setInt(1, groupId);
        updateConviteStmt.setInt(2, userId);
        int rowsAffected = updateConviteStmt.executeUpdate();

        if (rowsAffected == 0) {
            return false;  // Nenhum convite pendente encontrado ou erro ao aceitar
        }

        // Adicionar o utilizador ao grupo
        addUserToGroupStmt.setInt(1, groupId);
        addUserToGroupStmt.setInt(2, userId);
        addUserToGroupStmt.executeUpdate();

        return true;
    } catch (SQLException e) {
        e.printStackTrace();
        return false;
    }
}

    public static boolean editarNomeGrupo(String username, String nomeAtualGrupo, String novoNomeGrupo, String dbFilePath) {
        String url = "jdbc:sqlite:" + dbFilePath;

        String sqlVerificarMembro = """
            SELECT gu.id_grupo 
            FROM grupo_utilizador gu 
            JOIN utilizador u ON gu.id_utilizador = u.id_utilizador 
            JOIN grupo g ON gu.id_grupo = g.id_grupo 
            WHERE u.email = ? AND g.nome = ?""";

        String sqlAtualizarNomeGrupo = "UPDATE grupo SET nome = ? WHERE id_grupo = ?";

        try (Connection connection = DriverManager.getConnection(url);
             PreparedStatement verificarMembroStmt = connection.prepareStatement(sqlVerificarMembro);
             PreparedStatement atualizarNomeGrupoStmt = connection.prepareStatement(sqlAtualizarNomeGrupo)) {

            // Verificar se o utilizador é membro do grupo
            verificarMembroStmt.setString(1, username);
            verificarMembroStmt.setString(2, nomeAtualGrupo);
            ResultSet resultSet = verificarMembroStmt.executeQuery();

            if (!resultSet.next()) {
                return false;  // Utilizador não é membro do grupo
            }

            int grupoId = resultSet.getInt("id_grupo");

            // Atualizar o nome do grupo
            atualizarNomeGrupoStmt.setString(1, novoNomeGrupo);
            atualizarNomeGrupoStmt.setInt(2, grupoId);
            atualizarNomeGrupoStmt.executeUpdate();

            return true;  // Nome do grupo alterado com sucesso
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
}

    public static boolean inserirDespesa(String usernamePagador, String nomeGrupo, String descricao, double valor, List<String> participantes, String data, String dbFilePath) {
    String url = "jdbc:sqlite:" + dbFilePath;

    String sqlVerificarMembro = """
        SELECT gu.id_grupo 
        FROM grupo_utilizador gu 
        JOIN utilizador u ON gu.id_utilizador = u.id_utilizador 
        JOIN grupo g ON gu.id_grupo = g.id_grupo 
        WHERE u.email = ? AND g.nome = ?""";

    String sqlInserirDespesa = "INSERT INTO despesa (descricao, valor, id_utilizador, id_grupo, data) VALUES (?, ?, ?, ?, ?)";

    String sqlInserirParticipacao = "INSERT INTO despesa_utilizador (id_despesa, id_utilizador, valor_participacao) VALUES (?, ?, ?)";

    String sqlObterIdUtilizador = "SELECT id_utilizador FROM utilizador WHERE email = ?";

    try (Connection connection = DriverManager.getConnection(url);
         PreparedStatement verificarMembroStmt = connection.prepareStatement(sqlVerificarMembro);
         PreparedStatement inserirDespesaStmt = connection.prepareStatement(sqlInserirDespesa, Statement.RETURN_GENERATED_KEYS);
         PreparedStatement inserirParticipacaoStmt = connection.prepareStatement(sqlInserirParticipacao);
         PreparedStatement obterIdUtilizadorStmt = connection.prepareStatement(sqlObterIdUtilizador)) {

        // Verificar se o utilizador pagador pertence ao grupo
        verificarMembroStmt.setString(1, usernamePagador);
        verificarMembroStmt.setString(2, nomeGrupo);
        ResultSet resultSet = verificarMembroStmt.executeQuery();

        if (!resultSet.next()) {
            return false;  // O utilizador pagador não pertence ao grupo
        }

        int grupoId = resultSet.getInt("id_grupo");

        // Obter ID do utilizador pagador
        obterIdUtilizadorStmt.setString(1, usernamePagador);
        ResultSet resultSetPagador = obterIdUtilizadorStmt.executeQuery();
        if (!resultSetPagador.next()) {
            return false;  // O utilizador pagador não foi encontrado
        }
        int idPagador = resultSetPagador.getInt("id_utilizador");

        // Inserir a despesa
        inserirDespesaStmt.setString(1, descricao);
        inserirDespesaStmt.setDouble(2, valor);
        inserirDespesaStmt.setInt(3, idPagador);
        inserirDespesaStmt.setInt(4, grupoId);
        inserirDespesaStmt.setString(5, data);
        inserirDespesaStmt.executeUpdate();

        // Obter o ID da despesa inserida
        ResultSet generatedKeys = inserirDespesaStmt.getGeneratedKeys();
        if (!generatedKeys.next()) {
            return false;  // Falha ao obter o ID da despesa
        }
        int despesaId = generatedKeys.getInt(1);

        // Inserir participações
        for (String participante : participantes) {
            // Verificar se cada participante ainda pertence ao grupo
            verificarMembroStmt.setString(1, participante);
            verificarMembroStmt.setString(2, nomeGrupo);
            ResultSet resultSetParticipante = verificarMembroStmt.executeQuery();
            if (resultSetParticipante.next()) {
                int idParticipante = resultSetParticipante.getInt("id_utilizador");

                double valorParticipacao = valor / participantes.size();

                inserirParticipacaoStmt.setInt(1, despesaId);
                inserirParticipacaoStmt.setInt(2, idParticipante);
                inserirParticipacaoStmt.setDouble(3, valorParticipacao);
                inserirParticipacaoStmt.executeUpdate();
            } else {
                System.out.println("O participante " + participante + " não pertence ao grupo " + nomeGrupo + " e não será adicionado.");
            }
        }

        return true;  // Despesa inserida com sucesso
    } catch (SQLException e) {
        System.out.println(e.getMessage());
        return false;
    }
}

    public static boolean eliminarGrupoPorNome(String nomeGrupo, String dbFilePath) {

        String url = "jdbc:sqlite:" + dbFilePath;


        String sqlVerificaDespesas = """
            SELECT COUNT(*) as count 
            FROM despesa 
            WHERE id_grupo = (SELECT id_grupo FROM grupo WHERE nome = ?)
            """;

        String sqlVerificaDividas = """
            SELECT COUNT(*) as count 
            FROM despesa_utilizador 
            WHERE id_despesa IN (
                SELECT id_despesa 
                FROM despesa 
                WHERE id_grupo = (SELECT id_grupo FROM grupo WHERE nome = ?)
            )
            AND valor_participacao > 0
            """;

        String sqlEliminarGrupo = "DELETE FROM grupo WHERE nome = ?";

        try (Connection connection = DriverManager.getConnection(url);
             PreparedStatement verificaStmt = connection.prepareStatement(sqlVerificaDespesas);
             PreparedStatement verificaDividasStmt = connection.prepareStatement(sqlVerificaDividas);
             PreparedStatement eliminaStmt = connection.prepareStatement(sqlEliminarGrupo)) {

            verificaStmt.setString(1, nomeGrupo);
            ResultSet resultSet = verificaStmt.executeQuery();
            if (resultSet.next() && resultSet.getInt("count") > 0) {

                return false;
            }
            verificaDividasStmt.setString(1, nomeGrupo);
            ResultSet resultSetDividas = verificaDividasStmt.executeQuery();
            if (resultSetDividas.next() && resultSetDividas.getInt("count") > 0) {
                return false;
            }
            eliminaStmt.setString(1, nomeGrupo);
            eliminaStmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            return false;
        }
    }

    public static boolean sairDoGrupo(String nomeGrupo, String username, String dbFilePath) {
        String url = "jdbc:sqlite:" + dbFilePath;

        String sqlVerificaDespesas = """
            SELECT COUNT(*) as count 
            FROM despesa 
            WHERE id_grupo = (SELECT id_grupo FROM grupo WHERE nome = ?) 
            AND id_utilizador = (SELECT id_utilizador FROM utilizador WHERE email = ?)
        """;

        String sqlSairDoGrupo = "DELETE FROM grupo_utilizador WHERE id_grupo = (SELECT id_grupo FROM grupo WHERE nome = ?) AND id_utilizador = (SELECT id_utilizador FROM utilizador WHERE email = ?)";

        try (Connection connection = DriverManager.getConnection(url);
             PreparedStatement verificaStmt = connection.prepareStatement(sqlVerificaDespesas);
             PreparedStatement saiStmt = connection.prepareStatement(sqlSairDoGrupo)) {

            // Verifica se existem despesas associadas ao utilizador no grupo
            verificaStmt.setString(1, nomeGrupo);
            verificaStmt.setString(2, username);
            ResultSet resultSet = verificaStmt.executeQuery();

            if (resultSet.next() && resultSet.getInt("count") > 0) {
                // Existem despesas associadas ao utilizador no grupo
                return false;
            }

            // Se não houver despesas, sai do grupo
            saiStmt.setString(1, nomeGrupo);
            saiStmt.setString(2, username);
            int rowsAffected = saiStmt.executeUpdate();

            // Retorna true se o utilizador saiu com sucesso do grupo
            return rowsAffected > 0;

        } catch (SQLException e) {
            System.out.println("Erro ao sair do grupo: " + e.getMessage());
            return false;
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
                startHeartBeat(listeningPort, dbFilePath);

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
                        _islogged = true;
                        System.out.println("Autenticação bem sucedida para o cliente " + socket.getInetAddress());

                        while(_islogged){
                            String command = in.readLine();
                            if(command !=null){
                                if(command.startsWith("GRUPO:")){
                                    String groupName = command.split(":")[1].trim();
                                    if(Servidor.grupoExiste(groupName, dbFilePath)){
                                        out.println("FAIL_GROUP_EXIST");
                                    }else{
                                        if (Servidor.criarGrupo(groupName,username,dbFilePath)){
                                            out.println("GROUP_CREATED");
                                        }else{
                                            out.println("FAIL_CREATE_GROUP");
                                        }
                                    }
                                }
                                if (command.startsWith("VER GRUPOS:")){
                                    Servidor.imprimeGrupos(username,dbFilePath,out);
                                }
                                if (command.startsWith("CONVITES:")){
                                    String invites = in.readLine();
                                    String[] parte = invites.split(":");
                                    String user_to_invite = parte[1];
                                    String group_to_invite = parte[2];
                                    System.out.println(user_to_invite);
                                    System.out.println(group_to_invite);
                                    if(Servidor.enviarConvite(user_to_invite,group_to_invite,dbFilePath)){
                                        System.out.println("Enviado com sucesso");
                                    }else{
                                        System.out.println("Erro ao enviar convite");
                                    }

                                }if (command.startsWith("VER CONVITES")) {
                                    List<String> convitesPendentes = Servidor.listarConvitesPendentes(username, dbFilePath);


                                    if (convitesPendentes.isEmpty()) {
                                        out.println("Não tem convites pendentes.");
                                    } else {
                                        out.println("Convites pendentes:");
                                        for (String grupo : convitesPendentes) {
                                            out.println("Convite para o grupo: " + grupo);
                                        }
                                    }
                                    out.println("END");
                                    out.println("Deseja aceitar algum convite? (sim/não)");
                                    String resposta = in.readLine();

                                    if (resposta.equalsIgnoreCase("sim")) {
                                        out.println("Indique o nome do grupo para aceitar o convite:");
                                        String grupoEscolhido = in.readLine();

                                        if (Servidor.aceitarConvite(username, grupoEscolhido, dbFilePath)) {
                                            out.println("Convite para o grupo '" + grupoEscolhido + "' aceite com sucesso.");
                                        } else {
                                            out.println("Falha ao aceitar o convite. Verifique o grupo ou se há convite pendente.");
                                        }
                                    } else {
                                        out.println("Nenhum convite foi aceite.");
                                    }
                                }if (command.startsWith("EDITAR NOME GRUPO:")) {
                                    String[] splits = command.split(":");
                                    String nomeAtualGrupo = splits[1];
                                    String novoNomeGrupo = splits[2];

                                    if (Servidor.editarNomeGrupo(username, nomeAtualGrupo, novoNomeGrupo, dbFilePath)) {
                                        out.println("Nome do grupo alterado com sucesso para: " + novoNomeGrupo);
                                    } else {
                                        out.println("Erro: Não foi possível alterar o nome do grupo. Verifique se você pertence ao grupo.");
                                    }
                                }if (command.startsWith("INSERIR DESPESA:")) {
                                     String[] partes2 = command.split(":");
                                    String nomeGrupo = partes2[1];
                                    String descricao = partes2[2];
                                    double valor = Double.parseDouble(partes2[3]);
                                    String data = partes2[4];

                                    String participantesStr = in.readLine();
                                    List<String> participantes = Arrays.asList(participantesStr.split(","));

                                    if (Servidor.inserirDespesa(username, nomeGrupo, descricao, valor, participantes, data, dbFilePath)) {
                                        out.println("Despesa inserida com sucesso.");
                                    } else {
                                        out.println("Erro ao inserir a despesa. Verifique os dados e tente novamente.");
                                    }
                                }if (command.startsWith("ELIMINAR GRUPO:")) {
                                    String[] partes3 = command.split(":");
                                    if (partes3.length == 2) {
                                        String nomeGrupo = partes3[1];
                                        if (Servidor.eliminarGrupoPorNome(nomeGrupo, dbFilePath)) {
                                            out.println("Grupo '" + nomeGrupo + "' eliminado com sucesso.");
                                        } else {
                                            out.println("Não foi possível eliminar o grupo. Existem despesas associadas ou o grupo não existe.");
                                        }
                                    } else {
                                        out.println("Formato do comando errado.");
                                    }
                                }if (command.startsWith("SAIR DO GRUPO:")) {
                                    String nomeGrupo = command.split(":")[1];
                                    if (Servidor.sairDoGrupo(nomeGrupo, username, dbFilePath)) {
                                        out.println("Você saiu do grupo '" + nomeGrupo + "' com sucesso.");
                                    } else {
                                        out.println("Não foi possível sair do grupo '" + nomeGrupo + "'. Verifique se há despesas associadas.");
                                    }
                                }
                            }
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
                            Servidor.upVersionDB(dbFilePath);
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
            /*Este pedaço de codigo não pode estar aqui porque asssim a comunicação com cada cliente
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

