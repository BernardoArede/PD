package pt.isec.deis.pd.cliente;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;
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

                            for(int i = 0;i<20;i++){
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


                            System.out.println();
                            System.out.println("Escolhe uma opção do menu: ");
                            int option = sc.nextInt();

                            switch (option){
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
                                            out.println("CONVIDADO:"+guest +  ":" + group);
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

                                                    // Receber resposta do servidor
                                                    String respostaEdicao = in.readLine();
                                                    System.out.println(respostaEdicao);
                                                    break;

                                                    case 6:
                                                        /*TODO Eliminar grupo caso não haja despesas associadas*/
                                                        break;
                                                        case 7:
                                                            /*TODO Sair de um grupo*/
                                                            break;
                                                            case 8:
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
                                default:
                                    System.out.println("ERRO DE ESOLHA DE OPÇÃO");
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


