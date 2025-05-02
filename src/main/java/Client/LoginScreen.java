package Client;

import Server.Message;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import javafx.scene.control.PasswordField;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.util.Objects;

public class LoginScreen extends VBox {
    private final Stage primaryStage;
    private TextField usernameField;
    private TextField hostField;
    private TextField portField;
    private Label statusLabel;
    private PasswordField passwordField;

    public LoginScreen(Stage primaryStage) {
        this.primaryStage = primaryStage;
        setupUI();

        primaryStage.setWidth(500);
        primaryStage.setHeight(500);
        primaryStage.setResizable(false);

        this.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/styles/login.css")).toExternalForm());
    }

    private void setupUI() {
        this.setPadding(new Insets(20));
        this.setSpacing(15);
        this.setAlignment(Pos.CENTER);

        TabPane authTabPane = new TabPane();

        Tab loginTab = new Tab("Login");
        loginTab.setClosable(false);
        VBox loginBox = new VBox(10);
        loginBox.setPadding(new Insets(20));
        loginBox.setAlignment(Pos.CENTER);

        GridPane loginGrid = new GridPane();
        loginGrid.setHgap(10);
        loginGrid.setVgap(10);
        loginGrid.setAlignment(Pos.CENTER);

        Label usernameLabel = new Label("Username:");
        usernameField = new TextField();
        usernameField.setPromptText("Enter your username");

        Label passwordLabel = new Label("Password:");
        passwordField = new PasswordField();
        passwordField.setPromptText("Enter your password");

        Label hostLabel = new Label("Server Host:");
        hostField = new TextField("localhost");

        Label portLabel = new Label("Server Port:");
        portField = new TextField("8080");

        loginGrid.add(usernameLabel, 0, 0);
        loginGrid.add(usernameField, 1, 0);
        loginGrid.add(passwordLabel, 0, 1);
        loginGrid.add(passwordField, 1, 1);
        loginGrid.add(hostLabel, 0, 2);
        loginGrid.add(hostField, 1, 2);
        loginGrid.add(portLabel, 0, 3);
        loginGrid.add(portField, 1, 3);

        Button loginButton = new Button("Login");
        loginButton.setDefaultButton(true);
        loginButton.setOnAction(e -> login());

        loginBox.getChildren().addAll(loginGrid, loginButton);
        loginTab.setContent(loginBox);

        Tab registerTab = new Tab("Register");
        registerTab.setClosable(false);

        VBox registerBox = new VBox(10);
        registerBox.setPadding(new Insets(20));
        registerBox.setAlignment(Pos.CENTER);

        GridPane registerGrid = new GridPane();
        registerGrid.setHgap(10);
        registerGrid.setVgap(10);
        registerGrid.setAlignment(Pos.CENTER);

        Label newUsernameLabel = new Label("Username:");
        TextField newUsernameField = new TextField();
        newUsernameField.setPromptText("Choose a username");

        Label newPasswordLabel = new Label("Password:");
        PasswordField newPasswordField = new PasswordField();
        newPasswordField.setPromptText("Choose a password");

        Label confirmPasswordLabel = new Label("Confirm Password:");
        PasswordField confirmPasswordField = new PasswordField();
        confirmPasswordField.setPromptText("Confirm your password");

        registerGrid.add(newUsernameLabel, 0, 0);
        registerGrid.add(newUsernameField, 1, 0);
        registerGrid.add(newPasswordLabel, 0, 1);
        registerGrid.add(newPasswordField, 1, 1);
        registerGrid.add(confirmPasswordLabel, 0, 2);
        registerGrid.add(confirmPasswordField, 1, 2);

        Button registerButton = new Button("Register");
        registerButton.setOnAction(e -> register(
                newUsernameField.getText(),
                newPasswordField.getText(),
                confirmPasswordField.getText()
        ));

        registerBox.getChildren().addAll(registerGrid, registerButton);
        registerTab.setContent(registerBox);

        authTabPane.getTabs().addAll(loginTab, registerTab);

        statusLabel = new Label();
        statusLabel.getStyleClass().add("status-label");

        ImageView logoView = null;
        try {
            Image logo = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/images/connect4logo.png")));
            logoView = new ImageView(logo);
            logoView.setFitWidth(250);
            logoView.setFitHeight(250);
            logoView.setPreserveRatio(true);
        } catch (Exception e) {
            Label placeholderLogo = new Label("Connect Four");
            placeholderLogo.setStyle("-fx-font-size: 40px; -fx-font-weight: bold; -fx-text-fill: #3E2723;");
            this.getChildren().add(placeholderLogo);
        }

        if (logoView != null) {

            this.getChildren().addAll(logoView, authTabPane, statusLabel);
        } else {

            this.getChildren().addAll(authTabPane, statusLabel);
        }
    }

    private void login() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        String host = hostField.getText().trim();
        String portText = portField.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            statusLabel.setText("Please enter both username and password");
            return;
        }

        if (host.isEmpty()) {
            statusLabel.setText("Please enter a server host");
            return;
        }

        if (UserManager.isUsernameTaken(username)) {
            statusLabel.setText("This account is already logged in");
            return;
        }

        if (!UserManager.isRegisteredUser(username)) {
            statusLabel.setText("Username not found. Please register first.");
            return;
        }

        if (!UserManager.validateCredentials(username, password)) {
            statusLabel.setText("Invalid username or password");
            return;
        }

        int port;
        try {
            port = Integer.parseInt(portText);
        } catch (NumberFormatException e) {
            statusLabel.setText("Invalid port number");
            return;
        }

        Client client = new Client(host, port, username,
                this::handleMessage,
                status -> statusLabel.setText(status));

        if (client.connect(password)) {

            UserManager.addUser(username);

            GameLobby lobby = new GameLobby(primaryStage, client);
            primaryStage.getScene().setRoot(lobby);
            primaryStage.setTitle("Connect Four - " + username);
        }
    }

    private void register(String username, String password, String confirmPassword) {
        if (username.isEmpty() || password.isEmpty()) {
            statusLabel.setText("Please enter both username and password");
            return;
        }

        if (!password.equals(confirmPassword)) {
            statusLabel.setText("Passwords do not match");
            return;
        }

        if (password.length() < 6) {
            statusLabel.setText("Password must be at least 6 characters");
            return;
        }

        if (UserManager.isRegisteredUser(username)) {
            statusLabel.setText("Username already exists");
            return;
        }

        if (UserManager.registerUser(username, password)) {
            statusLabel.setText("Registration successful! You can now log in.");
        } else {
            statusLabel.setText("Registration failed. Please try again.");
        }
    }

    
    private void handleMessage(Message message) {
        if (message.getType() == Message.MessageType.AUTHENTICATION) {
            if (message.getContent().startsWith("FAILED")) {

                UserManager.removeActiveUser(usernameField.getText().trim());

                if (message.getContent().contains("already logged in") ||
                        (message.getData() != null && message.getData().equals("USERNAME_ACTIVE"))) {
                    statusLabel.setText("Error: Username is already logged in on the server");
                } else {
                    statusLabel.setText("Authentication failed: " + message.getContent());
                }
            }
        }
        System.out.println("Received message: " + message);
    }

}
