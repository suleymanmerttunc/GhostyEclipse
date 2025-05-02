package com.example.ghosty;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;

public class MainGUI implements RoomList.RoomListObserver {

    @FXML private Label lobbyText;
    @FXML private Label roomLabel1, roomLabel2, roomLabel3, roomLabel4, roomLabel5;
    @FXML private Label firstText, secondText, thirdText, fourthText;
    @FXML private Label welcomeLabel;
    @FXML private Button sendMessage;
    @FXML private TextField MessageField;
    @FXML private Button sendButton;
    @FXML private Button goBackLobby;
    @FXML private Button joinRoom;
    @FXML private VBox messageBox;
    @FXML private ScrollPane scrollPane;
    
    public Client client;
    private final int roomLimit = 9;
    private List<Room> rooms = new ArrayList<>();
    private List<Label> labels = new ArrayList<>();
    private Timer roomUpdateTimer;
    private final String SERVER_IP = "localhost";
    private List<Room> roomsCreatedByThisUser = new ArrayList<>();
    private final int TRENDING_ROOM_COUNT = 4;

    public void initialize() {

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
        
        // Setup periodic room list updates
        startRoomUpdates();
        
        // Start background thread to load trending topics
        new Thread(() -> {
            try {
                // Load trending topics
                String[] trends = UtilFunctions.getTopTrends();
                
                Platform.runLater(() -> {
                    firstText.setText(trends[0]);
                    secondText.setText(trends[1]);
                    thirdText.setText(trends[2]);
                    fourthText.setText(trends[3]);
                });
                
                // Get current room list
                updateRoomList(client.getRoomList());

                
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> displayMessage("Error loading trending topics: " + e.getMessage()));
            }
        }).start();
    }
    
    /**
     * Update the room list display
     */
    @Override
    public void updateRoomList(List<Room> updatedRooms) {
        this.rooms = updatedRooms;
        
        Platform.runLater(() -> {
            // Update room labels
            for (int i = TRENDING_ROOM_COUNT; i < rooms.size(); i++) {
                labels.get(i-TRENDING_ROOM_COUNT).setText(rooms.get(i).getName());
            }
            
            // Clear any unused labels
            for (int i = rooms.size()-TRENDING_ROOM_COUNT; i < labels.size(); i++) {
                labels.get(i).setText("");
            }
        });
    

    }
    
    /**
     * Handle opening a room when a room label is clicked
     */
    @FXML
    public void openRoom(MouseEvent event) {
        clearChatHistory();
        
        
        // Disconnect from current room if connected
        if (client.isConnected()) {
            client.disconnect();
        }
        
        // Get the clicked label
        Node source = (Node) event.getSource();
        if (source instanceof Label clickedLabel) {
            String roomName = clickedLabel.getText();
            
            if (!roomName.isEmpty()) {
                // Update the lobby text
                lobbyText.setText(roomName);
                
                // Find the room to connect to
                Room roomToConnect = null;
                for (Room room : rooms) {
                    if (room.getName().equals(roomName)) {
                        roomToConnect = room;
                        break;
                    }
                }
                
                if (roomToConnect != null) {
                    // Connect to the room
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
    
    /**
     * Clear the chat history
     */
    public void clearChatHistory() {
        Platform.runLater(() -> {
        	messageBox.getChildren().clear();
        	MessageField.setText("");
        	scrollPane.setVvalue(0.0);
        	
        
        });
    }
    
    /**
     * Display a message in the chat window
     */
    public void displayMessage(String message) {
        if (!message.isEmpty()) {
            Platform.runLater(() -> {
                Label messageLabel = new Label(message);
                messageLabel.setWrapText(true);
                messageBox.getChildren().add(messageLabel);
                
                // Auto-scroll if more than 25 messages
                if (messageBox.getChildren().size() > 25) {
                    scrollPane.setVvalue(scrollPane.getVmax());
                }
            });
        }
    }
    
    /**
     * Handle joining a room by IP and port
     */
    @FXML
    public void joinRoom(MouseEvent event) {
        // Prompt for server IP
        TextInputDialog ipDialog = new TextInputDialog(SERVER_IP);
        ipDialog.setTitle("Join Room");
        ipDialog.setHeaderText(null);
        ipDialog.setContentText("Server IP:");
        Optional<String> ipResult = ipDialog.showAndWait();
        
        if (!ipResult.isPresent()) {
            return;
        }
        
        // Prompt for port number
        TextInputDialog portDialog = new TextInputDialog("9999");
        portDialog.setTitle("Join Room");
        portDialog.setHeaderText(null);
        portDialog.setContentText("Port Number:");
        Optional<String> portResult = portDialog.showAndWait();
        
        if (!portResult.isPresent()) {
            return;
        }
        
        try {
            final String ip = ipResult.get();
            final int port = Integer.parseInt(portResult.get());
            
            // Disconnect if already connected
            if (client.isConnected()) {
                client.disconnect();
            }
            
            // Clear chat history
            clearChatHistory();
            
            // Connect to the room
            Thread joinThread = new Thread(() -> {
                if (client.connect(ip, port, welcomeLabel.getText())) {
                    // Find room name by port
                    String roomName = "Custom Room";
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
            showAlert("Invalid Port", "Please enter a valid port number.");
        }
    }
    
    /**
     * Start periodic updates of the room list
     */
    private void startRoomUpdates() {
        // Cancel any existing timer
        if (roomUpdateTimer != null) {
            roomUpdateTimer.cancel();
        }
        
        // Create new timer for polling room list updates
        roomUpdateTimer = new Timer(true);
        roomUpdateTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    // Get updated room list from server
                    List<Room> updatedRooms = client.getRoomList();
                    Platform.runLater(() -> updateRoomList(updatedRooms));
                } catch (Exception e) {
                    System.err.println("Error updating room list: " + e.getMessage());
                }
            }
        }, 5000, 5000); // Update every 5 seconds
    }
    

    public void setUsername(String username) {
        welcomeLabel.setText(username);
    }
    
    
    @FXML
    public void deleteRooms(MouseEvent event) {
        // Get non-empty room labels
        List<String> availableRooms = new ArrayList<>();
        Map<String, Integer> roomIndexMap = new HashMap<>();
        Room currentRoom;
        if (!lobbyText.getText().equals("#Lobby")) 
        {
        	currentRoom = UtilFunctions.findRoom(lobbyText.getText(), rooms);
		}
        else 
        {
        	currentRoom = rooms.get(0);
        }
        for (int i = 0; i < labels.size(); i++) {
            String roomName = labels.get(i).getText();
            if (!roomName.isEmpty()) {
                availableRooms.add(roomName);
                roomIndexMap.put(roomName, i);
            }
        }
        
        // If no rooms to delete, return
        if (availableRooms.isEmpty()) {
            showAlert("No Rooms", "There are no rooms available to delete.");
            return;
        }

        
        // Create a choice dialog for room selection
        ChoiceDialog<String> dialog = new ChoiceDialog<>(availableRooms.get(0), availableRooms);
        dialog.setTitle("Delete Room");
        dialog.setHeaderText("Select a room to delete:");
        dialog.setContentText("Room:");
        
        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()) {
            String roomToDelete = result.get();
            
            // Find if this is a custom room (not one of the trending topic rooms)
            boolean isTrendingRoom = false;
            for (int i = 0; i < TRENDING_ROOM_COUNT; i++) { // First 4 rooms are trending rooms
                if (i < rooms.size() && rooms.get(i).getName().equals(roomToDelete)) {
                    isTrendingRoom = true;
                    break;
                }
            }
            
            if (isTrendingRoom) {
                showAlert("Cannot Delete", "You cannot delete default trending rooms.");
            } else {
                // Delete the room from server
                if (currentRoom.equals(UtilFunctions.findRoom(roomToDelete, rooms))) 
                {
                	showAlert("Error!", "You can't delete the room you are currently in!");
                	return;
        		}
                if ((UtilFunctions.findRoom(roomToDelete, roomsCreatedByThisUser)) == null) 
                {
                	showAlert("Error Deleting Room!", "You Can't delete a room you didn't create!");
                	return;
				}
            	boolean deleted = client.deleteRoom(roomToDelete);
                
                if (deleted) {
                    // Clear the label in the UI
                    int labelIndex = roomIndexMap.get(roomToDelete);
                    labels.get(labelIndex).setText("");
                    displayMessage("Room '" + roomToDelete + "' has been deleted.");
                    
                    // Request updated room list
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
        System.out.println(roomsCreatedByThisUser);
    }
    
    /**
     * Handle room creation
     */
    @FXML
    public void createRooms(MouseEvent event) {
        // Check room limit
        if (getCustomRoomCount() >= roomLimit - TRENDING_ROOM_COUNT) {
            showAlert("Limit Reached", "Upgrade to premium to create more rooms.");
            return;
        }

        // Ask for room name
        TextInputDialog nameDialog = new TextInputDialog();
        nameDialog.setTitle("New Room");
        nameDialog.setHeaderText(null);
        nameDialog.setContentText("Enter room name:");
        Optional<String> nameResult = nameDialog.showAndWait();

        if (nameResult.isPresent() && !nameResult.get().trim().isEmpty()) {
            String roomName = nameResult.get().trim();
            
            // Ask for port number
            TextInputDialog portDialog = new TextInputDialog();
            portDialog.setTitle("Port Number");
            portDialog.setHeaderText(null);
            portDialog.setContentText("Enter port number:");
            Optional<String> portResult = portDialog.showAndWait();
            
            if (portResult.isPresent() && !portResult.get().trim().isEmpty()) {
                try {
                    int port = Integer.parseInt(portResult.get().trim());
                    
                    // Request room creation from server
                    client.createRoom(roomName, port);
                    roomsCreatedByThisUser.add(new Room(roomName,-1));
                    
                } catch (NumberFormatException e) {
                    showAlert("Invalid Port", "Please enter a valid port number.");
                }
            }
        }
        System.out.println(roomsCreatedByThisUser);
    }
    
    /**
     * Count the number of custom rooms (non-trending rooms)
     */
    private int getCustomRoomCount() {
        int count = rooms.size() - TRENDING_ROOM_COUNT;
        return count;
    }
    
    /**
     * Show a simple alert dialog
     */
    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}