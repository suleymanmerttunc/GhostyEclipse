package com.example.ghostymodel;

import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.stage.Stage;


public class LoginController {

    @FXML
    private TextField usernameField;

    @FXML
    private void handleLogin() {
        String isim = usernameField.getText();

        System.out.println("isim: " + isim);

        if (isim.isEmpty()) {
            System.out.println("isim girilmedi.");
            return;
        }

        try {

            Parent root = MainRoomLoader.loadMainRoom(isim); // Burada MainRoomLoader kullanılıyor

            Scene mainRoomScene = new Scene(root);

            Stage stage = (Stage) usernameField.getScene().getWindow();
            stage.setScene(mainRoomScene);
            stage.setTitle("GHOSTY");

            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}

