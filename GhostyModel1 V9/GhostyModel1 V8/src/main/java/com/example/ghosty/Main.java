package com.example.ghosty;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import java.io.IOException;


public class Main extends Application {
	private final String serverIp = "localhost";
    
    private static boolean isServerRunning = false;
    
    @Override
    public void start(Stage stage) throws Exception {
        ensureServerRunning();
        
        FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("login.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 650, 550);
        stage.setTitle("GHOSTY");
        stage.setResizable(false);
        stage.setScene(scene);
        stage.getIcons().add(new Image(getClass().getResourceAsStream("/images/ghosty.png")));
        stage.show();
        stage.setOnCloseRequest(e -> {
            Platform.exit();     
            System.exit(0);      
        });
    
    }
    
    
    /**
     * Çalışan bir server olduğundan emin olmak için kullandığım method. eğer halihazırda çalışan bir server varsa ona bağlan, yoksa yeni bir server oluştur.
     */
    private void ensureServerRunning() {
        if (!isServerRunning) {
            // Eğer server zaten çalışıyorsa ona bağlan
            try (java.net.Socket socket = new java.net.Socket(serverIp, 9990)) {
                System.out.println("Connected to existing server");
            } catch (IOException e) {
                // çalışan server yoksa yeni server oluştur.
                System.out.println("Starting new server instance");
                new Thread(() -> {
                    Server.main(new String[0]);
                }).start();
                
                // Servera calışması için zaman tanı
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
        Platform.exit();
    }
    

    public static void main(String[] args) {
        launch();
    }
}