package com.chatapp.server;

import com.chatapp.model.Message;
import com.chatapp.model.MessageType;
import com.chatapp.util.CustomStreamReader;
import com.chatapp.util.CustomStringUtil;
import com.chatapp.util.MessageFormatter;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private Socket socket;
    private CustomStreamReader inputReader;
    private OutputStream outputStream;
    private String username;
    private ChatServer server;
    private boolean isConnected;
    
    public ClientHandler(Socket socket, ChatServer server) {
        this.socket = socket;
        this.server = server;
        this.isConnected = true;
        
        try {
            inputReader = new CustomStreamReader(socket.getInputStream());
            outputStream = socket.getOutputStream();
        } catch (IOException e) {
            System.err.println("Error creating client handler: " + e.getMessage());
        }
    }
    
    @Override
    public void run() {
        try {
            // Get username from client
            sendMessage("WELCOME|Please enter your username:");
            String rawUsername = inputReader.readLine();
            
            // Check if this is an HTTP health check request from Render/deployment environment
            if (rawUsername != null) {
                String trimmed = CustomStringUtil.customTrim(rawUsername);
                if (CustomStringUtil.customStartsWith(trimmed, "GET ") || 
                    CustomStringUtil.customStartsWith(trimmed, "HEAD ") ||
                    CustomStringUtil.customStartsWith(trimmed, "GET/") ||
                    CustomStringUtil.customStartsWith(trimmed, "HEAD/")) {
                    
                    // Respond with HTTP 200 OK JSON status response
                    String httpResponse = "HTTP/1.1 200 OK\r\n" +
                                          "Content-Type: application/json\r\n" +
                                          "Content-Length: 15\r\n" +
                                          "Connection: close\r\n\r\n" +
                                          "{\"status\":\"UP\"}";
                    byte[] responseBytes = CustomStringUtil.customGetBytes(httpResponse);
                    outputStream.write(responseBytes);
                    outputStream.flush();
                    disconnect();
                    return;
                }
            }
            
            if (rawUsername != null) {
                username = CustomStringUtil.customTrim(rawUsername);
            }
            
            if (username == null || username.length() == 0 || server.isUsernameTaken(username)) {
                sendMessage("ERROR|Username invalid or already taken");
                disconnect();
                return;
            }
            
            sendMessage(CustomStringUtil.customFormat("SUCCESS|Welcome %s!", username));
            System.out.println("✅ " + username + " joined the chat!");
            
            // Register client with server
            server.registerClient(username, this);
            
            // Send welcome message
            sendSystemMessage(CustomStringUtil.customFormat("Welcome to the Chat Room, %s!", username));
            sendSystemMessage("Type /help for available commands");
            
            // Main message loop
            String inputLine;
            while (isConnected && (inputLine = inputReader.readLine()) != null) {
                if (CustomStringUtil.customStartsWith(inputLine, "/")) {
                    handleCommand(inputLine);
                } else {
                    // Broadcast public message
                    Message message = new Message(
                        username,
                        inputLine,
                        System.currentTimeMillis(),
                        MessageType.PUBLIC,
                        null
                    );
                    server.broadcastMessage(message);
                }
            }
            
        } catch (IOException e) {
            if (isConnected) {
                System.out.println("⚠️ " + (username != null ? username : "Client") + " disconnected unexpectedly");
            }
        } finally {
            disconnect();
        }
    }
    
    private void handleCommand(String command) {
        String[] parts = CustomStringUtil.customSplit(command, ' ', 3);
        String cmd = CustomStringUtil.customToLowerCase(parts[0]);
        
        if (CustomStringUtil.customEqualsIgnoreCase(cmd, "/quit") || CustomStringUtil.customEqualsIgnoreCase(cmd, "/exit")) {
            sendMessage("Goodbye!");
            disconnect();
        } else if (CustomStringUtil.customEqualsIgnoreCase(cmd, "/users") || CustomStringUtil.customEqualsIgnoreCase(cmd, "/who")) {
            server.sendUserList(this);
        } else if (CustomStringUtil.customEqualsIgnoreCase(cmd, "/pm") || CustomStringUtil.customEqualsIgnoreCase(cmd, "/private")) {
            if (parts.length < 3) {
                sendMessage("❌ Usage: /pm <username> <message>");
                return;
            }
            String recipient = parts[1];
            String message = parts[2];
            server.sendPrivateMessage(username, recipient, message);
        } else if (CustomStringUtil.customEqualsIgnoreCase(cmd, "/help")) {
            sendHelp();
        } else {
            sendMessage(CustomStringUtil.customFormat("❌ Unknown command: %s. Type /help for commands", cmd));
        }
    }
    
    private void sendHelp() {
        sendMessage("");
        sendMessage("┌────────────────────────────────────────┐");
        sendMessage("│           📖 AVAILABLE COMMANDS        │");
        sendMessage("├────────────────────────────────────────┤");
        sendMessage("│  /users      - List all online users   │");
        sendMessage("│  /pm <user> <msg> - Send private msg   │");
        sendMessage("│  /quit       - Exit the chat           │");
        sendMessage("│  /help       - Show this help          │");
        sendMessage("└────────────────────────────────────────┘");
        sendMessage("");
    }
    
    public void sendMessage(String message) {
        if (outputStream != null && isConnected) {
            try {
                byte[] bytes = CustomStringUtil.customGetBytes(message);
                outputStream.write(bytes);
                outputStream.write('\n'); // Carriage return / newline
                outputStream.flush();
            } catch (IOException e) {
                disconnect();
            }
        }
    }
    
    public void sendSystemMessage(String message) {
        Message sysMsg = new Message("SYSTEM", message, System.currentTimeMillis(), MessageType.SYSTEM, null);
        sendMessage(MessageFormatter.format(sysMsg));
    }
    
    public void disconnect() {
        if (!isConnected) return;
        
        isConnected = false;
        
        if (username != null) {
            server.unregisterClient(username);
            System.out.println("❌ " + username + " left the chat");
        }
        
        try {
            if (inputReader != null) inputReader.close();
            if (outputStream != null) outputStream.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            System.err.println("Error closing client connection: " + e.getMessage());
        }
    }
    
    public String getUsername() { return username; }
    public boolean isConnected() { return isConnected; }
}
