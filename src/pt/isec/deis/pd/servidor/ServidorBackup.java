package pt.isec.deis.pd.servidor;

import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.time.Instant;
import java.util.Objects;

import static java.lang.System.exit;
import static pt.isec.deis.pd.servidor.Servidor.HEARTBEAT_ADDRESS;
import static pt.isec.deis.pd.servidor.Servidor.HEARTBEAT_PORT;

public class ServidorBackup {

    private static final int TIMEOUT_SECONDS = 30;

    public static void main(String[] args)  {

        String dbFilePath;

        if(args.length != 2) {
            System.out.println("Sintaxe : java ServidorBackup <port> <DBCopy package>");
        }

        dbFilePath = args[1].trim();
        File dbFile = new File(dbFilePath);

        if(!dbFile.isDirectory() || Objects.requireNonNull(dbFile.list()).length != 0) {
            System.out.println("A diretoria não está vazia.");
            System.out.println("A terminar backup...");
            exit(-1);
        }

      try {
            InetAddress group = InetAddress.getByName(HEARTBEAT_ADDRESS);
            try (MulticastSocket multicastSocket = new MulticastSocket(HEARTBEAT_PORT)) {
                multicastSocket.joinGroup(group); // Junta-se ao grupo multicast
                byte[] buffer = new byte[1024];
                System.out.println("Escutando heartbeats...");
                Instant lastHeartbeatTime = Instant.now();

                while (true) {
                    multicastSocket.setSoTimeout(1000);
                    try {
                        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                        multicastSocket.receive(packet); //bloqueante


                        lastHeartbeatTime = Instant.now();

                        String message = new String(packet.getData(), 0, packet.getLength());
                        System.out.println("Heartbeat recebido: " + message);

                    } catch (IOException e) {

                        if (Instant.now().isAfter(lastHeartbeatTime.plusSeconds(TIMEOUT_SECONDS))) {
                            System.out.println("Heartbeat não recebido por 30 segundos. Encerrando servidor de backup...");
                            break;
                        }
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Erro no servidor de backup: " + e.getMessage());
        }
    }


}
