package pt.isec.deis.pd.servidor;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.sql.Connection;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class ClientHandler implements Runnable {

    private final Socket socket;
    private final Connection connection;
    private final String dbFilePath;
    private boolean _islogged = false;
    private int listeningPort;

    public ClientHandler(int listeningPort,Socket socket, Connection connection, String dbFilePath) {
        this.socket = socket;
        this.connection = connection;
        this.dbFilePath = dbFilePath;
        this.listeningPort = listeningPort;
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
                            if(command != null) {
                                if (command.startsWith("GRUPO:")) {
                                    String groupName = command.split(":")[1].trim();
                                    if (Servidor.grupoExiste(groupName, dbFilePath)) {
                                        out.println("FAIL_GROUP_EXIST");
                                    } else {
                                        if (Servidor.criarGrupo(groupName, username, dbFilePath)) {
                                            out.println("GROUP_CREATED");
                                            Servidor.upVersionDB(dbFilePath);
                                            Servidor.startHeartBeat(listeningPort,dbFilePath);
                                        } else {
                                            out.println("FAIL_CREATE_GROUP");
                                        }
                                    }
                                }
                                if (command.startsWith("VER GRUPOS:")) {
                                    Servidor.imprimeGrupos(username, dbFilePath, out);
                                }
                                if (command.startsWith("CONVITES:")) {
                                    String invites = in.readLine();
                                    String[] parte = invites.split(":");
                                    String user_to_invite = parte[1];
                                    String group_to_invite = parte[2];
                                    System.out.println(user_to_invite);
                                    System.out.println(group_to_invite);
                                    if (Servidor.enviarConvite(user_to_invite, group_to_invite, dbFilePath)) {
                                        System.out.println("Enviado com sucesso");
                                    } else {
                                        System.out.println("Erro ao enviar convite");
                                    }

                                }
                                if (command.startsWith("VER CONVITES")) {
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
                                }
                                if (command.startsWith("EDITAR NOME GRUPO:")) {
                                    String[] splits = command.split(":");
                                    String nomeAtualGrupo = splits[1];
                                    String novoNomeGrupo = splits[2];

                                    if (Servidor.editarNomeGrupo(username, nomeAtualGrupo, novoNomeGrupo, dbFilePath)) {
                                        out.println("Nome do grupo alterado com sucesso para: " + novoNomeGrupo);
                                        Servidor.upVersionDB(dbFilePath);
                                    } else {
                                        out.println("Erro: Não foi possível alterar o nome do grupo. Verifique se você pertence ao grupo.");
                                    }
                                }
                                if (command.startsWith("INSERIR DESPESA:")) {
                                    String[] partes2 = command.split(":");
                                    String nomeGrupo = partes2[1];
                                    String descricao = partes2[2];
                                    double valor = Double.parseDouble(partes2[3]);
                                    String data = partes2[4];

                                    String participantesStr = in.readLine();
                                    List<String> participantes = Arrays.asList(participantesStr.split(","));

                                    if (Servidor.inserirDespesa(username, nomeGrupo, descricao, valor, participantes, data, dbFilePath)) {
                                        out.println("Despesa inserida com sucesso.");
                                        Servidor.upVersionDB(dbFilePath);
                                    } else {
                                        out.println("Erro ao inserir a despesa. Verifique os dados e tente novamente.");
                                    }
                                }
                                if (command.startsWith("ELIMINAR GRUPO:")) {
                                    String[] partes3 = command.split(":");
                                    if (partes3.length == 2) {
                                        String nomeGrupo = partes3[1];
                                        if (Servidor.eliminarGrupoPorNome(nomeGrupo, dbFilePath)) {
                                            out.println("Grupo '" + nomeGrupo + "' eliminado com sucesso.");
                                            Servidor.upVersionDB(dbFilePath);
                                        } else {
                                            out.println("Não foi possível eliminar o grupo. Existem despesas associadas ou o grupo não existe.");
                                        }
                                    } else {
                                        out.println("Formato do comando errado.");
                                    }
                                }
                                if (command.startsWith("SAIR DO GRUPO:")) {
                                    String nomeGrupo = command.split(":")[1];
                                    if (Servidor.sairDoGrupo(nomeGrupo, username, dbFilePath)) {
                                        out.println("Você saiu do grupo '" + nomeGrupo + "' com sucesso.");
                                    } else {
                                        out.println("Não foi possível sair do grupo '" + nomeGrupo + "'. Verifique se há despesas associadas.");
                                    }
                                }
                                if (command.startsWith("OBTER TOTAL GASTOS:")) {
                                    String[] partes3 = command.split(":");
                                    String nomeGrupo = partes3[1];

                                    double totalGastos = Servidor.obterTotalGastos(nomeGrupo, username, dbFilePath);
                                    if (totalGastos >= 0) {
                                        out.println("Total de gastos do grupo " + nomeGrupo + ": " + totalGastos);
                                    } else {
                                        out.println("Erro ao obter os gastos ou o utilizador não pertence ao grupo.");
                                    }
                                }
                                if (command.startsWith("OBTER HISTORICO DESPESAS:")) {
                                    String[] partes4 = command.split(":");
                                    String nomeGrupo = partes4[1];

                                    List<String> historicoDespesas = Servidor.obterHistoricoDespesas(nomeGrupo, username, dbFilePath);
                                    if (historicoDespesas != null) {
                                        for (String detalhe : historicoDespesas) {
                                            out.println(detalhe);
                                        }
                                        out.println("FIM HISTORICO");
                                    } else {
                                        out.println("Erro ao obter o histórico ou o utilizador não pertence ao grupo.");
                                    }
                                }/*if (command.startsWith("EXPORTAR DESPESAS CSV:")) {
                                    String[] partes4 = command.split(":");
                                    String nomeGrupo = partes4[1];
                                    String caminhoCSV = partes4[2];

                                    boolean sucesso = Servidor.exportarDespesasCSV(nomeGrupo, caminhoCSV, username, dbFilePath);
                                    if (sucesso) {
                                        out.println("Exportação concluída com sucesso.");
                                    } else {
                                        out.println("Erro ao exportar as despesas para CSV ou o utilizador não pertence ao grupo.");
                                    }
                                }*/
                                if (command.startsWith("OBTER DIVIDAS:")) {
                                    String[] partes5 = command.split(":");
                                    String nomeGrupo = partes5[1];

                                    List<String> dividas = Servidor.obterDividas(nomeGrupo, username, dbFilePath);

                                    if (dividas.isEmpty()) {
                                        out.println("NENHUMA DÍVIDA");
                                    } else {
                                        System.out.println("Número de dívidas encontradas: " + dividas.size());
                                        for (String divida : dividas) {
                                            out.println(divida);
                                        }
                                        out.println("FIM");
                                    }
                                }
                                if (command.startsWith("EFETUAR PAGAMENTO:")) {
                                    String[] partes5 = command.split(":");
                                    int idDivida = Integer.parseInt(partes5[1]);
                                    double valorPagamento = Double.parseDouble(partes5[2]);

                                    String resposta = Servidor.efetuarPagamento(username, idDivida, valorPagamento, dbFilePath);
                                    out.println(resposta);
                                }
                                if (command.startsWith("VISUALIZAR SALDOS:")) {
                                    String[] partes6 = command.split(":");
                                    String nomeGrupo = partes6[1];

                                    Map<String, Object> saldos = Servidor.visualizarSaldos(nomeGrupo, dbFilePath);

                                    if (saldos.isEmpty()) {
                                        out.println("NENHUM SALDO ENCONTRADO");
                                    } else {
                                        for (Map.Entry<String, Object> entry : saldos.entrySet()) {
                                            String utilizador = entry.getKey();
                                            Map<String, Object> detalhes = (Map<String, Object>) entry.getValue();
                                            StringBuilder resposta = new StringBuilder();
                                            resposta.append("Utilizador: ").append(utilizador)
                                                    .append(", Gasto Total: ").append(detalhes.get("gasto_total"))
                                                    .append(", Total que Deve: ").append(detalhes.get("total_deve"))
                                                    .append(", Total a Receber: ").append(detalhes.get("total_receber"))
                                                    .append(", Dividas por Utilizador: ").append(detalhes.get("dividas_por_utilizador"))
                                                    .append(", Recebimentos por Utilizador: ").append(detalhes.get("recebimentos_por_utilizador"));
                                            out.println(resposta);
                                            Servidor.upVersionDB(dbFilePath);
                                        }
                                    }
                                }if(command.startsWith("LOGOUT")){
                                        String[] logout = command.split(":");
                                        String usernameLogout = logout[1];  // Obtenha o nome do usuário


                                        out.println("Logout bem-sucedido para o usuário: " + usernameLogout);

                                }if(command.startsWith("ELIMINAR DESPESA:")){
                                    String[] eliminar = command.split(":");
                                    String descricaoDespesa = eliminar[1]; // Pegar a descrição da despesa

                                    boolean resposta = Servidor.eliminarDespesa(descricaoDespesa, dbFilePath);
                                    String toServerResp;
                                    if(resposta){toServerResp = "Despesa " + descricaoDespesa + " excluída com sucesso.";}
                                    else{toServerResp = "Despesa não encontrada";}
                                    out.println(toServerResp);
                                    Servidor.upVersionDB(dbFilePath);


                                }if(command.startsWith("EDITAR DESPESA")){
                                    String[] editar = command.split(":");
                                    String descricaoAntiga = editar[1]; // Descrição atual da despesa
                                    String novaDescricao = editar[2]; // Nova descrição da despesa
                                    double novoValor = Double.parseDouble(editar[3]); // Novo valor da despesa
                                    String novaData = editar[4]; // Nova data da despesa

                                    boolean resposta = Servidor.editarDespesa(descricaoAntiga, novaDescricao, novoValor, novaData, dbFilePath);
                                    String toServerResp;
                                    if(resposta){toServerResp = "Despesa " + descricaoAntiga + " editada com sucesso.";}
                                    else{toServerResp = "Despesa não encontrada";}
                                    out.println(toServerResp);
                                    Servidor.upVersionDB(dbFilePath);
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

