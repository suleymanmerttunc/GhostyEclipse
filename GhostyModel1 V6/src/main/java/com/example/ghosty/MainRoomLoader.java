package com.example.ghosty;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

public class MainRoomLoader {

    public static Parent loadMainRoom(String username) throws Exception {
        FXMLLoader loader = new FXMLLoader(MainRoomLoader.class.getResource("MainRoom.fxml"));
        Parent root = loader.load();
        MainGUI controller = loader.getController();
        controller.setUsername(username);

        return root;
    }
}
