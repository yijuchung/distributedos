import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

public class Start{
	
	public static void main(String[] args) {

		Properties prop = new Properties();
		try {
			prop.load(new FileInputStream("system.properties"));
		} catch (FileNotFoundException e) {
			System.out.println(e.getMessage());
		    System.out.flush();
		} catch (IOException e) {
			System.out.println(e.getMessage());
		    System.out.flush();
		}
		
		String sAddServer = prop.getProperty("RW.server");
		String path = System.getProperty("user.dir");
		Process tRoot = null;
		
		try {
			tRoot = Runtime.getRuntime().exec("ssh -o StrictHostKeyChecking=no "+sAddServer+" cd " + path + 
					"; java -Djava.security.policy=rmi.policy Server");
		} catch (IOException e) {
			System.out.println(e.getMessage());
		    System.out.flush();
		}
		try {
			tRoot.waitFor();
		} catch (InterruptedException e) {
			System.out.println(e.getMessage());
		    System.out.flush();
		}
		return;
	}

}
