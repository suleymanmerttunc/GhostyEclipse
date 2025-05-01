package com.example.ghosty;
import java.awt.*;
import java.awt.event.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

public class Server extends JFrame {
    private JTextField chatroomNameField;
    private JTextField portField;
    private JButton createButton;
    private JTextArea logArea;
    public static Map<String, ChatroomServer> chatrooms = new HashMap<>();;
    private static Server INSTANCE;
    private static RoomList roomList;
    
    private Server() {
    	roomList = new RoomList();
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
        portField = new JTextField("9999");
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
                createChatroom(chatroomNameField.getText(),portField.getText());
            }
        });
        
        add(mainPanel);
        setVisible(true);
        
        
    }

    public static Server getInstance() 
    {
    	if(INSTANCE == null) 
    	{
    		INSTANCE = new Server();
    		return INSTANCE;
    	}
    	return INSTANCE;
    }
    
    public static void createChatroom(String chatroomName, String portText) {

		chatroomName = chatroomName.trim();
        portText = portText.trim();
        
        if (chatroomName.isEmpty()) {
            //JOptionPane.showMessageDialog(this, "Please enter a chatroom name.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        int port;
        try {
            port = Integer.parseInt(portText);
            if (port < 1024 || port > 65535) {
                throw new NumberFormatException("Port must be between 1024 and 65535");
            }
        } catch (NumberFormatException ex) {
            //JOptionPane.showMessageDialog(this, "Please enter a valid port number (1024-65535).", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        // Check if port is already in use by another chatroom
       if(Server.chatrooms != null) {
        for (ChatroomServer server : Server.chatrooms.values()) {
            if (server.getPort() == port) {
               System.out.println("Port is Already in Use!");
                return;
            }
        }
       }
        
        // Create a new chatroom server
        try {
            ChatroomServer chatroom = new ChatroomServer(chatroomName,port);
            Server.chatrooms.put(chatroomName, chatroom);
            System.out.println("Chatroom Created!");
            Room room = new Room(chatroomName,port);
            if (Server.roomList == null) 
            {
            	Server.roomList = new RoomList();
			}
            Server.roomList.addRoom(room);
            
            // Start the chatroom server in a new thread
            Thread serverThread = new Thread(chatroom);
            serverThread.start();
            
            //log("Created chatroom '" + chatroomName + "' on port " + port);
            
            // Clear input fields
            //chatroomNameField.setText("");
            //portField.setText("");
        } catch (IOException ex) {
        	System.out.println(ex);
            //log("Error creating chatroom '" + chatroomName + "': " + ex.getMessage());
        }
    }

    
    public static void main(String[] args) {
    	
    	String[] trends;
		try {
			trends = UtilFunctions.getTopTrends();
	    	String[] portList = {"9999","9998","9997","9996"};
	    	
	    	for (int i = 0; i < 4; i++) {	

	    	createChatroom(trends[i], portList[i]);
	    	}
	    	
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	

    	
    	
    	SwingUtilities.invokeLater(() ->  Server.getInstance());
        
    }
    

    public static class ChatroomServer implements Runnable {
        private String chatroomName;
        private int port;
        private ArrayList<ConnectionHandler> connections;
        private ServerSocket server;
        private boolean done;
        private ExecutorService pool;
        private Map<String,ChatroomServer> chatrooms;

        
        public ChatroomServer(String chatroomName, int port) throws IOException {
            this.chatroomName = chatroomName;
            this.port = port;
            this.connections = new ArrayList<>();
            this.done = false;
            this.server = new ServerSocket(port);
            this.pool = Executors.newCachedThreadPool();
            this.chatrooms = Server.chatrooms;

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
                    
                    ConnectionHandler handler = new ConnectionHandler(clientSocket);
                    connections.add(handler);
                    pool.execute(handler);
                }
            } catch (IOException e) {
                if (!done) {
                    shutdown();
                }
            }
        }
        
        public void broadcast(String message) {
            for (ConnectionHandler ch : connections) {
                if (ch != null) {
                    ch.sendMessage(message);
                }
            }
        }
        
        public void shutdown() {
            try {
                done = true;
                
                if (server != null && !server.isClosed()) {
                    server.close();
                }
                
                for (ConnectionHandler ch : connections) {
                    ch.shutdown();
                }
                
                pool.shutdown();
                

                chatrooms.remove(chatroomName);
            } catch (IOException e) {
            }
        }
        
        class ConnectionHandler implements Runnable {
            private Socket clientSocket;
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
                    

                    connections.remove(this);
                } catch (IOException e) 
                {

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
