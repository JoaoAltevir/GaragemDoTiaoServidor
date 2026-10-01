package service;

import com.google.gson.JsonObject;

import java.io.*;
import java.sql.*;
import entities.User;
import dao.UserDAO;
import java.util.UUID;



public class UserService {



    public JsonObject register(JsonObject req){
        UserDAO bancoUser = new UserDAO();
        JsonObject res = new JsonObject();

        try {
            JsonObject dataObj = req.get("data").getAsJsonObject();

            String name = dataObj.get("name").getAsString();
            String password = dataObj.get("password").getAsString();
            String username = dataObj.get("username").getAsString();

            User userExist = bancoUser.getUserByUsername(username);

            if(userExist != null){
                res.addProperty("statusCode", "400");
                res.addProperty("message", "Usuário já existe!");
                return res;
            }
            
            boolean isValid = registerValidation(name, password, username);

            if(!isValid){
                res.addProperty("statusCode", "400");
                res.addProperty("message", "Campos inválidos!");
                return res;
            }

            bancoUser.register(name, password, username);

            res.addProperty("statusCode", "200");
            res.addProperty("message", "Usuário criado com sucesso!");

            return res;

        } catch (SQLException e) {

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
        if(name == null || name.isEmpty()){
            return false;
        }
        if(password == null || password.isEmpty()){
            return false;
        }
        if(username == null || username.isEmpty()){
            return false;
        }
        return true;
    }
    
    
}
