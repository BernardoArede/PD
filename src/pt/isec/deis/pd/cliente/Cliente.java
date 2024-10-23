package pt.isec.deis.pd.cliente;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.sql.SQLOutput;
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
                            System.out.println();
                            System.out.println("MENU:");
                            System.out.println("1- Criar grupo");
                            System.out.println("2- Ver Grupos");
                            System.out.println("3- Criar Convite");
                            System.out.println("4- Ver Convites");
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
                                            System.out.println("Indique o utilizador que pretende convidar indicando o seu username(email):");
                                            String guest = sc.nextLine();
                                            out.println(guest);
                                            break;

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
