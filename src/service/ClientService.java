package service;

import java.io.*;
import java.net.*;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import entities.User;
import java.util.List;


import gui.HomeWindow;

public class ClientService extends Thread{
    
    private Socket socket;
    private BufferedReader entrada;
    private PrintWriter saida;
    private UserService userService;
    private SessionService sessionService;
    private HomeWindow homeServer;

    public ClientService(Socket socket, HomeWindow gui){
        this.socket = socket;
        this.homeServer = gui;
        this.userService = new UserService();
        this.sessionService = new SessionService();
    }

    @Override
    public void run(){
        Gson gson = new Gson();
        JsonObject response = new JsonObject();

        try {
            entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            saida = new PrintWriter(socket.getOutputStream(), true);

            String mensagemJson = entrada.readLine();
           
            JsonObject requisicao = gson.fromJson(mensagemJson, JsonObject.class);
            
            System.out.println("Requisição recebida: " + requisicao);
            
            String method = requisicao.get("method").getAsString();
            
            
            
            if(mensagemJson == null || mensagemJson.isEmpty()){
                response.addProperty("statusCode", 400);
                response.addProperty("message", "Requisição não pode ser nula!");
                socket.close();
                return;
            }
            
            List <User> allUsers = sessionService.getAllUsers();

            switch (method) {
                case "register":
                    response = userService.register(requisicao);
                    allUsers = sessionService.getAllUsers();
                    homeServer.refreshUserTable(allUsers);
                    break;
                case "login":
                    response = userService.login(requisicao, socket.getInetAddress().getHostAddress());
                    allUsers = sessionService.getAllUsers();
                    homeServer.refreshUserTable(allUsers);
                    break;
                case "logout":
                    response = userService.logout(requisicao);
                    allUsers = sessionService.getAllUsers();
                    homeServer.refreshUserTable(allUsers);
                    break;
                case "getuser":
                    response = userService.getUser(requisicao);
                    break;
                case "updateusername":
                    response = userService.updateUserName(requisicao);
                    break;
                case "updateuserpassword":
                    response = userService.updateUserPassword(requisicao);
                    break;
                case "deleteuser":
                    response = userService.deleteUser(requisicao);
                    allUsers = sessionService.getAllUsers();
                    homeServer.refreshUserTable(allUsers);
                    break;
                default:
                    break;
            }

            if (response != null){
                saida.println(response.toString());
                socket.close();
            }
    
        } catch (Exception e) {

            response.addProperty("statusCode", 500);
            response.addProperty("message", "Erro ao processar requisição: " + e.getMessage());
            saida.println(response.toString());
            System.out.println("Erro ao processar requisição: " + e.getMessage());

        } finally {
            try {
                socket.close();
            } catch (IOException e) {

                response.addProperty("statusCode", 500);
                response.addProperty("message", "Erro ao processar requisição: " + e.getMessage());
                System.out.println("Erro ao fechar socket");
                saida.println(response.toString());
            }
        }
    }

}
