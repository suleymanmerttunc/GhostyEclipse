package com.example.ghosty;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.scene.media.AudioClip;
import java.io.*;
import java.util.*;


public class MainGUI implements RoomList.RoomListObserver {

    @FXML private Label lobbyText;
    @FXML private Label roomLabel1, roomLabel2, roomLabel3, roomLabel4, roomLabel5;
    @FXML private Label firstText, secondText, thirdText, fourthText;
    @FXML private Label welcomeLabel;
    @FXML private Button sendMessage;
    @FXML private TextField MessageField;
    @FXML private Button sendButton;
    @FXML private ImageView avatarImage;
    @FXML private VBox messageBox;
    @FXML private ScrollPane scrollPane;
    @FXML private ImageView settingsButton;
    @FXML private HBox bottomHBox;
    @FXML private VBox leftVBox;
    @FXML private HBox topHbox;
    @FXML private ToggleButton createRoomButton;
    @FXML private ToggleButton deleteRoomButton;
    @FXML private Text groupChatText;
    @FXML private Text usernameText;
    @FXML private Text trendRoomText;
    @FXML private Button joinRoom;
    public String currentLanguage = "en";
    @FXML private Label timeLabel;
    private static boolean InstanceNotificationSound=true;
    @FXML private ImageView notificationIcon;
    private static MainGUI instance;

    public static MainGUI getInstance() {
        if (instance == null) {
            instance = new MainGUI();
        }
        return instance;
    }

    public ImageView getNotificationIcon() {
        return notificationIcon;
    }


    public static void setInstanceNotificationSound(boolean value) {
        InstanceNotificationSound = value;
    }

    public static boolean getInstanceNotificationSound() {
        return InstanceNotificationSound;
    }

    public Label getLobbyText() {
        return lobbyText;
    }

    public Button getJoinRoom() {
        return joinRoom;
    }

    public Text getTrendRoomText() {
        return trendRoomText;
    }

    public Text getUsernameText() {
        return usernameText;
    }

    public Text getGroupChatText() {
        return groupChatText;
    }

    public ToggleButton getDeleteRoomButton() {
        return deleteRoomButton;
    }

    public ToggleButton getCreateRoomButton() {
        return createRoomButton;
    }

    public TextField getMessageField() {
        return MessageField;
    }

    public HBox getBottomHBox() {
        return bottomHBox;
    }

    public VBox getLeftVBox() {
        return leftVBox;
    }

    public HBox getTopHbox() {
        return topHbox;
    }

    public Client client;
    private final int roomLimit = 9;
    private List<Room> rooms = new ArrayList<>();
    private List<Label> labels = new ArrayList<>();
    private Timer roomUpdateTimer;
    private final String SERVER_IP = "localhost";
    private List<Room> roomsCreatedByThisUser = new ArrayList<>();
    private final int TRENDING_ROOM_COUNT = 4;
    private AudioClip notificationSound;
    private long timeRemaining;


