package service;

import com.google.gson.JsonObject;

import java.io.*;
import java.net.*;
import java.sql.*;
import com.google.gson.*;
import dao.UserDAO;
import gui.HomeWindow;


public class UserService {

    private Socket userSocket;
    private HomeWindow homeWindow;



    public void register(JsonObject req){
        Connection conn = BancoDados.conectar();
        UserDAO bancoUser = new UserDAO();
        try (
            Socket client = userSocket;
            PrintWriter out = new PrintWriter(client.getOutputStream(), true)
        ){
            JsonObject res = new JsonObject();
            JsonObject dataObj = req.get("data").getAsJsonObject();

            String name = dataObj.get("name").getAsString();
            String password = dataObj.get("password").getAsString();
            String username = dataObj.get("username").getAsString();

            bancoUser.verifyUsers(username);
            
            //fazer validações dos campos...

            bancoUser.register(name, password, username);

            res.addProperty("StatusCode", "200");
            res.addProperty("Message", "Usuário criado com sucesso!");

            out.println(gson.toGson(res));

        } catch (Exception e) {
            
        }finally{
            BancoDados.desconectar();
        }
    }
    public void login(JsonObject req){}
    public void logout(){}
    public void getUser(JsonObject req){}

}
