package com.example.ghostymodel;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

public class MainRoomLoader {

    //LOGİNDEN SONRAKİ GELEN EKRANIN OLUŞTUĞU SINIF

    public static Parent loadMainRoom(String username) throws Exception {
        FXMLLoader loader = new FXMLLoader(MainRoomLoader.class.getResource("MainRoom.fxml"));
        Parent root = loader.load();
        MainRoomController controller = loader.getController();
        controller.setUsername(username);

        return root;
    }
}
