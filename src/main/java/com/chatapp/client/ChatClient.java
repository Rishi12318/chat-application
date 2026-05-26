package com.chatapp.client;

import com.chatapp.util.CustomStreamReader;
import com.chatapp.util.CustomStringUtil;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

public class ChatClient {
    public static void main(String[] args) throws Exception {
        String host = "127.0.0.1";
        int port = 5000;
        if (args.length > 0) host = args[0];
        if (args.length > 1) {
            try {
                port = CustomStringUtil.customParseInt(args[1]);
            } catch (NumberFormatException ignored) {
            }
        }
        
        Socket socket = null;
        CustomStreamReader in = null;
        OutputStream out = null;
        CustomStreamReader consoleReader = null;
        
        try {
            socket = new Socket(host, port);
            in = new CustomStreamReader(socket.getInputStream());
            out = socket.getOutputStream();
            consoleReader = new CustomStreamReader(System.in);
            
            // Welcome prompt
            String serverLine = in.readLine();
            if (serverLine != null && CustomStringUtil.customStartsWith(serverLine, "WELCOME|")) {
                System.out.print(CustomStringUtil.customSubstringFrom(serverLine, 8) + " ");
            }
            
            // send username
            String username = consoleReader.readLine();
            if (username != null) {
                byte[] ubytes = CustomStringUtil.customGetBytes(username);
                out.write(ubytes);
                out.write('\n');
                out.flush();
            }
            
            // read server ack
            String ack = in.readLine();
            if (ack != null) {
                System.out.println(ack);
            }
            
            // Start reader thread
            final CustomStreamReader readerIn = in;
            Thread reader = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        String line;
                        while ((line = readerIn.readLine()) != null) {
                            System.out.println(line);
                        }
                    } catch (IOException e) {
                        // ignore
                    }
                }
            });
            reader.setDaemon(true);
            reader.start();
            
            // main loop send input
            String input;
            while ((input = consoleReader.readLine()) != null) {
                byte[] ibytes = CustomStringUtil.customGetBytes(input);
                out.write(ibytes);
                out.write('\n');
                out.flush();
                if (CustomStringUtil.customEqualsIgnoreCase("/quit", input) || CustomStringUtil.customEqualsIgnoreCase("/exit", input)) {
                    break;
                }
            }
        } finally {
            try {
                if (in != null) in.close();
                if (out != null) out.close();
                if (consoleReader != null) consoleReader.close();
                if (socket != null && !socket.isClosed()) socket.close();
            } catch (IOException ignored) {
            }
        }
    }
}
