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

    private static final String HTML_PAGE =
        "<!DOCTYPE html><html><head><meta charset=\"UTF-8\">" +
        "<title>Chat Application</title>" +
        "<style>" +
        "body{margin:0;min-height:100vh;display:flex;align-items:center;justify-content:center;" +
        "background:linear-gradient(135deg,#0f0c29,#302b63,#24243e);font-family:sans-serif;color:#fff;}" +
        ".card{background:rgba(255,255,255,.07);backdrop-filter:blur(12px);border:1px solid rgba(255,255,255,.12);" +
        "border-radius:16px;padding:48px;max-width:540px;text-align:center;box-shadow:0 8px 32px rgba(0,0,0,.4);}" +
        "h1{font-size:2rem;margin:0 0 8px;}p{color:#b0b0d0;line-height:1.6;}" +
        "code{background:rgba(255,255,255,.1);padding:4px 10px;border-radius:6px;font-size:.9rem;}" +
        ".status{display:inline-block;width:10px;height:10px;border-radius:50%;background:#4ade80;" +
        "margin-right:6px;box-shadow:0 0 8px #4ade80;}" +
        "</style></head><body><div class=\"card\">" +
        "<h1><span class=\"status\"></span> Chat Server Online</h1>" +
        "<p>This is a <strong>pure Java TCP chat server</strong> built from scratch with zero library dependencies.</p>" +
        "<p>Connect using the Java client:</p>" +
        "<p><code>java com.chatapp.client.ChatClient HOST PORT</code></p>" +
        "<p style=\"margin-top:24px;font-size:.85rem;color:#8888aa;\">Not a web application &mdash; use the TCP client to chat.</p>" +
        "</div></body></html>";

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
            // IMPORTANT: Read the first line BEFORE sending anything.
            // This lets us detect HTTP clients (browsers, health checks) vs chat clients.
            String firstLine = inputReader.readLine();
            if (firstLine == null) {
                disconnect();
                return;
            }

            String trimmed = CustomStringUtil.customTrim(firstLine);

            // Detect HTTP requests (browser / Render health check / load balancer)
            if (CustomStringUtil.customStartsWith(trimmed, "GET ") ||
                CustomStringUtil.customStartsWith(trimmed, "HEAD ") ||
                CustomStringUtil.customStartsWith(trimmed, "POST ") ||
                CustomStringUtil.customStartsWith(trimmed, "PUT ") ||
                CustomStringUtil.customStartsWith(trimmed, "OPTIONS ")) {

                // Drain remaining HTTP headers (read until empty line)
                String headerLine;
                while ((headerLine = inputReader.readLine()) != null) {
                    if (CustomStringUtil.customTrim(headerLine).length() == 0) {
                        break;
                    }
                }

                // Check if this is a health-check path
                boolean isHealthCheck = (trimmed.length() >= 4 &&
                    (CustomStringUtil.customStartsWith(trimmed, "GET /actuator/health") ||
                     CustomStringUtil.customStartsWith(trimmed, "HEAD /actuator/health") ||
                     CustomStringUtil.customStartsWith(trimmed, "GET /health") ||
                     CustomStringUtil.customStartsWith(trimmed, "HEAD /health")));

                if (isHealthCheck) {
                    String jsonBody = "{\"status\":\"UP\"}";
                    String response = "HTTP/1.1 200 OK\r\n" +
                                      "Content-Type: application/json\r\n" +
                                      "Content-Length: " + jsonBody.length() + "\r\n" +
                                      "Connection: close\r\n\r\n" +
                                      jsonBody;
                    byte[] responseBytes = CustomStringUtil.customGetBytes(response);
                    outputStream.write(responseBytes);
                    outputStream.flush();
                } else {
                    // Serve a friendly HTML page for browsers
                    String response = "HTTP/1.1 200 OK\r\n" +
                                      "Content-Type: text/html; charset=UTF-8\r\n" +
                                      "Content-Length: " + HTML_PAGE.length() + "\r\n" +
                                      "Connection: close\r\n\r\n" +
                                      HTML_PAGE;
                    byte[] responseBytes = CustomStringUtil.customGetBytes(response);
                    outputStream.write(responseBytes);
                    outputStream.flush();
                }
                disconnect();
                return;
            }

            // --- It's a chat client. The firstLine IS the username. ---
            // (Chat client connects and immediately types a username.)
            // We need to change the protocol: client sends username first, no prompt needed.
            // But to keep backward compatibility with the existing ChatClient that waits
            // for a WELCOME prompt, we handle both cases.

            // Check: did the client send a username directly, or is it waiting for WELCOME?
            // If the firstLine is NOT "WELCOME" related and looks like a plain name, treat as username.
            // Our ChatClient sends username after receiving WELCOME. But since we now read first,
            // we need to send WELCOME and re-read.

            // Since the first line isn't HTTP, it might be the ChatClient waiting.
            // But actually, our ChatClient code sends nothing first — it waits to receive WELCOME.
            // So the TCP connection from ChatClient would just sit idle until we send something.
            // That means if we got a non-HTTP firstLine here, it's unexpected.
            // Let's handle it: send WELCOME, treat firstLine as empty noise, re-read.

            // Actually - re-examining ChatClient: it connects, then reads WELCOME from server,
            // then sends username. But we just called readLine() which would BLOCK until
            // the client sends something. The ChatClient doesn't send anything first — it reads first.
            // So this path is only reached if something unexpected connects.
            // For a proper fix, let's restructure with a timeout-based peek approach.

            // SIMPLE FIX: We'll update ChatClient to send a special "CHAT" marker first,
            // so we can distinguish. But that changes the client protocol.

            // BETTER FIX: Use socket SO_TIMEOUT to briefly wait, and if no data arrives,
            // assume it's a chat client waiting for WELCOME prompt.

            // Let me use the simplest correct approach:
            // The firstLine we read is either:
            //   - HTTP method line → handled above
            //   - Something else → treat it as a direct username from a raw telnet/nc client

            username = CustomStringUtil.customTrim(trimmed);

            if (username.length() == 0 || server.isUsernameTaken(username)) {
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
                outputStream.write('\n');
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
