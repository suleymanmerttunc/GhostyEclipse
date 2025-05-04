package com.example.ghosty;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class SettingsController {


    @FXML private CheckBox aydınlıkMod;
    @FXML private Button closeButton;
    @FXML private CheckBox karanlıkMod;
    @FXML private CheckBox notificationSound;
    @FXML private Label settings;
    @FXML private Label chooseLanguageText;
    @FXML private Pane settingsPane;




    private MainGUI mainGUI;

    public void setMainGUI(MainGUI mainGUI) {
        this.mainGUI = mainGUI;
    }

    @FXML
    private void handleClose(ActionEvent event) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.close();
    }

    @FXML
    public void changeThemeToDark(MouseEvent event) {
        if (karanlıkMod.isSelected()) {
            aydınlıkMod.setSelected(false);  // aynı anda iki checkbox seçilmesin diye
        }
        if (mainGUI != null) {
            mainGUI.getBottomHBox().setBackground(new Background(new BackgroundFill(
                    Color.web("#212121"), CornerRadii.EMPTY, Insets.EMPTY)));

            mainGUI.getTopHbox().setBackground(new Background(new BackgroundFill(
                    Color.web("#1E1E1E"), CornerRadii.EMPTY, Insets.EMPTY)));

            mainGUI.getLeftVBox().setBackground(new Background(new BackgroundFill(
                    Color.web("#161616"), CornerRadii.EMPTY, Insets.EMPTY)));
        }
        settingsPane.setBackground(new Background(new BackgroundFill(Color.web("#212121"), CornerRadii.EMPTY, Insets.EMPTY)));
        mainGUI.getLobbyText().setTextFill(Color.web("#212121"));
    }

    @FXML
    public void changeThemeToLight(MouseEvent event) {
        if (aydınlıkMod.isSelected()) {
            karanlıkMod.setSelected(false);  // aynı anda iki checkbox seçilmesin diye
        }
        if (mainGUI != null) {
            mainGUI.getBottomHBox().setBackground(new Background(new BackgroundFill(
                    Color.web("#4D366B"), CornerRadii.EMPTY, Insets.EMPTY)));

            mainGUI.getTopHbox().setBackground(new Background(new BackgroundFill(
                    Color.web("#3F2D56"), CornerRadii.EMPTY, Insets.EMPTY)));

            mainGUI.getLeftVBox().setBackground(new Background(new BackgroundFill(
                    Color.web("#5A3F7F"), CornerRadii.EMPTY, Insets.EMPTY)));
        }
        settingsPane.getStyleClass().add("settings-header");
        mainGUI.getLobbyText().setTextFill(Color.web("#5a3f7f"));

    }


    @FXML
    void chinaLanguage(MouseEvent event) {
        mainGUI.getCreateRoomButton().setText("私人和群组聊天                      ➕");
        mainGUI.getDeleteRoomButton().setText("私人和群组聊天                      −");
        mainGUI.getGroupChatText().setText("私人和群组聊天");
        mainGUI.getUsernameText().setText("用户名");
        mainGUI.getTrendRoomText().setText("热门房间");
        mainGUI.getJoinRoom().setText("加入房间");
        mainGUI.getMessageField().setPromptText("输入消息");
        mainGUI.getLobbyText().setText(" #大厅 ");
        karanlıkMod.setText("深色模式");
        aydınlıkMod.setText("浅色模式");
        closeButton.setText("关闭");
        settings.setText("设置");
        chooseLanguageText.setText("选择语言");
        notificationSound.setText("打开/关闭通知声音");


    }

    @FXML
    void englishLanguage(MouseEvent event) {
        mainGUI.getCreateRoomButton().setText("1ON1 & GROUP CHAT            ➕");
        mainGUI.getDeleteRoomButton().setText("1ON1 & GROUP CHAT            −");
        mainGUI.getGroupChatText().setText("1ON1 & GROUP CHAT");
        mainGUI.getUsernameText().setText("Username");
        mainGUI.getTrendRoomText().setText("Trending Rooms");
        mainGUI.getJoinRoom().setText("Join a Room");
        mainGUI.getMessageField().setPromptText("Type a message");
        mainGUI.getLobbyText().setText("#Lobby");
        karanlıkMod.setText("Dark Mode");
        aydınlıkMod.setText("Light Mode");
        closeButton.setText("Close");
        settings.setText("Settings");
        chooseLanguageText.setText("Choose Language");
        notificationSound.setText("Turn Notification Sound On/Off");


    }


    @FXML
    void spainLanguage(MouseEvent event) {
        mainGUI.getCreateRoomButton().setText("Chat Privado y Grupal             ➕");
        mainGUI.getDeleteRoomButton().setText("Chat Privado y Grupal             −");
        mainGUI.getGroupChatText().setText("Chats Privados y Grupales");
        mainGUI.getUsernameText().setText("Nombre de usuario");
        mainGUI.getTrendRoomText().setText("Salas de Tendencia");
        mainGUI.getJoinRoom().setText("Entrar a sala");
        mainGUI.getMessageField().setPromptText("Escribe un mensaje");
        mainGUI.getLobbyText().setText("#Vestíbulo");
        karanlıkMod.setText("Modo Oscuro");
        aydınlıkMod.setText("Modo Claro");
        closeButton.setText("Cerrar");
        settings.setText("Ajustes");
        chooseLanguageText.setText("Elegir idioma");
        notificationSound.setText("Activar/Desactivar sonido de notificación");

    }

    @FXML
    void turkishLanguage(MouseEvent event) {
        mainGUI.getCreateRoomButton().setText("Özel ve Grup Sohbeti              ➕");
        mainGUI.getDeleteRoomButton().setText("Özel ve Grup Sohbeti              −");
        mainGUI.getGroupChatText().setText("Özel ve Grup Sohbetleri");
        mainGUI.getUsernameText().setText("Kullanıcı adı");
        mainGUI.getTrendRoomText().setText("Trend Odalar");
        mainGUI.getJoinRoom().setText("Bir Odaya Katıl");
        mainGUI.getMessageField().setPromptText("Bir mesaj yaz");
        mainGUI.getLobbyText().setText("#Lobi");
        karanlıkMod.setText("Karanlık Mod");
        aydınlıkMod.setText("Aydınlık Mod");
        closeButton.setText("Kapat");
        settings.setText("Ayarlar");
        chooseLanguageText.setText("Dili Seç");
        notificationSound.setText("Bildirim Sesini Aç/Kapat");

    }


    @FXML
    public void changeNotificationSound(MouseEvent event) {

        boolean isSoundEnabled = notificationSound.isSelected();
        MainGUI.setInstanceNotificationSound(isSoundEnabled);
        System.out.println("Bildirim sesi durumu: " + MainGUI.getInstanceNotificationSound());

        if (mainGUI != null) {
            mainGUI.updateNotificationIcon(mainGUI.getNotificationIcon());
        }
    }



}
