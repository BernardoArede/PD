package pt.isec.deis.pd.servidor;

import pt.isec.deis.pd.dataBase.ManageDB;

import java.net.*;
import java.io.*;
import java.sql.*;
import java.util.*;

import static pt.isec.deis.pd.dataBase.ManageDB.getVersion;


public class Servidor  {

    private static final int HEARTBEAT_INTERVAL = 10000; // Intervalo
    public static final int HEARTBEAT_PORT = 4444; //Porta de multicast
    public static final String HEARTBEAT_ADDRESS = "230.44.44.44"; //Address de multicast
    private static final int TIMEOUT = 5000000;

    private static final List<String> queryQueue = new ArrayList<>();

    public static boolean loginUtilizador(String email, String password, String dbPath) {

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

    public static boolean registoUtilizador(String email, String password, String telefone, String dbPath) {

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

    public static void startHeartBeat(int listeningPort, String dbPath){

                     Timer timer = new Timer(true);

                        TimerTask heartbeatTask = new TimerTask() {
                            @Override
                            public void run() {
                                try {
                                        double version = getVersion(dbPath);

                                         String queriesToSend;
                                            synchronized (queryQueue) {
                                            queriesToSend = String.join(";", queryQueue);
                                            queryQueue.clear();
                                        }

                                        String message = String.format(
                                            "Versão: %.1f; Queries: [%s]; Porto: %d",
                                            version, queriesToSend, listeningPort
                                        );


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

    public static void executeQuery(String query) {
        System.out.println("A executar query: " + query);
        synchronized (queryQueue) {
            queryQueue.add(query);
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
                        connection.setAutoCommit(false);

                        try (PreparedStatement insertGroupStmt = connection.prepareStatement(sqlInsertGroup);
                             PreparedStatement getUserIdStmt = connection.prepareStatement(sqlGetUserId);
                             PreparedStatement getGroupIdStmt = connection.prepareStatement(sqlGetGroupId);
                             PreparedStatement addMemberStmt = connection.prepareStatement(sqlAddMember)) {


                            insertGroupStmt.setString(1, groupName);
                            insertGroupStmt.executeUpdate();


                            getUserIdStmt.setString(1, creatorEmail);
                            ResultSet userIdResult = getUserIdStmt.executeQuery();
                            if (!userIdResult.next()) {
                                connection.rollback();
                                return false;
                            }
                            int userId = userIdResult.getInt("id_utilizador");


                            getGroupIdStmt.setString(1, groupName);
                            ResultSet groupIdResult = getGroupIdStmt.executeQuery();
                            if (!groupIdResult.next()) {
                                connection.rollback();
                                return false;
                            }
                            int groupId = groupIdResult.getInt("id_grupo");
                            addMemberStmt.setInt(1, groupId);
                            addMemberStmt.setInt(2, userId);
                            addMemberStmt.executeUpdate();

                            connection.commit();

                            return true;
                        } catch (SQLException e) {
                            connection.rollback();
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


                        preparedStatement.setString(1, username);


                        ResultSet resultSet = preparedStatement.executeQuery();

                        boolean hasGroups = false;
                        while (resultSet.next()) {
                            hasGroups = true;
                            String groupName = resultSet.getString("nome");
                            out.println("Grupo: " + groupName);
                        }

                        if (!hasGroups) {
                            out.println("Não pertence a nenhum grupo.");
                        }

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


                        getUserIdStmt.setString(1, username);
                        ResultSet userResultSet = getUserIdStmt.executeQuery();
                        if (!userResultSet.next()) {
                            return gruposPendentes;  // Utilizador não encontrado
                        }
                        int userId = userResultSet.getInt("id_utilizador");


                        getConvitesStmt.setInt(1, userId);
                        ResultSet convitesResultSet = getConvitesStmt.executeQuery();

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


                        getGroupIdStmt.setString(1, nomeGrupo);
                        ResultSet groupResultSet = getGroupIdStmt.executeQuery();
                        if (!groupResultSet.next()) {
                            return false;  // Grupo não encontrado
                        }
                        int groupId = groupResultSet.getInt("id_grupo");


                        getUserIdStmt.setString(1, username);
                        ResultSet userResultSet = getUserIdStmt.executeQuery();
                        if (!userResultSet.next()) {
                            return false;  // Utilizador não encontrado
                        }
                        int userId = userResultSet.getInt("id_utilizador");

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


                            verificarMembroStmt.setString(1, username);
                            verificarMembroStmt.setString(2, nomeAtualGrupo);
                            ResultSet resultSet = verificarMembroStmt.executeQuery();

                            if (!resultSet.next()) {
                                return false;  // Utilizador não é membro do grupo
                            }

                            int grupoId = resultSet.getInt("id_grupo");


                            atualizarNomeGrupoStmt.setString(1, novoNomeGrupo);
                            atualizarNomeGrupoStmt.setInt(2, grupoId);
                            atualizarNomeGrupoStmt.executeUpdate();


                            return true;
                        } catch (SQLException e) {
                            e.printStackTrace();
                            return false;
                        }
                }

    public static boolean inserirDespesa(String usernameInseriu, String nomeGrupo, String descricao, double valor, List<String> participantes, String data, String dbFilePath) {
                        String url = "jdbc:sqlite:" + dbFilePath;

                        String sqlObterIdGrupo = """
                            SELECT gu.id_grupo 
                            FROM grupo_utilizador gu 
                            JOIN utilizador u ON gu.id_utilizador = u.id_utilizador 
                            JOIN grupo g ON gu.id_grupo = g.id_grupo 
                            WHERE u.email = ? AND g.nome = ?""";

                        String sqlInserirDespesa = "INSERT INTO despesa (descricao, valor, id_grupo, data, id_utilizador) VALUES (?, ?, ?, ?, ?)";

                        String sqlInserirParticipacao = "INSERT INTO despesa_utilizador (id_despesa, id_utilizador, valor_participacao) VALUES (?, ?, ?)";

                        String sqlObterIdUtilizador = "SELECT id_utilizador FROM utilizador WHERE email = ?";

                        try (Connection connection = DriverManager.getConnection(url);
                             PreparedStatement obterIdGrupoStmt = connection.prepareStatement(sqlObterIdGrupo);
                             PreparedStatement inserirDespesaStmt = connection.prepareStatement(sqlInserirDespesa, Statement.RETURN_GENERATED_KEYS);
                             PreparedStatement inserirParticipacaoStmt = connection.prepareStatement(sqlInserirParticipacao);
                             PreparedStatement obterIdUtilizadorStmt = connection.prepareStatement(sqlObterIdUtilizador)) {

                            obterIdGrupoStmt.setString(1, usernameInseriu);
                            obterIdGrupoStmt.setString(2, nomeGrupo);
                            ResultSet resultSetGrupo = obterIdGrupoStmt.executeQuery();
                            if (!resultSetGrupo.next()) {
                                System.out.println("Erro: O utilizador não pertence ao grupo especificado.");
                                return false;
                            }
                            int grupoId = resultSetGrupo.getInt("id_grupo");


                            obterIdUtilizadorStmt.setString(1, usernameInseriu);
                            ResultSet resultSetUtilizador = obterIdUtilizadorStmt.executeQuery();
                            if (!resultSetUtilizador.next()) {
                                System.out.println("Erro: Utilizador que inseriu a despesa não encontrado.");
                                return false;
                            }
                            int idInseriu = resultSetUtilizador.getInt("id_utilizador");


                            inserirDespesaStmt.setString(1, descricao);
                            inserirDespesaStmt.setDouble(2, valor);
                            inserirDespesaStmt.setInt(3, grupoId);
                            inserirDespesaStmt.setString(4, data);
                            inserirDespesaStmt.setInt(5, idInseriu);
                            inserirDespesaStmt.executeUpdate();

                            ResultSet generatedKeys = inserirDespesaStmt.getGeneratedKeys();
                            if (!generatedKeys.next()) {
                                System.out.println("Erro: Não foi possível obter o ID da despesa.");
                                return false;
                            }
                            int despesaId = generatedKeys.getInt(1);
                            System.out.println("Despesa inserida com sucesso com ID: " + despesaId);

                            // Calcular o valor de participação para cada utilizador
                            double valorParticipacao = valor / participantes.size();

                            // Inserir cada participante na tabela `despesa_utilizador`
                            for (String participante : participantes) {
                                obterIdUtilizadorStmt.setString(1, participante);
                                ResultSet resultSetParticipante = obterIdUtilizadorStmt.executeQuery();
                                if (resultSetParticipante.next()) {
                                    int idParticipante = resultSetParticipante.getInt(1);

                                    inserirParticipacaoStmt.setInt(1, despesaId);
                                    inserirParticipacaoStmt.setInt(2, idParticipante);
                                    inserirParticipacaoStmt.setDouble(3, valorParticipacao);
                                    inserirParticipacaoStmt.executeUpdate();
                                    System.out.println("Participação inserida para utilizador: " + participante);
                                } else {
                                    System.out.println("Erro: Participante " + participante + " não encontrado.");
                                }
                        }

                            return true;
                        } catch (SQLException e) {
                            System.out.println("Erro SQL: " + e.getMessage());
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


            verificaStmt.setString(1, nomeGrupo);
            verificaStmt.setString(2, username);
            ResultSet resultSet = verificaStmt.executeQuery();

            if (resultSet.next() && resultSet.getInt("count") > 0) {

                return false;
            }


            saiStmt.setString(1, nomeGrupo);
            saiStmt.setString(2, username);
            int rowsAffected = saiStmt.executeUpdate();

            return rowsAffected > 0;

        } catch (SQLException e) {
            System.out.println("Erro ao sair do grupo: " + e.getMessage());
            return false;
        }
}

    public static double obterTotalGastos(String nomeGrupo, String username, String dbFilePath) {
        String url = "jdbc:sqlite:" + dbFilePath;


        String sqlObterIdGrupo = """
            SELECT gu.id_grupo 
            FROM grupo_utilizador gu 
            JOIN utilizador u ON gu.id_utilizador = u.id_utilizador 
            JOIN grupo g ON gu.id_grupo = g.id_grupo 
            WHERE u.email = ? AND g.nome = ?""";

        // SQL para calcular o total de despesas para o grupo
        String sqlCalcularTotalGastos = "SELECT SUM(valor) AS total_gastos FROM despesa WHERE id_grupo = ?";

        try (Connection connection = DriverManager.getConnection(url);
             PreparedStatement obterIdGrupoStmt = connection.prepareStatement(sqlObterIdGrupo);
             PreparedStatement calcularTotalGastosStmt = connection.prepareStatement(sqlCalcularTotalGastos)) {

            // Obter ID do grupo
            obterIdGrupoStmt.setString(1, username);
            obterIdGrupoStmt.setString(2, nomeGrupo);
            ResultSet resultSetGrupo = obterIdGrupoStmt.executeQuery();

            if (!resultSetGrupo.next()) {
                return -1;  // O utilizador não pertence ao grupo
            }

            int grupoId = resultSetGrupo.getInt("id_grupo");

            // Calcular total de gastos
            calcularTotalGastosStmt.setInt(1, grupoId);
            ResultSet resultSetTotal = calcularTotalGastosStmt.executeQuery();

            if (resultSetTotal.next()) {
                return resultSetTotal.getDouble("total_gastos");
            } else {
                return 0;  // Não há despesas registradas
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            return -1;  // Erro ao acessar a base de dados
        }
}

    public static List<String> obterHistoricoDespesas(String nomeGrupo, String username, String dbFilePath) {
            String url = "jdbc:sqlite:" + dbFilePath;


            String sqlObterIdGrupo = """
                SELECT gu.id_grupo 
                FROM grupo_utilizador gu 
                JOIN utilizador u ON gu.id_utilizador = u.id_utilizador 
                JOIN grupo g ON gu.id_grupo = g.id_grupo 
                WHERE u.email = ? AND g.nome = ?""";


            String sqlHistoricoDespesas = """
                SELECT d.descricao, d.valor, d.data, u.email AS inserido_por
                FROM despesa d
                JOIN utilizador u ON d.id_utilizador = u.id_utilizador
                WHERE d.id_grupo = ?
                ORDER BY d.data ASC""";

            List<String> historico = new ArrayList<>();

            try (Connection connection = DriverManager.getConnection(url);
                 PreparedStatement obterIdGrupoStmt = connection.prepareStatement(sqlObterIdGrupo);
                 PreparedStatement historicoDespesasStmt = connection.prepareStatement(sqlHistoricoDespesas)) {


                obterIdGrupoStmt.setString(1, username);
                obterIdGrupoStmt.setString(2, nomeGrupo);
                ResultSet resultSetGrupo = obterIdGrupoStmt.executeQuery();

                if (!resultSetGrupo.next()) {
                    return null;  // O utilizador não pertence ao grupo
                }

                int grupoId = resultSetGrupo.getInt("id_grupo");


                historicoDespesasStmt.setInt(1, grupoId);
                ResultSet resultSetHistorico = historicoDespesasStmt.executeQuery();

                while (resultSetHistorico.next()) {
                    String descricao = resultSetHistorico.getString("descricao");
                    double valor = resultSetHistorico.getDouble("valor");
                    String data = resultSetHistorico.getString("data");
                    String inseridoPor = resultSetHistorico.getString("inserido_por");

                    String detalhe = String.format("Descrição: %s, Valor: %.2f, Data: %s, Inserido por: %s",
                                                     descricao, valor, data, inseridoPor);
                    historico.add(detalhe);
                }
            } catch (SQLException e) {
                System.out.println(e.getMessage());
                return null;
            }

            return historico;
        }

    public static List<String> obterDividas(String nomeGrupo, String username, String dbFilePath) {
            List<String> dividas = new ArrayList<>();

            try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbFilePath)) {
                String sql = """
                    SELECT d.id_despesa, d.descricao, d.valor AS valor_total, d.data, uCriador.email AS criador,
                           du.valor_participacao
                    FROM despesa d
                    JOIN despesa_utilizador du ON d.id_despesa = du.id_despesa
                    JOIN utilizador uCriador ON d.id_utilizador = uCriador.id_utilizador  -- Criador da dívida
                    JOIN grupo g ON d.id_grupo = g.id_grupo
                    WHERE g.nome = ? 
                      AND du.id_utilizador = (SELECT id_utilizador FROM utilizador WHERE email = ?)
                """;

                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setString(1, nomeGrupo);
                    pstmt.setString(2, username);
                    ResultSet rs = pstmt.executeQuery();

                    while (rs.next()) {
                        dividas.add("ID Despesa: " + rs.getInt("id_despesa") + " | Criador: " + rs.getString("criador") +
                                    " | Valor Total: " + rs.getDouble("valor_total") + " | Valor Devido: " + rs.getDouble("valor_participacao") +
                                    " | Data: " + rs.getString("data") + " | Descrição: " + rs.getString("descricao"));
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }

            return dividas;
        }

    public static String efetuarPagamento(String username, int idDivida, double valorPagamento, String dbFilePath) {
          try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbFilePath)) {
              String sqlVerificarDivida = """
                SELECT d.id_utilizador AS id_utilizador_criador, d.valor
                FROM despesa d
                JOIN despesa_utilizador du ON d.id_despesa = du.id_despesa
                WHERE d.id_despesa = ? AND du.id_utilizador = (SELECT id_utilizador FROM utilizador WHERE email = ?)
        """;

              try (PreparedStatement pstmtVerificar = conn.prepareStatement(sqlVerificarDivida)) {
                  pstmtVerificar.setInt(1, idDivida);
                  pstmtVerificar.setString(2, username);
                  ResultSet rs = pstmtVerificar.executeQuery();

                  if (rs.next()) {
                      int idUtilizadorRecebedor = rs.getInt("id_utilizador_criador"); // ID do criador da dívida
                      double valorDivida = rs.getDouble(2);
                      System.out.println(valorPagamento);
                      System.out.println(valorDivida);

                      if (valorPagamento > valorDivida) {
                          return "Valor do pagamento excede a dívida.";
                      }


                      String sqlPagamento = """
                    INSERT INTO pagamento (valor, id_utilizador_pagador, id_utilizador_recebedor, id_despesa, data)
                    VALUES (?, (SELECT id_utilizador FROM utilizador WHERE email = ?), ?, ?, CURRENT_DATE)
                """;

                      try (PreparedStatement pstmtPagamento = conn.prepareStatement(sqlPagamento)) {
                          pstmtPagamento.setDouble(1, valorPagamento);
                          pstmtPagamento.setString(2, username);
                          pstmtPagamento.setInt(3, idUtilizadorRecebedor); // ID do criador da dívida
                          pstmtPagamento.setInt(4, idDivida);
                          pstmtPagamento.executeUpdate();
                      }

                      String sqlRemoverDivida = """
                    DELETE FROM despesa_utilizador
                    WHERE id_despesa = ? AND id_utilizador = (SELECT id_utilizador FROM utilizador WHERE email = ?)
                """;

                      try (PreparedStatement pstmtRemover = conn.prepareStatement(sqlRemoverDivida)) {
                          pstmtRemover.setInt(1, idDivida);
                          pstmtRemover.setString(2, username);
                          pstmtRemover.executeUpdate();
                      }

                      return "Pagamento efetuado com sucesso.";
                  } else {
                      return "Dívida não encontrada ou já paga.";
                  }
              }
          } catch (SQLException e) {
              System.out.println(e.getMessage());
              return "Erro ao efetuar pagamento.";
          }


      }

    public static Map<String, Object> visualizarSaldos(String nomeGrupo, String dbFilePath) {
        Map<String, Object> saldos = new HashMap<>();

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbFilePath)) {

            String sqlGrupo = "SELECT id_grupo FROM grupo WHERE nome = ?";
            PreparedStatement stmtGrupo = conn.prepareStatement(sqlGrupo);
            stmtGrupo.setString(1, nomeGrupo);
            ResultSet rsGrupo = stmtGrupo.executeQuery();

            if (rsGrupo.next()) {
                int idGrupo = rsGrupo.getInt("id_grupo");

                String sqlUtilizadores = "SELECT u.id_utilizador, u.email " +
                        "FROM utilizador u " +
                        "JOIN grupo_utilizador gu ON u.id_utilizador = gu.id_utilizador " +
                        "WHERE gu.id_grupo = ?";
                PreparedStatement stmtUtilizadores = conn.prepareStatement(sqlUtilizadores);
                stmtUtilizadores.setInt(1, idGrupo);
                ResultSet rsUtilizadores = stmtUtilizadores.executeQuery();

                while (rsUtilizadores.next()) {
                    int idUtilizador = rsUtilizadores.getInt("id_utilizador");
                    String emailUtilizador = rsUtilizadores.getString("email");


                    double gastoTotal = 0;
                    double totalDeve = 0;
                    double totalReceber = 0;
                    Map<String, Double> dividasPorUtilizador = new HashMap<>();
                    Map<String, Double> recebimentosPorUtilizador = new HashMap<>();

                    String sqlGastos = "SELECT SUM(d.valor) as gasto_total " +
                                       "FROM despesa d " +
                                       "WHERE d.id_utilizador = ? AND d.id_grupo = ?";
                    PreparedStatement stmtGastos = conn.prepareStatement(sqlGastos);
                    stmtGastos.setInt(1, idUtilizador);
                    stmtGastos.setInt(2, idGrupo);
                    ResultSet rsGastos = stmtGastos.executeQuery();
                    if (rsGastos.next()) {
                        gastoTotal = rsGastos.getDouble("gasto_total");
                    }

                    String sqlDividas = "SELECT u.email, du.valor_participacao " +
                                        "FROM despesa_utilizador du " +
                                        "JOIN despesa d ON du.id_despesa = d.id_despesa " +
                                        "JOIN utilizador u ON d.id_utilizador = u.id_utilizador " +
                                        "WHERE d.id_grupo = ? AND du.id_utilizador = ?";
                    PreparedStatement stmtDividas = conn.prepareStatement(sqlDividas);
                    stmtDividas.setInt(1, idGrupo);
                    stmtDividas.setInt(2, idUtilizador);
                    ResultSet rsDividas = stmtDividas.executeQuery();
                    while (rsDividas.next()) {
                        String emailCredor = rsDividas.getString("email");
                        double valorDivida = rsDividas.getDouble("valor_participacao");
                        totalDeve += valorDivida;


                        dividasPorUtilizador.put(emailCredor, valorDivida);
                    }

                    String sqlRecebimentos = "SELECT u.email, du.valor_participacao " +
                                             "FROM despesa_utilizador du " +
                                             "JOIN despesa d ON du.id_despesa = d.id_despesa " +
                                             "JOIN utilizador u ON du.id_utilizador = u.id_utilizador " +
                                             "WHERE d.id_grupo = ? AND d.id_utilizador = ?";
                    PreparedStatement stmtRecebimentos = conn.prepareStatement(sqlRecebimentos);
                    stmtRecebimentos.setInt(1, idGrupo);
                    stmtRecebimentos.setInt(2, idUtilizador);
                    ResultSet rsRecebimentos = stmtRecebimentos.executeQuery();
                    while (rsRecebimentos.next()) {
                        String emailDevedor = rsRecebimentos.getString("email");
                        double valorRecebimento = rsRecebimentos.getDouble("valor_participacao");
                        totalReceber += valorRecebimento;

                        recebimentosPorUtilizador.put(emailDevedor, valorRecebimento);
                    }

                    Map<String, Object> detalhes = new HashMap<>();
                    detalhes.put("gasto_total", gastoTotal);
                    detalhes.put("total_deve", totalDeve);
                    detalhes.put("total_receber", totalReceber);
                    detalhes.put("dividas_por_utilizador", dividasPorUtilizador);
                    detalhes.put("recebimentos_por_utilizador", recebimentosPorUtilizador);

                    saldos.put(emailUtilizador, detalhes);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return saldos;
    }

    public static boolean eliminarDespesa(String descricao, String dbFilePath) {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbFilePath)) {

            String sqlGetId = "SELECT id_despesa FROM despesa WHERE descricao = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sqlGetId)) {
                pstmt.setString(1, descricao);
                ResultSet rs = pstmt.executeQuery();

                if (rs.next()) {
                    int idDespesa = rs.getInt("id_despesa");

                    String sqlDeleteUtilizador = "DELETE FROM despesa_utilizador WHERE id_despesa = ?";
                    try (PreparedStatement pstmtDelete = conn.prepareStatement(sqlDeleteUtilizador)) {
                        pstmtDelete.setInt(1, idDespesa);
                        pstmtDelete.executeUpdate();
                    }

                    String sqlDeleteDespesa = "DELETE FROM despesa WHERE id_despesa = ?";
                    try (PreparedStatement pstmtDeleteDespesa = conn.prepareStatement(sqlDeleteDespesa)) {
                        pstmtDeleteDespesa.setInt(1, idDespesa);
                        pstmtDeleteDespesa.executeUpdate();
                    }

                    return true;

                } else {
                   return false;
                }
            }
        } catch (SQLException e) {
            return false;
        }
    }

    public static boolean editarDespesa(String descricaoAntiga, String novaDescricao, double novoValor, String novaData, String dbFilePath) {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbFilePath)) {

            String sqlGetId = "SELECT id_despesa FROM despesa WHERE descricao = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sqlGetId)) {
                pstmt.setString(1, descricaoAntiga);
                ResultSet rs = pstmt.executeQuery();

                if (rs.next()) {
                    int idDespesa = rs.getInt("id_despesa");

                    String sqlUpdate = "UPDATE despesa SET descricao = ?, valor = ?, data = ? WHERE id_despesa = ?";
                    try (PreparedStatement pstmtUpdate = conn.prepareStatement(sqlUpdate)) {
                        pstmtUpdate.setString(1, novaDescricao);
                        pstmtUpdate.setDouble(2, novoValor);
                        pstmtUpdate.setString(3, novaData);
                        pstmtUpdate.setInt(4, idDespesa);
                        pstmtUpdate.executeUpdate();
                    }

                    return true;
                } else {
                    return false;
                }
            }
        } catch (SQLException e) {
            return false;
        }
    }

