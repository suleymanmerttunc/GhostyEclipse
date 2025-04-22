import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server implements Runnable {
	
	private ArrayList<ConnectionHandler> connections;
	private ServerSocket server;
	private boolean done;
	private ExecutorService pool;
	
	
	public Server() 
	{
		connections = new ArrayList<>();
		done = false;
		
	}
	@Override
	
	// run metodu javada default gelen Runnable interface'inin bir gereksinimi, Multithreading yapılırken kullanılıyor. run metodunu normal bir classtaki main metodu gibi düşünebilirsiniz.
	
	public void run() 
	{
		
		/*
		 * Server'in ana mantığının olduğu blok. 9999 numaralı portta server socket açıyor, bağlantıları kabul ediyor,
		 * bağlanan clientların listesini tutuyor, her bir client için ConnectionHandler oluşturup yeni bir threadde çalıştırıyor.
		
		*/
		try {	
		server = new ServerSocket(9999);
		pool = Executors.newCachedThreadPool();	
		while(!done) {
		Socket client_socket = server.accept(); // Client bağlanmasını bekle, bağlantı olursa kabul et
		ConnectionHandler handler = new ConnectionHandler(client_socket);
		connections.add(handler); // mevcut bağlantılar listesine bağlantıyı ekle
		pool.execute(handler); 
		}
		
		}
		catch (Exception e)
		{
			
			
			shutdown();
		}
	
	}
	// Bu fonksiyon bağlı olan bütün Clientlara aynı mesajı gönderiyor.
	public void broadcast(String message) 
	{
		for(ConnectionHandler ch: connections) 
		{
			
			if (ch != null) 
			{
				ch.sendMessage(message);
			}

		}
	}
	
	/*serveri güvenli bir şekilde kapatıyor. ConnectionHandler'daki shutdown ile karıştırmayın, bu server'ın kapatılma fonksiyonu, ConnectionHandler'daki shutdown Clientın bağlantısını kapatıyor.
	İsimlerinin aynı olduğnu sonra fark ettim, onu değiştirsem daha okunur bir kod olur.
	*/
	public void shutdown() 
	{
		try {
		done = true;
		if (!server.isClosed()) 
		{
			server.close();
		}
		
		for (ConnectionHandler ch : connections) 
		{
			ch.shutdown();
		}
		
		}
		catch (IOException e) {
				e.printStackTrace();
			}
			


	}
	
	/* Her bir client bağlantısıyla ilgilenen class. Yani her bir Client'ın serverla bağlantısını denetleyen bir ConnectionHandler'ı var. 

	 * client_socket değişkeni clientın bağlı olduğu socket, 
	 * in clientın gönderdiği mesajların okunmasını sağlayan değişken, 
	 * out bir mesaj göndermeyi sağlayan değişken.
	 * nickname ilgilendiği clientın nickname'i.
	*/
	class ConnectionHandler implements Runnable 
	{

		private Socket client_socket;
		private BufferedReader in;
		private PrintWriter out;
		private String nickname;
		
		public ConnectionHandler(Socket client_socket) 
		{
			this.client_socket = client_socket;
		
		}
	
		@Override
		public void run() 
		{
			
			try 
			{
				out = new PrintWriter(client_socket.getOutputStream(), true); // serverın clienta mesaj göndermesini sağlayan değişken
				in = new BufferedReader(new InputStreamReader(client_socket.getInputStream())); // servara gelen mesajların okunmasını sağlayan değişken
				
				nickname = in.readLine(); // client'ın gönderdiği ilk mesaj clientın nicki kabul ediliyor. Client kodunda bunu açıkladım

				broadcast(nickname + " joined chat!");
				String message;
				while((message = in.readLine()) != null ) 
				{
					//  /nick (yeninick) yazdığında nickini değiştirme özelliği (parantezler olmadan yapılıyor)
					if(message.startsWith("/nick ")) 
					{
						String[] messageSplit = message.split(" ",2);
						if (messageSplit.length == 2) 
						{
							broadcast(nickname + " renamed themselves to " + messageSplit[1]);
							nickname = messageSplit[1];
							out.println("Successfully changed nickname to " + nickname);
						}
						else
						{
							out.println("didnt provide a nick!");
						}
					}
					else if (message.startsWith("/quit")) 
					{
						broadcast(nickname + " left the chat!");
						shutdown();
					}

					else 
					
					{
						//eğer clientın gönderdiği mesaj / ile başlayan özel kodlu mesajlardan değilse yazdığı şeyi nickiyle birlikte bağlanan herkese gönder
						
						broadcast(nickname + ": " +message);
					}
				}
			} 
			
			catch (IOException e) {
				shutdown();
			}
		}
		public void shutdown() {
			try {
			in.close();
			out.close();
			if (!client_socket.isClosed()) 
			{
				client_socket.close();
			}
			
		}
			catch(IOException e) 
			{
				//TODO: Handle
			}
		}

		
		public void sendMessage(String message) 
		{
			out.println(message);
		}
	}

	
	//Server'in initilize edilmesi
	public static void main(String[] args) 
	{
		Server server = new Server();
		server.run();
	}

	
}

