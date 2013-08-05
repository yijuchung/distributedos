import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.DatagramPacket;


public class UDPServer implements Runnable{
	
	public Msg recvUniUDP(){
		
		byte[] buf = new byte[256];
		DatagramPacket recvdp = new DatagramPacket(buf, buf.length);
		
		try {
			Start.dsUniSocket.receive(recvdp);
		} catch (IOException e) {
			return null;
		}
		//System.out.println("recv from "+recvdp.getAddress()+":"+recvdp.getPort());
	    //System.out.flush();
        
		ByteArrayInputStream baos = new ByteArrayInputStream(buf);
	    ObjectInputStream oos = null;
	    Msg m = null;
		
		try{
			oos = new ObjectInputStream(baos);
			m = (Msg)oos.readObject();
			baos.close();
			oos.close();
		}catch (ClassNotFoundException e) {
			System.out.println("1udp "+e.getMessage());
		    System.out.flush();
		} catch (IOException e) {
			System.out.println("2udp "+e.getMessage());
		    System.out.flush();
		}
		
		return m;
	}

	public void run() {
		if(Start.iID != 1){
			// other clients wait client 1 notify
			Msg m = null;
		    while(m == null){
		    	m = (Msg)recvUniUDP();
		    }
			
			if(m.iHeader == 2)
		    	Start.sFirst.release();
		    else{
		    	System.out.println("first msg is not notify !?");
			    System.out.flush();
			    System.exit(0);
		    }
		}else{
			// wait other clients start
		    int iClient = 0;
		    while(iClient < Start.iTotalClient-1)
		    {
		    	Msg m = null;
			    while(m == null){
			    	m = (Msg)recvUniUDP();
			    }
				
				//System.out.println("recv msg header "+m.iHeader+" from "+m.iFromID);
			    //System.out.flush();
			    if(m.iHeader == 1)
			    	iClient++;
			    else{
			    	System.out.println("first msg is not start !?");
				    System.out.flush();
				    System.exit(0);
			    }
		    }
		    Start.sFirst.release();
		}
		
		while(!Start.bExit){
			Msg m = (Msg)recvUniUDP();
			if(m == null)
				break;
			
			switch(m.iHeader){
			case 4:
				//System.out.println("recv ack of accept from "+m.iFromID);
			    //System.out.flush();
				Start.ackTable.setRecv(m.iFromID-1);
				break;
			case 5:
				//System.out.println("recv ack of reject from "+m.iFromID);
			    //System.out.flush();
				Start.ackTable.setRecv(m.iFromID-1);
				break;
			case 6:
				//System.out.println("recv ack of buffer from "+m.iFromID);
			    //System.out.flush();
				Start.ackTable.setRecv(m.iFromID-1);
				break;
			case 7:
				//System.out.println("recv req all done from "+m.iFromID);
			    //System.out.flush();
				Start.reqTable.setRecv(m.iFromID-1);
				break;
			case 8:
				//System.out.println("recv shutdown from "+m.iFromID);
			    //System.out.flush();
				Start.semaReqDone.release();
				break;				
			default:
				System.out.println("recv unicast which is impossible");
			    System.out.flush();
				break;
			}
		}
		Start.pLog.flush();
		
		System.exit(0);
	}

}
