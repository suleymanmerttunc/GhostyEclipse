package com.example.ghostymodel;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Optional;

public class ChatRoomController {


    private final ObservableList<Room> dynamicRooms = FXCollections.observableArrayList();

    @FXML
    private ListView<Room> userCreatedRoomList;


    @FXML
    private Label firstText, fourthText, secondText, thirdText;

    @FXML
    private Label lobbyText;

    @FXML
    private Label welcomeLabel;

    public void initialize() {


    }

    public void setTrends(String[] trends) {
        if (trends != null && trends.length >= 4) {
            firstText.setText(trends[0]);
            secondText.setText(trends[1]);
            thirdText.setText(trends[2]);
            fourthText.setText(trends[3]);
        }
    }

    public void setUsername(String username) {
        welcomeLabel.setText(username);
    }

    public void setLobbyText(String trendName) {
        lobbyText.setText(trendName);
    }

    @FXML
    void lobbyClicked(MouseEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(Main.class.getResource("chatRoom.fxml"));
            Parent root = loader.load();

            // Controller'a erişmek içim
            ChatRoomController chatRoomController = loader.getController();

            // Trend verilerini gönder
            String[] trends = {
                    firstText.getText(),
                    secondText.getText(),
                    thirdText.getText(),
                    fourthText.getText()
            };
            chatRoomController.setTrends(trends);

            Label clickedLabel = (Label) event.getSource();
            chatRoomController.setLobbyText(clickedLabel.getText());

            // Kullanıcı ismini de göndermek için
            chatRoomController.setUsername(welcomeLabel.getText());

            Stage stage = new Stage();
            stage.setTitle("Ghosty");
            stage.setScene(new Scene(root, 1000, 700));
            stage.show();

            ((Stage)(((javafx.scene.Node) event.getSource()).getScene().getWindow())).close();

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @FXML
    private void createNewRoom() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Yeni Oda");
        dialog.setHeaderText(null);
        dialog.setContentText("Oda ismi gir:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(roomName -> {
            if (!roomName.trim().isEmpty()) {
                dynamicRooms.add(new Room(roomName, false));
            }
        });
    }

    @FXML
    private void deleteSelectedRoom() {
        Room selected = (Room) userCreatedRoomList.getSelectionModel().getSelectedItem();
        if (selected != null) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Oda Sil");
            alert.setHeaderText(null);
            alert.setContentText("'" + selected.getName() + "' odasını silmek istiyor musun?");

            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                dynamicRooms.remove(selected);
            }
        }
    }
    public void setRooms(ObservableList<Room> rooms) {
        dynamicRooms.setAll(rooms); // Mevcut listeyi yeni odalarla güncelle
        userCreatedRoomList.setItems(dynamicRooms);
    }



}

