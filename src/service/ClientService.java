package service;

import java.io.*;
import java.net.*;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import gui.HomeWindow;

public class ClientService extends Thread{
    
    private Socket socket;
    private BufferedReader entrada;
    private UserService userService;
    private HomeWindow homeServer;

    public ClientService(Socket socket, HomeWindow gui){
        this.socket = socket;
        this.homeServer = gui;

        this.userService = new UserService();
    }

    @Override
    public void run(){
        try {
            entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String mensagemJson;
            Gson gson = new Gson();

            while((mensagemJson = entrada.readLine()) != null){
                JsonObject requisicao = gson.fromJson(mensagemJson, JsonObject.class);
                String method = requisicao.get("method").getAsString();

                switch (method) {
                    case "REGISTER":
                        userService.register(requisicao);
                        break;
                    case "LOGIN":
                        userService.login(requisicao);
                        break;
                    case "LOGOUT":
                        userService.logout();
                        break;
                    case "GETUSER":
                        userService.getUser(requisicao);
                        break;
                    default:
                        break;
                }
            }
        } catch (Exception e) {
            System.out.println("Conexão perdida com o cliente");
        } finally {
            userService.logout();
        }
    }

}
