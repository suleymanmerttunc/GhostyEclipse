import javax.swing.SwingUtilities;

public class Main {

	public static void main(String[] args) 
	{
		
		//uygulamayı çalıştırmak server kodunu çalıştırın, daha sonra 9999 numaralı porta bağlanın.
        SwingUtilities.invokeLater(() -> {
            ClientGUI gui = new ClientGUI();
        });
	}

}
