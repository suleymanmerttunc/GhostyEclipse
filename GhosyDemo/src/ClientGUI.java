import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class ClientGUI {
    private JFrame frame;
    private JTextField inputField;
    private JButton submitButton;
    private JTextArea outputArea;
    private Client client;
    
    public ClientGUI() {

        frame = new JFrame("Chat Client");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(500, 400);
        frame.setVisible(true);
        
        // Output area
        outputArea = new JTextArea();
        outputArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(outputArea);
        
        // Input field and submit button
        inputField = new JTextField(30);
        submitButton = new JButton("Send");
        
        // Layout
        JPanel inputPanel = new JPanel();
        inputPanel.add(inputField);
        inputPanel.add(submitButton);
        
        frame.getContentPane().add(scrollPane, BorderLayout.CENTER);
        frame.getContentPane().add(inputPanel, BorderLayout.SOUTH);
        
        // Initialize the client
        client = new Client(this);
        
        // Add action listener to submit button
        submitButton.addActionListener(e -> {
            if (client.isConnected()) {
                String message = inputField.getText();
                
                if (message.equals("/nuke")) 
                {
                	nukeChatHistory();
                	inputField.setText("");
                	return;
				}
                
                if (!message.isEmpty()) {
                    client.sendMessage(message);
                    inputField.setText("");
                }
            } else {
                displayMessage("Not connected to server! Click Connect first.");
            }
        });
        
        // Create a menu for connection options
        JMenuBar menuBar = new JMenuBar();
        JMenu connectionMenu = new JMenu("Connection Settings");
        
        JMenuItem connectItem = new JMenuItem("Connect");
        connectItem.addActionListener(e -> showConnectDialog());
        
        JMenuItem disconnectItem = new JMenuItem("Disconnect");
        disconnectItem.addActionListener(e -> client.disconnect());
        
        JMenuItem changeNickItem = new JMenuItem("Change Nickname");
        changeNickItem.addActionListener(e -> showChangeNicknameDialog());
        
        connectionMenu.add(connectItem);
        connectionMenu.add(disconnectItem);
        connectionMenu.add(changeNickItem);
        menuBar.add(connectionMenu);
        frame.setJMenuBar(menuBar);
        
        // Handle window closing
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (client.isConnected()) {
                    client.disconnect();
                }
            }
        });
    }
    
    
    private void showConnectDialog() {
    	
    	if (client.isConnected()) 
    	{
    		return;
		}
        JTextField serverField = new JTextField("localhost");
        JTextField portField = new JTextField("9999");
        JTextField nicknameField = new JTextField();
        
        JPanel panel = new JPanel(new GridLayout(0, 1));
        panel.add(new JLabel("Server:"));
        panel.add(serverField);
        panel.add(new JLabel("Port:"));
        panel.add(portField);
        panel.add(new JLabel("Nickname:"));
        panel.add(nicknameField);
        
        int result = JOptionPane.showConfirmDialog(frame, panel, "Connect to Server", 
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        
        if (result == JOptionPane.OK_OPTION) {
            String server = serverField.getText();
            int port;
            try {
                port = Integer.parseInt(portField.getText());
            } catch (NumberFormatException e) {
                displayMessage("Invalid port number");
                return;
            }
            String nickname = nicknameField.getText();
            
            if (nickname.isEmpty()) {
                displayMessage("Nickname cannot be empty");
                return;
            }
            
            displayMessage("Connecting to " + server + ":" + port + "...");
            new Thread(() -> {
                boolean connected = client.connect(server, port, nickname);
                if (connected) {
                    SwingUtilities.invokeLater(() -> 
                        displayMessage("Connected to server as " + nickname)
                    );
                }
            }).start();
        }
    }
    
    private void showChangeNicknameDialog() {
        
    	if (!client.isConnected()) 
    	{
    		displayMessage("Cannot change name if not connected!");
    		return;
		}
    	String newNickname = JOptionPane.showInputDialog(frame, "Enter new nickname:");
        if (newNickname != null && !newNickname.isEmpty()) {
            client.changeNickname(newNickname);
        }
    }
    
    public void displayMessage(String message) {
        outputArea.append(message + "\n");
        // Auto-scroll to bottom
        outputArea.setCaretPosition(outputArea.getDocument().getLength());
    }
    
    public void nukeChatHistory() 
    {
    	outputArea.setText("");
    	
    }
    

}