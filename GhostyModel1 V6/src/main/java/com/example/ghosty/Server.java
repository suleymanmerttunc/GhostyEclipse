package com.example.ghosty;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

public class Server extends JFrame {
    private JTextField chatroomNameField;
    private JTextField portField;
    private JButton createButton;
    private JTextArea logArea;
    
    // Use ConcurrentHashMap for thread safety across clients
    private static final Map<String, ChatroomServer> chatrooms = Collections.synchronizedMap(new HashMap<>());
    private static final RoomList roomList = new RoomList();
    
    // Service discovery/registration socket
    private static final int REGISTRATION_PORT = 9990;
    private static ServerSocket registrationServer;
    private static boolean isRunning = false;
    
    private static Server INSTANCE;
    
    private Server() {
        setTitle("Ghosty Server Manager");
        setSize(500, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        
        JPanel inputPanel = new JPanel(new GridLayout(3, 2, 5, 5));
        JLabel nameLabel = new JLabel("Chatroom Name:");
        chatroomNameField = new JTextField(15);
        JLabel portLabel = new JLabel("Port Number:");
        portField = new JTextField(String.valueOf(getNextAvailablePort()));
        createButton = new JButton("Create Chatroom");
        
        inputPanel.add(nameLabel);
        inputPanel.add(chatroomNameField);
        inputPanel.add(portLabel);
        inputPanel.add(portField);
        inputPanel.add(new JLabel()); // Empty cell for layout
        inputPanel.add(createButton);
        
        logArea = new JTextArea();
        logArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(logArea);
        
        mainPanel.add(inputPanel, BorderLayout.NORTH);
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        
        // Add action listener to create button
        createButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                createChatroom(chatroomNameField.getText(), portField.getText());
                // After creation, update the port field to the next available
                portField.setText(String.valueOf(getNextAvailablePort()));
            }
        });
        
        add(mainPanel);
        setVisible(true);
    }

    // Get the singleton instance of Server
    public static synchronized Server getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new Server();
            startRegistrationServer();
        }
        return INSTANCE;
    }
    
    // Start the service registration server
    private static void startRegistrationServer() {
        if (isRunning) return;
        
        isRunning = true;
        Thread registrationThread = new Thread(() -> {
            try {
                registrationServer = new ServerSocket(REGISTRATION_PORT);
                log("Room registration server started on port " + REGISTRATION_PORT);
                
                while (isRunning) {
                    try {
                        Socket clientSocket = registrationServer.accept();
                        handleClientRegistration(clientSocket);
                    } catch (IOException e) {
                        if (isRunning) {
                            log("Error accepting client connection: " + e.getMessage());
                        }
                    }
                }
            } catch (IOException e) {
                log("Error starting registration server: " + e.getMessage());
                isRunning = false;
            }
        });
        registrationThread.setDaemon(true);
        registrationThread.start();
    }
    
    private static void handleClientRegistration(Socket clientSocket) {
        try {
            ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream());
            
            String command = (String) in.readObject();
            
            if ("GET_ROOMS".equals(command)) {
                // Send room list to client
                out.writeObject(new ArrayList<>(roomList.getRooms()));
               
            } else if ("CREATE_ROOM".equals(command)) {
                Room room = (Room) in.readObject();
                // Handle room creation request from client
                boolean success = createChatroomFromClient(room.getName(), room.getPort());
                out.writeBoolean(success);
                
                // Broadcast room list updates if room was created successfully
                if (success) {
                    broadcastRoomListUpdate();
                }
                
            } else if ("DELETE_ROOM".equals(command)) {
                String roomToDeleteName = (String) in.readObject();
                boolean removed = false;
                
                // Find the room by name
                Room roomToDelete = roomList.findRoomByName(roomToDeleteName);
                if (roomToDelete != null) {
                    // Remove from roomList
                    roomList.removeRoom(roomToDelete);
                    
                    // Check if a chatroom server exists for this room
                    ChatroomServer chatroomServer = chatrooms.get(roomToDeleteName);
                    if (chatroomServer != null) {
                        // Shutdown the chatroom server
                        chatroomServer.shutdown();
                    }
                    
                    removed = true;
                    log("Room '" + roomToDeleteName + "' has been deleted");
                    broadcastRoomListUpdate();
                }
                
                out.writeBoolean(removed);
                out.flush();
            }
            
            clientSocket.close();
        } catch (IOException | ClassNotFoundException e) {
            log("Error handling client registration: " + e.getMessage());
        }
    }
    
    // Broadcast room list updates to all connected clients
    private static void broadcastRoomListUpdate() {
        // This would involve sending notifications to connected clients
        // For now, we'll rely on clients polling for updates
        log("Room list updated, clients will receive updates on next poll");
    }
    
    // Create a chatroom from the server UI
    public static boolean createChatroom(String chatroomName, String portText) {
        chatroomName = chatroomName.trim();
        portText = portText.trim();
        
        if (chatroomName.isEmpty()) {
            log("Error: Empty chatroom name");
            return false;
        }
        
        int port;
        try {
            port = Integer.parseInt(portText);
            if (port < 1024 || port > 65535) {
                throw new NumberFormatException("Port must be between 1024 and 65535");
            }
        } catch (NumberFormatException ex) {
            log("Error: Invalid port number (must be between 1024 and 65535)");
            return false;
        }
        
        return createChatroomInternal(chatroomName, port);
    }
    
    // Create a chatroom from a client request
    private static boolean createChatroomFromClient(String chatroomName, int port) {
        return createChatroomInternal(chatroomName, port);
    }
    
    // Internal method to create a chatroom with synchronization
    private static synchronized boolean createChatroomInternal(String chatroomName, int port) {
        // Check if port is already in use by another chatroom
        if (isPortInUse(port)) {
            log("Error: Port " + port + " is already in use");
            return false;
        }
        
        // Create a new chatroom server
        try {
            ChatroomServer chatroom = new ChatroomServer(chatroomName, port);
            chatrooms.put(chatroomName, chatroom);
            
            Room room = new Room(chatroomName, port);
            roomList.addRoom(room);
            
            // Start the chatroom server in a new thread
            Thread serverThread = new Thread(chatroom);
            serverThread.setDaemon(true);
            serverThread.start();
            
            log("Created chatroom '" + chatroomName + "' on port " + port);
            return true;
        } catch (IOException ex) {
            log("Error creating chatroom '" + chatroomName + "': " + ex.getMessage());
            return false;
        }
    }
    
    // Find the next available port starting from 9000
    private static int getNextAvailablePort() {
        int port = 9000;
        while (isPortInUse(port)) {
            port++;
        }
        return port;
    }
    
    // Check if a port is already in use
    private static boolean isPortInUse(int port) {
        // First check our own chatrooms
        for (ChatroomServer server : chatrooms.values()) {
            if (server.getPort() == port) {
                return true;
            }
        }
        
        // Then check if port is used by any other application
        try (ServerSocket ignored = new ServerSocket(port)) {
            // Port is available
            return false;
        } catch (IOException e) {
            // Port is in use
            return true;
        }
    }
    
    private static void log(String message) {
        //System.out.println("[Server] " + message);
        if (INSTANCE != null) {
            INSTANCE.logArea.append(message + "\n");
            // Auto-scroll
            INSTANCE.logArea.setCaretPosition(INSTANCE.logArea.getDocument().getLength());
        }
    }
    
    // Shutdown the server gracefully
    public static void shutdown() {
        isRunning = false;
        
        for (ChatroomServer chatroom : chatrooms.values()) {
            chatroom.shutdown();
        }
        
        try {
            if (registrationServer != null && !registrationServer.isClosed()) {
                registrationServer.close();
            }
        } catch (IOException e) {
            System.err.println("Error closing registration server: " + e.getMessage());
        }
    }
    
    public static void main(String[] args) {
        // Initialize default chatrooms with trending topics
        try {
            String[] trends = UtilFunctions.getTopTrends();
            int basePort = 9999;
            
            for (int i = 0; i < Math.min(4, trends.length); i++) {
                createChatroomInternal(trends[i], basePort - i);
            }
        } catch (IOException e) {
            System.err.println("Error fetching trending topics: " + e.getMessage());
        }
        
        // Start the server UI on the Event Dispatch Thread
        SwingUtilities.invokeLater(() -> Server.getInstance());
    }
    
    // Inner class for handling individual chatrooms
    public static class ChatroomServer implements Runnable {
        private final String chatroomName;
        private final int port;
        private final List<ConnectionHandler> connections = Collections.synchronizedList(new ArrayList<>());
        private ServerSocket server;
        private boolean done;
        private final ExecutorService pool;
        
        public ChatroomServer(String chatroomName, int port) throws IOException {
            this.chatroomName = chatroomName;
            this.port = port;
            this.done = false;
            this.server = new ServerSocket(port);
            this.pool = Executors.newCachedThreadPool();
        }
        
        public int getPort() {
            return port;
        }
        
        @Override
        public void run() {
            try {
                while (!done) {
                    Socket clientSocket = server.accept();
                    String clientAddress = clientSocket.getInetAddress().getHostAddress();
                    log("New client connected to " + chatroomName + " from " + clientAddress);
                    
                    ConnectionHandler handler = new ConnectionHandler(clientSocket);
                    synchronized (connections) {
                        connections.add(handler);
                    }
                    pool.execute(handler);
                }
            } catch (IOException e) {
                if (!done) {
                    shutdown();
                }
            }
        }
        
        public void broadcast(String message) {
            synchronized (connections) {
                for (ConnectionHandler ch : connections) {
                    if (ch != null) {
                        ch.sendMessage(message);
                    }
                }
            }
        }
        
        public void shutdown() {
            try {
                done = true;
                
                if (server != null && !server.isClosed()) {
                    server.close();
                }
                
                synchronized (connections) {
                    for (ConnectionHandler ch : connections) {
                        ch.shutdown();
                    }
                    connections.clear();
                }
                
                pool.shutdown();
                
                synchronized (chatrooms) {
                    chatrooms.remove(chatroomName);
                }
                
                // Update the room list
                synchronized (roomList) {
                    roomList.removeRoomByName(chatroomName);
                }
                
                log("Chatroom '" + chatroomName + "' was shut down");
            } catch (IOException e) {
                log("Error shutting down chatroom: " + e.getMessage());
            }
        }
        
        class ConnectionHandler implements Runnable {
            private final Socket clientSocket;
            private BufferedReader in;
            private PrintWriter out;
            private String nickname;
            
            public ConnectionHandler(Socket clientSocket) {
                this.clientSocket = clientSocket;
            }
            
            @Override
            public void run() {
                try {
                    out = new PrintWriter(clientSocket.getOutputStream(), true);
                    in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                    
                    out.println("You have joined chatroom: " + chatroomName);
                    
                    nickname = in.readLine();
                    broadcast(nickname + " joined chat!");
                    
                    String message;
                    while ((message = in.readLine()) != null) {
                        if (message.startsWith("/nick ")) {
                            String[] messageSplit = message.split(" ", 2);
                            if (messageSplit.length == 2) {
                                String oldNickname = nickname;
                                nickname = messageSplit[1];
                                broadcast(oldNickname + " renamed themselves to " + nickname);
                                out.println("Successfully changed nickname to " + nickname);
                            } else {
                                out.println("Error: No nickname provided");
                            }
                        } else if (message.startsWith("/quit")) {
                            broadcast(nickname + " left the chat!");
                            shutdown();
                            break;
                        }
                        
                        else {
                            broadcast(nickname + ": " + message);
                        }
                    }
                } catch (IOException e) {
                    shutdown();
                }
            }
            
            public void shutdown() {
                try {
                    if (in != null) in.close();
                    if (out != null) out.close();
                    if (clientSocket != null && !clientSocket.isClosed()) {
                        clientSocket.close();
                    }
                    
                    synchronized (connections) {
                        connections.remove(this);
                    }
                } catch (IOException e) {
                    log("Error closing client connection: " + e.getMessage());
                }
            }
            
            public void sendMessage(String message) {
                if (out != null) {
                    out.println(message);
                }
            }
        }
    }
}