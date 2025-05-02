package Client;

import Server.Message;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.util.Objects;

public class GameResultScreen extends BorderPane {
    private final Stage primaryStage;
    private final Client client;
    private final String opponentName;
    private final String result;
    private Label statusLabel;
    private Button rematchButton;
    private static final String REMATCH_REQUEST = "REMATCH_REQUEST_SPECIAL";

    
    public GameResultScreen(Stage primaryStage, Client client, String opponentName, String result,
                            int turnCount, long gameDuration) {
        this.primaryStage = primaryStage;
        this.client = client;
        this.opponentName = opponentName;
        this.result = result;

        setupUI(turnCount, gameDuration);
        setupMessageHandling();

        primaryStage.setWidth(500);
        primaryStage.setHeight(500);
        primaryStage.setResizable(false);

        this.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/styles/result.css")).toExternalForm());
    }
    
    private void setupUI(int turnCount, long gameDuration) {
        this.setPadding(new Insets(20));

        VBox header = new VBox(10);
        header.getStyleClass().add("header");
        header.setAlignment(Pos.CENTER);

        Label titleLabel = new Label("Game Over");
        titleLabel.getStyleClass().add("title-label");

        Label resultLabel = new Label(result);
        resultLabel.getStyleClass().add("result-label");

        if (result.contains("won")) {
            resultLabel.setTextFill(Color.YELLOW);
        } else if (result.contains("lost")) {
            resultLabel.setTextFill(Color.RED);
        }

        header.getChildren().addAll(titleLabel, resultLabel);
        this.setTop(header);

        long durationInSeconds = gameDuration / 1000;
        long minutes = durationInSeconds / 60;
        long seconds = durationInSeconds % 60;
        String durationText = String.format("%02d:%02d", minutes, seconds);

        Label statsLabel = new Label("Game Statistics: " + turnCount + " turns | Time: " + durationText);
        statsLabel.getStyleClass().add("stats-label");

        VBox buttonBox = new VBox(15);
        buttonBox.getStyleClass().add("button-box");
        buttonBox.setAlignment(Pos.CENTER);

        rematchButton = new Button("Request Rematch");
        rematchButton.getStyleClass().addAll("button", "rematch-button");
        rematchButton.setOnAction(e -> requestRematch());

        Button lobbyButton = new Button("Return to Lobby");
        lobbyButton.getStyleClass().addAll("button", "lobby-button");
        lobbyButton.setOnAction(e -> returnToLobby());

        buttonBox.getChildren().addAll(statsLabel, rematchButton, lobbyButton);
        this.setCenter(buttonBox);

        statusLabel = new Label("Game completed");
        statusLabel.getStyleClass().add("status-label");
        this.setBottom(statusLabel);
    }

    private void setupMessageHandling() {
        client.setMessageHandler(this::handleMessage);
    }

    private void handleMessage(Message message) {
        System.out.println("GameResultScreen received message: " + message.getType() +
                " from " + message.getSender() +
                ", content: " + message.getContent());

        Platform.runLater(() -> {

            if (message.getSender().equals(opponentName) &&
                    (message.getContent().equals(REMATCH_REQUEST) ||
                            (message.getType() == Message.MessageType.CHAT && message.getContent().equals(REMATCH_REQUEST)))) {

                statusLabel.setText(opponentName + " has requested a rematch. Returning to lobby...");


                new Thread(() -> {
                    try {
                        Thread.sleep(1500);
                        Platform.runLater(this::returnToLobby);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }).start();
            }
        });
    }

    private void requestRematch() {
        rematchButton.setDisable(true);


        client.sendMessage(new Message(Message.MessageType.REMATCH_REQUEST,
                client.getUsername(),
                "requested a rematch",
                opponentName));


        client.sendMessage(new Message(Message.MessageType.CHAT,
                client.getUsername(),
                REMATCH_REQUEST,
                opponentName));

        statusLabel.setText("Rematch request sent. Returning to lobby...");

        new Thread(() -> {
            try {
                Thread.sleep(1500);
                Platform.runLater(this::returnToLobbyAndChallenge);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void returnToLobbyAndChallenge() {
        System.out.println("Returning to lobby and will challenge " + opponentName);

        GameLobby lobby = new GameLobby(primaryStage, client);
        primaryStage.getScene().setRoot(lobby);
        primaryStage.setTitle("Connect Four - " + client.getUsername());

        new Thread(() -> {
            try {
                Thread.sleep(1000);

                Platform.runLater(() -> {

                    System.out.println("Auto-challenging " + opponentName + " for rematch");
                    client.challengePlayer(opponentName);
                });
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }
    
    private void returnToLobby() {
        System.out.println("Returning to lobby");

        GameLobby lobby = new GameLobby(primaryStage, client);
        primaryStage.getScene().setRoot(lobby);
        primaryStage.setTitle("Connect Four - " + client.getUsername());
    }
}
