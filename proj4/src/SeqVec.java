import java.util.Iterator;
import java.util.Vector;

public class SeqVec {
	public Vector<Integer> vSeq;
	
	public SeqVec(){
		vSeq = new Vector<Integer>();
	}
	
	public SeqVec(int iSize){
		vSeq = new Vector<Integer>();
		int i = 0;
		while(i < iSize){
			vSeq.add(0);
			i++;
		}
	}
	
	public synchronized void printSeqVec(){
		Iterator<Integer> it = this.vSeq.iterator();
		
		//boolean bDone = true;
		while(it.hasNext()){
			int i = it.next();
			
			//if(i != Start.iReq)
			//	bDone = false;
			
			System.out.print(i);
			Start.pLog.print(i);
			if(it.hasNext()){
				System.out.print(" ");
				Start.pLog.print(" ");
			}
		}
		
		
		System.out.print("\n");
		Start.pLog.print("\n");
	    System.out.flush();
	    
	    /*
	    if(bDone){
	    	System.out.println("All Requests received");
			Start.pLog.println("All Requests received");
			System.out.flush();
			Start.pLog.flush();
	    }*/
	    	
	}
	
	public synchronized void increment(int iIndex){
		int itemp = vSeq.get(iIndex);
		vSeq.set(iIndex, itemp+1);
	}
	
	public synchronized String Serialize(){
		Iterator<Integer> it = this.vSeq.iterator();
		
		String s = "";
		while(it.hasNext()){
			s += it.next();
			
			if(it.hasNext())
				s += " ";
		}
		
		return s;
	}
	
	public synchronized void deSerialize( String s ){
		this.vSeq.clear();
		
		String[] sa = s.split(" ");
		
		for(int i = 0;i<sa.length;i++){
			this.vSeq.add(Integer.valueOf(sa[i]));
		}
	}
}
