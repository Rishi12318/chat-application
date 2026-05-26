package com.chatapp.server;

import com.chatapp.model.Message;
import com.chatapp.model.MessageType;
import com.chatapp.util.CustomMap;
import com.chatapp.util.CustomDeque;
import com.chatapp.util.CustomStringUtil;
import com.chatapp.util.MessageFormatter;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ChatServer {
    private final int port;
    private final ServerSocket serverSocket;
    private final CustomMap clients = new CustomMap();
    private final CustomDeque history = new CustomDeque();
    private final int HISTORY_SIZE = 200;
    
    public ChatServer(int port) throws IOException {
        this.port = port;
        this.serverSocket = new ServerSocket(port);
    }
    
    public void start() {
        System.out.println("Chat server started on port " + port);

        try {
            while (!serverSocket.isClosed()) {
                Socket clientSocket = serverSocket.accept();
                ClientHandler handler = new ClientHandler(clientSocket, this);
                Thread clientThread = new Thread(handler);
                clientThread.start();
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        } finally {
            shutdown();
        }
    }
    
    public void shutdown() {
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            // Disconnect all registered clients
            Object[] handlers = clients.values();
            for (int i = 0; i < handlers.length; i++) {
                ClientHandler ch = (ClientHandler) handlers[i];
                if (ch != null) {
                    ch.disconnect();
                }
            }
            System.out.println("Server shutdown");
        } catch (IOException e) {
            System.err.println("Error shutting down server: " + e.getMessage());
        }
    }
    
    public void registerClient(String username, ClientHandler handler) {
        clients.put(username, handler);
        sendSystemBroadcast("USERS:" + CustomStringUtil.customJoin(",", clients.keys(), clients.size()));
        sendHistoryTo(handler);
    }
    
    public void unregisterClient(String username) {
        clients.remove(username);
        sendSystemBroadcast("USERS:" + CustomStringUtil.customJoin(",", clients.keys(), clients.size()));
    }
    
    public boolean isUsernameTaken(String username) {
        return clients.containsKey(username);
    }
    
    public void broadcastMessage(Message message) {
        if (message == null) return;
        
        addToHistory(message);
        String formatted = MessageFormatter.format(message);
        Object[] handlers = clients.values();
        for (int i = 0; i < handlers.length; i++) {
            ClientHandler ch = (ClientHandler) handlers[i];
            if (ch != null) {
                ch.sendMessage(formatted);
            }
        }
    }
    
    public void sendPrivateMessage(String from, String to, String content) {
        ClientHandler recipient = (ClientHandler) clients.get(to);
        if (recipient == null) {
            ClientHandler sender = (ClientHandler) clients.get(from);
            if (sender != null) sender.sendMessage("❌ User not found: " + to);
            return;
        }
        Message pm = new Message(from, content, System.currentTimeMillis(), MessageType.PRIVATE, to);
        recipient.sendMessage(MessageFormatter.format(pm));
    }
    
    public void sendSystemBroadcast(String content) {
        Message sys = new Message("SERVER", content, System.currentTimeMillis(), MessageType.SYSTEM, null);
        addToHistory(sys);
        String formatted = MessageFormatter.format(sys);
        Object[] handlers = clients.values();
        for (int i = 0; i < handlers.length; i++) {
            ClientHandler ch = (ClientHandler) handlers[i];
            if (ch != null) ch.sendMessage(formatted);
        }
    }
    
    private void addToHistory(Message m) {
        history.addLast(m);
        while (history.size() > HISTORY_SIZE) history.removeFirst();
    }
    
    private void sendHistoryTo(ClientHandler handler) {
        Object[] snapshot = history.toSnapshot();
        for (int i = 0; i < snapshot.length; i++) {
            Message m = (Message) snapshot[i];
            if (m != null) {
                handler.sendMessage(MessageFormatter.format(m));
            }
        }
    }
    
    public void sendUserList(ClientHandler requester) {
        String list = "USERS:" + CustomStringUtil.customJoin(",", clients.keys(), clients.size());
        requester.sendMessage(list);
    }
    
    // Server main
    public static void main(String[] args) throws Exception {
        int port = 5000;
        if (args.length > 0) {
            try { port = CustomStringUtil.customParseInt(args[0]); } catch (NumberFormatException ignored) {}
        }
        final ChatServer server = new ChatServer(port);
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                server.shutdown();
            }
        }));
        server.start();
    }
}
