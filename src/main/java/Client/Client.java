package Client;

import Server.Message;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.function.Consumer;

import javafx.application.Platform;

public class Client {
    private final String host;
    private final int port;
    private final String username;
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private Consumer<Message> messageHandler;
    private Consumer<String> connectionStatus;
    private boolean isConnected = false;

    
    public Client(String host, int port, String username,
                  Consumer<Message> messageHandler,
                  Consumer<String> connectionStatus) {
        this.host = host;
        this.port = port;
        this.username = username;
        this.messageHandler = messageHandler;
        this.connectionStatus = connectionStatus;
    }
    
    public boolean connect(String password) {
        try {
            socket = new Socket(host, port);
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
            isConnected = true;

            sendMessage(new Message(Message.MessageType.PLAYER_JOINED, username, password));

            Thread receiveThread = new Thread(this::receiveMessages);
            receiveThread.setDaemon(true);
            receiveThread.start();

            Platform.runLater(() -> connectionStatus.accept("Connected to server"));
            return true;
        } catch (IOException e) {
            Platform.runLater(() -> connectionStatus.accept("Connection failed: " + e.getMessage()));
            return false;
        }
    }

    private void handleAuthenticationFailure(String message) {
        UserManager.removeActiveUser(username);
        disconnect();
        Platform.runLater(() -> connectionStatus.accept("Authentication failed: " + message));
    }

    public void disconnect() {
        try {
            if (isConnected) {
                sendMessage(new Message(Message.MessageType.PLAYER_LEFT, username, "has left the server"));

                isConnected = false;
                if (in != null) in.close();
                if (out != null) out.close();
                if (socket != null) socket.close();

                UserManager.removeActiveUser(username);

                Platform.runLater(() -> connectionStatus.accept("Disconnected from server"));
            }
        } catch (IOException e) {
            Platform.runLater(() -> connectionStatus.accept("Error disconnecting: " + e.getMessage()));
        }
    }
    
    public void sendMessage(Message message) {
        try {
            if (isConnected && out != null) {
                out.writeObject(message);
                out.flush();
            }
        } catch (IOException e) {
            Platform.runLater(() -> connectionStatus.accept("Error sending message: " + e.getMessage()));
            disconnect();
        }
    }
    
    public void sendChatMessage(String content) {
        sendMessage(new Message(Message.MessageType.CHAT, username, content));
    }
    
    public void acceptGame(String opponent) {
        sendMessage(new Message(Message.MessageType.GAME_ACCEPT, username, "accepted game request", opponent));
    }

    public void declineGame(String opponent) {
        sendMessage(new Message(Message.MessageType.GAME_DECLINE, username, "declined game request", opponent));
    }
    
    private void receiveMessages() {
        try {
            while (isConnected) {
                Message message = (Message) in.readObject();

                if (message.getType() == Message.MessageType.AUTHENTICATION) {
                    if (message.getContent().startsWith("FAILED")) {
                        handleAuthenticationFailure(message.getContent());
                        return;
                    }
                }
                Platform.runLater(() -> messageHandler.accept(message));
            }
        } catch (IOException | ClassNotFoundException e) {
            if (isConnected) {
                Platform.runLater(() -> connectionStatus.accept("Connection lost: " + e.getMessage()));
                disconnect();
            }
        }
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setMessageHandler(Consumer<Message> messageHandler) {
        this.messageHandler = messageHandler;
    }
    
    public void setConnectionStatusHandler(Consumer<String> connectionStatus) {
        this.connectionStatus = connectionStatus;
    }
    
    public void challengePlayer(String opponent) {
        sendMessage(new Message(Message.MessageType.GAME_REQUEST, username, "wants to play a game", opponent));
    }
}
