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
    
    private static final String SERVER_HOST = "localhost";
    private static final int REGISTRATION_PORT = 9990;
    
    public Client(MainGUI gui) {
        this.gui = gui;
        this.connected = false;
    }
    
    /**
     * Bir Server'a bağlanmayı sağlayan method
     */
    public boolean connect(String serverAddress, int serverPort, String nickname) {
        try {
            // hali hazırda bir server'a bağlıysan bağlantıyı kes
            if (connected) {
                disconnect();
            }
            
            this.socket = new Socket(serverAddress, serverPort);
            this.out = new PrintWriter(socket.getOutputStream(), true);
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.nickname = nickname;
            this.connected = true;
            
            Thread messageListener = new Thread(this::listenForMessages);
            messageListener.setDaemon(true);
            messageListener.start();
            
            Platform.runLater(() -> gui.displayMessage("Connected to room on port " + serverPort));
            return true;
            
        } catch (IOException e) {
            Platform.runLater(() -> gui.displayMessage("Error connecting to server: " + e.getMessage()));
            return false;
        }
    }
    
    /**
     * Serverdan mevcut odaların listesini al.
     */
    public List<Room> getRoomList() {
        List<Room> rooms = new ArrayList<>();
        
        try (Socket registrationSocket = new Socket(SERVER_HOST, REGISTRATION_PORT);
             ObjectOutputStream out = new ObjectOutputStream(registrationSocket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(registrationSocket.getInputStream())) {
            

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
     * Client Serverden oda oluşturmasını ister. Servere "CREATE_ROOM" içerikli bir mesaj gönderir, ardından oluşturacağı oda objesini gönderir. 
     * "CREATE_ROOM" mesajını alan server bir oda objesi için beklemeye geçer ve eğer beklediği objeyi alırsa ilgili detaylarla odayı oluşturur.
     * 
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
            
            // Oda oluşturma isteği
            out.writeObject("CREATE_ROOM");
            out.writeObject(new Room(roomName, port));
            out.flush();
            

            success = in.readBoolean();
            
            return success;
            
        } catch (IOException e) {
            String errorMsg = (e.getMessage() != null) ? e.getMessage() : "";
            Platform.runLater(() -> gui.displayMessage(errorMsg));
            return false;
        } finally {
            try {
                if (in != null) in.close();
                if (out != null) out.close();
                if (registrationSocket != null) registrationSocket.close();
            } catch (IOException e) {

                Platform.runLater(() -> gui.displayMessage("Warning: Could not properly close connection: " + e.getMessage()));
            }
        }
    }
    
    /**
     * Serverdan gelen mesajları dinle. "/clear_chat_history" gibi özel komutlar gelirse ilgili komutu uygula
     */
    private void listenForMessages() {
        try {
            //bir serverin client bağlantı sağladığında beklediği ilk mesaj client'ın nickname'i oluyor. server'a bağlanır bağlanmaz kendi nickini göndermeli.
            out.println(nickname);
            
            String serverMessage;
            while (connected && (serverMessage = in.readLine()) != null) {
                // Özel komut durumları
                if (serverMessage.equals("/clear_chat_history")) {
                    // Her 5dk'de bir mesajların silinme özelliği.
                    Platform.runLater(() -> {
                        gui.clearChatHistory();
                        gui.displayMessage(gui.getLocalizedMessage("Chat history has been cleared by the server (auto-clear timer)"));
                    });
                } else if (serverMessage.startsWith("/time_remaining ")) {
                    try {
                        final long timeRemaining = Long.parseLong(serverMessage.substring("/time_remaining ".length()));
                        Platform.runLater(() -> gui.updateTimeRemaining(timeRemaining));
                    } catch (NumberFormatException e) {
                        Platform.runLater(() -> gui.displayMessage("Error parsing time remaining: " + e.getMessage()));
                    }
                } else {
                    //Eğer üstteki özel komutlardan biri değilse normal mesajdır, gelen mesajı göster.
                    final String message = serverMessage;
                    Platform.runLater(() -> gui.displayMessage(message));
                }
            }
        } catch (IOException e) {
            if (connected) {
                Platform.runLater(() -> gui.displayMessage("Disconnected from server: " + e.getMessage()));
                disconnect();
            }
        }
    }
    
    /**
     * Server'a bir mesaj gönder
     */
    public void sendMessage(String message) {
        if (connected && out != null) {
            out.println(message);
        } else {
            Platform.runLater(() -> gui.displayMessage("Not connected to server!"));
        }
    }
    
    /**
     * kullanıcı nickini değiştir
     */
    public void changeNickname(String newNickname) {
        if (connected) {
            this.nickname = newNickname;
            sendMessage("/nick " + newNickname);
        }
    }
    

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
    

    public boolean isConnected() 
    {
        return connected;
    }
    
    public boolean deleteRoom(String roomName) {
        try (Socket registrationSocket = new Socket(SERVER_HOST, REGISTRATION_PORT);
             ObjectOutputStream out = new ObjectOutputStream(registrationSocket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(registrationSocket.getInputStream())) {
            
            out.writeObject("DELETE_ROOM");
            out.writeObject(roomName); 
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