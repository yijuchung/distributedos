import java.util.Iterator;
import java.util.Vector;


public class BufferMsg {
	public static Vector<Msg> vmQueue;
	
	public BufferMsg(){
		vmQueue = new Vector<Msg>();
	}
	
	public synchronized void addMsg(Msg m){
		vmQueue.add(m);
	}
	
	public synchronized void checkBuf(){
		Iterator<Msg> it = vmQueue.iterator();
		
		boolean bNone = true;
		
		while( it.hasNext() ){
			Msg m = it.next();
			
			int iState = MulticastServer.compareSeqVec(m);
			if( iState == 1 ){
				
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
				
				bNone = false;
			}else if(iState == 2){
				bNone = false;
				it.remove();
			}
			
			if(!it.hasNext())
			{
				if(bNone)
					break;
				else{
					bNone = true;
					it = vmQueue.iterator();
				}
			}
		}
	}
}
