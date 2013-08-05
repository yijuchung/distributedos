import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Properties;
import java.util.concurrent.Semaphore;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class Client extends UnicastRemoteObject implements Notify, Runnable{

	protected Client() throws RemoteException {
		super();
	}

	private static final long serialVersionUID = 1L;
	
	public RW server;
	public static String sName;
	public static String iName;
	public static String sAddServer;
	public static int iPort;
	
	public static int iIndex;
	public static int iSleep;
	public static int iOp;
	public int iType;
	// 0 writer, 1 reader
	public static int iAccessTime;
	public static int iStartTime;
	
	public Semaphore semaReq;
	public Semaphore semaOp;
	public Semaphore semaStart;
	public int iFinishedIndex;
	
	public void setStartTime(int iTime) throws RemoteException {
		iStartTime = iTime;
		semaStart.release();
	}
	
	public int getType() throws RemoteException {
		return iType;
	}
	
	public static void main(String[] args) {
		// Client [sName] [iType] [iName]
				
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
		
		iPort = Integer.parseInt(prop.getProperty("Rmiregistry.port").trim());
		sAddServer = prop.getProperty("RW.server").trim();
		sName = args[0].trim();
		iName = args[2].trim();
		
		iSleep = Integer.parseInt(prop.getProperty(sName+".sleepTime").trim());
		iOp = Integer.parseInt(prop.getProperty(sName+".opTime").trim());
		iAccessTime = Integer.parseInt(prop.getProperty("RW.numberOfAccesses").trim());

		Thread t = null;
		
		try {
			Client c = new Client();
			c.iType = Integer.parseInt(args[1].trim());
			t = new Thread(c);
			t.start();
		} catch (RemoteException e) {
			System.out.println("Client exception: " + e.toString());
            System.out.flush();
		}
		
		if(t != null){
			try {
				t.join();
			} catch (InterruptedException e) {
				System.out.println(e.toString());
	            System.out.flush();
			}
		}
	}
	
	public void doOperation() throws RemoteException {
		
		try {
			Thread.sleep(iOp);
		} catch (InterruptedException e) {
			System.out.println("op " + e.toString());
            System.out.flush();
		}
		/*
		int iLastTime = (int)System.currentTimeMillis();
	    
	    while(true){
	    	
	    	int iCurTime = (int)System.currentTimeMillis();
	    	
	    	if((iCurTime - iLastTime) >= iOp)
	    	{
	    		break;
	    	}
	    }*/
		
		this.semaOp.release();
	}
	
	public void setFinishedIndex(int iIndex) throws RemoteException {
		this.iFinishedIndex = iIndex;
		this.semaReq.release();
	}

	public void run() {
		
		this.semaReq = new Semaphore(0);
		this.semaOp = new Semaphore(0);
		this.semaStart = new Semaphore(0);
		
		PrintStream console = System.out;
	    PrintStream out = null;
	    
		try {
			out = new PrintStream(new BufferedOutputStream(new FileOutputStream(sName+"_event.log")));
		} catch (FileNotFoundException e1) {
			System.out.println(e1.getMessage());
		    System.out.flush();
		}
	    System.setOut(out);
	    
 		try {
 			String s = "rmi://"+sAddServer+":"+iPort+"/yjchung";
			System.out.println(s);
		    System.out.flush();
			this.server = (RW) Naming.lookup(s);
		} catch (MalformedURLException e2) {
			System.out.println("mal"+e2.getMessage());
		    System.out.flush();
		} catch (RemoteException e2) {
			System.out.println("er "+e2.getMessage());
		    System.out.flush();
		} catch (NotBoundException e2) {
			System.out.println("nb "+e2.getMessage());
		    System.out.flush();
		}
 		
 		System.out.println("1");
	    System.out.flush();
 		
        try {
			iIndex = this.server.registerClient(this);
		} catch (RemoteException e2) {
			System.out.println("ne2 "+e2.getMessage());
		    System.out.flush();
		}
        
        System.out.println("2");
	    System.out.flush();
	    
	    try {
			semaStart.acquire();
		} catch (InterruptedException e2) {
			System.out.println("start "+e2.getMessage());
		    System.out.flush();
		}
	    
	    int iCur = (int)System.currentTimeMillis();
	    while(true){
	    	if(iCur >= Client.iStartTime)
	    		break;
	    	else{
	    		iCur = (int)System.currentTimeMillis();
	    	}
	    }
	    
	    System.out.println("Client start time :"+iCur);
	    System.out.flush();
 		
 		// begin throw requests
	    
	    int iTry = iAccessTime;
	    
	    while(iTry > 0){
	    	
	    	try {
	    		System.out.println("begin req at "+((int)System.currentTimeMillis()-iCur));
	 			this.server.sendRequest(iName, iIndex,iSleep);
	 			System.out.println("end req at "+((int)System.currentTimeMillis()-iCur));
	 		    System.out.flush();
	 			this.semaOp.acquire();
	 			System.out.println("4");
	 		    System.out.flush();
	 		    this.semaReq.acquire();
	 			this.server.updateFinishedTime(this.iFinishedIndex);
	 		} catch (Exception e) {
	            System.out.println("send " + e.toString());
	            System.out.flush();
		    }
	 		
	 		System.out.println("5");
		    System.out.flush();
		    
		    iTry--;
	    }
 		
 		// end
 		
 		try {
 			this.server.doneClient(iIndex);
 		} catch (RemoteException e) {
 			System.out.println("done " + e.toString());
            System.out.flush();
 		}
 		
 		System.out.println("6");
	    System.out.flush();
	    
	    out.close();
		System.setOut(console);
		
		System.exit(0);
	}
}