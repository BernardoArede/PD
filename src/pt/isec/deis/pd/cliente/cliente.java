package pt.isec.deis.pd.cliente;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;


public class cliente {

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
                        while(true){
                            System.out.println("Estou ligado cliente com o identificador    " + email);
                            try{
                                Thread.sleep(5000);
                            }catch (InterruptedException e){
                                System.out.println("Erro ao pausar a execuação do cliente" + e.getMessage());
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

                        System.out.println("Deseja fazer login?(yes/no)");
                        break;
            }


        }catch (IOException e) {
            System.out.println("Erro ao conectar com o servidor: " + e.getMessage());
        }
    }
}
