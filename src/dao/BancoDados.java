package dao;

import java.sql.*;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class BancoDados {
    
    public static Connection conectar() throws SQLException, IOException {
        Properties props = carregarPropriedades();
        String url = props.getProperty("dburl");

        return DriverManager.getConnection(url, props);
    }

    private static Properties carregarPropriedades() throws IOException{
        try(FileInputStream propriedadesBanco = new FileInputStream("database.properties")){
            Properties props = new Properties();
            props.load(propriedadesBanco);
            return props;
        }
    }
}
