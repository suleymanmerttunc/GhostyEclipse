package com.example.ghosty;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import javafx.application.Platform;

public class Client 
{
	private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private String nickname;
    private boolean connected;
    private MainGUI gui;
    
    public Client(MainGUI gui) {
        this.gui = gui;
        this.connected = false;
    }
    
    
	//server'a bağlanmayı sağlayan fonksiyon
    public boolean connect(String serverAddress, int serverPort, String nickname) {
        try {
            this.socket = new Socket(serverAddress, serverPort);
            this.out = new PrintWriter(socket.getOutputStream(), true);
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.nickname = nickname;
            this.connected = true;
            
            // Serverdan gelen mesajları dinlemek için bir Thread başlat ve listenforMessages metodunu çalıştır.
            new Thread(this::listenForMessages).start();
            
            return true;
        } catch (IOException e) {
            gui.displayMessage("Error connecting to server: " + e.getMessage());
            return false;
        }
    }
    
    
    
    
    private void listenForMessages() {
        
    	//Bir client servara bağlandığında çalıştırdığı metod. servardan gelen mesajların okunmasıyla ve servarın clientın nickini öğrenmesini sağlıyor.
    	try {
            
    		/*server kodu bir client bağlandığında o clienttan aldığı ilk mesajı clientın nicki olarak kabul ediyor, 
    		 * o yüzden bağlantı sağlandığında clientın yaptığı ilk şey servara nickini göndermek olmalı
    		
    		*/
    		String serverMessage;
            out.println(nickname); // servar'a nickini gönder
            
            while (this.connected && (serverMessage = in.readLine()) != null) {
                final String message = serverMessage;
                // serverdan gelen mesajın guide gösterilmesini sağlıyor
                Platform.runLater(() -> {
                    gui.displayMessage(message);
                });
            }
        } catch (IOException e) {
        	//eğer serverla bağlantıda bir hata oluştuysa bağlantıyı kesiyor.
            if (this.connected) {
                gui.displayMessage("Disconnected from server: " + e.getMessage());
                disconnect();
            }
        }
    }
 
    public void sendMessage(String message) {
        if (connected && out != null) {
            out.println(message);
        } else {
            gui.displayMessage("Not connected to server!");
        }
    }
    
    public void changeNickname(String newNickname) {
        if (connected) {
            sendMessage("/nick " + newNickname);
        }
    }
    
    //serverdan çıkma
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
                gui.displayMessage("Disconnected from server.");
            } catch (IOException e) {
                gui.displayMessage("Error during disconnect: " + e.getMessage());
            }
        }
    }
    
    public boolean isConnected() {
        return connected;
    }
}
	

