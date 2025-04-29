package com.example.ghosty;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class ChatRoomController {

    @FXML
    private Label roomLabel1;

    @FXML
    private Label firstText, fourthText, secondText, thirdText;

    @FXML
    private Label welcomeLabel;


    //BU METOT İLE MainRoomControllerdan Trend isimlerini çekeceğiz
    public void setTrends(String[] trends) {
        if (trends != null && trends.length >= 4) {
            firstText.setText(trends[0]);
            secondText.setText(trends[1]);
            thirdText.setText(trends[2]);
            fourthText.setText(trends[3]);
        }
    }

    //BU METOT İLE MainRoomControllerdan username çekeceğiz
    public void setUsername(String username) {
        welcomeLabel.setText(username);
    }

    //BU METOT İLE MainRoomControllerdan özel odamızı çekeceğiz
    public void setRoomLabel(String roomLabel) {
        roomLabel1.setText(roomLabel);
    }



    public void initialize() {

    }

}
