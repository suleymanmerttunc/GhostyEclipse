package com.example.ghosty;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
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
    
    // Client threadlerinin saçma sapan hatalar vermemesi için ConcurrentHashMap kullanıyorum
    private static final Map<String, ChatroomServer> chatrooms = Collections.synchronizedMap(new HashMap<>());
    private static final RoomList roomList = new RoomList();
    
    // Mevcut odaların keşfedilmesi, yeni oda oluşturma ve silme gibi işlerle uğraşan Registration serverinin portu
    private static final int REGISTRATION_PORT = 9990;
    private static ServerSocket registrationServer;
    private static boolean isRunning = false;
    
    // Timer interval for chat clearing (in milliseconds)
    public static final long CLEAR_INTERVAL = (long) (5 * 60 * 1000); // milisaniye cinsinden 5 dakika (6 saniye)
    private static Timer autoClearTimer;
    private static long nextClearTime = 0; 
    
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


    public static synchronized Server getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new Server();
            startRegistrationServer();
            startAutoClearTimer();
        }
        return INSTANCE;
    }
    
    private static void startAutoClearTimer() {
        if (autoClearTimer != null) {
            autoClearTimer.cancel();
        }
        
        autoClearTimer = new Timer(true);
        nextClearTime = System.currentTimeMillis() + CLEAR_INTERVAL;
        
        autoClearTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                broadcastClearChatCommand();
                nextClearTime = System.currentTimeMillis() + CLEAR_INTERVAL;
                log("Chat history cleared - next clear in " + (CLEAR_INTERVAL / 1000) + " seconds");
                broadcastClearTimeRemaining(nextClearTime - System.currentTimeMillis());            }
        }, CLEAR_INTERVAL, CLEAR_INTERVAL);
        
        
        log("Auto-clear timer started - chat history will be cleared every " + (CLEAR_INTERVAL / 1000) + " seconds");
    }
    
    

    private static void broadcastClearChatCommand() {
        synchronized (chatrooms) {
            for (ChatroomServer chatroom : chatrooms.values()) {
                chatroom.broadcast("/clear_chat_history");
            }
        }
    }
    private static void broadcastClearTimeRemaining(long timeRemaining) 
    {
        synchronized (chatrooms) {
            for (ChatroomServer chatroom : chatrooms.values()) {
                chatroom.broadcast("/time_remaining " + String.valueOf(timeRemaining));
            }
        }
    }
    
    
    
    
    //kullanıcılara oda listelerinin gönderilmesi, oda oluşturma ve silme ile ilgilenen RegistrationServer'ı başlatan method
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
    
    // Client'ları güncel tutan, oda oluşturma ve silme özelliklerini sağlayan method.
    private static void handleClientRegistration(Socket clientSocket) {
        try {
            ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream());
            
            String command = (String) in.readObject();
            
            if ("GET_ROOMS".equals(command)) {
                // Client'a mevcut oda listesini gönder
                out.writeObject(new ArrayList<>(roomList.getRooms()));
               
            } else if ("CREATE_ROOM".equals(command)) {
                Room room = (Room) in.readObject();
                // Client'tan gelen oda oluşturma isteği ile ilgilen
                boolean success = createChatroomFromClient(room.getName(), room.getPort());
                out.writeBoolean(success);

            } else if ("DELETE_ROOM".equals(command)) {
                String roomToDeleteName = (String) in.readObject();
                boolean removed = false;
                
                Room roomToDelete = roomList.findRoomByName(roomToDeleteName);
                if (roomToDelete != null) {

                    roomList.removeRoom(roomToDelete);
                    
                    ChatroomServer chatroomServer = chatrooms.get(roomToDeleteName);
                    if (chatroomServer != null) {

                        chatroomServer.shutdown();
                    }
                    
                    removed = true;
                    log("Room '" + roomToDeleteName + "' has been deleted");
                }
                
                out.writeBoolean(removed);
                out.flush();
            }
            
            clientSocket.close();
        } catch (IOException | ClassNotFoundException e) {
            log("Error handling client registration: " + e.getMessage());
        }
    }
    
    

    // Client isteği üzerine chatrrom oluştur (normal chatroom oluşturmaktan hiçbir farkı yok, daha nizami durduğu için bu şekilde ayırdım)
    private static boolean createChatroomFromClient(String chatroomName, int port) {
        return createChatroomInternal(chatroomName, port);
    }
    
    // synchronization kullanarak bir chatroom oluştur
    private static synchronized boolean createChatroomInternal(String chatroomName, int port) {
        if (isPortInUse(port)) {
            log("Error: Port " + port + " is already in use");
            return false;
        }
        
        try {
            ChatroomServer chatroom = new ChatroomServer(chatroomName, port);
            chatrooms.put(chatroomName, chatroom);
            
            Room room = new Room(chatroomName, port);
            roomList.addRoom(room);
            

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
        if (INSTANCE != null) {
            INSTANCE.logArea.append(message + "\n");
            // Auto scroll özelliği
            INSTANCE.logArea.setCaretPosition(INSTANCE.logArea.getDocument().getLength());
        }
    }
    
    public static void shutdown() {
        isRunning = false;
        
        if (autoClearTimer != null) {
            autoClearTimer.cancel();
            autoClearTimer = null;
        }
        
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
    
    // Chatroom'larla ilgilenen class
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
        
        
        /*
         * Bir chatrooma bağlanan her bir client'ın ilgili bağlantı ile ilgili her şeyden (mesaj gönderme, mesaj alma vs.) sorumlu ConnectionHandler class'ı.
         *  Her bir chatroom için her clientın bir ConnectionHandler'ı olur.
         */
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
                    
                    // Bir odaya bağlantı sağlandığında client'a mesaj sıfırlamaya ne kadar süre kaldığını gönder
                    long currentTime = System.currentTimeMillis();
                    long timeRemaining = nextClearTime - currentTime;
                    if (timeRemaining < 0) timeRemaining = 0;
                    sendMessage("/time_remaining " + timeRemaining);
                    
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
                        } else {
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