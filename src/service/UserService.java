package service;

import com.google.gson.JsonObject;

import java.io.*;
import java.sql.*;

import entities.Session;
import entities.User;
import entities.SessionUser;
import dao.UserDAO;
import java.util.UUID;
import java.util.regex.PatternSyntaxException;



public class UserService {



    public JsonObject register(JsonObject req){
        UserDAO bancoUser = new UserDAO();
        JsonObject res = new JsonObject();

        try {
            JsonObject dataObj = req.get("data").getAsJsonObject();

            String name = dataObj.get("name").getAsString();
            String password = dataObj.get("password").getAsString();
            String username = dataObj.get("username").getAsString();

            
            boolean isValid = registerValidation(name, password, username); //testa os campos de entrada para ver se são válidos...
            
            User userExist = bancoUser.getUserByUsername(username); //caso passe da primeira validação, procura no banco pra ver se usuário já existe...

            if(userExist != null){
                res.addProperty("statusCode", "400");
                res.addProperty("message", "Usuário já existe!");
                return res;
            }
            if(!isValid){
                res.addProperty("statusCode", "400");
                res.addProperty("message", "Campos inválidos!");
                return res;
            }

            User user = new User();
            user.setName(name);
            user.setPassword(password);
            user.setUsername(username);

            bancoUser.register(user);

            res.addProperty("statusCode", "200");
            res.addProperty("message", "Usuário criado com sucesso!");

            Session.insertUser(user);

            return res;

        } catch (SQLException e) {

            res.addProperty("statusCode", "400");
            res.addProperty("message", "Erro ao criar usuário: " + e.getMessage());

            return res;

        } catch (IllegalArgumentException e) {

            res.addProperty("statusCode", "400");
            res.addProperty("message", e.getMessage());

            return res;

        } catch (Exception e) {

            res.addProperty("statusCode", "400");
            res.addProperty("message", "Erro ao criar usuário: " + e.getMessage());

            return res;
        }
    }
    public JsonObject login(JsonObject req, String ipAddress){

        UserDAO bancoUser = new UserDAO();
        JsonObject res = new JsonObject();

        try{
            JsonObject dataObj = req.get("data").getAsJsonObject();

            String username = dataObj.get("username").getAsString();
            String password = dataObj.get("password").getAsString();

            User user = bancoUser.getUserByUsername(username);
            
            if (user == null || !user.getPassword().equals(password)) {
                res.addProperty("statusCode", "401");
                res.addProperty("message", "Credenciais inválidas!");
                return res;
            }

            if(user.getUsername().equals(username) && user.getPassword().equals(password)){
                
                String token = UUID.randomUUID().toString();

                bancoUser.login(user.getUsername(), token, ipAddress);
    
                res.addProperty("statusCode", "200");
                res.addProperty("message", "Login realizado com sucesso!");
                res.addProperty("token", token);

                SessionUser sessionUser = new SessionUser();
                sessionUser.setUsername(user.getUsername());
                sessionUser.setToken(token);
                sessionUser.setIpAddress(ipAddress);
                Session.insertSessionUser(sessionUser);
                
                return res;
            }

            res.addProperty("statusCode","400");
            res.addProperty("message", "Senha ou usuário incorreto!");

            return res;
        }catch(Exception e){

            res.addProperty("statusCode", "400");
            res.addProperty("message", "Erro ao realizar login: " + e.getMessage());
            return res;
        }
    }


    
    public JsonObject logout(){
        JsonObject res = new JsonObject();
       
        try{
            return res;
        }catch(Exception e){
            return res;
        }
    }
    public JsonObject getUser(JsonObject req){
        JsonObject res = new JsonObject();
       
        try{
            return res;
        }catch(Exception e){
            return res;
        }
    }

    private boolean registerValidation(String name, String password, String username){
       
        validadorNome(name);
        validadorUsername(username);
        validadorSenha(password);

        return true;
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
