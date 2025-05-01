package com.example.ghosty;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class MainGUI {

    private int roomPort;

    @FXML
    private Label lobbyText;

    @FXML
    private Label roomLabel1, roomLabel2, roomLabel3, roomLabel4, roomLabel5;

    @FXML
    private Label firstText, fourthText, secondText, thirdText;

    @FXML
    private javafx.scene.control.Label welcomeLabel;

    @FXML
    private Button sendMessage;
    @FXML
    private TextField MessageField;
    @FXML
    private Button sendButton;
    @FXML
    private VBox messageBox;
    private Client client;
    private RoomList roomList;

    @FXML private ScrollPane scrollPane;

    List<Room> rooms = new ArrayList<>();
    List<Label> labels = new ArrayList<>();


    public void initialize() {
    	this.client = new Client(this);
    	roomList = new RoomList();
    	roomList.addObserver(this);
    	
        labels = Arrays.asList(roomLabel1, roomLabel2, roomLabel3, roomLabel4, roomLabel5);
        
        new Thread(() -> {
            try {
                String[] trends = UtilFunctions.getTopTrends();

                Platform.runLater(() -> {
                    firstText.setText(trends[0]);
                    secondText.setText(trends[1]);
                    thirdText.setText(trends[2]);
                    fourthText.setText(trends[3]);
                });
                String[] portList = {"9999","9998","9997","9996"};
               for (int i = 0; i < 4; i++) 
               {
               	roomList.addRoom(new Room(trends[i],Integer.parseInt(portList[i])));
               	
				}

            } catch (IOException e) {
                e.printStackTrace();
            }
            
        }).start();
        
        

        sendMessage.setOnAction(e -> {
            String message = MessageField.getText();
            client.sendMessage(message);
        });


    }



    public void setUsername(String username) {
        welcomeLabel.setText(username);
    }

    @FXML
    public void deleteRooms(MouseEvent event) {
        if (roomLabel1.getText().isEmpty()) {
            return;
        }

        TextInputDialog deleteDialog = new TextInputDialog();
        deleteDialog.setTitle("Title");
        deleteDialog.setHeaderText(null);
        deleteDialog.setContentText("Oda index'i gir:");
        Optional<String> deleteResult = deleteDialog.showAndWait();

        if (deleteResult.isPresent() && Integer.parseInt(deleteResult.get()) < rooms.size()) {
            int deleteIndex = Integer.parseInt(deleteResult.get());
            labels.get(deleteIndex).setText("");
            rooms.remove(deleteIndex);
        }
        else {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Sınır Aşıldı");
            alert.setHeaderText(null);
            alert.setContentText("Oda Mevcut Değil");
            alert.showAndWait();
        }

    }

    @FXML
    public void createRooms(MouseEvent event) {
        // Eğer oda zaten oluşturulmuşsa boş değilse uyarı ver
        if ((rooms.size() > 4)) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Sınır Aşıldı");
            alert.setHeaderText(null);
            alert.setContentText("Daha fazla oda oluşturmak için Premium sürüme yükselt.");
            alert.showAndWait();
            return;
        }

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
                            roomPort = Integer.parseInt(port);
                            Room room = new Room(nameResult.get(), roomPort);
                            //rooms.add(room);
                            roomList.addRoom(room);
                            Server.createChatroom(roomName, port);
                            listRoomNames(rooms);

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

    public void listRoomNames(List<Room> rooms) {
        for (int i = 0; i < rooms.size(); i++) {
            int index = i;
        	Platform.runLater(() -> {
        		labels.get(index).setText(rooms.get(index).getName());
        	    
        	});



        }

    }

    @FXML
    public void openRoom(MouseEvent event) {
        clearChatHistory();
        if(client.isConnected()) 
        {
        	client.disconnect();
        }
        String roomName = null;
        Node source = (Node) event.getSource();
        if (source instanceof Label clickedLabel) {
            
        	roomName = clickedLabel.getText();
            
            if (!roomName.isEmpty()) {
                lobbyText.setText(roomName);
            }
        }
        String finalRoomName = roomName;
       Thread openRoomThread =  new Thread(() -> {
        	Room roomToLoad = UtilFunctions.findRoom(finalRoomName, rooms);
        	int port =roomToLoad.getPort();
            client.connect("localhost", port, welcomeLabel.getText());
            
        });
       openRoomThread.setDaemon(true);
       openRoomThread.start();
        
    }

    public void clearChatHistory() {
        messageBox.getChildren().clear();
    }

    public void displayMessage(String message) {
        if (!message.isEmpty() && !lobbyText.getText().isEmpty()) {
            Label messageLabel = new Label(message);
            messageLabel.setWrapText(true);
            messageBox.getChildren().add(messageLabel);
            MessageField.clear();

            // Sadece 1'den fazla mesaj varsa scroll'u aşağı kaydır
            if (messageBox.getChildren().size() > 25) {
                Platform.runLater(() -> scrollPane.setVvalue(1.0));
            }
        }
    }



	public void updateRoomList(List<Room> rooms2) {
		this.rooms = rooms2;
		listRoomNames(rooms);
		
	}



}
