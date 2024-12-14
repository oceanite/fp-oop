import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.*;
import java.net.*;

public class MessengerClient extends JFrame {
    private JTextArea messageArea;
    private JTextField inputField;
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private DataInputStream dataIn;
    private DataOutputStream dataOut;

    public MessengerClient() {
        // Frame setup
        setTitle("Enhanced Messenger Client");
        setSize(600, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Create components
        messageArea = new JTextArea();
        messageArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(messageArea);

        inputField = new JTextField();
        JButton sendButton = new JButton("Send");
        JButton sendFileButton = new JButton("Send File");

        // Layout
        setLayout(new BorderLayout());
        add(scrollPane, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new BorderLayout());
        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.add(sendButton);
        buttonPanel.add(sendFileButton);

        bottomPanel.add(inputField, BorderLayout.CENTER);
        bottomPanel.add(buttonPanel, BorderLayout.EAST);
        add(bottomPanel, BorderLayout.SOUTH);

        // Send button actions
        sendButton.addActionListener(e -> sendMessage());
        inputField.addActionListener(e -> sendMessage());

        sendFileButton.addActionListener(e -> sendFile());
    }

    private void sendMessage() {
        String message = inputField.getText();
        if (!message.isEmpty()) {
            try {
                out.println("Client: " + message);
                out.flush();
                messageArea.append("Me: " + message + "\n");
                inputField.setText("");
            } catch (Exception ex) {
                messageArea.append("Error sending message: " + ex.getMessage() + "\n");
            }
        }
    }

    private void sendFile() {
        try {
            JFileChooser fileChooser = new JFileChooser();
            int result = fileChooser.showOpenDialog(this);

            if (result == JFileChooser.APPROVE_OPTION) {
                File selectedFile = fileChooser.getSelectedFile();

                // Send file metadata
                out.println("FILE_TRANSFER:" + selectedFile.getName() + ":" + selectedFile.length());

                // Send file content
                try (FileInputStream fis = new FileInputStream(selectedFile)) {
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = fis.read(buffer)) != -1) {
                        dataOut.write(buffer, 0, bytesRead);
                    }
                    dataOut.flush();

                    messageArea.append("File sent: " + selectedFile.getName() + "\n");
                }
            }
        } catch (IOException ex) {
            messageArea.append("File send error: " + ex.getMessage() + "\n");
        }
    }

    public void connectToServer() {
        try {
            socket = new Socket("localhost", 5000);
            messageArea.append("Connected to server!\n");

            // Set up input and output streams
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
            dataIn = new DataInputStream(socket.getInputStream());
            dataOut = new DataOutputStream(socket.getOutputStream());

            // Start receiving messages and files
            new Thread(() -> {
                try {
                    String message;
                    while ((message = in.readLine()) != null) {
                        if (message.startsWith("FILE_TRANSFER:")) {
                            handleFileReceive(message);
                        } else {
                            final String finalMessage = message;
                            SwingUtilities.invokeLater(() -> messageArea.append(finalMessage + "\n"));
                        }
                    }
                } catch (IOException e) {
                    SwingUtilities.invokeLater(() -> messageArea.append("Connection lost: " + e.getMessage() + "\n"));
                }
            }).start();

        } catch (IOException e) {
            messageArea.append("Connection error: " + e.getMessage() + "\n");
        }
    }

    private void handleFileReceive(String fileInfo) {
        try {
            // Parse file metadata
            String[] parts = fileInfo.split(":");
            String fileName = parts[1];
            long fileSize = Long.parseLong(parts[2]);

            // Choose save location
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setSelectedFile(new File(fileName));
            int result = fileChooser.showSaveDialog(this);

            if (result == JFileChooser.APPROVE_OPTION) {
                File saveFile = fileChooser.getSelectedFile();

                try (FileOutputStream fos = new FileOutputStream(saveFile)) {
                    byte[] buffer = new byte[4096];
                    long bytesReceived = 0;
                    int bytesRead;

                    while (bytesReceived < fileSize) {
                        bytesRead = dataIn.read(buffer);
                        if (bytesRead == -1)
                            break;
                        fos.write(buffer, 0, bytesRead);
                        bytesReceived += bytesRead;
                    }

                    SwingUtilities.invokeLater(() -> messageArea.append("File received: " + fileName + "\n"));
                }
            }
        } catch (IOException ex) {
            SwingUtilities.invokeLater(() -> messageArea.append("File receive error: " + ex.getMessage() + "\n"));
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            MessengerClient client = new MessengerClient();
            client.setVisible(true);
            client.connectToServer();
        });
    }
}