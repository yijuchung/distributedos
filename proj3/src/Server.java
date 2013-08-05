import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import java.net.MalformedURLException;
import java.rmi.AlreadyBoundException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;
        
public class Server{
	
	public static String sAddServer;
	public static int iPort;
	public static int iWriter;
	public static int iReader;
	public static boolean bStopServer;
	public static int iLaunchTime;
	
	public static ArrayList<Req> arrFinishedReq;
	public static AtomicInteger iObjVal;
        
    public Server() {}
    
    public static void main(String args[]) {
    	
    	PrintStream console = System.out;
	    PrintStream out = null;
	    
		try {
			out = new PrintStream(new BufferedOutputStream(new FileOutputStream("event.log")));
		} catch (FileNotFoundException e1) {
			System.out.println(e1.getMessage());
		    System.out.flush();
		}
	    System.setOut(out);
        
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
		
		prop.list(out);
		
		iPort = Integer.parseInt(prop.getProperty("Rmiregistry.port").trim());
		sAddServer = prop.getProperty("RW.server").trim();
		
		iWriter = Integer.parseInt(prop.getProperty("RW.numberOfWriters").trim());
		iReader = Integer.parseInt(prop.getProperty("RW.numberOfReaders").trim());
		
		arrFinishedReq = new ArrayList<Req>();
		
		String path = System.getProperty("user.dir");
		
		try {
			LocateRegistry.createRegistry(iPort);
		} catch (IOException e1) {
			System.out.println(e1.getMessage());
		    System.out.flush();
		    return;
		}
		
		System.out.println("2");
	    System.out.flush();
		
		Thread t = null;
		ServerThread obj = null;
		try {
			System.out.println("2-1");
		    System.out.flush();
			obj = new ServerThread();
			System.out.println("2-2");
		    System.out.flush();
		} catch (RemoteException e2) {
			System.out.println("re "+e2.getMessage());
		    System.out.flush();
		}
		
		System.out.println("3");
	    System.out.flush();
		
		try {
			bStopServer = false;
			iObjVal = new AtomicInteger(-1);
			String s = "rmi://"+sAddServer+":"+iPort+"/yjchung";
			System.out.println(s);
		    System.out.flush();
			Naming.bind(s, obj);
		} catch (MalformedURLException e) {
			System.out.println("malurl "+e.getMessage());
		    System.out.flush();
		} catch (RemoteException e) {
			System.out.println("re "+e.getMessage());
		    System.out.flush();
		} catch (AlreadyBoundException e) {
			System.out.println("ab "+e.getMessage());
		    System.out.flush();
		}
		
		System.out.println("4");
	    System.out.flush();
		
		t = new Thread(obj);
		t.start();
		
		for(int i = 1;i<=iReader;i++){
			String sProcName = "RW.reader"+i;
			System.out.println(sProcName);
		    System.out.flush();
						
			String s = "ssh -o StrictHostKeyChecking=no "+prop.getProperty(sProcName).trim()
					+" cd " + path + "; java -Djava.security.policy=rmi.policy Client "+sProcName+" 1 "+i;
			System.out.println(s);
			System.out.flush();
			try {
				Runtime.getRuntime().exec(s);
			} catch (IOException e) {
				System.out.println(e.getMessage());
			    System.out.flush();
			}
		}
		
		System.out.println("c");
        System.out.flush();
		
		for(int i = 1;i<=iWriter;i++){
			int iNext = i+iReader;
			String sProcName = "RW.writer"+iNext;
			System.out.println(sProcName);
		    System.out.flush();
		    
		    String s = "ssh -o StrictHostKeyChecking=no "+prop.getProperty(sProcName).trim()
					+" cd " + path + "; java -Djava.security.policy=rmi.policy Client "+sProcName+" 0 "+iNext;
			System.out.println(s);
		    System.out.flush();
		    
			try {
				Runtime.getRuntime().exec(s);
			} catch (IOException e) {
				System.out.println(e.getMessage());
			    System.out.flush();
			}
		}
		
		System.out.println("f");
        System.out.flush();
		
        int iTotal = iWriter+iReader;
        
		while(true){
			if(ServerThread.arrClient.size() != iTotal){
				try {
					Thread.sleep(50);
				} catch (InterruptedException e) {
					System.out.println(e.getMessage());
				    System.out.flush();
				}
				continue;
			}
			
			//System.out.println(ServerThread.arrClient.get(0)+" "+ServerThread.arrClient.get(1));
	        //System.out.flush();
			
			boolean bAllDone = true;
			for(int i = 0;i<ServerThread.arrClient.size();i++){
				if( !ServerThread.arrBoolClient.get(i) ){
					//System.out.println(i+": not done yet");
			        //System.out.flush();
					bAllDone = false;
					break;
				}
			}
			
			if(bAllDone)
				break;
			
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
				System.out.println(e.getMessage());
			    System.out.flush();
			}
		}
		
		System.out.println("g");
        System.out.flush();
		
		out.close();
		System.setOut(console);
		
		if( t != null ){
			bStopServer = true;
			try {
				t.join();
			} catch (InterruptedException e) {
				System.out.println(e.getMessage());
			    System.out.flush();
			}
		}
		
		console = System.out;
	    out = null;
	    
		try {
			out = new PrintStream(new BufferedOutputStream(new FileOutputStream("crew.log")));
		} catch (FileNotFoundException e1) {
			System.out.println(e1.getMessage());
		    System.out.flush();
		}
	    System.setOut(out);
	    
	    Collections.sort(arrFinishedReq);
	    System.out.println("ServiceSequence\tObjectValue\tReadby\tOptTime");
	    
	    Iterator<Req> it = arrFinishedReq.iterator();
	    int i = 1;
	    while(it.hasNext()){
	    	Req r = it.next();
	    	String s = i+"\t"+r.iObjVal+"\t";
	    	if(r.iType == 0){
	    		s += "W"+r.sClientIndex+"\t";
	    	}else
	    		s += "R"+r.sClientIndex+"\t";
	    	
	    	int iStart = r.iStartTime - iLaunchTime;
	    	int iEnd = r.iEndTime - iLaunchTime;
	    	
	    	s += iStart+"-"+iEnd;
	    	System.out.println(s);
	    	i++;
	    }
	    
	    out.close();
		System.setOut(console);
		
		try {
			Registry reg = LocateRegistry.getRegistry(iPort);
			reg.unbind("rmi://"+sAddServer+":"+iPort+"/yjchung");
			UnicastRemoteObject.unexportObject(obj, true);
		} catch (Exception e) {
			System.out.println(e.getMessage());
		    System.out.flush();
		}
		
		System.exit(0);
    }
}
