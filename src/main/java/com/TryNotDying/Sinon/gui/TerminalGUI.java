package com.TryNotDying.Sinon.gui; // Project's package name

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.Socket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TerminalGUI extends JFrame {
    private static final Logger LOGGER = LoggerFactory.getLogger(TerminalGUI.class);
    private JTextField commandField;
    public static JTextArea outputArea; // Make outputArea static

    public TerminalGUI() {
        setTitle("Terminal GUI for Sinon");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(600, 400);
        setLayout(new BorderLayout());

        // Text Area for Output
        outputArea = new JTextArea();
        outputArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(outputArea);
        add(scrollPane, BorderLayout.CENTER);

        // Command Input Area
        JPanel bottomPanel = new JPanel(new BorderLayout());
        commandField = new JTextField();
        bottomPanel.add(commandField, BorderLayout.CENTER);

        // Send Button
        JButton sendButton = new JButton("Send");
        sendButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String command = commandField.getText();
                if (!command.isEmpty()) {
                    sendCommand(command);
                    commandField.setText(""); // Clear the input field
                }
            }
        });
        bottomPanel.add(sendButton, BorderLayout.EAST);
        add(bottomPanel, BorderLayout.SOUTH);

        setVisible(true);
    }

    // Method to send the command to the bot
    private void sendCommand(String command) {
        try (Socket socket = new Socket("localhost", 12345); // Connect to your bot on port 12345
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            out.println(command); // Send the command
            String response;
            while ((response = in.readLine()) != null) {
                SwingUtilities.invokeLater(() -> outputArea.append(response + "\n")); // Use SwingUtilities for thread safety
            }
        } catch (IOException e) {
            SwingUtilities.invokeLater(() -> outputArea.append("Error: " + e.getMessage() + "\n"));
            LOGGER.error("Error: {}", e.getMessage(), e);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new TerminalGUI()); // Use SwingUtilities for thread safety
    }
}