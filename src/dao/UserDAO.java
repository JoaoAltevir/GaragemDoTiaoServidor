package dao;

import java.sql.*;

import entities.User;

import java.io.IOException;

public class UserDAO {
    public void register(String name, String password, String username) throws SQLException{

        String sql = "INSERT INTO users (name, password, username) VALUES (?, ?, ?)";
        try(Connection conn = BancoDados.conectar();
        PreparedStatement stmt = conn.prepareStatement(sql);
         ){
        
            stmt.setString(1, name);
            stmt.setString(2, password);
            stmt.setString(3, username);
            stmt.executeUpdate();

        }catch(SQLException e){
            throw e;
        }catch(IOException e){
            throw new SQLException("Erro ao conectar ao banco de dados", e);
        }
    }


    public User getUserByUsername(String username) throws SQLException{
        
        String sql = "SELECT * FROM users WHERE username = ?";
        try(Connection conn = BancoDados.conectar();
        PreparedStatement stmt = conn.prepareStatement(sql);
         ){
        
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            User user = new User();

            if(rs.next()){
                
                user.setUsername(rs.getString("username"));
                user.setPassword(rs.getString("password"));
                user.setName(rs.getString("name"));
                user.setCreatedAt(rs.getTimestamp("created_at"));
                user.setUpdatedAt(rs.getTimestamp("updated_at"));

                return user;
            }

            return user;

        }catch(SQLException e){
            throw e;
        }catch(IOException e){
            throw new SQLException("Erro ao conectar ao banco de dados", e);
        }
    }
    
}
