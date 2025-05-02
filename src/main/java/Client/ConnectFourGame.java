package Client;

import Server.Message;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.util.Objects;

public class ConnectFourGame extends BorderPane {
    private static final int ROWS = 6;
    private static final int COLS = 7;
    private static final int CELL_SIZE = 80;
    private static final int CIRCLE_RADIUS = 30;

    private final int[][] board = new int[ROWS][COLS];
    private boolean isPlayerTurn;
    private boolean gameOver = false;

    private final Circle[][] circles = new Circle[ROWS][COLS];
    private final Button[] dropButtons = new Button[COLS];
    private Label statusLabel;

    private final Client client;
    private final String opponentName;
    private final Stage primaryStage;

    private final Color playerColor = Color.RED;
    private final Color opponentColor = Color.YELLOW;

    private TextArea chatArea;
    private TextField messageField;

    private final long gameStartTime;
    private int turnCount = 0;
    
    public ConnectFourGame(Stage primaryStage, Client client, String opponentName, boolean playerStarts) {
        this.primaryStage = primaryStage;
        this.client = client;
        this.opponentName = opponentName;
        this.isPlayerTurn = playerStarts;
        this.gameStartTime = System.currentTimeMillis();

        setupUI();
        setupMessageHandling();

        client.sendMessage(new Message(Message.MessageType.GAME_STARTED,
                client.getUsername(),

                playerStarts ? "I_START" : "YOU_START",
                opponentName));

        updateStatus();

        this.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/styles/game.css")).toExternalForm());

        primaryStage.setWidth(900);
        primaryStage.setHeight(650);
        primaryStage.setResizable(false);
    }

    private void setupUI() {
        this.setPadding(new Insets(20));

        HBox header = createHeader();
        header.getStyleClass().add("header");
        this.setTop(header);

        VBox boardContainer = createGameBoard();
        boardContainer.getStyleClass().add("game-board");
        this.setCenter(boardContainer);

        VBox chatPanel = createChatPanel();
        chatPanel.getStyleClass().add("chat-panel");
        this.setRight(chatPanel);

        statusLabel = new Label();
        statusLabel.getStyleClass().add("status-label");
        statusLabel.setPadding(new Insets(10, 0, 0, 0));
        this.setBottom(statusLabel);
    }
    
