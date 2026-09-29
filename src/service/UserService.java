package service;

import com.google.gson.JsonObject;

import java.io.*;
import java.net.*;
import java.sql.*;
import com.google.gson.*;
import dao.UserDAO;
import gui.HomeWindow;


public class UserService {

    public JsonObject register(JsonObject req){
        Connection conn = BancoDados.conectar();
        UserDAO bancoUser = new UserDAO();
        JsonObject res = new JsonObject();

        try {
            JsonObject dataObj = req.get("data").getAsJsonObject();

            String name = dataObj.get("name").getAsString();
            String password = dataObj.get("password").getAsString();
            String username = dataObj.get("username").getAsString();

            bancoUser.verifyUsers(username);
            
            //fazer validações dos campos...

            bancoUser.register(name, password, username);

            res.addProperty("statusCode", "200");
            res.addProperty("message", "Usuário criado com sucesso!");

            return res;

        } catch (Exception e) {

            res.addProperty("statusCode", "400");
            res.addProperty("message", "");

        }finally{
            
            BancoDados.desconectar();
        }
    }
    public JsonObject login(JsonObject req){

        //TODO validar se usuário existe no banco e gerar token com UUID e gerar registro na tabela de UserSession

    }
    public JsonObject logout(){}
    public JsonObject getUser(JsonObject req){}
    
}
