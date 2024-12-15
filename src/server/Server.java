package src.server;

import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;

public class Server {
    private static final int PORT = 5000;
    private static final Map<String, ClientHandler> clients = new ConcurrentHashMap<>();
    private static final File logFile = new File("server_logs.txt");
    private ServerSocket serverSocket;

    public Server() {
        try {
            serverSocket = new ServerSocket(PORT);
            System.out.println("Server started on port " + PORT);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void start() {
        try {
            while (!serverSocket.isClosed()) {
                Socket socket = serverSocket.accept();
                ClientHandler clientHandler = new ClientHandler(socket);
                new Thread(clientHandler).start();
            }
        } catch (IOException e) {
            closeServerSocket();
        }
    }

    public static void log(String message) {
        synchronized (logFile) {
            try (FileWriter fw = new FileWriter(logFile, true)) {
                fw.write(message + "\n");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void broadcast(String message, String sender) {
        for (ClientHandler client : clients.values()) {
            if (!client.getUsername().equals(sender)) {
                client.sendMessage(message);
            }
        }
    }

    public static void directMessage(String message, String sender, String recipient) {
        ClientHandler client = clients.get(recipient);
        if (client != null) {
            client.sendMessage("From " + sender + ": " + message);
            log("DM: " + sender + " -> " + recipient + " at " + timestamp());
        }
    }

    public static void sendClientList(ClientHandler client) {
        StringBuilder clientList = new StringBuilder("Connected Clients: ");
        for (String username : clients.keySet()) {
            if (!username.equals(client.getUsername())) {
                clientList.append(username).append(", ");
            }
        }
        client.sendMessage(clientList.toString());
    }

    public static void addClient(String username, ClientHandler client) {
        clients.put(username, client);
        broadcast(username + " has joined the server!", username);
        log("CONNECTED: " + username + " at " + timestamp());
    }

    public static void removeClient(String username) {
        if (clients.containsKey(username)) {
            clients.remove(username);
            broadcast(username + " has left the server!", username);
            log("DISCONNECTED: " + username + " at " + timestamp());
        }
    }

    public static ClientHandler getClient(String username) {
        return clients.get(username);
    }

    public void closeServerSocket() {
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String timestamp() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
    }

    public static void main(String[] args) {
        Server server = new Server();
        server.start();
    }
}