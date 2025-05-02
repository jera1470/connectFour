package Server;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class ServerUI extends Application {
    private Server server;
    private TextArea logArea;
    private Button startButton;
    private Button stopButton;
    private TextField portField;
    private ListView<String> clientListView;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Connect Four Server");

        BorderPane mainLayout = new BorderPane();
        mainLayout.setPadding(new Insets(10));

        VBox controlPanel = createControlPanel();
        mainLayout.setTop(controlPanel);

        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setWrapText(true);
        logArea.setPrefHeight(300);
        mainLayout.setCenter(logArea);

        VBox clientPanel = createClientPanel();
        mainLayout.setRight(clientPanel);

        Scene scene = new Scene(mainLayout, 800, 500);
        primaryStage.setScene(scene);
        primaryStage.show();

        primaryStage.setOnCloseRequest(e -> {
            if (server != null) {
                server.stop();
            }
            Platform.exit();
        });
    }

    private VBox createControlPanel() {
        VBox panel = new VBox(10);
        panel.setPadding(new Insets(10));

        Label titleLabel = new Label("Connect Four Server Control");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        HBox portBox = new HBox(10);
        Label portLabel = new Label("Port:");
        portField = new TextField("8080");
        portField.setPrefWidth(80);
        portBox.getChildren().addAll(portLabel, portField);

        HBox buttonBox = new HBox(10);
        startButton = new Button("Start Server");
        stopButton = new Button("Stop Server");
        stopButton.setDisable(true);

        startButton.setOnAction(e -> startServer());
        stopButton.setOnAction(e -> stopServer());

        buttonBox.getChildren().addAll(startButton, stopButton);

        panel.getChildren().addAll(titleLabel, portBox, buttonBox);
        return panel;
    }

    private VBox createClientPanel() {
        VBox panel = new VBox(10);
        panel.setPadding(new Insets(10));
        panel.setPrefWidth(200);

        Label clientLabel = new Label("Connected Clients");
        clientLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

        clientListView = new ListView<>();
        clientListView.setPrefHeight(200);

        panel.getChildren().addAll(clientLabel, clientListView);
        return panel;
    }

    private void startServer() {
        try {
            int port = Integer.parseInt(portField.getText());
            server = new Server(port, this::logMessage);
            server.setClientListUpdater(this::updateClientList);
            server.start();

            startButton.setDisable(true);
            stopButton.setDisable(false);
            portField.setDisable(true);

            logMessage("Server started on port " + port);
        } catch (NumberFormatException e) {
            logMessage("Invalid port number");
        } catch (Exception e) {
            logMessage("Error starting server: " + e.getMessage());
        }
    }

    private void stopServer() {
        if (server != null) {
            server.stop();
            server = null;

            startButton.setDisable(false);
            stopButton.setDisable(true);
            portField.setDisable(false);


            Platform.runLater(() -> clientListView.getItems().clear());

            logMessage("Server stopped");
        }
    }

    private void logMessage(String message) {
        Platform.runLater(() -> {
            logArea.appendText(message + "\n");

            logArea.setScrollTop(Double.MAX_VALUE);
        });
    }

    private void updateClientList(String[] clients) {
        Platform.runLater(() -> {
            clientListView.getItems().clear();
            for (String client : clients) {
                clientListView.getItems().add(client);
            }
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}
