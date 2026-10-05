package service;

import com.google.gson.JsonObject;

import java.sql.*;

import entities.Session;
import entities.User;
import entities.SessionUser;
import dao.UserDAO;
import java.util.UUID;
import java.util.regex.PatternSyntaxException;
import java.util.List;
import gui.HomeWindow;



public class UserService {

    private final UserDAO bancoUser = new UserDAO();
    private JsonObject res = new JsonObject();
    private HomeWindow home;

    public UserService(HomeWindow home){

        this.home = home;

    }

    public UserService(){}

    public JsonObject register(JsonObject req){
        
        try {
            JsonObject dataObj = req.get("data").getAsJsonObject();

            String name = dataObj.get("name").getAsString();
            String password = dataObj.get("password").getAsString();
            String username = dataObj.get("username").getAsString();

            
            boolean isValid = registerValidation(name, password, username); //testa os campos de entrada para ver se são válidos...
            
            User userExist = this.bancoUser.getUserByUsername(username); //caso passe da primeira validação, procura no banco pra ver se usuário já existe...

            if(userExist != null){
                res.addProperty("statusCode", 400);
                res.addProperty("message", "Usuário já existe!");
                return res;
            }
            if(!isValid){
                res.addProperty("statusCode", 400);
                res.addProperty("message", "Campos inválidos!");
                return res;
            }

            User user = new User();
            user.setName(name);
            user.setPassword(password);
            user.setUsername(username);

            this.bancoUser.register(user);

            res.addProperty("statusCode", 201);
            res.addProperty("message", "Usuário criado com sucesso!");

            Session.insertUser(user);

            return res;

        } catch (SQLException e) {

            res.addProperty("statusCode", 400);
            res.addProperty("message", "Erro ao criar usuário: " + e.getMessage());

            return res;

        } catch (IllegalArgumentException e) {

            res.addProperty("statusCode", 400);
            res.addProperty("message", e.getMessage());

            return res;

        } catch (Exception e) {

            res.addProperty("statusCode", 400);
            res.addProperty("message", "Erro ao criar usuário: " + e.getMessage());

            return res;
        }
    }
    public JsonObject login(JsonObject req, String ipAddress){

        
        try{
            JsonObject dataObj = req.get("data").getAsJsonObject();

            String username = dataObj.get("username").getAsString();
            String password = dataObj.get("password").getAsString();

            User user = this.bancoUser.getUserByUsername(username);
            
            if (user == null || !user.getPassword().equals(password)) {
                res.addProperty("statusCode", 401);
                res.addProperty("message", "Credenciais inválidas!");
                return res;
            }

            if(user.getUsername().equals(username) && user.getPassword().equals(password)){
                
                String token = UUID.randomUUID().toString();

                this.bancoUser.login(user.getUsername(), token, ipAddress);
    
                res.addProperty("statusCode", 200);
                res.addProperty("message", "Login realizado com sucesso!");
                res.addProperty("token", token);

                SessionUser sessionUser = new SessionUser();
                sessionUser.setUsername(user.getUsername());
                sessionUser.setToken(token);
                sessionUser.setIpAddress(ipAddress);
                Session.insertSessionUser(sessionUser);
                
                return res;
            }

            res.addProperty("statusCode", 400);
            res.addProperty("message", "Senha ou usuário incorreto!");

            return res;
        }catch(Exception e){

            res.addProperty("statusCode", 400);
            res.addProperty("message", "Erro ao realizar login: " + e.getMessage());
            return res;
        }
    }


    
    public JsonObject logout(JsonObject req){
        
        try{
            JsonObject dataObj = req.get("data").getAsJsonObject();

            String token = dataObj.get("token").getAsString();

            if(!Session.logout(token)){

                res.addProperty("statusCode", 401);
                res.addProperty("message", "Sessão encerrada ou não autorizada!");
                return res;

            }

            this.bancoUser.logout(token);

            res.addProperty("statusCode", 200);
            res.addProperty("message", "Usuário deslogado com sucesso!");

            return res;
            
        }catch(SQLException e){

            res.addProperty("statusCode", 500);
            res.addProperty("message", "Erro ao realizar logout: " + e.getMessage());
            return res;

        }
    }

    public JsonObject getUser(JsonObject req){
        try{

            JsonObject dataObj = req.get("data").getAsJsonObject();
            String token = dataObj.get("token").getAsString();
            String usernameRequested = dataObj.get("username").getAsString();

            isValidTokenByUsername(token, usernameRequested);

            User user = this.bancoUser.getUserByUsername(usernameRequested);

            JsonObject userData = new JsonObject();
            userData.addProperty("name", user.getName());
            userData.addProperty("username", user.getUsername());

            res.addProperty("statusCode", 200);
            res.add("data", userData);

            return res;
        }catch(IllegalArgumentException e){

            res.addProperty("statusCode", 400);
            res.addProperty("message", e.getMessage());
            return res;
        }catch(Exception e){

            res.addProperty("statusCode", 500);
            res.addProperty("message", "Erro ao buscar usuário: " + e.getMessage());

            return res;
        }
    }

