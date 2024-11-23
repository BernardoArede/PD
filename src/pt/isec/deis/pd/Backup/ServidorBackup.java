package pt.isec.deis.pd.Backup;

import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.Instant;


import static java.lang.System.exit;
import static pt.isec.deis.pd.servidor.Servidor.HEARTBEAT_ADDRESS;
import static pt.isec.deis.pd.servidor.Servidor.HEARTBEAT_PORT;


public class ServidorBackup {

    private static final int TIMEOUT_SECONDS = 30;
    private static final String BACKUP_DIR = "src/pt/isec/deis/pd/Backup/DB_Backup";
     private static final String DB_DIR = "src/pt/isec/deis/pd/resources/identifier.sqlite";
     private static double localVersion = 0.0;

    public static void main(String[] args)  {

        String dbFilePath;
        if(args.length != 2) {
            System.out.println("Sintaxe : java ServidorBackup <port> <DBCopy package>");
        }

        dbFilePath = args[1].trim();
        File dbFile = new File(dbFilePath);

        if(!dbFile.isDirectory() || dbFile.list().length != 0 ) {
            System.out.println("A diretoria não está vazia.");
            System.out.println("A terminar backup...");
            exit(-1);
        }


      try {
            InetAddress group = InetAddress.getByName(HEARTBEAT_ADDRESS);
            try (MulticastSocket multicastSocket = new MulticastSocket(HEARTBEAT_PORT)) {
                multicastSocket.joinGroup(group);
                byte[] buffer = new byte[1024];
                System.out.println("Escutando heartbeats...");
                Instant lastHeartbeatTime = Instant.now();
                while (true) {
                    multicastSocket.setSoTimeout(1000);
                    try {
                        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                        multicastSocket.receive(packet);
                        lastHeartbeatTime = Instant.now();

                           String message = new String(packet.getData(), 0, packet.getLength());
                        System.out.println("Heartbeat recebido: " + message);

                        if (processHeartbeat(message)) {
                            upgradeDataBase();
                        } else {
                            System.out.println("Finalizando devido à inconsistência detectada...");
                            break;
                        }

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

     private static boolean processHeartbeat(String message) {
        try {
            String[] parts = message.split("; ");
            double version = 0.0;
            String query = null;

            for (String part : parts) {
                if (part.startsWith("Versão:")) {
                    version = Double.parseDouble(part.split(": ")[1]);
                } else if (part.startsWith("Query:")) {
                    query = part.split(": ")[1].trim();
                }
            }

            if (query == null || query.isEmpty()) {

                if (version != localVersion) {
                    System.out.println("Versão incompatível sem query! Local: " + localVersion + ", Recebida: " + version);
                    return false;
                }
            } else {

                if (version != localVersion + 1) {
                    System.out.println("Versão incompatível com query! Local: " + localVersion + ", Recebida: " + version);
                    return false;
                }

                System.out.println("Query recebida: " + query);
            }


            localVersion = version;
            return true;

        } catch (Exception e) {
            System.err.println("Erro ao processar heartbeat: " + e.getMessage());
            return false;
        }
    }

    private static void upgradeDataBase() throws IOException {
        File sourceFile = new File((DB_DIR));
        String backupFileName = BACKUP_DIR + "/backup.sqlite";
        File backupFile = new File(backupFileName);
        try {
            Files.copy(sourceFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            System.out.println("Base de dados copiada para: " + backupFileName);
        } catch (IOException e) {
            System.err.println("Erro ao copiar base de dados: " + e.getMessage());
        }
    }


}
