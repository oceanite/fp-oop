package messenger.client;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;

public class Client extends JFrame {
    private JTextArea messageArea;
    private JTextField inputField;
    private JTextField toField;
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private String username;
    private DataInputStream dataIn;
    private DataOutputStream dataOut;

    public Client() {
        // Username input dialog
        username = JOptionPane.showInputDialog("Enter your username:");
        if (username == null || username.trim().isEmpty()) {
            JOptionPane.showMessageDialog(null, "Username cannot be empty!");
            System.exit(0);
        }

        // Frame setup
        setTitle("Messenger - " + username);
        setSize(900, 1000);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Create components
        messageArea = new JTextArea();
        messageArea.setEditable(false);
        messageArea.setFont(new Font("Monospaced", Font.PLAIN, 16));
        messageArea.setLineWrap(true);
        messageArea.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(messageArea);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Messages"));

        // Ensure scroll pane takes up more space
        scrollPane.setPreferredSize(new Dimension(800, 700));

        inputField = new JTextField(50);
        toField = new JTextField(30);

        JButton sendButton = new JButton("Send");
        sendButton.setBackground(new Color(100, 200, 100));
        sendButton.setForeground(Color.WHITE);

        JButton listClientsButton = new JButton("List Clients");
        listClientsButton.setBackground(new Color(70, 130, 180));
        listClientsButton.setForeground(Color.WHITE);

        JButton sendFileButton = new JButton("Send File");
        sendFileButton.setBackground(new Color(220, 100, 100));
        sendFileButton.setForeground(Color.WHITE);

        // Layout with GridBag for more precise control
        setLayout(new BorderLayout(10, 10));
        JPanel mainPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);

        // To field
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        mainPanel.add(new JLabel("To:"), gbc);

        gbc.gridx = 1;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        mainPanel.add(toField, gbc);

        // Message input area
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 3;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        mainPanel.add(inputField, gbc);

        // Send button
        gbc.gridx = 3;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.NONE;
        mainPanel.add(sendButton, gbc);

        // Buttons panel
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 4;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        buttonPanel.add(listClientsButton);
        buttonPanel.add(sendFileButton);
        mainPanel.add(buttonPanel, gbc);

        // Add components to frame
        add(scrollPane, BorderLayout.CENTER);
        add(mainPanel, BorderLayout.SOUTH);
        add(new JPanel(), BorderLayout.WEST);
        add(new JPanel(), BorderLayout.EAST);

        // Button actions
        sendButton.addActionListener(e -> sendMessage());
        inputField.addActionListener(e -> sendMessage());
        listClientsButton.addActionListener(e -> listClients());
        sendFileButton.addActionListener(e -> sendFile());

        pack();
        setLocationRelativeTo(null);
        connectToServer();
    }

    private void connectToServer() {
        try {
            socket = new Socket("localhost", 5000);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
            dataIn = new DataInputStream(socket.getInputStream());
            dataOut = new DataOutputStream(socket.getOutputStream());

            // Send username to server
            out.println(username);

            // Start receiving messages
            new Thread(this::receiveMessages).start();

        } catch (IOException e) {
            JOptionPane.showMessageDialog(this,
                    "Could not connect to server: " + e.getMessage());
            System.exit(1);
        }
    }

    private void receiveMessages() {
        try {
            String message;
            while ((message = in.readLine()) != null) {
                if (message.startsWith("FILE:")) {
                    handleFileReceive(message);
                } else {
                    final String finalMessage = message;
                    SwingUtilities.invokeLater(() -> {
                        messageArea.append(finalMessage + "\n");
                        messageArea.setCaretPosition(messageArea.getDocument().getLength());
                    });
                }
            }
        } catch (IOException e) {
            SwingUtilities.invokeLater(() ->
                    messageArea.append("Connection lost: " + e.getMessage() + "\n")
            );
        }
    }

    private void sendMessage() {
        String message = inputField.getText().trim();
        String toUser = toField.getText().trim();
        if (!message.isEmpty()) {
            try {
                if (!toUser.isEmpty()) {
                    out.println("TO:" + toUser + ":" + message);
                } else {
                    out.println(message);
                }
                inputField.setText("");
            } catch (Exception ex) {
                messageArea.append("Error sending message: " + ex.getMessage() + "\n");
            }
        }
    }

    private void listClients() {
        out.println("LIST_CLIENTS");
    }

    private void sendFile() {
        String toUser = toField.getText().trim();
        if (toUser.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please specify a recipient in the 'To' field.");
            return;
        }

        JFileChooser fileChooser = new JFileChooser();
        int result = fileChooser.showOpenDialog(this);

        if (result == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();

            try {
                // Send file metadata first
                out.println("FILE:" + toUser + ":" + file.getName() + ":" + file.length());

                // Send file contents
                try (FileInputStream fis = new FileInputStream(file)) {
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = fis.read(buffer)) != -1) {
                        dataOut.write(buffer, 0, bytesRead);
                    }
                    dataOut.flush();
                }

                messageArea.append("File sent to " + toUser + ": " + file.getName() + "\n");
            } catch (IOException ex) {
                messageArea.append("Error sending file: " + ex.getMessage() + "\n");
            }
        }
    }

    private void handleFileReceive(String fileInfo) {
        try {
            // Parse file metadata
            String[] parts = fileInfo.split(":");
            if (parts.length < 3) {
                throw new IllegalArgumentException("Invalid file transfer metadata");
            }
            String sender = parts[1];
            String fileName = parts[2];
            long fileSize = Long.parseLong(parts[3]);

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
                        if (bytesRead == -1) break;
                        fos.write(buffer, 0, bytesRead);
                        bytesReceived += bytesRead;
                    }

                    SwingUtilities.invokeLater(() -> {
                        messageArea.append("File received from " + sender + ": " + fileName + "\n");
                        JOptionPane.showMessageDialog(this,
                                "File received: " + fileName,
                                "File Transfer",
                                JOptionPane.INFORMATION_MESSAGE);
                    });
                }
            }
        } catch (Exception ex) {
            SwingUtilities.invokeLater(() ->
                    messageArea.append("File receive error: " + ex.getMessage() + "\n")
            );
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Client client = new Client();
            client.setVisible(true);
        });
    }
}