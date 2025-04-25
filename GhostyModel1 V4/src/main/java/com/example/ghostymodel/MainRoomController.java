package com.example.ghostymodel;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

public class MainRoomController {

    @FXML
    private Label roomLabel1, roomLabel2, roomLabel3, roomLabel4, roomLabel5;

    @FXML
    private ImageView avatarImage;

    @FXML
    private Label firstText, fourthText, secondText, thirdText;

    @FXML
    private javafx.scene.control.Label welcomeLabel;


    private final Label[] roomLabels = new Label[5];

    public void initialize() {
        roomLabels[0] = roomLabel1;
        roomLabels[1] = roomLabel2;
        roomLabels[2] = roomLabel3;
        roomLabels[3] = roomLabel4;
        roomLabels[4] = roomLabel5;

        // İlk başta tüm label'ları gizleyelim
        for (Label label : roomLabels) {
            label.setVisible(false);
        }

        // Yeni bir thread başlatalım, çünkü jsoup ağ bağlantısı ana thread'i bloklamasın
        new Thread(() -> {
            try {
                String[] trends = getTopTrends();

                // JavaFX UI güncellemeleri Platform.runLater ile yapılır
                Platform.runLater(() -> {
                    firstText.setText(trends[0]);
                    secondText.setText(trends[1]);
                    thirdText.setText(trends[2]);
                    fourthText.setText(trends[3]);
                });

            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();

        avatarImage.imageProperty().bind(UserData.getInstance().avatarImageProperty());
        AvatarUtil.setupAvatarClickHandler(avatarImage);
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

    private String[] getTopTrends() throws IOException {
        Document doc = Jsoup.connect("https://trends24.in/turkey/").get();
        Elements trendElements = doc.select(".trend-card__list li a");

        String[] trends = new String[5];
        for (int i = 0; i < 5; i++) {
            trends[i] = trendElements.get(i).text();
        }
        return trends;
    }

    public void setUsername(String username) {
        welcomeLabel.setText(username);
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
                // Boşta olan ilk label'ı bul
                for (Label label : roomLabels) {
                    if (!label.isVisible()) {
                        label.setText(roomName);
                        label.setVisible(true);

                        // Tıklanınca chatRoom.fxml'e geçiş
                        label.setOnMouseClicked(e -> openChatRoom(roomName));
                        return;
                    }
                }

                // Eğer hiç boş label yoksa uyarı ver
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Sınır Aşıldı");
                alert.setHeaderText(null);
                alert.setContentText("En fazla 5 oda oluşturabilirsin.");
                alert.showAndWait();
            }
        });
    }

    private void openChatRoom(String roomName) {
        try {
            FXMLLoader loader = new FXMLLoader(Main.class.getResource("chatRoom.fxml"));
            Parent root = loader.load();

            ChatRoomController controller = loader.getController();
            controller.setLobbyText(roomName);
            controller.setUsername(welcomeLabel.getText());



            controller.setTrends(new String[]{
                    firstText.getText(),
                    secondText.getText(),
                    thirdText.getText(),
                    fourthText.getText()
            });

            Stage stage = new Stage();
            stage.setTitle("Ghosty");
            stage.setScene(new Scene(root, 1000, 700));
            stage.show();

            ((Stage) roomLabel1.getScene().getWindow()).close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void deleteRooms(MouseEvent event) {
        // Odaları silmek için tüm label'ları kontrol et
        for (Label label : roomLabels) {
            if (label.isVisible()) {  // Görünür olan label'ları kontrol et
                // Kullanıcıdan onay al
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("Oda Sil");
                alert.setHeaderText(null);
                alert.setContentText("'" + label.getText() + "' odasını silmek istiyor musun?");
                Optional<ButtonType> result = alert.showAndWait();

                if (result.isPresent() && result.get() == ButtonType.OK) {
                    label.setText("");  // Label'ın ismini yok et
                    label.setVisible(false);  // Label'ı görünmez yap
                    label.setOnMouseClicked(null);  // Label'ın tıklama olayını sıfırla
                }
            }
        }
    }

    @FXML
    void lobbyClicked(MouseEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(Main.class.getResource("chatRoom.fxml"));
            Parent root = loader.load();

            // Controller'a erişiyoruz
            ChatRoomController chatRoomController = loader.getController();

            // Trend verilerini gönder chatRoomController için
            String[] trends = {
                    firstText.getText(),
                    secondText.getText(),
                    thirdText.getText(),
                    fourthText.getText()
            };
            chatRoomController.setTrends(trends);

            Label clickedLabel = (Label) event.getSource();
            chatRoomController.setLobbyText(clickedLabel.getText());

            // Kullanıcı ismini de gönderiyoruz
            chatRoomController.setUsername(welcomeLabel.getText());

            // Yeni sahneyi açıp
            Stage stage = new Stage();
            stage.setTitle("Ghosty");
            stage.setScene(new Scene(root, 1000, 700));
            stage.show();

            // Mevcut sahneyi kapatıyoruz
            ((Stage)(((javafx.scene.Node) event.getSource()).getScene().getWindow())).close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