    public void initialize() {

        updateNotificationIcon(notificationIcon);


        if (InstanceNotificationSound) {
            try {
                String soundPath = getClass().getResource("/sounds/notification.wav").toExternalForm();
                notificationSound = new AudioClip(soundPath);
            } catch (Exception e) {
                System.err.println("Bildirim sesi yüklenmesinde hata oluştu: " + e.getMessage());
            }
        }


        this.client = new Client(this);
        Platform.runLater(()->lobbyText.setText("#Lobby"));
        labels = Arrays.asList(roomLabel1, roomLabel2, roomLabel3, roomLabel4, roomLabel5);
        

        sendMessage.setOnAction(e -> {
            String message = MessageField.getText();
            if (message.equals("/clear"))
            {
            	clearChatHistory();
            	return;
			}
            if (!message.trim().isEmpty()) {
                client.sendMessage(message);
                MessageField.clear();
            }
            if (messageBox.getChildren().size() > 25) {
                scrollPane.setVvalue(scrollPane.getVmax());
            }
        });
        MessageField.setOnAction(e -> sendMessage.fire());



        startRoomUpdates();
        

        new Thread(() -> {
            try {
                String[] trends = UtilFunctions.getTopTrends();
                
                Platform.runLater(() -> {
                    firstText.setText(trends[0]);
                    secondText.setText(trends[1]);
                    thirdText.setText(trends[2]);
                    fourthText.setText(trends[3]);
                });
                
                updateRoomList(client.getRoomList());

                
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> displayMessage("Error loading trending topics: " + e.getMessage()));
            }
        }).start();
    }
    public String getCurrentLanguage() {
        String langText = trendRoomText.getText();

        if (langText.equals("Trending Rooms")) {
            this.currentLanguage = "en";
        } else if (langText.equals("Trend Odalar")) {
            this.currentLanguage = "tr";
        } else if (langText.equals("Salas de Tendencia")) {
            this.currentLanguage = "es";
        } else if (langText.equals("热门房间")){
            this.currentLanguage = "zh";
        }else{
            this.currentLanguage = "en";
        }

        return this.currentLanguage;
    }

    /**
     * Odaların en güncel haliyle listelenmesini sağlayan fonksiyon.
     */
    @Override
    public void updateRoomList(List<Room> updatedRooms) {
        this.rooms = updatedRooms;
        
        Platform.runLater(() -> {
            for (int i = TRENDING_ROOM_COUNT; i < rooms.size(); i++) {
                labels.get(i-TRENDING_ROOM_COUNT).setText(rooms.get(i).getName());
            }

            for (int i = rooms.size()-TRENDING_ROOM_COUNT; i < labels.size(); i++) {
                labels.get(i).setText("");
            }
        });
    

    }
    
    /**
     * ilgili odanın üstüne tıklandığı zaman odanın yüklenmesini sağlayan fonksiyon.
     */
    @FXML
    public void openRoom(MouseEvent event) {
        clearChatHistory();
        
        

        if (client.isConnected()) {
            client.disconnect();
        }
        
        // Tıklanan label'ın içeriğini al
        Node source = (Node) event.getSource();
        if (source instanceof Label clickedLabel) {
            String roomName = clickedLabel.getText();
            
            if (!roomName.isEmpty()) {
                lobbyText.setText(roomName);
                
                // Bağlanılacak odayı bul
                Room roomToConnect = null;
                for (Room room : rooms) {
                    if (room.getName().equals(roomName)) {
                        roomToConnect = room;
                        break;
                    }
                }
                
                if (roomToConnect != null) {
                    // Odaya bağlan
                    final Room finalRoom = roomToConnect;
                    Thread connectThread = new Thread(() -> {
                        client.connect(SERVER_IP, finalRoom.getPort(), welcomeLabel.getText());
                        List<Room> updatedRooms = client.getRoomList();
                        updateRoomList(updatedRooms);
                        System.out.println("");
                    });
                    connectThread.setDaemon(true);
                    connectThread.start();
                    
                } else {
                    displayMessage("Room not found: " + roomName);
                }
            }
        }
    }
    

    public void clearChatHistory() {
        Platform.runLater(() -> {
        	messageBox.getChildren().clear();
        	MessageField.setText("");
        	scrollPane.setVvalue(0.0);
        	
        
        });
    }


    public void displayMessage(String message) {
        if (!message.isEmpty()) {
            Platform.runLater(() -> {
                Label messageLabel = new Label(message);
                messageLabel.setWrapText(true);
                messageBox.getChildren().add(messageLabel);

                String myUsername = welcomeLabel.getText();


                if (!message.startsWith(myUsername + ":")) {

                    if (MainGUI.getInstanceNotificationSound() && notificationSound != null) {
                        notificationSound.play();
                    }
                }

                if (messageBox.getChildren().size() > 25) {
                    scrollPane.setVvalue(scrollPane.getVmax());
                }
            });
        }
    }


    /**
     * IP ve port bilgisiyle bir Server'a bağlanmayı sağlayan fonksiyon.
     */
    @FXML
    public void joinRoom(MouseEvent event) {
        String lang = getCurrentLanguage();

        String title, serverIpText, portText, invalidPortTitle, invalidPortMessage, customRoomName;

        switch (lang) {
            case "tr":
                title = "Odaya Katıl";
                serverIpText = "Sunucu IP'si:";
                portText = "Port Numarası:";
                invalidPortTitle = "Geçersiz Port";
                invalidPortMessage = "Lütfen geçerli bir port numarası girin.";
                customRoomName = "Özel Oda";
                break;
            case "es":
                title = "Unirse a la Sala";
                serverIpText = "IP del Servidor:";
                portText = "Número de Puerto:";
                invalidPortTitle = "Puerto Inválido";
                invalidPortMessage = "Por favor, ingrese un número de puerto válido.";
                customRoomName = "Sala Personalizada";
                break;
            case "zh":
                title = "加入房间";
                serverIpText = "服务器 IP：";
                portText = "端口号：";
                invalidPortTitle = "无效端口";
                invalidPortMessage = "请输入有效的端口号。";
                customRoomName = "自定义房间";
                break;
            case "en":
            default:
                title = "Join Room";
                serverIpText = "Server IP:";
                portText = "Port Number:";
                invalidPortTitle = "Invalid Port";
                invalidPortMessage = "Please enter a valid port number.";
                customRoomName = "Custom Room";
                break;
        }

        // Server ip girme ekranı
        TextInputDialog ipDialog = new TextInputDialog(SERVER_IP);
        ipDialog.setTitle(title);
        ipDialog.setHeaderText(null);
        ipDialog.setContentText(serverIpText);
        Optional<String> ipResult = ipDialog.showAndWait();

        if (!ipResult.isPresent()) {
            return;
        }

        //Port numarası girme ekranı
        TextInputDialog portDialog = new TextInputDialog("9999");
        portDialog.setTitle(title);
        portDialog.setHeaderText(null);
        portDialog.setContentText(portText);
        Optional<String> portResult = portDialog.showAndWait();

        if (!portResult.isPresent()) {
            return;
        }

        try {
            final String ip = ipResult.get();
            final int port = Integer.parseInt(portResult.get());

            if (client.isConnected()) {
                client.disconnect();
            }

            clearChatHistory();

            Thread joinThread = new Thread(() -> {
                if (client.connect(ip, port, welcomeLabel.getText())) {
                    String roomName = customRoomName;
                    for (Room room : rooms) {
                        if (room.getPort() == port) {
                            roomName = room.getName();
                            break;
                        }
                    }

                    final String finalRoomName = roomName;
                    Platform.runLater(() -> lobbyText.setText(finalRoomName));
                }
            });
            joinThread.setDaemon(true);
            joinThread.start();

        } catch (NumberFormatException e) {
            showAlert(invalidPortTitle, invalidPortMessage);
        }
    }


    /**
     * Her 5 saniyede bir Server'dan güncel odaların listesini iste
     */
    private void startRoomUpdates() {
        if (roomUpdateTimer != null) {
            roomUpdateTimer.cancel();
        }
        
        roomUpdateTimer = new Timer(true);
        roomUpdateTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    List<Room> updatedRooms = client.getRoomList();
                    Platform.runLater(() -> updateRoomList(updatedRooms));
                } catch (Exception e) {
                    System.err.println("Error updating room list: " + e.getMessage());
                }
            }
        }, 5000, 5000); 
    }
    

    public void setUsername(String username) {
        welcomeLabel.setText(username);
    }


    @FXML
    public void deleteRooms(MouseEvent event) {
        List<String> availableRooms = new ArrayList<>();
        Map<String, Integer> roomIndexMap = new HashMap<>();

        Room currentRoom;
        if (!lobbyText.getText().equals("#Lobby")) {
            currentRoom = UtilFunctions.findRoom(lobbyText.getText(), rooms);
        } else if (!rooms.isEmpty()) {
            currentRoom = rooms.get(0);
        } else {
            currentRoom = null;
        }

        for (int i = 0; i < labels.size(); i++) {
            String roomName = labels.get(i).getText();
            if (!roomName.isEmpty()) {
                availableRooms.add(roomName);
                roomIndexMap.put(roomName, i);
            }
        }

        if (availableRooms.isEmpty()) {
            showAlert(getLocalizedMessage("No Rooms"), getLocalizedMessage("There are no rooms available to delete."));
            return;
        }

        ChoiceDialog<String> dialog = new ChoiceDialog<>(availableRooms.get(0), availableRooms);
        dialog.setTitle(getLocalizedMessage("Delete Room"));
        dialog.setHeaderText(getLocalizedMessage("Select a room to delete:"));
        dialog.setContentText(getLocalizedMessage("Room:"));

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()) {
            String roomToDelete = result.get();

            boolean isTrendingRoom = false;
            for (int i = 0; i < TRENDING_ROOM_COUNT; i++) {
                if (i < rooms.size() && rooms.get(i).getName().equals(roomToDelete)) {
                    isTrendingRoom = true;
                    break;
                }
            }

            if (isTrendingRoom) {
                showAlert(getLocalizedMessage("Cannot Delete"), getLocalizedMessage("You cannot delete default trending rooms."));
            } else {
                Room roomToDeleteObj = UtilFunctions.findRoom(roomToDelete, rooms);

                if (currentRoom != null && currentRoom.equals(roomToDeleteObj)) {
                    showAlert(getLocalizedMessage("Error!"), getLocalizedMessage("You can't delete the room you are currently in!"));
                    return;
                }

                if (UtilFunctions.findRoom(roomToDelete, roomsCreatedByThisUser) == null) {
                    showAlert(getLocalizedMessage("Error Deleting Room!"), getLocalizedMessage("You can't delete a room you didn't create!"));
                    return;
                }

                boolean deleted = client.deleteRoom(roomToDelete);
                if (deleted) {
                    int labelIndex = roomIndexMap.get(roomToDelete);
                    labels.get(labelIndex).setText("");
                    displayMessage(getLocalizedMessage("Room ") + "'" + roomToDelete + "' " + getLocalizedMessage("has been deleted."));

                    Thread updateThread = new Thread(() -> {
                        List<Room> updatedRooms = client.getRoomList();
                        updateRoomList(updatedRooms);
                        Room room = UtilFunctions.findRoom(roomToDelete, roomsCreatedByThisUser);
                        roomsCreatedByThisUser.remove(room);
                        System.out.println(room);
                    });
                    updateThread.setDaemon(true);
                    updateThread.start();
                }
            }
        }
    }

    public String getLocalizedMessage(String key) {
        String lang = getCurrentLanguage();
        switch (lang) {
            case "tr":
                switch (key) {
                    case "No Rooms": return "Odalar Yok";
                    case "There are no rooms available to delete.": return "Silinecek odalar yok.";
                    case "Delete Room": return "Oda Sil";
                    case "Select a room to delete:": return "Silinecek bir oda seçin:";
                    case "Room:": return "Oda:";
                    case "Cannot Delete": return "Silinemez";
                    case "You cannot delete default trending rooms.": return "Varsayılan trend odalarını silemezsiniz.";
                    case "Error!": return "Hata!";
                    case "You can't delete the room you are currently in!": return "Şu anda bulunduğunuz odayı silemezsiniz!";
                    case "Error Deleting Room!": return "Oda Silme Hatası!";
                    case "You can't delete a room you didn't create!": return "Kendi oluşturmadığınız bir odayı silemezsiniz!";
                    case "has been deleted.": return "silindi.";
                    case "Chat history has been cleared by the server (auto-clear timer)": return "Chat geçmişi sunucu tarafından temizlendi (otomatik temizlik zamanlayıcısı)";
                    default: return key;
                }
            case "es":
                switch (key) {
                    case "No Rooms": return "No hay habitaciones";
                    case "There are no rooms available to delete.": return "No hay habitaciones disponibles para eliminar.";
                    case "Delete Room": return "Eliminar habitación";
                    case "Select a room to delete:": return "Seleccione una habitación para eliminar:";
                    case "Room:": return "Habitación:";
                    case "Cannot Delete": return "No se puede eliminar";
                    case "You cannot delete default trending rooms.": return "No puede eliminar las habitaciones predeterminadas.";
                    case "Error!": return "¡Error!";
                    case "You can't delete the room you are currently in!": return "¡No puedes eliminar la habitación en la que estás!";
                    case "Error Deleting Room!": return "¡Error al eliminar la habitación!";
                    case "You can't delete a room you didn't create!": return "¡No puedes eliminar una habitación que no creaste!";
                    case "has been deleted.": return "ha sido eliminada.";
                    case "Chat history has been cleared by the server (auto-clear timer)": return "El historial de chat ha sido borrado por el servidor (temporizador de borrado automático)";
                    
                    default: return key;
                }
            case "zh":
                switch (key) {
                    case "No Rooms": return "没有房间";
                    case "There are no rooms available to delete.": return "没有可删除的房间。";
                    case "Delete Room": return "删除房间";
                    case "Select a room to delete:": return "选择一个要删除的房间：";
                    case "Room:": return "房间：";
                    case "Cannot Delete": return "无法删除";
                    case "You cannot delete default trending rooms.": return "您不能删除默认的流行房间。";
                    case "Error!": return "错误！";
                    case "You can't delete the room you are currently in!": return "您不能删除您当前所在的房间！";
                    case "Error Deleting Room!": return "删除房间错误！";
                    case "You can't delete a room you didn't create!": return "您不能删除未创建的房间！";
                    case "has been deleted.": return "已被删除。";
                    case "Chat history has been cleared by the server (auto-clear timer)": return "聊天历史已由服务器清除（自动清除计时器）";
                    default: return key;
                }
            case "en":
            default:
                return key;
        }
    }


    /**
     * Oda oluşturmakla yükümlü fonskiyon
     */
    @FXML
    public void createRooms(MouseEvent event) {
        // Check room limit
        if (getCustomRoomCount() >= roomLimit - TRENDING_ROOM_COUNT) {
            showAlert("Limit Reached", "Upgrade to premium to create more rooms.");
            return;
        }
        if (trendRoomText.getText().equals("Trend Odalar")){
            TextInputDialog nameDialog = new TextInputDialog();
            nameDialog.setTitle("Yeni Oda");
            nameDialog.setHeaderText(null);
            nameDialog.setContentText("Oda Adı Gir:");
            Optional<String> nameResult = nameDialog.showAndWait();
            if (nameResult.isPresent() && !nameResult.get().trim().isEmpty()) {
                String roomName = nameResult.get().trim();

                TextInputDialog portDialog = new TextInputDialog();
                portDialog.setTitle("Port Numarası");
                portDialog.setHeaderText(null);
                portDialog.setContentText("Port Numarası Giriniz:");
                Optional<String> portResult = portDialog.showAndWait();

                if (portResult.isPresent() && !portResult.get().trim().isEmpty()) {
                    try {
                        int port = Integer.parseInt(portResult.get().trim());

                        client.createRoom(roomName, port);
                        roomsCreatedByThisUser.add(new Room(roomName,-1));

                    } catch (NumberFormatException e) {
                        showAlert("Invalid Port", "Please enter a valid port number.");
                    }
                }
            }
        }
        else if (trendRoomText.getText().equals("Trending Rooms")){
            TextInputDialog nameDialog = new TextInputDialog();
            nameDialog.setTitle("New Room");
            nameDialog.setHeaderText(null);
            nameDialog.setContentText("Enter room name:");
            Optional<String> nameResult = nameDialog.showAndWait();
            if (nameResult.isPresent() && !nameResult.get().trim().isEmpty()) {
                String roomName = nameResult.get().trim();

                TextInputDialog portDialog = new TextInputDialog();
                portDialog.setTitle("Port Number");
                portDialog.setHeaderText(null);
                portDialog.setContentText("Enter port number:");
                Optional<String> portResult = portDialog.showAndWait();

                if (portResult.isPresent() && !portResult.get().trim().isEmpty()) {
                    try {
                        int port = Integer.parseInt(portResult.get().trim());

                        client.createRoom(roomName, port);
                        roomsCreatedByThisUser.add(new Room(roomName,-1));

                    } catch (NumberFormatException e) {
                        showAlert("Invalid Port", "Please enter a valid port number.");
                    }
                }
            }

        }
        else if (trendRoomText.getText().equals("Salas de Tendencia")){
            TextInputDialog nameDialog = new TextInputDialog();
            nameDialog.setTitle("Nueva habitación");
            nameDialog.setHeaderText(null);
            nameDialog.setContentText("Ingrese el nombre de la sala:");
            Optional<String> nameResult = nameDialog.showAndWait();
            if (nameResult.isPresent() && !nameResult.get().trim().isEmpty()) {
                String roomName = nameResult.get().trim();

                TextInputDialog portDialog = new TextInputDialog();
                portDialog.setTitle("Número de Puerto");
                portDialog.setHeaderText(null);
                portDialog.setContentText("Ingrese el número de puerto:");
                Optional<String> portResult = portDialog.showAndWait();

                if (portResult.isPresent() && !portResult.get().trim().isEmpty()) {
                    try {
                        int port = Integer.parseInt(portResult.get().trim());

                        client.createRoom(roomName, port);
                        roomsCreatedByThisUser.add(new Room(roomName,-1));

                    } catch (NumberFormatException e) {
                        showAlert("Invalid Port", "Please enter a valid port number.");
                    }
                }
            }

        }
        else if((trendRoomText.getText().equals("热门房间"))){
            TextInputDialog nameDialog = new TextInputDialog();
            nameDialog.setTitle("Шинэ өрөө");
            nameDialog.setHeaderText(null);
            nameDialog.setContentText("输入房间名称：");
            Optional<String> nameResult = nameDialog.showAndWait();
            if (nameResult.isPresent() && !nameResult.get().trim().isEmpty()) {
                String roomName = nameResult.get().trim();

                TextInputDialog portDialog = new TextInputDialog();
                portDialog.setTitle("端口号");
                portDialog.setHeaderText(null);
                portDialog.setContentText("请输入端口号");
                Optional<String> portResult = portDialog.showAndWait();

                if (portResult.isPresent() && !portResult.get().trim().isEmpty()) {
                    try {
                        int port = Integer.parseInt(portResult.get().trim());

                        // server'dan oda oluşturmasını iste
                        client.createRoom(roomName, port);
                        roomsCreatedByThisUser.add(new Room(roomName,-1));

                    } catch (NumberFormatException e) {
                        showAlert("Invalid Port", "Please enter a valid port number.");
                    }
                }
            }
        }
        else {
            TextInputDialog nameDialog = new TextInputDialog();
            nameDialog.setTitle("New Room");
            nameDialog.setHeaderText(null);
            nameDialog.setContentText("Enter room name:");
            Optional<String> nameResult = nameDialog.showAndWait();
            if (nameResult.isPresent() && !nameResult.get().trim().isEmpty()) {
                String roomName = nameResult.get().trim();

                //kullanıcıdan Port girmesini iste
                TextInputDialog portDialog = new TextInputDialog();
                portDialog.setTitle("Port Number");
                portDialog.setHeaderText(null);
                portDialog.setContentText("Enter port number:");
                Optional<String> portResult = portDialog.showAndWait();

                if (portResult.isPresent() && !portResult.get().trim().isEmpty()) {
                    try {
                        int port = Integer.parseInt(portResult.get().trim());
                        
                        //Servera oda oluşturma isteği gönder
                        client.createRoom(roomName, port);
                        roomsCreatedByThisUser.add(new Room(roomName,-1));

                    } catch (NumberFormatException e) {
                        showAlert("Invalid Port", "Please enter a valid port number.");
                    }
                }
            }
        }

        System.out.println(roomsCreatedByThisUser);
    }
    
    /**
     * Trend odası olmayan oda sayısını döndür
     */
    private int getCustomRoomCount() {
        int count = rooms.size() - TRENDING_ROOM_COUNT;
        return count;
    }
    
    /**
     * title başlıklı content içerikle bir pop-up çıkartıp kullanıcıyı uyar
     */
    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    @FXML
    public void goBackLobby(MouseEvent event){
        if (client.isConnected()) 
        {
        	client.disconnect();
		}
    	
    	clearChatHistory();

        if (trendRoomText.getText().equals("Trend Odalar")){
            lobbyText.setText("#Lobi");
        }
        else if (trendRoomText.getText().equals("Trending Rooms")){
            lobbyText.setText("#Lobby");
        }
        else if (trendRoomText.getText().equals("Salas de Tendencia")){
            lobbyText.setText("#Vestíbulo");
        }else if((trendRoomText.getText().equals("热门房间"))){
            lobbyText.setText("#大厅");
        }
        else {
            lobbyText.setText("#Lobby");
        }
        

    }

    @FXML
    private void onAvatarImageClick() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Resim Seç");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Resim Dosyaları", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );
        File selectedFile = fileChooser.showOpenDialog(getStage());

        if (selectedFile != null) {
            try {
                Image image = new Image(new FileInputStream(selectedFile));
                avatarImage.setImage(image);
            } catch (FileNotFoundException e) {
                e.printStackTrace(); // veya uyarı göster
            }
        }
    }

    private Stage getStage() {
        return (Stage) avatarImage.getScene().getWindow();
    }
    
    @FXML
    private void onSendFileClicked() {
        if (lobbyText.getText().equals("#Lobby")) {
            Label hataMesajı = new Label("Please join a room to send files!");
            messageBox.getChildren().add(hataMesajı);
        }
        else{
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Select File");
            fileChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("PDF Files", "*.pdf"),
                    new FileChooser.ExtensionFilter("Word Documents", "*.doc", "*.docx"),
                    new FileChooser.ExtensionFilter("Text Files", "*.txt")
            );
            Stage stage = (Stage) sendButton.getScene().getWindow();
            File selectedFile = fileChooser.showOpenDialog(stage);

            if (selectedFile != null) {
                String fileName = selectedFile.getName();

                // Burada dosyanın adı mesaj olarak ekleniyor
                String message = "This feature will be added in future updates";

                // Mesajı vBox'a ekleyelim, tıpkı normal bir mesaj gibi
                Label messageLabel = new Label(message);
                messageLabel.getStyleClass().add("message");
                messageBox.getChildren().add(messageLabel);
            }
        }

    }

    @FXML
    public void showActiveUsers() {
        List<String> users = Server.ChatroomServer.ConnectionHandler.getActiveUsers(); // sunucudan kullanıcıları al


        for (String user : users) {
            Label userLabel = new Label(user + " Online");
            messageBox.getChildren().add(userLabel);
        }
    }

    @FXML
    private void openSettingsWindow() {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/com/example/ghosty/Settings.fxml"));
            Parent root = fxmlLoader.load();

            // SettingsController'a MainGUI'yi geçirmek için
            SettingsController settingsController = fxmlLoader.getController();
            settingsController.setMainGUI(this);

            Stage stage = new Stage();
            stage.setTitle("Settings");
            stage.setScene(new Scene(root));
            stage.setResizable(false);
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(settingsButton.getScene().getWindow());

            stage.getIcons().add(new Image(getClass().getResourceAsStream("/images/grey-settings-gear-22621.png")));

            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

	public void updateTimeRemaining(long timeRemaining) {
		this.timeRemaining = timeRemaining;
		startCountdown(timeRemaining);
		
	}

    public void startCountdown(long durationInMillis) {
        Timer timer = new Timer(true);
        final long interval = 1000;
        final long[] timeLeft = {durationInMillis};

        TimerTask task = new TimerTask() {
            public void run() {
                if (timeLeft[0] > 0) {
                    timeLeft[0] -= interval;

                    long secondsLeft = timeLeft[0] / 1000;

                    Platform.runLater(() -> {
                        timeLabel.setText(secondsLeft + "s");

                        if (secondsLeft <= 10) {
                            timeLabel.setTextFill(Color.RED);
                        } else {
                            timeLabel.setTextFill(Color.BLACK);
                        }
                    });
                } else {
                    timer.cancel();
                }
            }
        };

        timer.scheduleAtFixedRate(task, 0, interval);
    }

    public void updateNotificationIcon(ImageView iconView) {
        String imagePath = MainGUI.getInstanceNotificationSound() ? "/images/volume.png" : "/images/noVolume.png";

        try {
            Image iconImage = new Image(getClass().getResourceAsStream(imagePath));
            iconView.setImage(iconImage);
        } catch (Exception e) {
            System.err.println("İkon resmi yüklenemedi: " + e.getMessage());
        }
    }






}


