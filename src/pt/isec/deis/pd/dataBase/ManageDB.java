package pt.isec.deis.pd.dataBase;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class ManageDB {

    public static void main(String[] args) {

        String url = "jdbc:sqlite:src/resources/identifier.sqlite";

        String sql = """
                
                CREATE TABLE IF NOT EXISTS db_version(
                db_version REAL PRIMARY KEY 
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
            System.out.println("Base de dados e tabelas criadas com sucesso.");
        } catch (Exception e) {
            System.out.println("Erro ao criar a base de dados: " + e.getMessage());
        }
    }
}
