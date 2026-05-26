# Pure Java Chat Application (Zero Inbuilt Library Functions)

A lightweight, multi-client TCP chat application built from scratch in pure Java, avoiding standard JDK collections, helper utility methods, scanners, formatters, and thread pools.

## Features
- **TCP Chat Server & Client**: Communicates using native OS-level raw socket streams (`java.net.Socket`, `java.net.ServerSocket`).
- **Zero Built-In Collections**: All data structures are built from scratch:
  - `CustomList`: Thread-safe dynamically resizing array list.
  - `CustomMap`: Thread-safe synchronized bucket-based hash map with custom string hashing.
  - `CustomDeque`: Thread-safe doubly-linked queue for message history management.
- **Custom Utilities**:
  - `CustomStringUtil`: Hand-coded methods for splitting, joining, trimming, case conversion, formatting, parsing, and byte conversion.
  - `CustomStreamReader`: Custom buffered byte reader for line-by-line streaming from input streams (sockets/console).
  - `CustomTimeUtil`: Epoch millisecond calculation and manual timezone-offset string padding/formatting (GMT+5:30 default).
- **Embedded Health Check Interceptor**: Intercepts HTTP `GET` or `HEAD` requests (e.g. from Render or AWS ELB) and returns HTTP `200 OK` status before disconnecting, making a raw TCP socket server deployable as a Web Service.

## Architecture
- `com.chatapp.model`: Data objects (`Message`, `MessageType`).
- `com.chatapp.util`: Hand-rolled collections, string, time, and stream reading utilities.
- `com.chatapp.server`: `ChatServer` listener and client threads (`ClientHandler`).
- `com.chatapp.client`: `ChatClient` that connects to the server, starts a daemon thread to read responses, and sends console inputs.

## Run Locally

### 1. Compile
```bash
javac -d target/classes src/main/java/com/chatapp/model/*.java src/main/java/com/chatapp/util/*.java src/main/java/com/chatapp/server/*.java src/main/java/com/chatapp/client/*.java
```

### 2. Start Server
```bash
java -cp target/classes com.chatapp.server.ChatServer 6000
```

### 3. Connect Clients
```bash
java -cp target/classes com.chatapp.client.ChatClient 127.0.0.1 6000
```

## Available Chat Commands
- `/users` - List all online users
- `/pm <username> <message>` - Send a private message
- `/quit` - Exit the chat
- `/help` - Show help menu