    public static boolean exportarDespesasCSV(String nomeGrupo, String caminhoCSV, String username, String dbFilePath) {
        String url = "jdbc:sqlite:" + dbFilePath;

        String sqlGrupo = """
            SELECT id_grupo
            FROM grupo
            WHERE nome = ?
        """;

        String sqlElementos = """
            SELECT u.email
            FROM grupo_utilizador gu
            JOIN utilizador u ON gu.id_utilizador = u.id_utilizador
            WHERE gu.id_grupo = ?
        """;

        String sqlDespesas = """
            SELECT d.data, 
                   u.email AS responsavel, 
                   d.valor, 
                   COALESCE((SELECT u2.email 
                             FROM pagamento p 
                             JOIN utilizador u2 ON p.id_utilizador_pagador = u2.id_utilizador
                             WHERE p.id_despesa = d.id_despesa 
                             LIMIT 1), 'Não especificado') AS pago_por,
                   (SELECT GROUP_CONCAT(email, ';') 
                    FROM (SELECT DISTINCT u3.email 
                          FROM despesa_utilizador du 
                          JOIN utilizador u3 ON du.id_utilizador = u3.id_utilizador 
                          WHERE du.id_despesa = d.id_despesa)
                   ) AS a_dividir_com
            FROM despesa d
            JOIN utilizador u ON d.id_utilizador = u.id_utilizador
            WHERE d.id_grupo = ?
            GROUP BY d.id_despesa
            ORDER BY d.data;
        """;

        try (Connection conn = DriverManager.getConnection(url)) {

            int idGrupo;
            try (PreparedStatement stmtGrupo = conn.prepareStatement(sqlGrupo)) {
                stmtGrupo.setString(1, nomeGrupo);
                ResultSet rsGrupo = stmtGrupo.executeQuery();
                if (!rsGrupo.next()) {
                    System.out.println("Grupo não encontrado: " + nomeGrupo);
                    return false;
                }
                idGrupo = rsGrupo.getInt("id_grupo");
            }


            boolean pertenceAoGrupo = false;
            List<String> elementos = new ArrayList<>();

            try (PreparedStatement stmtElementos = conn.prepareStatement(sqlElementos)) {
                stmtElementos.setInt(1, idGrupo);
                ResultSet rsElementos = stmtElementos.executeQuery();
                while (rsElementos.next()) {
                    String email = rsElementos.getString("email");
                    elementos.add(email);
                    if (email.equalsIgnoreCase(username)) {
                        pertenceAoGrupo = true;
                    }
                }
            }

            if (!pertenceAoGrupo) {
                System.out.println("Utilizador não pertence ao grupo: " + username);
                return false;
            }


            StringBuilder csvContent = new StringBuilder();
            csvContent.append("Nome do grupo\n").append(nomeGrupo).append("\n");
            csvContent.append("Elementos\n").append(String.join(";", elementos)).append("\n");
            csvContent.append("Data;Responsável pelo registo da despesa;Valor;Pago por;A dividir com\n");


            try (PreparedStatement stmtDespesas = conn.prepareStatement(sqlDespesas)) {
                stmtDespesas.setInt(1, idGrupo);
                ResultSet rsDespesas = stmtDespesas.executeQuery();
                while (rsDespesas.next()) {
                    String data = rsDespesas.getString("data");
                    String responsavel = rsDespesas.getString("responsavel");
                    double valor = rsDespesas.getDouble("valor");
                    String pagoPor = rsDespesas.getString("pago_por");
                    String aDividirCom = rsDespesas.getString("a_dividir_com");

                    csvContent.append(String.format("%s;%s;%.2f;%s;%s\n",
                            data, responsavel, valor, pagoPor, aDividirCom));
                }
            }

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(caminhoCSV))) {
                writer.write(csvContent.toString());
            }

