import java.net.*;
import java.io.*;

public class Child implements Runnable {
	public int iBoolInx;
	public Socket sSocket;
	
	public Child(Socket sk, int inx)
	{
		this.sSocket = sk;
		this.iBoolInx = inx;
	}
	
	public void run() {
		
		ObjectInputStream in = null;
		try {
			in = new ObjectInputStream(sSocket.getInputStream());
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} 
		
		while(true){
			try {
				Msg mTemp = (Msg)in.readObject();
				if(mTemp.iId == 1)
					Comm.bChild.set(iBoolInx, true);
				else{
					// update time
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}
