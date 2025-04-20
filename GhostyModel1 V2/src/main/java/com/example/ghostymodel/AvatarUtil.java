package com.example.ghostymodel;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;

import java.io.File;

public class AvatarUtil {

    public static void setupAvatarClickHandler(ImageView avatarImageView) {
        // UserData ile bind işlemi
        avatarImageView.imageProperty().bind(UserData.getInstance().avatarImageProperty());

        // Tıklama olayı
        avatarImageView.setOnMouseClicked(event -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Avatar Seç");
            fileChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Resimler", "*.png", "*.jpg", "*.jpeg")
            );
            File selectedFile = fileChooser.showOpenDialog(avatarImageView.getScene().getWindow());
            if (selectedFile != null) {
                Image newAvatar = new Image(selectedFile.toURI().toString());
                UserData.getInstance().setAvatarImage(newAvatar);
            }
        });
    }
}
