package pt.isec.deis.pd.cliente;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Scanner;


public class Cliente {

    public static void main(String[] args) {

        String serverAddress;
        int opt;
        int port;
        Scanner sc = new Scanner(System.in);
        boolean login = false;

        if(args.length != 2) {
            System.out.println("Sintaxa: [Endereço] [Porta]");
            return;
        }

        serverAddress = args[0];
        port = Integer.parseInt(args[1]);


        try (Socket socket = new Socket(serverAddress, port);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(),true)){

            //Menu
            System.out.println("Escolha uma opção");
            System.out.println("1. Login");
            System.out.println("2. Registo");
            System.out.print("\nOpção: ");
            opt = sc.nextInt();

               switch(opt){
                case 1:
                    sc.nextLine();
                    System.out.println("Digite o Email do Cliente: ");
                    String email = sc.nextLine();
                    System.out.println("Digite a password do Cliente: ");
                    String password = sc.nextLine();

                    //Envio das credencias para o servidor
                    out.println("AUTH: " + email + " " + password);

                    String credenciais_resp;
                    try{
                        credenciais_resp =  in.readLine();
                    }catch (IOException e){
                        credenciais_resp = null;
                    }

                    System.out.println(credenciais_resp);

                    if(credenciais_resp != null) {
                        login = true;

                        while(login){

                            for(int i = 0;i<5;i++){
                                System.out.println();
                            }
                            System.out.println("MENU:");
                            System.out.println("1- Criar grupo");
                            System.out.println("2- Ver Grupos");
                            System.out.println("3- Criar Convite");
                            System.out.println("4- Ver Convites");
                            System.out.println("5- Editar nome de grupo");
                            System.out.println("6- Eliminar um grupo");
                            System.out.println("7- Sair do grupo");
                            System.out.println("8- Adicionar uma despesa");
                            System.out.println("9- Ver gastos totais do grupo");
                            System.out.println("10- Ver histórico de despesas");
                            System.out.println("11- Editar despesa");
                            System.out.println("12- Eliminar despesa");
                            System.out.println("13- Exportar para CSV detalhes do grupo");
                            System.out.println("14- Liquidação de dividas");
                            System.out.println("15- Visualização dos saldos do grupo corrente");
                            System.out.println("16- Logout");
                            System.out.println();
                            System.out.println("Escolhe uma opção do menu: ");
                            int option = sc.nextInt();

                            switch (option) {
                                case 1:
                                    sc.nextLine();
                                    System.out.println("Digite o nome do Grupo: ");
                                    String nome = sc.nextLine();
                                    out.println("GRUPO: " + nome);
                                    String resp_criar_grupo = in.readLine();
                                    System.out.println("SERVIDOR->" + resp_criar_grupo);
                                    break;

                                case 2:
                                    sc.nextLine();
                                    out.println("VER GRUPOS:");
                                    String resp_ver_grupos;
                                    while (!(resp_ver_grupos = in.readLine()).equals("FIM")) {
                                        System.out.println(resp_ver_grupos);
                                    }
                                    break;

                                case 3:
                                    sc.nextLine();
                                    out.println("CONVITES:");
                                    System.out.println("Indique o utilizador que pretende convidar indicando o seu username(email): ");
                                    String guest = sc.nextLine();
                                    System.out.println("Indique o nome do grupo para o qual pretende convidar: ");
                                    String group = sc.nextLine();
                                    out.println("CONVIDADO:" + guest + ":" + group);
                                    break;

                                case 4:
                                    sc.nextLine();
                                    out.println("VER CONVITES:");
                                    String response;
                                    while (!(response = in.readLine()).equals("END")) {
                                        System.out.println(response);
                                    }

                                    response = in.readLine();
                                    System.out.println(response);
                                    String escolha = sc.nextLine();

                                    if (escolha.equalsIgnoreCase("sim")) {
                                        out.println(escolha);
                                        response = in.readLine();
                                        System.out.println(response);
                                        String grupoEscolhido = sc.nextLine();
                                        out.println(grupoEscolhido);
                                        response = in.readLine();
                                        System.out.println(response);
                                    } else {
                                        out.println("Nenhum convite foi aceite.");
                                    }
                                    break;

                                case 5:
                                    sc.nextLine();
                                    System.out.println("Indique o nome do grupo que deseja editar:");
                                    String nomeAtualGrupo = sc.nextLine();
                                    System.out.println("Indique o novo nome para o grupo:");
                                    String novoNomeGrupo = sc.nextLine();
                                    out.println("EDITAR NOME GRUPO:" + nomeAtualGrupo + ":" + novoNomeGrupo);
                                    String respostaEdicao = in.readLine();
                                    System.out.println(respostaEdicao);
                                    break;

                                case 6:
                                    //Eliminar grupo
                                    sc.nextLine();
                                    System.out.println("Indique o nome do grupo que deseja eliminar:");
                                    String nomeGrupo = sc.nextLine();
                                    out.println("ELIMINAR GRUPO:" + nomeGrupo);String resposta = in.readLine();
                                    System.out.println(resposta);
                                    break;

                                case 7:
                                    // SAIR DO GRUPO
                                    sc.nextLine();
                                    System.out.println("Indique o nome do grupo do qual deseja sair:");
                                    String nomeGrupoEliminarUtilizador = sc.nextLine();
                                    // Enviar pedido para o servidor
                                    out.println("SAIR DO GRUPO:" + nomeGrupoEliminarUtilizador);
                                    // Ler a resposta do servidor
                                    String resposta2 = in.readLine();
                                    System.out.println(resposta2);
                                    break;

                                case 8:
                                    /* ADICIONAR DESPESA */
                                    sc.nextLine();
                                    System.out.println("Indique o nome do grupo:");
                                    String nomeGrupoDespesa = sc.nextLine();
                                    System.out.println("Descrição da despesa:");
                                    String descricaoDespesa = sc.nextLine();
                                    System.out.println("Valor da despesa:");
                                    double valorDespesa = sc.nextDouble();
                                    sc.nextLine();
                                    System.out.println("Data da despesa (formato YYYY-MM-DD):");
                                    String dataDespesa = sc.nextLine();
                                    System.out.println("Indique os usernames dos participantes (separados por vírgula):");
                                    String participantesInput = sc.nextLine();
                                    List<String> participantes = Arrays.asList(participantesInput.split(","));
                                    out.println("INSERIR DESPESA:" + nomeGrupoDespesa + ":" + descricaoDespesa + ":" + valorDespesa + ":" + dataDespesa);
                                    out.println(String.join(",", participantes));
                                    String respostaDespesa = in.readLine();
                                    System.out.println(respostaDespesa);
                                    break;
                                case 9:
                                    //GASTOS TOTAIS DO GRUPO SEM SER DESCRIMINADO
                                    sc.nextLine();
                                    System.out.println("Indique o nome do grupo:");
                                    String nomeGrupoGastos = sc.nextLine();
                                    out.println("OBTER TOTAL GASTOS:" + nomeGrupoGastos);

                                    String respostaTotalGastos = in.readLine();
                                    System.out.println(respostaTotalGastos);
                                    break;

                                case 10:
                                    //VER HISTORICO DE DESPESAS
                                    sc.nextLine();
                                    System.out.println("Indique o nome do grupo:");
                                    String nomeGrupoHistorico = sc.nextLine();
                                    out.println("OBTER HISTORICO DESPESAS:" + nomeGrupoHistorico);

                                    String respostaHistorico = in.readLine();
                                    while (!respostaHistorico.equals("FIM HISTORICO")) {
                                        System.out.println(respostaHistorico);
                                        respostaHistorico = in.readLine();
                                    }

                                break;

                                case 11:
                                     sc.nextLine();
                                    System.out.println("Indique o nome da despesa que deseja editar:");
                                    String descricaoAntiga = sc.nextLine();

                                    System.out.println("Insira a nova descrição da despesa:");
                                    String novaDescricao = sc.nextLine();

                                    System.out.println("Insira o novo valor da despesa:");
                                    double novoValor = sc.nextDouble();
                                    sc.nextLine();

                                    System.out.println("Insira a nova data da despesa (Formato: YYYY-MM-DD):");
                                    String novaData = sc.nextLine();


                                    out.println("EDITAR DESPESA:" + descricaoAntiga + ":" + novaDescricao + ":" + novoValor + ":" + novaData);
                                    String respostaEdicaoDespesa = in.readLine();
                                    System.out.println(respostaEdicaoDespesa);


                                    break;

                                case 12:
                                    /*TODO->Eliminar uma despesa-> Podemos inserir para listar despesas
                                       antes apesar de não ser necessario*/

                                     sc.nextLine(); // Limpa o buffer
                                     System.out.println("Indique o nome da despesa que deseja excluir:");
                                    String nomeDespesa = sc.nextLine();

                                    out.println("ELIMINAR DESPESA:" + nomeDespesa);
                                    String respostaEliminacao = in.readLine();
                                    System.out.println(respostaEliminacao);
                                    break;

                                case 13:
                                    //EXPORTAR DESPESAS FICHEIRO CSV
                                    sc.nextLine();
                                    System.out.print("Digite o nome do grupo: ");
                                    String nomeGrupoCSV = sc.nextLine();


                                    System.out.print("Deseja salvar no caminho padrão (src/resources)? (s/n): ");
                                    String escolhaCaminho = sc.nextLine().trim().toLowerCase();

                                    String caminhoCSV;
                                    if (escolhaCaminho.equals("s")) {
                                        caminhoCSV = "src/resources/despesas.csv";
                                    } else {
                                        System.out.print("Digite o caminho completo para salvar o ficheiro CSV (ex: C:\\Users\\user\\Desktop\\despesas.csv): ");
                                        caminhoCSV = sc.nextLine();
                                    }

                                    out.println("EXPORTAR DESPESAS CSV:" + nomeGrupoCSV + ":" + caminhoCSV);


                                    String respostaExportacao;
                                    while (!(respostaExportacao = in.readLine()).equals("FIM EXPORTACAO")) {
                                        System.out.println(respostaExportacao);
                                    }
                                    break;
                                case 14:


                                     sc.nextLine();
                                    System.out.println("Indique o nome do grupo:");
                                    String nomeGrupoPagamento = sc.nextLine();


                                    out.println("OBTER DIVIDAS:" + nomeGrupoPagamento);
                                    System.out.println("Suas dívidas:");
                                    String respostaDividas;
                                    while ((respostaDividas = in.readLine()) != null) {
                                        if(respostaDividas.equals("FIM") || respostaDividas.equals("NENHUMA DÍVIDA")){
                                            break;
                                        }else{
                                            System.out.println(respostaDividas);
                                        }

                                    }

                                    if (respostaDividas.equals("NENHUMA DÍVIDA")) {
                                        System.out.println("Você não tem dívidas com ninguém neste grupo.");
                                    } else {
                                        System.out.println("Insira o ID da dívida que deseja pagar:");

                                        // Adicionando validação para o ID da dívida
                                        int idDivida = sc.nextInt();
                                        sc.nextLine();  // Limpar o buffer do scanner

                                        System.out.println("Insira o valor que deseja pagar:");
                                        double valorPagamento = sc.nextDouble();
                                        sc.nextLine();  // Limpar o buffer do scanner

                                        // Efetuar o pagamento
                                        out.println("EFETUAR PAGAMENTO:" + idDivida + ":" + valorPagamento);
                                        String respostaPagamento = in.readLine();
                                        System.out.println(respostaPagamento);
                                    }
                                    break;
                                case 15:
                                    /*TODO->Visualização dos saldos do grupo corrente com, para cada elemento, indicação do:
                                        o gasto total;
                                        o valor total que deve;
                                        o valor que que deve a cada um dos restantes elementos;
                                        o valor total que tem a receber;
                                        o valor que tem a receber de cada um dos restantes elementos;


                                        NÃO ESTÁ TOTALMENTE CORRETA A IMPLEMENTAÇÃO FEITA-CORRIGIR

                                        */

                                    sc.nextLine();


                                    System.out.println("Indique o nome do grupo:");
                                    String nomeGrupoSaldos = sc.nextLine();

                                    // Solicitar a visualização dos saldos
                                    out.println("VISUALIZAR SALDOS:" + nomeGrupoSaldos);
                                    String respostaSaldos = in.readLine();

                                    if (respostaSaldos.equals("NENHUM SALDO ENCONTRADO")) {
                                        System.out.println("Nenhum saldo encontrado para o grupo " + nomeGrupoSaldos);
                                    } else {
                                        // Exibir os saldos recebidos
                                        System.out.println(respostaSaldos);
                                        while (in.ready()) {
                                            System.out.println(in.readLine());
                                        }
                                    }

                                    break;
                                case 16:

                                    System.out.println("Fazendo logout...");
                                    out.println("LOGOUT:" + email);
                                    String respostaLogout = in.readLine();
                                    System.out.println(respostaLogout);
                                    System.exit(0);

                                    break;

                                default:
                                        System.out.println("ERRO DE ESCOLHA DE OPÇÃO");
                            }
                        }
                    }else{
                        System.out.println("Credencias incorretas");
                    }
                    break;

                    case 2:
                        sc.nextLine();
                        System.out.println("Digite o Email do Cliente: ");
                        String email_registo = sc.nextLine();
                        System.out.println("Digite a password do Cliente: ");
                        String password_registo = sc.nextLine();
                        System.out.println("Digite o numero de telefone do Cliente: ");
                        String telefone_registo = sc.nextLine();

                        out.println("REGIST: " + email_registo + " " + password_registo + " " + telefone_registo);

                        String regist_resp;
                        try{
                            regist_resp =  in.readLine();
                        }catch (IOException e){
                            regist_resp = null;
                        }

                        System.out.println(regist_resp);
                        break;
            }


        }catch (IOException e) {
            System.out.println("Erro ao conectar com o servidor: " + e.getMessage());
        }
    }
}