            return true;

        } catch (SQLException | IOException e) {
            System.out.println("Erro ao exportar despesas: " + e.getMessage());
            return false;
        }
    }




    public static void main(String[] args) throws IOException {

        int listeningPort;
        String dbFilePath;
        File dbFile;
        ManageDB db = new ManageDB();

        if (args.length != 2) {
            System.out.println("Sintaxe: java Servidor [port] [fileDataBaseName]");
            return;
        }

        dbFilePath = args[1].trim();
        dbFile = new File(dbFilePath);

        db.initializeDatabase();

        if (!dbFile.exists()) {
            System.out.println("A base de dados " + dbFile + " não foi criada corretamente.");
            return;
        } else if (!dbFile.canRead()) {
            System.out.println("Sem permissões de leitura na diretoria " + dbFile + "!");
            return;
        }

        try {

            listeningPort = Integer.parseInt(args[0]);

               try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + dbFilePath)) {
                System.out.println("Conectado à base de dados SQLite com sucesso.");

                startHeartBeat(listeningPort, dbFilePath);

            try (ServerSocket serverSocket = new ServerSocket(listeningPort)) {

                System.out.println("Servidor iniciado na porta " + listeningPort + ". Aguardando conexões...");

                while (true) {
                    try {
                        Socket socket = serverSocket.accept();

                        socket.setSoTimeout(TIMEOUT);

                        System.out.println("Cliente conectado: " + socket.getInetAddress());

                        ClientHandler clientHandler = new ClientHandler(listeningPort,socket, connection, dbFilePath);
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




