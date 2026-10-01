package service;

import java.io.*;
import java.net.*;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import gui.HomeWindow;

public class ClientService extends Thread{
    
    private Socket socket;
    private BufferedReader entrada;
    private PrintWriter saida;
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
            saida = new PrintWriter(socket.getOutputStream(), true);
            String mensagemJson;
            Gson gson = new Gson();

            while((mensagemJson = entrada.readLine()) != null){
                JsonObject requisicao = gson.fromJson(mensagemJson, JsonObject.class);
                
                System.out.println("Requisição recebida: " + requisicao);

                String method = requisicao.get("method").getAsString();

                JsonObject response = null;

                switch (method) {
                    case "register":
                        response = userService.register(requisicao);
                        break;
                    case "login":
                        response = userService.login(requisicao, socket.getInetAddress().getHostAddress());
                        break;
                    case "logout":
                        response = userService.logout();
                        break;
                    case "getuser":
                        response = userService.getUser(requisicao);
                        break;
                    default:
                        break;
                }

                if (response != null){
                    saida.println(response.toString());
                }
            }
        } catch (Exception e) {
            System.out.println("Conexão perdida com o cliente");
        } finally {
            userService.logout();
        }
    }

}
