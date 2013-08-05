import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.Semaphore;

public class Start {
	
	public static int iID;
	public static int iMultiPort;
	public static String sMultiAddr;
	
	public static int iUniPort;
	
	public static int iTotalClient;
	public static int iReq;
	public static int iRejPro;
	public static int iSleep;
	
	public static int iMsgHeaderSize;
	
	public static Semaphore sFirst;
	public static Semaphore semaSeqVec;
	public static Semaphore semaReqDone;
	
	public static Properties prop;
	
	public static InetAddress iaGroup;
	public static MulticastSocket msGroup;
	public static DatagramSocket dsUniSocket;
	
	public static SeqVec sv;
	
	public static boolean bExit;
	public static ACKtable ackTable;
	public static ACKtable reqTable;
	public static BufferMsg bmQueue;
	
	public static PrintStream pLog; 
	
	public static void sendMultiUDP( Msg m ){
		
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream oos;
		
		try {
			oos = new ObjectOutputStream(baos);
			oos.writeObject(m);
			oos.flush();
		} catch (IOException e) {
			System.out.println(e.getMessage());
		    System.out.flush();
		}
		
		byte[] Buf = baos.toByteArray();
		
	    DatagramPacket dp = new DatagramPacket(Buf, Buf.length, iaGroup, iMultiPort);
	    
	    try {
	    	msGroup.send(dp);
		} catch (SocketException e) {
			System.out.println(e.getMessage());
		    System.out.flush();
		} catch (IOException e) {
			System.out.println(e.getMessage());
		    System.out.flush();
		}
	    
		//System.out.println("multicast payload size "+Buf.length);
	    //System.out.flush();
	}
	
	public static void sendUniUDP( Msg m, int iTargetID ){
		InetAddress iaClient = null;
		try {
			iaClient = InetAddress.getByName(prop.getProperty("Client"+iTargetID).trim());
		} catch (UnknownHostException e) {
			System.out.println(e.getMessage());
		    System.out.flush();
		}
				
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream oos;
		try {
			oos = new ObjectOutputStream(baos);
			oos.writeObject(m);
			oos.flush();
		} catch (IOException e) {
			System.out.println(e.getMessage());
		    System.out.flush();
		}
		
		byte[] Buf = baos.toByteArray();
	    DatagramPacket dp = new DatagramPacket(Buf, Buf.length, iaClient, Integer.parseInt(prop.getProperty("Client"+iTargetID+".port").trim()));
	    
		try {
			DatagramSocket dsTemp = new DatagramSocket( );
		    dsTemp.send(dp); 
		    dsTemp.close( );
		} catch (SocketException e) {
			System.out.println(e.getMessage());
		    System.out.flush();
		} catch (IOException e) {
			System.out.println(e.getMessage());
		    System.out.flush();
		}
	    
		//System.out.println("send to "+dp.getAddress()+":"+dp.getPort()+" payload size "+Buf.length);
	   // System.out.flush();
	}

