package com.example.ghosty;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.imageio.ImageIO;
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
        setTitle("Ghosty Server");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(400, 300);
        setLocationRelativeTo(null);
        setResizable(false);

        // Ana panel yeni açık mor (#5A3F7F) arka planlı
        JPanel mainPanel = new JPanel();
        mainPanel.setBackground(new Color(90, 63, 127)); // #5A3F7F rengi
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Sohbet odası ismi
        JLabel chatroomNameLabel = new JLabel("Chatroom Name");
        styleLabel(chatroomNameLabel);
        chatroomNameField = new JTextField(20);
        chatroomNameField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        styleTextField(chatroomNameField);

        // Port
        JLabel portLabel = new JLabel("Port");
        styleLabel(portLabel);
        portField = new JTextField(20);
        portField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        styleTextField(portField);

        // Oluştur butonu
        createButton = new JButton("Create");
        styleButton(createButton);
        createButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                logArea.append("Create button clicked!\n");
            }
        });


        // Log alanı (arka plan beyaz)
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setBackground(Color.WHITE);  // Beyaz arka plan
        logArea.setForeground(Color.BLACK);  // Siyah yazı
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane scrollPane = new JScrollPane(logArea);
        scrollPane.setPreferredSize(new Dimension(380, 120));

        // Bileşenleri panele ekle
        mainPanel.add(chatroomNameLabel);
        mainPanel.add(chatroomNameField);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        mainPanel.add(portLabel);
        mainPanel.add(portField);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        mainPanel.add(createButton);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 15)));
        mainPanel.add(scrollPane);

        try {
            Image icon = ImageIO.read(getClass().getResource("/images/ghosty.png"));
            setIconImage(icon);
        } catch (IOException | IllegalArgumentException e) {
            System.out.println("Icon yüklenemedi: " + e.getMessage());
        }
        add(mainPanel);
        setVisible(true);



    }
    private void styleLabel(JLabel label) {
        label.setForeground(Color.WHITE);
        label.setFont(new Font("Segoe UI", Font.BOLD, 14));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
    }

    // Metin kutusu stilleri
    private void styleTextField(JTextField textField) {
        textField.setBackground(Color.WHITE); // beyaz
        textField.setForeground(Color.BLACK);
        textField.setCaretColor(Color.BLACK);
        textField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        textField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(150, 0, 255), 1),
                BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
    }

    // Buton stilleri
    private void styleButton(JButton button) {
        button.setBackground(new Color(138, 43, 226)); // neon mor
        button.setForeground(Color.WHITE);
        button.setFont(new Font("Segoe UI", Font.BOLD, 14));
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createLineBorder(new Color(200, 150, 255), 2));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
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
            private static List<String> activeUsers = Collections.synchronizedList(new ArrayList<>());
            
            public ConnectionHandler(Socket clientSocket) {
                this.clientSocket = clientSocket;
            }

            public static List<String> getActiveUsers() {
                return new ArrayList<>(activeUsers);
            }
            @Override
            public void run() {

                try {
                    out = new PrintWriter(clientSocket.getOutputStream(), true);
                    in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                    
                    out.println("You have joined chatroom: " + chatroomName);
                    
                    nickname = in.readLine();
                    broadcast(nickname + " joined chat!");
                    activeUsers.add(nickname);
                    
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
                        activeUsers.remove(nickname);
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