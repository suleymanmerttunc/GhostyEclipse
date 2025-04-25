package com.example.ghostymodel;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.image.Image;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class UserData {
    private static UserData instance;

    private final ObjectProperty<Image> avatarImage = new SimpleObjectProperty<>();

    private UserData() {
        // ClassLoader ile kaynak dosyayı yükle
        URL imageUrl = getClass().getResource("/images/default-avatar.png");
        if (imageUrl != null) {
            avatarImage.set(new Image(imageUrl.toExternalForm()));
        } else {
            System.err.println("Varsayılan avatar bulunamadı!");
        }
    }

    public static UserData getInstance() {
        if (instance == null) {
            instance = new UserData();
        }
        return instance;
    }

    public ObjectProperty<Image> avatarImageProperty() {
        return avatarImage;
    }

    public Image getAvatarImage() {
        return avatarImage.get();
    }

    public void setAvatarImage(Image image) {
        avatarImage.set(image);
    }



}
