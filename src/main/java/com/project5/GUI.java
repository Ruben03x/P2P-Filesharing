package com.project5;

import java.io.IOException;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class GUI extends Application {

    /**
     * Start the GUI
     */
    @Override
    public void start(Stage primaryStage) {
        try {
            // Load the login screen
            Parent root = FXMLLoader.load(getClass().getResource("/com/project5/GUI_Login.fxml"));
            primaryStage.setScene(new Scene(root));
            primaryStage.setTitle("Login Screen");
            primaryStage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Main method
     */
    public static void main(String[] args) {
        launch(args);
    }
}
