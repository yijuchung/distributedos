import java.rmi.Remote;
import java.rmi.RemoteException;

public interface Notify extends Remote {
	void doOperation() throws RemoteException;
	void setFinishedIndex(int iIndex) throws RemoteException;
	int getType() throws RemoteException;
	void setStartTime(int iLaunchTime) throws RemoteException;
}