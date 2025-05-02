package Client;

import Server.Message;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;


public class GameLobby extends BorderPane {
    private final Client client;
    private final Stage primaryStage;

    private ListView<String> playerListView;
    private TextArea chatArea;
    private TextField messageField;
    private Label statusLabel;

    private final Map<String, Alert> pendingGameRequests = new HashMap<>();

    public GameLobby(Stage primaryStage, Client client) {
        this.primaryStage = primaryStage;
        this.client = client;

        setupUI();
        setupMessageHandling();

        client.sendMessage(new Message(Message.MessageType.PLAYER_LIST, client.getUsername(), "request player list"));

        primaryStage.setWidth(900);
        primaryStage.setHeight(650);
        primaryStage.setResizable(false);

        primaryStage.setTitle("Connect Four - Logged in as " + client.getUsername());

        this.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/styles/lobby.css")).toExternalForm());
    }

    private void setupUI() {
        this.setPadding(new Insets(10));

        HBox header = createHeader();
        header.getStyleClass().add("header");
        this.setTop(header);

        VBox playerListPanel = createPlayerListPanel();
        playerListPanel.getStyleClass().add("player-list-panel");
        this.setRight(playerListPanel);

        VBox chatPanel = createChatPanel();
        chatPanel.getStyleClass().add("chat-panel");
        this.setCenter(chatPanel);

        statusLabel = new Label("Connected to server");
        statusLabel.getStyleClass().add("status-label");
        statusLabel.setPadding(new Insets(5));
        this.setBottom(statusLabel);
    }

