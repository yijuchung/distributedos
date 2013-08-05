import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RW extends Remote {
	int registerClient(Notify c) throws RemoteException;
	
	void sendRequest(String si, int iIndex, int iSleepTime) throws RemoteException;
	void updateFinishedTime(int iIndex) throws RemoteException;
	
	int getCurrentTime() throws RemoteException;
	
	void doneClient( int iIndex ) throws RemoteException;
}
