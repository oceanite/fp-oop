package messenger.server;

import java.io.*;
import java.net.*;

class ClientHandler implements Runnable {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private DataInputStream dataIn;
    private DataOutputStream dataOut;
    private String username;

    public ClientHandler(Socket socket) {
        try {
            this.socket = socket;
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.out = new PrintWriter(socket.getOutputStream(), true);
            this.dataIn = new DataInputStream(socket.getInputStream());
            this.dataOut = new DataOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            closeEverything();
        }
    }

    @Override
    public void run() {
        try {
            // First message is the username
            username = in.readLine();
            Server.addClient(username, this);
            Server.sendClientList(this);

            String message;
            while ((message = in.readLine()) != null) {
                if (message.startsWith("LIST_CLIENTS")) {
                    Server.sendClientList(this);
                } else if (message.startsWith("TO:")) {
                    String[] parts = message.split(":", 3);
                    if (parts.length == 3) {
                        String recipient = parts[1];
                        String msg = parts[2];
                        Server.directMessage(msg, username, recipient);
                    }
                } else if (message.startsWith("FILE:")) {
                    handleFileTransfer(message);
                } else {
                    Server.broadcast(username + ": " + message, username);
                }
            }
        } catch (IOException e) {
            closeEverything();
        }
    }

    private void handleFileTransfer(String fileInfo) {
        try {
            // Parse file metadata
            String[] parts = fileInfo.split(":");
            if (parts.length < 4) {
                System.err.println("Invalid file transfer metadata");
                return;
            }
            String recipient = parts[1];
            String fileName = parts[2];
            long fileSize = Long.parseLong(parts[3]);

            // Find recipient's client handler
            ClientHandler recipientClient = Server.getClient(recipient);
            if (recipientClient == null) {
                out.println("Error: Recipient " + recipient + " not found.");
                return;
            }

            // Notify recipient about incoming file
            recipientClient.out.println("FILE:" + username + ":" + fileName + ":" + fileSize);

            // Transfer file contents
            byte[] buffer = new byte[4096];
            long bytesTransferred = 0;
            int bytesRead;

            while (bytesTransferred < fileSize) {
                bytesRead = dataIn.read(buffer);
                if (bytesRead == -1) break;
                recipientClient.dataOut.write(buffer, 0, bytesRead);
                bytesTransferred += bytesRead;
            }
            recipientClient.dataOut.flush();

            // Log file transfer
            Server.log("FILE TRANSFER: " + username + " -> " + recipient + " (" + fileName + ")");

        } catch (IOException e) {
            System.err.println("File transfer error: " + e.getMessage());
        }
    }

    public void sendMessage(String message) {
        out.println(message);
    }

    public String getUsername() {
        return username;
    }

    public void closeEverything() {
        Server.removeClient(username);
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (dataIn != null) dataIn.close();
            if (dataOut != null) dataOut.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}