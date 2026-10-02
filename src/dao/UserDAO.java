package dao;

import java.sql.*;
import java.util.UUID;
import entities.User;

import java.io.IOException;

public class UserDAO {

    public void register(User user) throws SQLException{

        String sql = "INSERT INTO users (username, name, password, role) VALUES (?, ?, ?, ?)";
        try(Connection conn = BancoDados.conectar();
        PreparedStatement stmt = conn.prepareStatement(sql);
         ){
        
            stmt.setString(1, user.getUsername());
            stmt.setString(2, user.getName());
            stmt.setString(3, user.getPassword());
            stmt.setString(4, user.getRole());
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

    public void login(String username, String token, String ipAddress) throws SQLException{

        String sql = "INSERT INTO sessions (user_username, token, ip_address) VALUES (?, ?, ?)";
        try(Connection conn = BancoDados.conectar();
        PreparedStatement stmt = conn.prepareStatement(sql);
         ){
            
            UUID uuid = UUID.fromString(token);
            stmt.setString(1, username);
            stmt.setObject(2, uuid);
            stmt.setString(3, ipAddress);
            stmt.executeUpdate();

        }catch(SQLException e){
            throw e;
        }catch(IOException e){
            throw new SQLException("Erro ao conectar ao banco de dados", e);
        }

    }

    public void logout (String token) throws SQLException{

        String sql = "DELETE FROM Sessao_usuario WHERE token = ?";
        try(Connection conn = BancoDados.conectar();
        PreparedStatement stmt = conn.prepareStatement(sql);
         ){
            
            UUID uuid = UUID.fromString(token);
            stmt.setObject(1, uuid);
            stmt.executeUpdate();

        }catch(SQLException e){
            throw e;
        }catch(IOException e){
            throw new SQLException("Erro ao conectar ao banco de dados", e);
        }

    }

}
