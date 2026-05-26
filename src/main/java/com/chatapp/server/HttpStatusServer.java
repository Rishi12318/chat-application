package com.chatapp.server;

import com.chatapp.model.Message;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.Deque;
import java.util.stream.Collectors;

public class HttpStatusServer {
    private final ChatServer chatServer;
    private final HttpServer httpServer;

    public HttpStatusServer(ChatServer chatServer, int port) throws IOException {
        this.chatServer = chatServer;
        this.httpServer = HttpServer.create(new InetSocketAddress(port), 0);

        httpServer.createContext("/", new RootHandler());
        httpServer.createContext("/health", new HealthHandler());
        httpServer.setExecutor(null);
    }

    public void start() {
        httpServer.start();
    }

    public void stop() {
        httpServer.stop(1);
    }

    class RootHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            StringBuilder sb = new StringBuilder();
            sb.append("<html><head><meta charset=\"utf-8\"><title>Chat Status</title></head><body>");
            sb.append("<h2>Chat Server Status</h2>");
            sb.append("<p>Clients online: " + chatServer.getClientCount() + "</p>");

            Deque<Message> history = chatServer.getHistorySnapshot();
            sb.append("<h3>Recent Messages</h3>");
            sb.append("<pre style=\"white-space:pre-wrap;max-width:1000px\">");
            String messages = history.stream()
                    .map(Message::toString)
                    .collect(Collectors.joining("\n"));
            sb.append(escapeHtml(messages));
            sb.append("</pre>");
            sb.append("</body></html>");

            byte[] resp = sb.toString().getBytes("UTF-8");
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
            exchange.sendResponseHeaders(200, resp.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(resp);
            }
        }
    }

    class HealthHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String ok = "OK";
            byte[] resp = ok.getBytes("UTF-8");
            exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
            exchange.sendResponseHeaders(200, resp.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(resp);
            }
        }
    }

    private static String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
