package Server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.function.Consumer;

public class Server {
    private final int port;
    private ServerSocket serverSocket;
    private final ArrayList<ClientHandler> clients = new ArrayList<>();
    private final Consumer<String> serverLog;
    private Consumer<String[]> clientListUpdater;
    private final Map<String, ClientHandler> waitingPlayers = new HashMap<>();
    private boolean isRunning = false;
    private static final Set<String> activeUsernames = new HashSet<>();

    public Server(int port, Consumer<String> serverLog) {
        this.port = port;
        this.serverLog = serverLog;
    }

    public void setClientListUpdater(Consumer<String[]> clientListUpdater) {
        this.clientListUpdater = clientListUpdater;
    }

    public void start() {
        try {
            serverSocket = new ServerSocket(port);
            isRunning = true;
            serverLog.accept("Server initialized on host: localhost, port: " + port);

            new Thread(() -> {
                try {
                    while (isRunning) {
                        Socket clientSocket = serverSocket.accept();
                        serverLog.accept("New client connection from: " + clientSocket.getInetAddress());

                        ClientHandler handler = new ClientHandler(clientSocket);
                        clients.add(handler);
                        new Thread(handler).start();
                    }
                } catch (IOException e) {
                    if (isRunning) {
                        serverLog.accept("Server error: " + e.getMessage());
                    }
                }
            }).start();

        } catch (IOException e) {
            serverLog.accept("Failed to start server: " + e.getMessage());
        }
    }

