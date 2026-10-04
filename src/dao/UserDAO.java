package dao;

import java.sql.*;
import java.util.UUID;
import entities.User;
import entities.SessionUser;

import java.io.IOException;
import java.util.List;
import java.util.ArrayList;

public class UserDAO {

    public void register(User user) throws SQLException{

        String sql = "INSERT INTO usuario (username, name, password, role) VALUES (?, ?, ?, ?)";
        try(
            Connection conn = BancoDados.conectar();
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
        
        String sql = "SELECT * FROM usuario WHERE username = ?";
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

        String sql = "INSERT INTO sessao_usuario (username, token, ip_address) VALUES (?, ?, ?)";
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

        String sql = "DELETE FROM sessao_usuario WHERE token = ?";
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

    public void updateUserName(String username, String name) throws SQLException {
        
        String sql = "UPDATE usuario SET username = ?  WHERE username = ?";
        try (
            Connection conn = BancoDados.conectar();
            PreparedStatement st = conn.prepareStatement(sql);
        ){
            
            st.setString(1, username);
            st.setString(2, name);

            st.executeUpdate();

            
        } catch (Exception e) {
            throw new SQLException("Erro ao conectar no banco", e);
        }
    }

    public void updateUserPassword(String username, String password) throws SQLException {

        String sql = "UPDATE usuario SET password = ? WHERE username = ?";

        try (
            Connection conn = BancoDados.conectar();
            PreparedStatement st = conn.prepareStatement(sql);
        ){
            
            st.setString(1, password);
            st.setString(2, username);

            st.executeUpdate();

            
        } catch (Exception e) {
            // TODO: handle exception
        }
    }

    public void deleteUser (String username) throws SQLException {

        String sql = "DELETE FROM usuario WHERE username = ?";
        try(
            Connection conn = BancoDados.conectar();
            PreparedStatement stmt = conn.prepareStatement(sql);
        ){

            stmt.setString(1, username);
            stmt.executeUpdate();

        }catch (SQLException e){
            throw e;
        }catch (IOException e) {
            throw new SQLException("Erro ao conectar no banco", e);
        }
    }


    public List<User> getAllUsers () throws SQLException{

        String sql = "SELECT * FROM usuario";
        List<User> users = new ArrayList<User>();

        try (
            Connection conn = BancoDados.conectar();
            PreparedStatement stmt = conn.prepareStatement(sql);
        ){

            ResultSet rs = stmt.executeQuery();
            while(rs.next()){
                User u = new User();
                u.setUsername(rs.getString("username"));
                u.setName(rs.getString("name"));
                u.setPassword(rs.getString("password"));
                u.setCreatedAt(rs.getTimestamp("created_at"));
                u.setUpdatedAt(rs.getTimestamp("updated_at"));

                users.add(u);
            }

            return users;

        } catch (Exception e) {
            System.out.println("Falha na busca ou não tem usuários cadastrados " + e.getMessage());
            return null;
        }
    }

    public List<SessionUser> getAllSessionUsers() throws SQLException{
        String sql = "SELECT * FROM sessao_usuario";
        List<SessionUser> users = new ArrayList<SessionUser>();

        try (
            Connection conn = BancoDados.conectar();
            PreparedStatement st = conn.prepareStatement(sql);
        ){

            ResultSet rs = st.executeQuery();
            while(rs.next()){

                SessionUser u = new SessionUser();
                u.setUsername(rs.getString("username"));
                u.setToken(rs.getString("token"));

                users.add(u);
            
            }

            return users;

        } catch (Exception e) {
            System.out.println("Erro ao buscar ou sem sessões cadastradas!" + e.getMessage());
            return null;
        }
    }

    public String isPassword (String username) throws SQLException {

        String sql = "SELECT password FROM user WHERE username = ?";
        try (
            Connection conn = BancoDados.conectar();
            PreparedStatement st = conn.prepareStatement(sql);
        ) {

            st.setString(1, username);
            ResultSet rs = st.executeQuery();


            return rs.getString("password");

        } catch (Exception e) {
            throw new SQLException(e);
        }
    }

}