    private HBox createHeader() {
        HBox header = new HBox(20);
        header.setPadding(new Insets(0, 0, 20, 0));
        header.setAlignment(Pos.CENTER_LEFT);


        ImageView logoView = null;
        try {

            Image logo = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/images/connect4logo.png")));
            logoView = new ImageView(logo);
            logoView.setFitWidth(150);
            logoView.setFitHeight(150);
            logoView.setPreserveRatio(true);
        } catch (Exception e) {

            Label placeholderLogo = new Label("Connect Four");
            placeholderLogo.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: white;");
            header.getChildren().add(placeholderLogo);
        }

        Label vsLabel = new Label("You vs " + opponentName);
        vsLabel.getStyleClass().add("vs-label");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button exitButton = new Button("Exit Game");
        exitButton.setOnAction(e -> exitGame());

        if (logoView != null) {
            header.getChildren().addAll(logoView, vsLabel, spacer, exitButton);
        } else {
            header.getChildren().addAll(vsLabel, spacer, exitButton);
        }

        return header;
    }

    
    private VBox createGameBoard() {
        VBox boardContainer = new VBox(10);
        boardContainer.setAlignment(Pos.CENTER);

        HBox buttonRow = new HBox(5);
        buttonRow.setAlignment(Pos.CENTER);

        for (int col = 0; col < COLS; col++) {
            final int column = col;
            dropButtons[col] = new Button("↓");
            dropButtons[col].getStyleClass().add("drop-button");
            dropButtons[col].setPrefWidth(CELL_SIZE);
            dropButtons[col].setOnAction(e -> makeMove(column));
            buttonRow.getChildren().add(dropButtons[col]);
        }

        GridPane boardGrid = new GridPane();
        boardGrid.setAlignment(Pos.CENTER);
        boardGrid.setHgap(5);
        boardGrid.setVgap(5);

        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                StackPane cell = new StackPane();
                cell.setPrefSize(CELL_SIZE, CELL_SIZE);
                cell.getStyleClass().add("board-cell");

                Circle circle = new Circle(CIRCLE_RADIUS);
                circle.setFill(Color.WHITE);
                circle.setStroke(Color.BLACK);
                circle.setStrokeWidth(2);

                circles[row][col] = circle;
                cell.getChildren().add(circle);

                boardGrid.add(cell, col, row);
            }
        }

        boardContainer.getChildren().addAll(buttonRow, boardGrid);
        return boardContainer;
    }

    
    private VBox createChatPanel() {
        VBox chatPanel = new VBox(10);
        chatPanel.setPadding(new Insets(0, 0, 0, 20));
        chatPanel.setPrefWidth(250);

        Label chatLabel = new Label("Game Chat");
        chatLabel.getStyleClass().add("chat-title");

        chatArea = new TextArea();
        chatArea.setEditable(false);
        chatArea.setWrapText(true);
        chatArea.setPrefHeight(400);

        chatArea.setText("Game chat with " + opponentName + "\n" +
                "Type messages below to communicate with your opponent.\n\n");

        HBox messageBox = new HBox(5);
        messageField = new TextField();
        messageField.setPromptText("Type a message...");
        messageField.setOnAction(e -> sendChatMessage());
        HBox.setHgrow(messageField, Priority.ALWAYS);

        Button sendButton = new Button("Send");
        sendButton.setOnAction(e -> sendChatMessage());

        messageBox.getChildren().addAll(messageField, sendButton);

        chatPanel.getChildren().addAll(chatLabel, chatArea, messageBox);
        return chatPanel;
    }


    
    private void setupMessageHandling() {
        client.setMessageHandler(this::handleMessage);
    }

    
    private void handleMessage(Message message) {
        Platform.runLater(() -> {
            switch (message.getType()) {
                case CHAT:
                    if (message.getSender().equals(opponentName) &&
                            (message.getData() == null || message.getData().equals(client.getUsername()))) {
                        addChatMessage(message.getSender() + ": " + message.getContent());
                    }
                    break;

                case GAME_MOVE:
                    if (message.getSender().equals(opponentName)) {
                        handleOpponentMove(message.getContent());
                    }
                    break;

                case GAME_STARTED:
                    if (message.getSender().equals(opponentName)) {

                        if ("I_START".equals(message.getContent())) {
                            isPlayerTurn = false;

                        } else if ("YOU_START".equals(message.getContent())) {
                            isPlayerTurn = true;
                        }
                        updateStatus();
                    }
                    break;

                case GAME_OVER:
                    handleGameOver(message.getContent());
                    break;

                default:
                    System.out.println("Unhandled message type in game: " + message.getType());
            }
        });
    }

    private void sendChatMessage() {
        String message = messageField.getText().trim();
        if (!message.isEmpty()) {
            Message chatMessage = new Message(
                    Message.MessageType.CHAT,
                    client.getUsername(),
                    message,
                    opponentName
            );

            client.sendMessage(chatMessage);

            addChatMessage(client.getUsername() + ": " + message);

            messageField.clear();
        }
    }

    private void addChatMessage(String message) {
        chatArea.appendText(message + "\n");
        chatArea.setScrollTop(Double.MAX_VALUE);
    }

    
    private void makeMove(int column) {
        if (gameOver || !isPlayerTurn) {
            return;
        }

        int row = getLowestEmptyRow(column);
        if (row == -1) {
            return;
        }

        turnCount++;

        board[row][column] = 1;
        updateBoardUI();

        String moveData = column + "," + row;
        client.sendMessage(new Message(Message.MessageType.GAME_MOVE,
                client.getUsername(),
                moveData,
                opponentName));


        if (checkWin(row, column, 1)) {
            gameOver = true;
            client.sendMessage(new Message(Message.MessageType.GAME_OVER,
                    client.getUsername(),
                    client.getUsername() + " won against " + opponentName,
                    opponentName));


            long gameDuration = System.currentTimeMillis() - gameStartTime;

            GameResultScreen resultScreen = new GameResultScreen(primaryStage, client, opponentName,
                    "You won!", turnCount, gameDuration);
            primaryStage.getScene().setRoot(resultScreen);
            primaryStage.setTitle("Connect Four - Game Result");
            return;
        }

        if (isBoardFull()) {
            gameOver = true;
            client.sendMessage(new Message(Message.MessageType.GAME_OVER,
                    client.getUsername(),
                    "Game ended in a draw between " + client.getUsername() + " and " + opponentName,
                    opponentName));


            long gameDuration = System.currentTimeMillis() - gameStartTime;

            GameResultScreen resultScreen = new GameResultScreen(primaryStage, client, opponentName,
                    "Game ended in a draw!", turnCount, gameDuration);
            primaryStage.getScene().setRoot(resultScreen);
            primaryStage.setTitle("Connect Four - Game Result...");
            return;
        }

        isPlayerTurn = false;
        updateStatus();
    }
    
    private void handleOpponentMove(String moveData) {
        if (gameOver || isPlayerTurn) {
            return;
        }

        String[] parts = moveData.split(",");
        int column = Integer.parseInt(parts[0]);

        if (parts.length > 1) {
            int row = Integer.parseInt(parts[1]);
            board[row][column] = 2;
        } else {
            int row = getLowestEmptyRow(column);
            if (row != -1) {
                board[row][column] = 2;
            }
        }

        // why
        updateBoardUI();

        turnCount++;

        int tokenRow = -1;
        for (int r = 0; r < ROWS; r++) {
            if (board[r][column] == 2) {
                tokenRow = r;
                break;
            }
        }

        if (tokenRow != -1 && checkWin(tokenRow, column, 2)) {
            gameOver = true;


            long gameDuration = System.currentTimeMillis() - gameStartTime;

            GameResultScreen resultScreen = new GameResultScreen(primaryStage, client, opponentName,
                    opponentName + " won!", turnCount, gameDuration);
            primaryStage.getScene().setRoot(resultScreen);
            primaryStage.setTitle("Connect Four - Game Result");
            return;
        }

        if (isBoardFull()) {
            gameOver = true;


            long gameDuration = System.currentTimeMillis() - gameStartTime;

            GameResultScreen resultScreen = new GameResultScreen(primaryStage, client, opponentName,
                    "Game ended in a draw!", turnCount, gameDuration);
            primaryStage.getScene().setRoot(resultScreen);
            primaryStage.setTitle("Connect Four - Game Result");
            return;
        }

        isPlayerTurn = true;
        updateStatus();
    }
    
    private int getLowestEmptyRow(int column) {
        for (int row = ROWS - 1; row >= 0; row--) {
            if (board[row][column] == 0) {
                return row;
            }
        }
        return -1;
    }

    private void updateBoardUI() {
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                if (board[row][col] == 0) {
                    circles[row][col].setFill(Color.WHITE);
                } else if (board[row][col] == 1) {
                    circles[row][col].setFill(playerColor);
                } else {
                    circles[row][col].setFill(opponentColor);
                }
            }
        }
    }
    
    private boolean checkWin(int row, int col, int player) {
        if (checkDirection(row, col, 0, 1, player) + checkDirection(row, col, 0, -1, player) - 1 >= 4) {
            return true;
        }

        if (checkDirection(row, col, 1, 0, player) + checkDirection(row, col, -1, 0, player) - 1 >= 4) {
            return true;
        }

        if (checkDirection(row, col, -1, -1, player) + checkDirection(row, col, 1, 1, player) - 1 >= 4) {
            return true;
        }

        return checkDirection(row, col, -1, 1, player) + checkDirection(row, col, 1, -1, player) - 1 >= 4;
    }
    
    private int checkDirection(int row, int col, int rowDir, int colDir, int player) {
        int count = 0;
        int r = row;
        int c = col;

        while (r >= 0 && r < ROWS && c >= 0 && c < COLS && board[r][c] == player) {
            count++;
            r += rowDir;
            c += colDir;
        }

        return count;
    }
    
    private boolean isBoardFull() {
        for (int col = 0; col < COLS; col++) {
            if (board[0][col] == 0) {
                return false;
            }
        }
        return true;
    }

    private void updateStatus() {
        if (gameOver) {
            return;
        }


        long currentTime = System.currentTimeMillis();
        long durationInSeconds = (currentTime - gameStartTime) / 1000;
        long minutes = durationInSeconds / 60;
        long seconds = durationInSeconds % 60;
        String durationText = String.format("Game time: %02d:%02d", minutes, seconds);


        String turnText = "Turns: " + (turnCount / 2 + 1);

        if (isPlayerTurn) {
            statusLabel.setText("Your turn\n" + durationText + "\n" + turnText);
            statusLabel.setTextFill(playerColor);

            for (int col = 0; col < COLS; col++) {
                dropButtons[col].setDisable(getLowestEmptyRow(col) == -1);
            }
        } else {
            statusLabel.setText(opponentName + "'s turn\n" + durationText + "\n" + turnText);
            statusLabel.setTextFill(opponentColor);

            for (Button button : dropButtons) {
                button.setDisable(true);
            }
        }
    }

    private void showGameResult(String result) {
        gameOver = true;
        statusLabel.setText(result);
        statusLabel.setTextFill(Color.BLACK);

        for (Button button : dropButtons) {
            button.setDisable(true);
        }

        long gameDuration = System.currentTimeMillis() - gameStartTime;

        GameResultScreen resultScreen = new GameResultScreen(primaryStage, client, opponentName,
                result, turnCount, gameDuration);
        primaryStage.getScene().setRoot(resultScreen);
        primaryStage.setTitle("Connect Four - Game Result");
    }
    
    private void handleGameOver(String result) {
        gameOver = true;

        if (result.contains("has left the game")) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Opponent Left");
            alert.setHeaderText(null);
            alert.setContentText(result);

            alert.setOnHidden(e -> {
                client.sendMessage(new Message(Message.MessageType.GAME_OVER,
                        client.getUsername(),
                        "Game ended: Both players left",
                        "Server"));

                GameLobby lobby = new GameLobby(primaryStage, client);
                primaryStage.getScene().setRoot(lobby);
                primaryStage.setTitle("Connect Four - " + client.getUsername());
            });

            alert.showAndWait();
        } else {
            showGameResult(result);
        }
    }
    
    private void exitGame() {
        Alert confirmExit = new Alert(Alert.AlertType.CONFIRMATION);
        confirmExit.setTitle("Exit Game");
        confirmExit.setHeaderText("Are you sure you want to exit the game?");
        confirmExit.setContentText("Your opponent will be notified if you leave.");

        ButtonType buttonTypeYes = new ButtonType("Yes, Exit Game");
        ButtonType buttonTypeNo = new ButtonType("No, Continue Playing", ButtonBar.ButtonData.CANCEL_CLOSE);
        confirmExit.getButtonTypes().setAll(buttonTypeYes, buttonTypeNo);

        confirmExit.showAndWait().ifPresent(response -> {
            if (response == buttonTypeYes) {

                client.sendMessage(new Message(Message.MessageType.GAME_OVER,
                        client.getUsername(),
                        client.getUsername() + " has left the game",
                        opponentName));


                client.sendMessage(new Message(Message.MessageType.GAME_OVER,
                        client.getUsername(),
                        "Game ended: " + client.getUsername() + " left the game. " + opponentName + " returned to lobby.",
                        "Server"));

                GameLobby lobby = new GameLobby(primaryStage, client);
                primaryStage.getScene().setRoot(lobby);
                primaryStage.setTitle("Connect Four - " + client.getUsername());
            }
        });
    }
}
