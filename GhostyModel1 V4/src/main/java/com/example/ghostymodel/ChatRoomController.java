package com.example.ghostymodel;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;

public class ChatRoomController {

    @FXML
    private Label roomLabel1, roomLabel2, roomLabel3, roomLabel4, roomLabel5;
    @FXML
    private Button goBackLobby;
    @FXML
    private Button sendMessage;
    @FXML
    private TextField MessageField;
    @FXML
    private Button sendButton;
    @FXML
    private VBox messageBox;
    @FXML
    private ImageView avatarImage;
    @FXML
    private Label firstText, fourthText, secondText, thirdText;

    @FXML
    private Label lobbyText;

    @FXML
    private Label welcomeLabel;


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

    public void initialize() {
        avatarImage.imageProperty().bind(UserData.getInstance().avatarImageProperty());
        AvatarUtil.setupAvatarClickHandler(avatarImage);

        //emriş şimdilik böyle yazdım kontrol edebilmek icin sen kendi mesaj şeyini yaz muckk
        sendMessage.setOnAction(e -> {
            String message = MessageField.getText();
            if (!message.isEmpty()) {
                Label messageLabel = new Label(welcomeLabel.getText() + ": " + message);
                messageBox.getChildren().add(messageLabel);
                MessageField.clear();
            }
        });

        sendButton.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Resim Seç");
            fileChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Resimler", "*.png", "*.jpg", "*.jpeg")
            );
            File selectedFile = fileChooser.showOpenDialog(sendButton.getScene().getWindow());
            if (selectedFile != null) {
                Image image = new Image(selectedFile.toURI().toString());
                ImageView imageView = new ImageView(image);
                imageView.setFitWidth(200);
                imageView.setPreserveRatio(true);

                Label userLabel = new Label(welcomeLabel.getText() + " bir resim gönderdi:");
                messageBox.getChildren().addAll(userLabel, imageView);
            }
        });

    }
    @FXML
    private void handleAvatarClick(MouseEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Avatar Seç");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Resimler", "*.png", "*.jpg", "*.jpeg")
        );
        File selectedFile = fileChooser.showOpenDialog(((ImageView) event.getSource()).getScene().getWindow());
        if (selectedFile != null) {
            Image newAvatar = new Image(selectedFile.toURI().toString());
            UserData.getInstance().setAvatarImage(newAvatar);
        }
    }

    @FXML
    private void handleGoBackLobby() {
        try {
            FXMLLoader loader = new FXMLLoader(Main.class.getResource("mainRoom.fxml"));
            Parent root = loader.load();

            MainRoomController mainRoomController = loader.getController();
            mainRoomController.setUsername(welcomeLabel.getText());

            Stage stage = new Stage();
            stage.setTitle("Ghosty");
            stage.setScene(new Scene(root, 1000, 700));
            stage.show();

            Stage currentStage = (Stage) goBackLobby.getScene().getWindow();
            currentStage.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    // BU SINIFTA ODA OLUŞTURMAYA ÇALIŞINCA UYARI VERİYOR VE ANA LOBİDEN OLUŞTUR DİYE HATA VERİYOR
    @FXML
    private void handleToggleClick() {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Uyarı");
        alert.setHeaderText(null);
        alert.setContentText("Sadece Giriş Ekranında Oda Oluşturabilirsiniz!");
        alert.showAndWait();
    }


}