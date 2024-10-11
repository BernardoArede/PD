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
        int port;

        if(args.length != 2) {
            System.out.println("Sintaxa: [Endereço] [Porta]");
            return;
        }

        serverAddress = args[0];
        port = Integer.parseInt(args[1]);


        try (Socket socket = new Socket(serverAddress, port);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(),true)){

            Scanner sc = new Scanner(System.in);

            System.out.println("Digite o Email do Cliente: ");
            String email = sc.nextLine();
            System.out.println("Digite a password do Cliente: ");
            String password = sc.nextLine();

            out.println("AUTH: " + email + " " + password);


        }catch (IOException e) {
            System.out.println("Erro ao conectar com o servidor: " + e.getMessage());
        }



    }
}