    private HBox createHeader() {
        HBox header = new HBox(20);
        header.setPadding(new Insets(10));
        header.setAlignment(Pos.CENTER_LEFT);

        Label titleLabel = new Label("Connect Four Lobby");
        titleLabel.getStyleClass().add("header-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button playButton = new Button("Play Random Opponent");
        playButton.setOnAction(e -> requestRandomGame());

        Button logoutButton = new Button("Logout");
        logoutButton.setOnAction(e -> logout());

        header.getChildren().addAll(titleLabel, spacer, playButton, logoutButton);
        return header;
    }

    private VBox createPlayerListPanel() {
        VBox panel = new VBox(10);
        panel.setPadding(new Insets(10));
        panel.setPrefWidth(200);

        Label playersLabel = new Label("Online Players");
        playersLabel.getStyleClass().add("player-list-title");

        playerListView = new ListView<>();
        playerListView.setPrefHeight(400);

        Button challengeButton = new Button("Challenge Player");
        challengeButton.setMaxWidth(Double.MAX_VALUE);
        challengeButton.setOnAction(e -> {
            String selectedPlayer = playerListView.getSelectionModel().getSelectedItem();
            if (selectedPlayer != null && !selectedPlayer.equals(client.getUsername())) {
                requestGame(selectedPlayer);
            } else if (selectedPlayer != null && selectedPlayer.equals(client.getUsername())) {
                showAlert("You cannot challenge yourself!");
            } else {
                showAlert("Please select a player to challenge");
            }
        });

        panel.getChildren().addAll(playersLabel, playerListView, challengeButton);
        return panel;
    }

    private VBox createChatPanel() {
        VBox panel = new VBox(10);
        panel.setPadding(new Insets(10));

        Label chatLabel = new Label("Game Chat");
        chatLabel.getStyleClass().add("chat-title");

        chatArea = new TextArea();
        chatArea.setEditable(false);
        chatArea.setWrapText(true);
        chatArea.setPrefHeight(350);

        HBox messageBox = new HBox(10);
        messageField = new TextField();
        messageField.setPromptText("Type a message...");
        messageField.setOnAction(e -> sendChatMessage());
        HBox.setHgrow(messageField, Priority.ALWAYS);

        Button sendButton = new Button("Send");
        sendButton.setOnAction(e -> sendChatMessage());

        messageBox.getChildren().addAll(messageField, sendButton);

        panel.getChildren().addAll(chatLabel, chatArea, messageBox);
        return panel;
    }

    private void setupMessageHandling() {
        client.setMessageHandler(this::handleMessage);
        client.setConnectionStatusHandler(this::updateStatus);
    }

    private void handleMessage(Message message) {
        Platform.runLater(() -> {
            switch (message.getType()) {
                case CHAT:
                    if (message.getData() == null) {
                        addChatMessage(message.getSender() + ": " + message.getContent());
                    }
                    break;

                case PLAYER_JOINED:
                    addChatMessage("*** " + message.getContent() + " ***");
                    break;

                case PLAYER_LEFT:
                    addChatMessage("*** " + message.getContent() + " ***");
                    break;

                case GAME_REQUEST:
                    if (message.getData() != null) {
                        String challenger = (String) message.getData();
                        if (!challenger.equals(client.getUsername())) {
                            if (message.getContent() != null && message.getContent().equals("RANDOM_MATCH")) {
                                addChatMessage("*** Matched with " + challenger + " for a game! ***");
                                client.acceptGame(challenger);
                                startGame(challenger);
                            } else {
                                showGameRequest(challenger);
                            }
                        }
                    }
                    break;

                case GAME_ACCEPT:
                    if (message.getData() != null) {
                        String opponent = (String) message.getData();
                        addChatMessage("*** " + opponent + " accepted your game request! ***");
                        startGame(opponent);
                    }
                    break;

                case GAME_DECLINE:
                    addChatMessage("*** " + message.getContent() + " ***");
                    break;

                case PLAYER_LIST:
                    if (message.getData() != null) {
                        String playerListStr = (String) message.getData();
                        updatePlayerList(playerListStr.split(","));
                    }
                    break;

                default:
                    System.out.println("Unhandled message type: " + message.getType());
            }
        });
    }

    private void updateStatus(String status) {
        Platform.runLater(() -> {
            statusLabel.setText(status);
            if (status.startsWith("Error") || status.startsWith("Connection failed")) {
                statusLabel.setTextFill(Color.RED);
            } else {
                statusLabel.setTextFill(Color.BLACK);
            }
        });
    }

    private void addChatMessage(String message) {
        chatArea.appendText(message + "\n");

        chatArea.setScrollTop(Double.MAX_VALUE);
    }

    private void sendChatMessage() {
        String message = messageField.getText().trim();
        if (!message.isEmpty()) {
            client.sendChatMessage(message);
            messageField.clear();
        }
    }


    private void updatePlayerList(String[] players) {
        Platform.runLater(() -> {
            playerListView.getItems().clear();
            for (String player : players) {
                if (player.equals(client.getUsername())) {
                    playerListView.getItems().add(player + " (You)");
                } else {
                    playerListView.getItems().add(player);
                }
            }
        });
    }

    private void requestRandomGame() {
        client.sendMessage(new Message(Message.MessageType.GAME_REQUEST,
                client.getUsername(),
                "RANDOM_MATCH"));

        addChatMessage("*** Waiting for a random opponent... ***");
    }

    private void requestGame(String opponent) {
        client.challengePlayer(opponent);
        addChatMessage("*** Game request sent to " + opponent + " ***");
    }

    private void showGameRequest(String challenger) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Game Request");
        alert.setHeaderText("Game Request from " + challenger);
        alert.setContentText("Do you want to play Connect Four with " + challenger + "?");

        ButtonType acceptButton = new ButtonType("Accept");
        ButtonType declineButton = new ButtonType("Decline");

        alert.getButtonTypes().setAll(acceptButton, declineButton);
        pendingGameRequests.put(challenger, alert);

        alert.showAndWait().ifPresent(response -> {
            if (response == acceptButton) {
                client.acceptGame(challenger);
                startGame(challenger);
            } else {
                client.declineGame(challenger);
            }
            pendingGameRequests.remove(challenger);
        });
    }

    private void startGame(String opponent) {
        Alert pendingRequest = pendingGameRequests.remove(opponent);
        if (pendingRequest != null) {
            pendingRequest.close();
        }

        String player1 = client.getUsername();

        int minute = java.time.LocalDateTime.now().getMinute();
        boolean playerStarts;

        if (minute % 2 == 0) {
            playerStarts = player1.compareTo(opponent) < 0;
        } else {
            playerStarts = player1.compareTo(opponent) > 0;
        }

        System.out.println("Starting game with " + opponent + ", playerStarts=" + playerStarts);

        ConnectFourGame connectFourGame = new ConnectFourGame(primaryStage, client, opponent, playerStarts);
        primaryStage.getScene().setRoot(connectFourGame);
        primaryStage.setTitle("Connect Four - Game with " + opponent);
    }

    private void logout() {
        client.disconnect();
        LoginScreen loginScreen = new LoginScreen(primaryStage);
        primaryStage.getScene().setRoot(loginScreen);
        primaryStage.setTitle("Connect Four");
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