	public static void main(String[] args) {
		
		prop = new Properties();
		sFirst = new Semaphore(0);
		semaSeqVec = new Semaphore(1);
		semaReqDone = new Semaphore(0);
		
		try {
			prop.load(new FileInputStream("system.properties"));
		} catch (FileNotFoundException e) {
			System.out.println(e.getMessage());
		    System.out.flush();
		} catch (IOException e) {
			System.out.println(e.getMessage());
		    System.out.flush();
		}
		
		sMultiAddr = prop.getProperty("Multicast.address").trim();
		iMultiPort = Integer.parseInt(prop.getProperty("Multicast.port").trim());
		iTotalClient = Integer.parseInt(prop.getProperty("ClientNum").trim());
		iReq = Integer.parseInt(prop.getProperty("numberOfRequests").trim());

		iID = Integer.parseInt(args[0].trim());
		
		iRejPro = Integer.parseInt(prop.getProperty("Client"+String.valueOf(iID)+".RejectProbability").trim());
		iSleep = Integer.parseInt(prop.getProperty("Client"+String.valueOf(iID)+".sleepTime").trim());
		iUniPort = Integer.parseInt(prop.getProperty("Client"+String.valueOf(iID)+".port").trim());
		
		sv = new SeqVec(iTotalClient);
		ackTable = new ACKtable(iTotalClient);
		reqTable = new ACKtable(iTotalClient);
		bmQueue = new BufferMsg();
		
		try {
			pLog = new PrintStream(new FileOutputStream("request"+iID+".log"));
		} catch (FileNotFoundException e) {
			System.out.println(e.getMessage());
		    System.out.flush();
		}
		
		try {
			dsUniSocket = new DatagramSocket(iUniPort);
		} catch (SocketException e) {
			System.out.println(e.getMessage());
		    System.out.flush();
		}
		
		try {
			iaGroup = InetAddress.getByName(sMultiAddr);
		} catch (UnknownHostException e) {
			System.out.println(e.getMessage());
		    System.out.flush();
		}
		
		try {
			msGroup = new MulticastSocket(iMultiPort);
			msGroup.joinGroup(iaGroup);
		} catch (IOException e) {
			System.out.println(e.getMessage());
		    System.out.flush();
		}
		
		System.out.println("Process"+iID+" Started");
		pLog.println("Process"+iID+" Started");
		System.out.flush();
		
		Thread tUDP = new Thread(new UDPServer());
		tUDP.start();
		
		bExit = false;
		Thread tMultiUDP = new Thread(new MulticastServer());
		tMultiUDP.start();
		
		if(iID != 1){
			// other clients notify client 1
			Msg m = new Msg(iID,1);
			
			sendUniUDP(m,1);
		}
		
		try {
			sFirst.acquire();
		} catch (InterruptedException e) {
			System.out.println(e.getMessage());
		    System.out.flush();
		}
		
		if(iID == 1){
			// client 1 notify other clients
			
			for( int i = 2;i<iTotalClient+1;i++){
				Msg m = new Msg(iID,2);
				sendUniUDP(m,i);
			}
		}
		
		System.out.print("Initial Sequence Vector:");
		pLog.print("Initial Sequence Vector:");
		sv.printSeqVec();
		
		int iCurReq = 0;
		
		while(iCurReq < iReq){
			
			Random r = new Random();
			int iRand = r.nextInt(iSleep)+1;
			try {
				Thread.sleep(iRand);
			} catch (InterruptedException e) {
				System.out.println(e.getMessage());
			    System.out.flush();
			}
			
			try {
				semaSeqVec.acquire();
			} catch (InterruptedException e) {
				System.out.println(e.getMessage());
			    System.out.flush();
			}
			
			sv.increment(iID-1);
			
			String sTrans = sv.Serialize();
			
			Msg m = new Msg(iID,3,sTrans);			
			semaSeqVec.release();
			
			ackTable.reset();
			ackTable.setRecv(iID-1);
			
			sendMultiUDP(m);
			
			int iStartMulti = (int)System.currentTimeMillis();
			while(true){
				int iCurrentTime = (int)System.currentTimeMillis();
				
				if((iCurrentTime - iStartMulti) > 500){
					iStartMulti = iCurrentTime;
					if(ackTable.checkAll())
						break;
					else{
						m = new Msg(iID,3,sTrans);
						ackTable.reset();
						ackTable.setRecv(iID-1);
						
						sendMultiUDP(m);
					}
				}
			}
			
			iCurReq++;
		}
		
		if(iID == 1){
			// client 1
			reqTable.setRecv(iID-1);
			
			while(true){
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					System.out.println(e.getMessage());
					System.out.flush();
				}
				if(reqTable.checkAll()){
					break;
				}
			}
			System.out.print("Current Sequence Vector:");
			pLog.print("Current Sequence Vector:");
			sv.printSeqVec();
			System.out.println("All Requests received");
			Start.pLog.println("All Requests received");
			System.out.flush();
			Start.pLog.flush();
			
			Msg mDone = new Msg(iID,8);
			for(int i = 1;i<iTotalClient;i++){
				sendUniUDP(mDone,i+1);
			}
			
		}else{
			// other client
			//System.out.println(bmQueue.vmQueue.size());
		    //System.out.flush();
			System.out.print("Current Sequence Vector:");
			pLog.print("Current Sequence Vector:");
			sv.printSeqVec();
			System.out.println("All Requests received");
			Start.pLog.println("All Requests received");
			System.out.flush();
			Start.pLog.flush();
			Msg mDone = new Msg(iID,7);
			//System.out.println("send all done to Client 1");
		    System.out.flush();
		    pLog.flush();
			sendUniUDP(mDone,1);
			
			try {
				semaReqDone.acquire();
			} catch (InterruptedException e) {
				System.out.println(e.getMessage());
				System.out.flush();
			}
		}
		bExit = true;
		
		try {
			msGroup.leaveGroup(iaGroup);
		} catch (IOException e) {
			System.out.println(e.getMessage());
			System.out.flush();
		}
		msGroup.close();
		dsUniSocket.close();
		System.out.flush();
	    pLog.flush();
		pLog.close();
		
		while(tUDP.isAlive()){
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				System.out.println(e.getMessage());
				System.out.flush();
			}
		}
		
		while(tMultiUDP.isAlive()){
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				System.out.println(e.getMessage());
				System.out.flush();
			}
		}
		
		System.exit(0);
	}

}
