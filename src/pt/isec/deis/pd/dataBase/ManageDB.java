package pt.isec.deis.pd.dataBase;

import java.io.File;
import java.sql.*;

public class ManageDB {

    public void initializeDatabase() {
        File dbFile = new File("src/pt/isec/deis/pd/resources/identifier.sqlite");
        if (!dbFile.exists()) {
            System.out.println("Base de dados não existe. Criando...");
            createDB();
            System.out.println("Base de dados e tabelas criadas com sucesso.");
        } else {
            System.out.println("Base de dados já existe.");
        }
    }

    public  void createDB() {

     String url = "jdbc:sqlite:src/pt/isec/deis/pd/resources/identifier.sqlite";
     String sql = """
                
                CREATE TABLE IF NOT EXISTS db_version(
                db_version REAL PRIMARY KEY DEFAULT 0
        );
                CREATE TABLE IF NOT EXISTS convite (
                    id_convite INTEGER PRIMARY KEY AUTOINCREMENT,
                    id_grupo INTEGER,
                    id_utilizador_convidado INTEGER,
                    estado TEXT DEFAULT 'pendente',
                    FOREIGN KEY (id_grupo) REFERENCES grupo(id_grupo) ON DELETE CASCADE,
                    FOREIGN KEY (id_utilizador_convidado) REFERENCES utilizador(id_utilizador) ON DELETE CASCADE
                );

                CREATE TABLE IF NOT EXISTS grupo (
                    id_grupo INTEGER PRIMARY KEY AUTOINCREMENT,
                    nome TEXT NOT NULL
                );

                CREATE TABLE IF NOT EXISTS utilizador (
                    id_utilizador INTEGER PRIMARY KEY AUTOINCREMENT,
                    email TEXT NOT NULL UNIQUE, 
                    password TEXT NOT NULL,
                    telefone NUMERIC NOT NULL UNIQUE
                                      
                );

                CREATE TABLE IF NOT EXISTS grupo_utilizador (
                    id_grupo INTEGER,
                    id_utilizador INTEGER,
                    PRIMARY KEY (id_grupo, id_utilizador),
                    FOREIGN KEY (id_grupo) REFERENCES grupo(id_grupo) ON DELETE CASCADE,
                    FOREIGN KEY (id_utilizador) REFERENCES utilizador(id_utilizador) ON DELETE CASCADE
                );
                CREATE TABLE IF NOT EXISTS despesa_utilizador (
                    id_despesa INTEGER,
                    id_utilizador INTEGER,
                    valor_participacao REAL NOT NULL,
                    PRIMARY KEY (id_despesa, id_utilizador),
                    FOREIGN KEY (id_despesa) REFERENCES despesa(id_despesa) ON DELETE CASCADE,
                    FOREIGN KEY (id_utilizador) REFERENCES utilizador(id_utilizador) ON DELETE CASCADE
                );

                CREATE TABLE IF NOT EXISTS despesa (
                    id_despesa INTEGER PRIMARY KEY AUTOINCREMENT,
                    descricao TEXT NOT NULL,
                    valor REAL NOT NULL,
                    data TEXT NOT NULL,  -- Formato: YYYY-MM-DD
                    id_grupo INTEGER,
                    id_utilizador INTEGER,
                    FOREIGN KEY (id_grupo) REFERENCES grupo (id_grupo),
                    FOREIGN KEY (id_utilizador) REFERENCES utilizador (id_utilizador)
                );

                CREATE TABLE IF NOT EXISTS pagamento (
                    id_pagamento INTEGER PRIMARY KEY AUTOINCREMENT,
                    valor REAL NOT NULL,
                    id_utilizador_pagador INTEGER,
                    id_utilizador_recebedor INTEGER,
                    id_despesa INTEGER,
                    data DATE NOT NULL,
                    FOREIGN KEY (id_utilizador_pagador) REFERENCES utilizador(id_utilizador) ON DELETE CASCADE,
                    FOREIGN KEY (id_utilizador_recebedor) REFERENCES utilizador(id_utilizador) ON DELETE CASCADE,
                    FOREIGN KEY (id_despesa) REFERENCES despesa(id_despesa) ON DELETE SET NULL
                );
                """;

        try (Connection conn = DriverManager.getConnection(url);
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);

             String insertVersion = """
                    INSERT INTO db_version (db_version)
                    SELECT 0
                    WHERE NOT EXISTS (SELECT 1 FROM db_version);
                    """;
            stmt.executeUpdate(insertVersion);
        } catch (Exception e) {
            System.out.println("Erro ao criar a base de dados: " + e.getMessage());
        }
    }

    public static double getVersion(String dbPath) {
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

    public static void upVersionDB(String dbPath) {
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

}
