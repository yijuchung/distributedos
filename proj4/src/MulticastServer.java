import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.DatagramPacket;
import java.util.Random;


public class MulticastServer implements Runnable{
	
	public static int compareSeqVec( Msg m ){
		/*
		 * 1:accept
		 * 2:reject
		 * 3:buffer
		 */
		try {
			Start.semaSeqVec.acquire();
		} catch (InterruptedException e) {
			System.out.println(e.getMessage());
		    System.out.flush();
		}
		
		SeqVec sv = new SeqVec();
		sv.deSerialize(m.sSeqVec);
		
		int iMsgVec = sv.vSeq.get(m.iFromID-1);
		int iVec = Start.sv.vSeq.get(m.iFromID-1);
		
		if(iMsgVec == iVec+1){
			boolean bCheck = true;
			for(int i = 0;i<Start.iTotalClient;i++){
				if(i == m.iFromID-1)
					continue;
				
				if(Start.sv.vSeq.get(i) < sv.vSeq.get(i) ){
					bCheck = false;
					break;
				}
			}
			
			Start.semaSeqVec.release();
			if(bCheck)
				return 1;
			else
				return 3;
		}else if( iMsgVec > iVec+1 ){
			Start.semaSeqVec.release();
			return 3;
		}else
			Start.semaSeqVec.release();
			return 2;
		
		
	}
		
	public Msg recvMultiUDP(){
		byte[] buf = new byte[256];
        DatagramPacket recvdp = new DatagramPacket(buf, buf.length);
        
		try {
			Start.msGroup.receive(recvdp);
		} catch (IOException e) {
			return null;
		}
        
		ByteArrayInputStream baos = new ByteArrayInputStream(buf);
	    ObjectInputStream oos = null;
	    Msg m = null;
		
		try{
			oos = new ObjectInputStream(baos);
			m = (Msg)oos.readObject();
			baos.close();
			oos.close();
		}catch (ClassNotFoundException e) {
			return null;
		} catch (IOException e) {
			return null;
		}
		
		return m;
	}
	
	public void run() {
		while(!Start.bExit){
			Msg m = (Msg)recvMultiUDP();
			if(m == null)
				continue;
			
			if(m.iFromID == Start.iID)
				continue;
			
			System.out.println("Process "+Start.iID+" Received request from ID "+m.iFromID);
			System.out.println("Sequence Vector Received: "+m.sSeqVec);
			Start.pLog.println("Process "+Start.iID+" Received request from ID "+m.iFromID);
			Start.pLog.println("Sequence Vector Received: "+m.sSeqVec);
		    System.out.flush();
			
			Random r = new Random();
			int iRand = r.nextInt(11);
			
			System.out.println("Random Number generated :"+iRand);
			Start.pLog.println("Random Number generated :"+iRand);
		    System.out.flush();
			
			if(iRand <= Start.iRejPro){
				System.out.println("Probability test failed. Message is lost.");
				Start.pLog.println("Probability test failed. Message is lost.");
			    System.out.flush();
			    continue;
			}else{
				System.out.println("Probability test passed. Message is not lost.");
				Start.pLog.println("Probability test passed. Message is not lost.");
			    System.out.flush();
			}
			
			if( m.iHeader == 3 ){
				// request
				switch( compareSeqVec(m) ){
				case 1:{
					System.out.println("Message passed test condition 1");
					System.out.println("Message accepted.");
					Start.pLog.println("Message passed test condition 1");
					Start.pLog.println("Message accepted.");
				    System.out.flush();
					try {
						Start.semaSeqVec.acquire();
					} catch (InterruptedException e) {
						System.out.println(e.getMessage());
					    System.out.flush();
					}
					
					Start.sv.increment(m.iFromID-1);
					
					System.out.print("Current Sequence Vector:");
					Start.pLog.print("Current Sequence Vector:");
					Start.sv.printSeqVec();
					
					Start.semaSeqVec.release();
					
					Msg ack = new Msg(Start.iID,4);
					Start.sendUniUDP(ack, m.iFromID);
					//System.out.println("send ack of accept to "+m.iFromID);
				    //System.out.flush();
					
					Start.bmQueue.checkBuf();
					
					}break;
				case 2:{
					System.out.println("Message passed test condition 2");
					System.out.println("Message rejected.");
					Start.pLog.println("Message passed test condition 2");
					Start.pLog.println("Message rejected.");
				    System.out.flush();
					
				    Msg ack = new Msg(Start.iID,5);
					Start.sendUniUDP(ack, m.iFromID);
					//System.out.println("send ack of reject to "+m.iFromID);
				    //System.out.flush();
					}break;
				case 3:{
					System.out.println("Message passed test condition 3");
					System.out.println("Message buffered.");
					Start.pLog.println("Message passed test condition 3");
					Start.pLog.println("Message buffered.");
				    System.out.flush();
				    
					Start.bmQueue.addMsg(m);
					
					Msg ack = new Msg(Start.iID,6);
					Start.sendUniUDP(ack, m.iFromID);
					//System.out.println("send ack of buffer to "+m.iFromID);
				    //System.out.flush();
					}break;
				default :{
					//System.out.println("recv request which is impossible");
				    //System.out.flush();
					}break;
				}
			}else{
				System.out.println("recv others' multicast which is impossible");
			    System.out.flush();
			}
		}
		Start.pLog.flush();
		
		System.exit(0);
	}

}
