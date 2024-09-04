package com.project5;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;

public class InteractController {

    @FXML
    private Button buttonPause;

    @FXML
    public volatile Button buttonDownload;

    @FXML
    private TextField textLogin;

    @FXML
    private TextField textAddress; // The IP address of the server for establishing client connection

    @FXML
    private TextField textPort; // The port of the server for establishing client connection

    @FXML
    private volatile TextField textSearch; // The text field for sending messages

    @FXML
    private volatile ListView<String> fileListView; // The list view for displaying users

    @FXML
    private volatile ProgressBar progressDownload;

    @FXML
    private volatile ProgressBar progressUpload;

    private String username;

    private volatile Stage stage;

    private volatile Client client = null; // The client for this interaction controller

    /**
     * Handles the sign in process.
     * 
     * This method handles the sign in process. It retrieves the username, server
     * address, and server port from the text fields on the UI. It then initializes
     * the client connection to the server and sends the username to the server.
     * 
     * @param event The action event that triggered the method.
     */
    @FXML
    private void handleSignIn(ActionEvent event) {
        try {
            // get username, address and port
            username = textLogin.getText();
            String serverAddress = textAddress.getText();
            int serverPort = Integer.parseInt(textPort.getText());

            // initialise the client connection to the server
            if (client == null) {
                Socket socket = new Socket(serverAddress, serverPort);
                textAddress.setDisable(true);
                textPort.setDisable(true); // disables text fields for address and port while connection set up
                client = new Client(socket, this); // init client with socket containing server info and this
                                                   // interaction controller
                ClientService.setCurrentClient(client); // sets the client service client instance to the newly created
                                                        // client
                client.receiver(); // start client receiving messages from server
            }
            client.sendMessage(username);
        } catch (Exception e) {
            Platform.runLater(() -> showErrorDialog("Server not available of given address and port"));
        }

        if (client != null) {
            while (!client.checkedUsername) {
                try {
                    Thread.sleep(500); // sleep client thread to avoid race conditions on usernames
                } catch (InterruptedException e) {
                    // e.printStackTrace();
                }
            }
            client.checkedUsername = false;
            if (client.usernameOK) { // if client username allowed, start main GUI
                try {
                    // load main GUI
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/project5/GUI_Main.fxml"));
                    loader.setController(this);
                    stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                    stage.setScene(new Scene(loader.load()));
                    stage.setTitle(username); // starts main GUI
                    // set on close request to disconnect client
                    stage.setOnCloseRequest(e -> {
                        if (client != null) {
                            stage.close();
                            client.disconnect();
                        }
                    });
                    stage.show();
                } catch (Exception e) {
                    System.out.println("Error occurred loading Main GUI: " + e.getMessage());
                }
            }
        }

    }

    /**
     * Handle search process.
     * @param event
     */
    @FXML
    public void handleSearch(ActionEvent event) {
        // get search text
        String searchText = textSearch.getText();
        // if search text is not empty and client is not null
        if (!searchText.isEmpty() && client != null) {
            // clear the result list and map
            client.resultList = new ArrayList<>();
            client.resultMap = new HashMap<>();
            // update the file list with the result list
            updateFileList(client.resultList);
            client.sendMessage("##SEARCH," + searchText);
        } else {
            showErrorDialog("Please enter text to search!");
        }
    }

    /**
     * Handles the download process.
     * 
     * This method handles the download process. It retrieves the selected file from
     * the file list view and sends a download request to the server.
     * 
     * @param event The action event that triggered the method.
     */
    @FXML
    void handleDownload(ActionEvent event) {
        // get selected file
        String selectedFile = getSelectedFile();
        // disable download button
        buttonDownload.setDisable(true);
        // if no file selected, show error dialog
        if (selectedFile == null) {
            showErrorDialog("No file has been selected to download");
        } else {
            client.startDownloader(selectedFile);
        }
    }

    /**
     * Handles the pause/resume process.
     * 
     * This method handles the pause/resume process. It pauses or resumes the
     * download process.
     * 
     * @param event The action event that triggered the method.
     */
    @FXML
    void handlePause(ActionEvent event) {
        // if client is not null
        if (client.pauseDownload) {
            buttonPause.setText("Pause");
            client.pauseDownload = false;
        } else {
            // pause download
            buttonPause.setText("Resume");
            client.pauseDownload = true;
        }
    }

    /**
     * Updates the file list view with the specified list of users.
     * 
     * @param users The list of users to be displayed in the file list view.
     */
    public void updateFileList(ArrayList<String> users) {
        // update the file list view with the list of users
        Platform.runLater(() -> fileListView.getItems().setAll(users));
        System.out.println("File list updated");
    }

    /**
     * Appends a message to the message area.
     * 
     * @param message The message to be appended.
     */
    public void updateProgressBarDownload(long bytesRead, long fileSize) {
        // update the progress bar with the specified message
        double progress = (double) bytesRead / fileSize;
        final double clamped = Math.min(progress, 1.0);

        progressDownload.setProgress(clamped);
    }

    public void updateProgressBarUpload(long bytesRead, long fileSize) {
        // update the progress bar with the specified message
        double progress = (double) bytesRead / fileSize;
        final double clamped = Math.min(progress, 1.0);

        progressUpload.setProgress(clamped);
    }

    /**
     * Shows an error dialog with the specified message.
     * 
     * @param message The error message to be displayed.
     */
    public void showErrorDialog(String message) {
        // show error dialog with the specified message
        Platform.runLater(() -> {
            // create new error alert
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait(); // shows error and waits for user response
        });
    }

    /**
     * Shows an information dialog with the specified message.
     * 
     * @param message The information message to be displayed.
     */
    public void showDialog(String message) {
        // show information dialog with the specified message
        Platform.runLater(() -> {
            // create new information alert
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Information");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    /**
     * Retrieves the selected user from the user list view.
     * 
     * @return The username of the selected user.
     */
    public String getSelectedFile() {
        // return the selected user from the user list view
        return fileListView.getSelectionModel().getSelectedItem();
    }

    /**
     * Initializes the interaction controller.
     */
    public void initialize() {
        // initialize the interaction controller
        System.out.println("InteractController initialized.");
    }
}