    public void stop() {
        try {
            isRunning = false;
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();

                for (ClientHandler client : clients) {
                    client.close();
                }
                clients.clear();
                waitingPlayers.clear();

                serverLog.accept("Server stopped");
            }
        } catch (IOException e) {
            serverLog.accept("Error stopping server: " + e.getMessage());
        }
    }

    private void updateClientList() {
        if (clientListUpdater != null) {
            String[] clientNames = clients.stream()
                    .map(client -> client.username)
                    .filter(name -> name != null && !name.isEmpty())
                    .toArray(String[]::new);
            clientListUpdater.accept(clientNames);
        }
    }

    private void broadcast(Message message) {
        for (ClientHandler client : clients) {
            client.sendMessage(message);
        }
    }

    private void matchPlayers(String username, ClientHandler handler) {
        if (waitingPlayers.isEmpty()) {
            waitingPlayers.put(username, handler);
            serverLog.accept(username + " is waiting for a random opponent");
            handler.sendMessage(new Message(Message.MessageType.CHAT, "Server", "Waiting for an opponent..."));
        } else {
            String opponentName = waitingPlayers.keySet().iterator().next();
            ClientHandler opponent = waitingPlayers.remove(opponentName);

            serverLog.accept("Random match created: " + username + " vs " + opponentName);

            Message matchMessage1 = new Message(Message.MessageType.GAME_REQUEST,
                    "Server",
                    "RANDOM_MATCH",
                    opponentName);
            handler.sendMessage(matchMessage1);

            Message matchMessage2 = new Message(Message.MessageType.GAME_REQUEST,
                    "Server",
                    "RANDOM_MATCH",
                    username);
            opponent.sendMessage(matchMessage2);
        }
    }

    private void sendPlayerList(ClientHandler client) {
        String[] clientNames = clients.stream()
                .map(c -> c.username)
                .filter(name -> name != null && !name.isEmpty())
                .toArray(String[]::new);

        StringBuilder playerList = new StringBuilder();
        for (String name : clientNames) {
            playerList.append(name).append(",");
        }

        if (!playerList.isEmpty()) {
            playerList.setLength(playerList.length() - 1);
        }

        Message playerListMessage = new Message(
                Message.MessageType.PLAYER_LIST,
                "Server",
                "Player list",
                playerList.toString()
        );

        client.sendMessage(playerListMessage);
    }

    private void broadcastPlayerList() {
        String[] clientNames = clients.stream()
                .map(c -> c.username)
                .filter(name -> name != null && !name.isEmpty())
                .toArray(String[]::new);

        StringBuilder playerList = new StringBuilder();
        for (String name : clientNames) {
            playerList.append(name).append(",");
        }

        if (!playerList.isEmpty()) {
            playerList.setLength(playerList.length() - 1);
        }

        Message playerListMessage = new Message(
                Message.MessageType.PLAYER_LIST,
                "Server",
                "Player list",
                playerList.toString()
        );

        broadcast(playerListMessage);
    }

    private class ClientHandler implements Runnable {
        private final Socket clientSocket;
        private ObjectOutputStream out;
        private ObjectInputStream in;
        private String username;
        private boolean isRunning = true;

        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
            try {
                out = new ObjectOutputStream(socket.getOutputStream());
                in = new ObjectInputStream(socket.getInputStream());
            } catch (IOException e) {
                serverLog.accept("Error setting up client handler: " + e.getMessage());
            }
        }

        @Override
        public void run() {
            try {
                Message firstMessage = (Message) in.readObject();

                if (firstMessage.getType() == Message.MessageType.PLAYER_JOINED) {
                    username = firstMessage.getSender();

                    synchronized (activeUsernames) {
                        if (activeUsernames.contains(username.toLowerCase())) {
                            sendMessage(new Message(Message.MessageType.AUTHENTICATION, "Server",
                                    "FAILED: Username already logged in", "USERNAME_ACTIVE"));
                            serverLog.accept("Authentication failed: Username '" + username + "' is already active");
                            close();
                            return;
                        }

                        activeUsernames.add(username.toLowerCase());
                        serverLog.accept("Authentication successful: User '" + username + "' logged in");
                    }

                    sendMessage(new Message(Message.MessageType.AUTHENTICATION, "Server",
                            "SUCCESS: Authentication successful", "AUTH_SUCCESS"));

                    broadcast(new Message(Message.MessageType.PLAYER_JOINED, "Server",
                            username + " has joined the server"));
                    updateClientList();
                    sendPlayerList(this);
                    broadcastPlayerList();

                    serverLog.accept("Currently logged in users: " + String.join(", ", activeUsernames));
                }

                while (isRunning) {
                    Message message = (Message) in.readObject();

                    switch (message.getType()) {
                        case CHAT:
                            if (message.getData() != null) {
                                String targetPlayer = (String) message.getData();
                                serverLog.accept("Private message from " + username + " to " + targetPlayer + ": " + message.getContent());
                                for (ClientHandler client : clients) {
                                    if (client.username.equals(targetPlayer)) {
                                        client.sendMessage(message);
                                        break;
                                    }
                                }
                            } else {
                                serverLog.accept("Global message from " + username + ": " + message.getContent());
                                broadcast(message);
                            }
                            break;

                        case GAME_REQUEST:
                            if (message.getData() != null) {
                                String targetOpponent = (String) message.getData();
                                serverLog.accept(username + " challenged " + targetOpponent + " to a game");
                                for (ClientHandler client : clients) {
                                    if (client.username.equals(targetOpponent)) {
                                        client.sendMessage(new Message(Message.MessageType.GAME_REQUEST,
                                                username, message.getContent(), username));
                                        break;
                                    }
                                }
                            } else {
                                matchPlayers(username, this);
                            }
                            break;

                        case GAME_ACCEPT:
                            String opponent = (String) message.getData();
                            serverLog.accept(username + " accepted game request from " + opponent);
                            for (ClientHandler client : clients) {
                                if (client.username.equals(opponent)) {
                                    client.sendMessage(new Message(Message.MessageType.GAME_ACCEPT,
                                            "Server", username + " accepted your game request", username));
                                    break;
                                }
                            }
                            break;

                        case GAME_DECLINE:
                            String declinedOpponent = (String) message.getData();
                            serverLog.accept(username + " declined game request from " + declinedOpponent);
                            for (ClientHandler client : clients) {
                                if (client.username.equals(declinedOpponent)) {
                                    client.sendMessage(new Message(Message.MessageType.GAME_DECLINE,
                                            "Server", username + " declined your game request"));
                                    break;
                                }
                            }
                            break;

                        case GAME_MOVE:
                            String moveOpponent = (String) message.getData();

                            if (message.getContent().contains("placed token")) {
                                serverLog.accept(message.getContent());
                            } else {
                                String[] parts = message.getContent().split(",");
                                if (parts.length >= 2) {
                                    int column = Integer.parseInt(parts[0]);
                                    int row = Integer.parseInt(parts[1]);
                                    serverLog.accept(username + " placed token at column " + column + ", row " + row);
                                }
                            }
                            for (ClientHandler client : clients) {
                                if (client.username.equals(moveOpponent)) {
                                    client.sendMessage(message);
                                    break;
                                }
                            }
                            break;

                        case PLAYER_LEFT:
                            isRunning = false;
                            break;

                        case PLAYER_LIST:
                            sendPlayerList(this);
                            break;

                        case GAME_OVER:
                            if (message.getData() != null) {
                                if (message.getData().equals("Server")) {
                                    serverLog.accept(message.getContent());
                                } else {
                                    String gameOverOpponent = (String) message.getData();

                                    if (message.getContent().contains("draw")) {
                                        serverLog.accept("Game ended in a draw between " + username + " and " + gameOverOpponent);
                                    } else if (message.getContent().contains("won")) {
                                        serverLog.accept(username + " won against " + gameOverOpponent);
                                    } else if (message.getContent().contains("lost")) {
                                        serverLog.accept(username + " lost against " + gameOverOpponent);
                                    } else if (message.getContent().contains("left the game")) {
                                        serverLog.accept(username + " left the game with " + gameOverOpponent);
                                        serverLog.accept(gameOverOpponent + " returned to lobby");
                                    }

                                    for (ClientHandler client : clients) {
                                        if (client.username.equals(gameOverOpponent)) {
                                            client.sendMessage(message);
                                            break;
                                        }
                                    }
                                }
                            }
                            break;


                        case REMATCH_REQUEST:
                            if (message.getData() != null) {
                                String rematchOpponent = (String) message.getData();
                                serverLog.accept(username + " requested a rematch with " + rematchOpponent);
                                for (ClientHandler client : clients) {
                                    if (client.username.equals(rematchOpponent)) {
                                        client.sendMessage(message);
                                        break;
                                    }
                                }
                            }
                            break;

                        case REMATCH_ACCEPT:
                            if (message.getData() != null) {
                                String rematchAcceptOpponent = (String) message.getData();
                                serverLog.accept(username + " accepted rematch request from " + rematchAcceptOpponent);
                                for (ClientHandler client : clients) {
                                    if (client.username.equals(rematchAcceptOpponent)) {
                                        client.sendMessage(message);
                                        break;
                                    }
                                }
                            }
                            break;

                        case REMATCH_DECLINE:
                            if (message.getData() != null) {
                                String rematchDeclineOpponent = (String) message.getData();
                                serverLog.accept(username + " declined rematch request from " + rematchDeclineOpponent);
                                for (ClientHandler client : clients) {
                                    if (client.username.equals(rematchDeclineOpponent)) {
                                        client.sendMessage(message);
                                        break;
                                    }
                                }
                            }
                            break;

                        default:
                            break;
                    }
                }
            } catch (IOException | ClassNotFoundException e) {
                if (username != null) {
                    serverLog.accept("Client disconnected: " + username);
                }
            } finally {
                close();
                clients.remove(this);
                waitingPlayers.remove(username);

                broadcast(new Message(Message.MessageType.PLAYER_LEFT, "Server",
                        username + " has left the server"));
                updateClientList();
                broadcastPlayerList();
            }
        }

        public void sendMessage(Message message) {
            try {
                out.writeObject(message);
                out.flush();
            } catch (IOException e) {
                if (username != null) {
                    serverLog.accept("Error sending message to " + username);
                }
            }
        }

        public void close() {
            isRunning = false;
            try {
                if (username != null) {
                    synchronized (activeUsernames) {
                        activeUsernames.remove(username.toLowerCase());
                        serverLog.accept("User '" + username + "' logged out");
                    }
                }
                if (in != null) in.close();
                if (out != null) out.close();
                if (clientSocket != null && !clientSocket.isClosed()) clientSocket.close();
            } catch (IOException e) {
                serverLog.accept("Error closing client connection");
            }
        }
    }
}
