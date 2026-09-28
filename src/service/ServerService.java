package service;

import java.io.*;
import java.net.*;


import gui.HomeWindow;

public class ServerService{
    
    private int porta;
    private boolean rodando;
    private ServerSocket serverSocket;

    private HomeWindow homeServer;
    
    
    public ServerService (int porta, HomeWindow home) {
        this.porta = porta;
        this.homeServer = home;

    }

    public void iniciarServidor(){
        if (rodando) return;

        this.rodando = true;

        new Thread(() -> {
            try {
                this.serverSocket = new ServerSocket(porta);
                System.out.println("Servidor iniciado na porta: " + porta);

                while(rodando){

                    Socket socketClient = serverSocket.accept();
                    System.out.println("Novo client conectado" + socketClient.getInetAddress().getHostAddress());

                    ClientService clientThread = new ClientService(socketClient, this.homeServer);
                    clientThread.start();
                }
            } catch (IOException e) {
                if(rodando){
                    System.err.println("Erro no servidor: " + e.getMessage());
                }else{
                    System.out.println("Servidor encerrado");
                }
            }
        }).start();;
    }

    public void fecharServidor(){

        this.rodando = false;
        try {
            if(serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
