package com.example.ghosty;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import java.io.IOException;

/**
 * Main application launcher for the Ghosty chat application
 */
public class Main extends Application {
	private final String serverIp = "localhost";
    
    private static boolean isServerRunning = false;
    
    @Override
    public void start(Stage stage) throws Exception {
        // Check if the server is already running, if not, start it
        ensureServerRunning();
        
        FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("login.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 650, 550);
        stage.setTitle("GHOSTY");
        stage.setResizable(false);
        stage.setScene(scene);
        stage.getIcons().add(new Image(getClass().getResourceAsStream("/images/ghosty.png")));
        stage.show();
        stage.setOnCloseRequest(e -> {
            Platform.exit();     // Close JavaFX threads
            System.exit(0);      // Kill remaining non-daemon threads
        });
    
    }
    
    
    /**
     * Ensure the server is running
     */
    private void ensureServerRunning() {
        if (!isServerRunning) {
            // Try to connect to the registration port to check if server is running
            try (java.net.Socket socket = new java.net.Socket(serverIp, 9990)) {
                // Server is already running
                System.out.println("Connected to existing server");
            } catch (IOException e) {
                // Server is not running, start it
                System.out.println("Starting new server instance");
                new Thread(() -> {
                    Server.main(new String[0]);
                }).start();
                
                // Give the server time to start
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
            
            isServerRunning = true;
        }
    }
    

    @Override
    public void stop() {
        // Ensure clean shutdown
        Platform.exit();
    }
    

    public static void main(String[] args) {
        launch();
    }
}