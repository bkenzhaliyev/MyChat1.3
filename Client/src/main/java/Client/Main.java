package Client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception{
        Parent root = FXMLLoader.load(getClass().getResource("/ChatScreen.fxml"));
        primaryStage.setTitle("SAP чат");
        primaryStage.setScene(new Scene(root, 500, 400));
        primaryStage.getIcons().add(new Image("/image/icon_sap.png"));
        primaryStage.show();
    }


    public static void main(String[] args) {
        launch(args);
    }
}
