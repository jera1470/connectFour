import Client.LoginScreen;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class Main extends Application {
    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Connect Four Launcher");

        VBox root = new VBox(20);
        root.setPadding(new Insets(20));
        root.setAlignment(Pos.CENTER);

        Label titleLabel = new Label("Connect Four Game");
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");

        Button clientButton = new Button("Launch Client");
        clientButton.setPrefWidth(200);
        clientButton.setStyle("-fx-font-size: 14px;");
        clientButton.setOnAction(e -> {

            Stage loginStage = new Stage();
            loginStage.setTitle("Connect Four");
            loginStage.setScene(new Scene(new LoginScreen(loginStage), 400, 500));
            loginStage.show();
            primaryStage.close();
        });


        Button adminButton = new Button("Admin Access");
        adminButton.setPrefWidth(200);
        adminButton.setStyle("-fx-font-size: 14px;");
        adminButton.setOnAction(e -> showAdminLogin(primaryStage));

        root.getChildren().addAll(titleLabel, clientButton, adminButton);

        Scene scene = new Scene(root, 300, 250);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void showAdminLogin(Stage mainStage) {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Admin Authentication");
        dialog.setHeaderText("Enter admin password");

        ButtonType loginButtonType = new ButtonType("Login", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(loginButtonType, ButtonType.CANCEL);

        PasswordField password = new PasswordField();
        password.setPromptText("Password");

        VBox content = new VBox(10);
        content.getChildren().add(password);
        content.setPadding(new Insets(20, 10, 10, 10));

        dialog.getDialogPane().setContent(content);

        password.requestFocus();

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == loginButtonType) {
                return password.getText();
            }
            return null;
        });

        dialog.showAndWait().ifPresent(pwd -> {
            if ("admin123".equals(pwd)) {
                try {
                    Application serverUI = (Application) Class.forName("Server.ServerUI").getDeclaredConstructor().newInstance();
                    serverUI.start(new Stage());
                    mainStage.close();
                } catch (Exception ex) {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Error");
                    alert.setHeaderText(null);
                    alert.setContentText("Could not launch server UI");
                    alert.showAndWait();
                }
            } else {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Authentication Failed");
                alert.setHeaderText(null);
                alert.setContentText("Incorrect admin password");
                alert.showAndWait();
            }
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}
