package com.project5;

public class ClientService {
    private static Client currentClient;
    private static InteractController currentController;

    // Setters and Getters
    public static void setCurrentClient(Client client) {
        currentClient = client;
    }

    // Get the current client
    public static Client getCurrentClient() {
        return currentClient;
    }

    // Set the current controller
    public static synchronized void setCurrentController(InteractController controller) {
            currentController = controller;
    }

    // Get the current controller
    public static InteractController getCurrentController() {
        return currentController;
    }

    // Append a message to the current controller
    public static void safelyAppendMessage(String message) {
        InteractController controller = getCurrentController();
    }
}
