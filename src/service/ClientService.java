package service;

import java.io.*;
import java.net.*;
import com.google.gson.Gson;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import entities.User;
import java.util.List;

import gui.HomeWindow;

public class ClientService extends Thread {

    private Socket socket;
    private BufferedReader entrada;
    private PrintWriter saida;
    private UserService userService;
    private SessionService sessionService;
    private VagaService vagaService;
    private VehicleService vehicleService;
    private HomeWindow homeServer;

    public ClientService(Socket socket, HomeWindow gui) {
        this.socket = socket;
        this.homeServer = gui;
        this.userService = new UserService();
        this.sessionService = new SessionService();
        this.vagaService = new VagaService();
        this.vehicleService = new VehicleService();
    }

    @Override
    public void run() {
        Gson gson = new Gson();
        JsonObject response = new JsonObject();
        String clientIp = socket.getInetAddress() != null ? socket.getInetAddress().getHostAddress() : "desconhecido";

        try {
            entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            saida = new PrintWriter(socket.getOutputStream(), true);

            String mensagemJson = entrada.readLine();

            if (mensagemJson == null || mensagemJson.isEmpty()) {
                response.addProperty("statusCode", 400);
                response.addProperty("message", "Campos obrigatórios ausentes ou inválidos.");
                response.add("data", JsonNull.INSTANCE);
                saida.println(response.toString());
                System.out.println("[Servidor] Resposta enviada: " + response.toString());
                return;
            }

            JsonObject requisicao = gson.fromJson(mensagemJson, JsonObject.class);

            System.out.println("[Servidor] Requisicao recebida: " + requisicao);

            if (requisicao == null || !requisicao.has("method") || requisicao.get("method").isJsonNull()) {
                response.addProperty("statusCode", 400);
                response.addProperty("message", "Campos obrigatórios ausentes ou inválidos.");
                response.add("data", JsonNull.INSTANCE);
                saida.println(response.toString());
                System.out.println("[Servidor] Resposta enviada: " + response.toString());
                return;
            }

            String method = requisicao.get("method").getAsString().trim();

            List<User> allUsers = sessionService.getAllUsers();

            switch (method) {
                case "register":
                    response = userService.register(requisicao);
                    allUsers = sessionService.getAllUsers();
                    if (homeServer != null) homeServer.refreshUserTable(allUsers);
                    break;
                case "login":
                    response = userService.login(requisicao, clientIp);
                    allUsers = sessionService.getAllUsers();
                    if (homeServer != null) homeServer.refreshUserTable(allUsers);
                    break;
                case "logout":
                    response = userService.logout(requisicao);
                    allUsers = sessionService.getAllUsers();
                    if (homeServer != null) homeServer.refreshUserTable(allUsers);
                    break;
                case "getuser":
                    response = userService.getUser(requisicao);
                    break;
                case "updateusername":
                    response = userService.updateUserName(requisicao);
                    allUsers = sessionService.getAllUsers();
                    if (homeServer != null) homeServer.refreshUserTable(allUsers);
                    break;
                case "updateuserpassword":
                    response = userService.updateUserPassword(requisicao);
                    break;
                case "deleteuser":
                    response = userService.deleteUser(requisicao);
                    allUsers = sessionService.getAllUsers();
                    if (homeServer != null) {
                        homeServer.refreshUserTable(allUsers);
                        homeServer.refreshVagaTable(sessionService.getAllVagas());
                        homeServer.refreshVehicleTable(sessionService.getAllVehicles());
                    }
                    break;
                case "createvaga":
                    response = vagaService.createVaga(requisicao);
                    if (homeServer != null) homeServer.refreshVagaTable(sessionService.getAllVagas());
                    break;
                case "getvagas":
                    response = vagaService.getVagas(requisicao);
                    break;
                case "updatevaga":
                    response = vagaService.updateVaga(requisicao);
                    if (homeServer != null) homeServer.refreshVagaTable(sessionService.getAllVagas());
                    break;
                case "deletevaga":
                    response = vagaService.deleteVaga(requisicao);
                    if (homeServer != null) homeServer.refreshVagaTable(sessionService.getAllVagas());
                    break;
                case "findvehicle":
                    response = vagaService.findVehicle(requisicao);
                    break;
                case "createvehicle":
                    response = vehicleService.createVehicle(requisicao);
                    if (homeServer != null) homeServer.refreshVehicleTable(sessionService.getAllVehicles());
                    break;
                case "getvehicles":
                    response = vehicleService.getVehicles(requisicao);
                    break;
                case "getvehicle":
                    response = vehicleService.getVehicle(requisicao);
                    break;
                case "updatevehicle":
                    response = vehicleService.updateVehicle(requisicao);
                    if (homeServer != null) homeServer.refreshVehicleTable(sessionService.getAllVehicles());
                    break;
                case "deletevehicle":
                    response = vehicleService.deleteVehicle(requisicao);
                    if (homeServer != null) {
                        homeServer.refreshVehicleTable(sessionService.getAllVehicles());
                        homeServer.refreshVagaTable(sessionService.getAllVagas());
                    }
                    break;
                default:
                    response.addProperty("statusCode", 400);
                    response.addProperty("message", "Campos obrigatórios ausentes ou inválidos.");
                    response.add("data", JsonNull.INSTANCE);
                    break;
            }

            if (response != null) {
                if (!response.has("data")) {
                    response.add("data", JsonNull.INSTANCE);
                }
                saida.println(response.toString());
                System.out.println("[Servidor] Resposta enviada: " + response.toString());
            }

        } catch (Exception e) {
            response = new JsonObject();
            response.addProperty("statusCode", 400);
            response.addProperty("message", "Campos obrigatórios ausentes ou inválidos.");
            response.add("data", JsonNull.INSTANCE);
            if (saida != null) {
                saida.println(response.toString());
            }
            System.out.println("[Servidor] Resposta enviada: " + response.toString());

        } finally {
            try {
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
                System.out.println("Cliente desconectou: /" + clientIp);
            } catch (IOException e) {
                System.out.println("Erro ao fechar socket");
            }
        }
    }

}
