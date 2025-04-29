package com.example.ghosty;
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

    private int roomPort;

    @FXML
    private Label roomLabel1;

    @FXML
    private Label firstText, fourthText, secondText, thirdText;

    @FXML
    private javafx.scene.control.Label welcomeLabel;


    public void initialize() {

        new Thread(() -> {
            try {
                String[] trends = getTopTrends();

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
    public void deleteRooms(MouseEvent event) {
        if (roomLabel1.getText().isEmpty()) {
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Oda Sil");
        alert.setHeaderText(null);
        alert.setContentText("'" + roomLabel1.getText() + "' odasını silmek istiyor musun?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            roomLabel1.setText("");
        }
    }

    @FXML
    public void createRooms(MouseEvent event) {
        // Eğer oda zaten oluşturulmuşsa boş değilse uyarı ver
        if (!roomLabel1.getText().isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Sınır Aşıldı");
            alert.setHeaderText(null);
            alert.setContentText("Daha fazla oda oluşturmak için Premium sürüme yükselt.");
            alert.showAndWait();
            return;
        }

        // Oda ismi soruluyor
        TextInputDialog nameDialog = new TextInputDialog();
        nameDialog.setTitle("Yeni Oda");
        nameDialog.setHeaderText(null);
        nameDialog.setContentText("Oda ismi gir:");

        Optional<String> nameResult = nameDialog.showAndWait();
        nameResult.ifPresent(roomName -> {
            if (!roomName.trim().isEmpty()) {
                // Port numarası soruluyor
                TextInputDialog portDialog = new TextInputDialog();
                portDialog.setTitle("Port Girişi");
                portDialog.setHeaderText(null);
                portDialog.setContentText("Port numarası gir:");

                Optional<String> portResult = portDialog.showAndWait();
                portResult.ifPresent(port -> {
                    if (!port.trim().isEmpty()) {
                        try {
                            roomPort = Integer.parseInt(port); // portu değişkende sakla
                            roomLabel1.setText(roomName);     // sadece oda ismini göster
                        } catch (NumberFormatException e) {
                            Alert alert = new Alert(Alert.AlertType.ERROR);
                            alert.setTitle("Hatalı Giriş");
                            alert.setHeaderText(null);
                            alert.setContentText("Geçerli bir port numarası giriniz.");
                            alert.showAndWait();
                        }
                    }
                });
            }
        });
    }

    @FXML
    void openChatRoom(MouseEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(Main.class.getResource("chatRoom.fxml"));
            Parent root = loader.load();
            ChatRoomController chatRoomController = loader.getController();

            // Trend verilerini gönder chatRoomController için
            String[] trends = {
                    firstText.getText(),
                    secondText.getText(),
                    thirdText.getText(),
                    fourthText.getText()
            };
            //yukardaki diziyi aldık ChatRoomControlelr sınıfındaki setTrends metoduna yolladık
            chatRoomController.setTrends(trends);

            // Kullanıcı ismini de gönderiyoruz
            chatRoomController.setUsername(welcomeLabel.getText());

            //Oluşturduğumuz özel odayı da yolluyoz
            chatRoomController.setRoomLabel(roomLabel1.getText());

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