    public JsonObject updateUserName(JsonObject req){
        
        try{

            JsonObject dataObj = req.get("data").getAsJsonObject();
            
            String token = dataObj.get("token").getAsString();
            String username = dataObj.get("username").getAsString();
            String name = dataObj.get("name").getAsString();

            isValidTokenByUsername(token, username);

            bancoUser.updateUserName(username, name);        
            
            res.addProperty("statusCode", 200);
            res.addProperty("message", "Nome do usuário atualizado com sucesso!");

            return res;
        }catch(Exception e){
            return res;
        }
    }


    public JsonObject updateUserPassword(JsonObject req){
    
        try{

            JsonObject dataObj = req.get("data").getAsJsonObject();

            String token = dataObj.get("token").getAsString();
            String username = dataObj.get("username").getAsString();
            String oldPassword = dataObj.get("oldPassword").getAsString();
            String newPassword = dataObj.get("newPassword").getAsString();

            isValidTokenByUsername(token, username);
            isOldPassword(oldPassword, username);
            validadorSenha(newPassword);

            bancoUser.updateUserPassword(username, newPassword);

            res.addProperty("statusCode", 200);
            res.addProperty("message", "Senha Atualizada com sucesso!");

            return res;
        }catch(IllegalArgumentException e){

            res.addProperty("statusCode", 400);
            res.addProperty("message", "Erro de inserção: " + e.getMessage());
            return res;
        }catch(SQLException e){
            res.addProperty("statusCode", 500);
            res.addProperty("message", "Erro de banco: " + e.getMessage());
            return res;
        }
    }

    public JsonObject deleteUser(JsonObject req){
        try{

            JsonObject dataObj = req.get("data").getAsJsonObject();
            String token = dataObj.get("token").getAsString();
            String username = dataObj.get("username").getAsString();

            isValidTokenByUsername(token, username);

            bancoUser.deleteUser(username);

            res.addProperty("statusCode", 200);
            res.addProperty("message", "Usuário deletado com sucesso!");
            return res;
        }catch(SQLException e){

            res.addProperty("statusCode", 500);
            res.addProperty("message", "Erro de banco: " + e.getMessage());
            return res;
        }catch(IllegalArgumentException e){

            res.addProperty("statusCode", 400);
            res.addProperty("message", e.getMessage());
            return res;
        }
    }

    //FUNÇÕES AUXILIARES

    private void isOldPassword(String password, String username) throws IllegalArgumentException {
        try {
            
           String userPassword = bancoUser.isPassword(username);

           if(!password.equals(userPassword)){
                throw new IllegalArgumentException("Senhas diferentes!");
           }
            
        } catch (Exception e) {
           throw new IllegalArgumentException(e.getMessage());
        }
    }

    private void getAllUsers(){
        try {
            List<User> users = bancoUser.getAllUsers();
    
            for( User user : users){
                Session.insertUser(user);
            }
            
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

    }

    private void getAllSessionUsers(){
        try {
            
            List<SessionUser> users = bancoUser.getAllSessionUsers();

            for( SessionUser sessionUser : users){
                Session.insertSessionUser(sessionUser);
            }

        
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }


    }

    public void getAll(){

        SessionService session = new SessionService();
        getAllUsers();
        getAllSessionUsers();


        List<User> users = session.getAllUsers();       
        
            this.home.refreshUserTable(users);

    }
    
    private boolean isValidTokenByUsername (String token, String usernameRequested) throws IllegalArgumentException{

        String username = Session.findByToken(token);
        if(username != null){
            if(username.equals(usernameRequested)) return true;
            else throw new IllegalArgumentException("Token não corresponde ao usuário solicitado!");
        }else{
            throw new IllegalArgumentException("Token inválido!");
        }

    }

    private boolean registerValidation(String name, String password, String username){

        boolean valid = true;
        if(!validadorNome(name)){
            valid = false;
            if(!validadorUsername(username)){
                valid = false;
                if (!validadorSenha(password)){
                    valid = false;
            }
        }

        return valid;
    }

    private boolean validadorSenha(String password) throws IllegalArgumentException, PatternSyntaxException {

        if(password == null) throw new IllegalArgumentException("Senha não pode ser nula!");

        String regex = "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[#.*&%$@!()\\-_=+])[A-Za-z0-9#.*&%$@!()\\-_=+]{8,20}$";
        return password.trim().matches(regex);
        
    }

    private boolean validadorNome(String name) throws IllegalArgumentException, PatternSyntaxException {

        if(name == null) throw new IllegalArgumentException("Nome não pode ser nulo!");

        String regex = "^[A-Za-zÀ-ÿ ]{1,60}$";
        return name.trim().matches(regex);
    }

    private boolean validadorUsername(String username) throws IllegalArgumentException, PatternSyntaxException {

        if(username == null) throw new IllegalArgumentException("Username não pode ser nulo!");

        String regex = "^[a-z0-9._]{3,20}$";
        return username.trim().matches(regex);
    }


}
