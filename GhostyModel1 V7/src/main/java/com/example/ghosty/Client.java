package com.example.ghosty;

import javafx.application.Platform;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class Client {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private String nickname;
    private boolean connected;
    private MainGUI gui;
    
    // Server registration details
    private static final String SERVER_HOST = "localhost";
    private static final int REGISTRATION_PORT = 9990;
    
    public Client(MainGUI gui) {
        this.gui = gui;
        this.connected = false;
    }
    
    /**
     * Connect to a chat server
     */
    public boolean connect(String serverAddress, int serverPort, String nickname) {
        try {
            // Disconnect first if already connected
            if (connected) {
                disconnect();
            }
            
            this.socket = new Socket(serverAddress, serverPort);
            this.out = new PrintWriter(socket.getOutputStream(), true);
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.nickname = nickname;
            this.connected = true;
            
            // Start a thread to listen for messages
            Thread messageListener = new Thread(this::listenForMessages);
            messageListener.setDaemon(true);
            messageListener.start();
            
            // Update GUI with connected status
            Platform.runLater(() -> gui.displayMessage("Connected to room on port " + serverPort));
            return true;
            
        } catch (IOException e) {
            Platform.runLater(() -> gui.displayMessage("Error connecting to server: " + e.getMessage()));
            return false;
        }
    }
    
    /**
     * Get the list of available rooms from the server
     */
    public List<Room> getRoomList() {
        List<Room> rooms = new ArrayList<>();
        
        try (Socket registrationSocket = new Socket(SERVER_HOST, REGISTRATION_PORT);
             ObjectOutputStream out = new ObjectOutputStream(registrationSocket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(registrationSocket.getInputStream())) {
            
            // Request room list
            out.writeObject("GET_ROOMS");
            out.flush();
            
            // Read response
            @SuppressWarnings("unchecked")
            List<Room> serverRooms = (List<Room>) in.readObject();
            rooms.addAll(serverRooms);
            
        } catch (IOException | ClassNotFoundException e) {
            Platform.runLater(() -> gui.displayMessage("Error getting room list: " + e.getMessage()));
        }
        
        return rooms;
    }
    
    /**
     * Request room creation on the server
     */
    public boolean createRoom(String roomName, int port) {
        Socket registrationSocket = null;
        ObjectOutputStream out = null;
        ObjectInputStream in = null;
        boolean success = false;
        
        try {
            registrationSocket = new Socket(SERVER_HOST, REGISTRATION_PORT);
            out = new ObjectOutputStream(registrationSocket.getOutputStream());
            in = new ObjectInputStream(registrationSocket.getInputStream());
            
            // Send creation request
            out.writeObject("CREATE_ROOM");
            out.writeObject(new Room(roomName, port));
            out.flush();
            
            // Get result
            success = in.readBoolean();
            
            return success;
            
        } catch (IOException e) {
            String errorMsg = (e.getMessage() != null) ? e.getMessage() : "";
            Platform.runLater(() -> gui.displayMessage(errorMsg));
            return false;
        } finally {
            // Properly close resources without triggering exceptions
            try {
                if (in != null) in.close();
                if (out != null) out.close();
                if (registrationSocket != null) registrationSocket.close();
            } catch (IOException e) {
                // Log but don't change the return value
                Platform.runLater(() -> gui.displayMessage("Warning: Could not properly close connection: " + e.getMessage()));
            }
        }
    }
    
    /**
     * Listen for messages from the server
     */
    private void listenForMessages() {
        try {
            // Send nickname to server
            out.println(nickname);
            
            String serverMessage;
            while (connected && (serverMessage = in.readLine()) != null) {
                final String message = serverMessage;
                Platform.runLater(() -> gui.displayMessage(message));
            }
        } catch (IOException e) {
            if (connected) {
                Platform.runLater(() -> gui.displayMessage("Disconnected from server: " + e.getMessage()));
                disconnect();
            }
        }
    }
    
    /**
     * Send a message to the server
     */
    public void sendMessage(String message) {
        if (connected && out != null) {
            out.println(message);
        } else {
            Platform.runLater(() -> gui.displayMessage("Not connected to server!"));
        }
    }
    
    /**
     * Change the user's nickname
     */
    public void changeNickname(String newNickname) {
        if (connected) {
            this.nickname = newNickname;
            sendMessage("/nick " + newNickname);
        }
    }
    
    /**
     * Disconnect from the server
     */
    public void disconnect() {
        if (connected) {
            connected = false;
            try {
                if (out != null) {
                    out.println("/quit");
                    out.close();
                }
                if (in != null) {
                    in.close();
                }
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
                Platform.runLater(() -> gui.displayMessage("Disconnected from server."));
            } catch (IOException e) {
                Platform.runLater(() -> gui.displayMessage("Error during disconnect: " + e.getMessage()));
            }
        }
    }
    
    /**
     * Check if connected to a server
     */
    public boolean isConnected() 
    {
        return connected;
    }
    public boolean deleteRoom(String roomName) {
        try (Socket registrationSocket = new Socket(SERVER_HOST, REGISTRATION_PORT);
             ObjectOutputStream out = new ObjectOutputStream(registrationSocket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(registrationSocket.getInputStream())) {
            
            out.writeObject("DELETE_ROOM");
            out.writeObject(roomName);  // Port can be 0 since we're matching by name
            out.flush();
            
            boolean result = in.readBoolean();
            if (!result) {
                Platform.runLater(() -> gui.displayMessage("Room not found or could not be deleted."));
            }
            return result;
        } catch (IOException e) {
            Platform.runLater(() -> gui.displayMessage("Error deleting room: " + e.getMessage()));
            return false;
        }
    }
}