import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class ServerThread extends UnicastRemoteObject implements RW, Runnable {
	
	private static final long serialVersionUID = 1L;
	public static Semaphore semaReader;
	public static Semaphore semaWriter;
	public static Semaphore semaFinished;
	public static Semaphore semaiReaders;
	public static Semaphore semaObjAccess;
	
	public static Semaphore semaReq;
	public static AtomicInteger iReaders;
	
	public static ArrayList<Boolean> arrBoolClient;
	public static ArrayList<Notify> arrClient;
	
	public static ArrayList<Req> arrReaderReq;
	public static ArrayList<Req> arrWriterReq;
	
	public void outputQueue(){
		System.out.println("---------------------Print Queue--------------------------------------");
		System.out.println("[Reader Queue size:"+arrReaderReq.size()+"]");
		Iterator<Req> it = arrReaderReq.iterator();
		while(it.hasNext()){
			Req r = it.next();
			System.out.println("Reader["+r.sClientIndex+"], pri:"+r.iPriority+", req time:"+(r.iReqTime-Server.iLaunchTime));
		}
		System.out.println("[Writer Queue size:"+arrWriterReq.size()+"]");
		it = arrWriterReq.iterator();
		while(it.hasNext()){
			Req r = it.next();
			System.out.println("Writer["+r.sClientIndex+"], pri:"+r.iPriority+", req time:"+(r.iReqTime-Server.iLaunchTime));
		}
		System.out.println("----------------------------------------------------------------------");
		System.out.flush();
	}

	public ServerThread() throws RemoteException {
		super();
		arrBoolClient = new ArrayList<Boolean>();
		arrReaderReq = new ArrayList<Req>();
		arrWriterReq = new ArrayList<Req>();
		arrClient = new ArrayList<Notify>();
		
		semaReader = new Semaphore(1);
		semaWriter = new Semaphore(1);
		semaiReaders = new Semaphore(1);
		semaObjAccess = new Semaphore(1);
		semaFinished = new Semaphore(1);
		semaReq = new Semaphore(1);
		iReaders = new AtomicInteger(0);
	}

	public synchronized int registerClient(Notify c) throws RemoteException {
		arrBoolClient.add(false);
		arrClient.add(c);
		int ind = arrBoolClient.size()-1;
		System.out.println("Client "+ind+" login");
		System.out.flush();
		return ind;
	}

	public synchronized void doneClient(int iIndex) throws RemoteException {
		arrBoolClient.set(iIndex, true);
		System.out.println("Client "+iIndex+" done");
		System.out.flush();
	}
	
	public void sendRequest(String si, int iIndex) throws RemoteException {
		
		try {
			semaReq.acquire();
		} catch (InterruptedException e) {
			System.out.println(e.toString());
            System.out.flush();
		}
		if( arrClient.get(iIndex).getType() == 0 ){
			// writer
			try {
				semaWriter.acquire();
			} catch (InterruptedException e) {
				System.out.println(e.toString());
	            System.out.flush();
			}
			
			Req r = new Req(si, iIndex, (int)System.currentTimeMillis() , 0 , 3);
			System.out.println("recv writer req from "+r.sClientIndex+" at "+(r.iReqTime-Server.iLaunchTime));
            System.out.flush();
			arrWriterReq.add(r);
			semaWriter.release();
			try {
				semaFinished.acquire();
			} catch (InterruptedException e) {
				System.out.println(e.toString());
	            System.out.flush();
			}
			r.iFinishedIndex = Server.arrFinishedReq.size();
			Server.arrFinishedReq.add(r);
			semaFinished.release();
		}else{
			try {
				semaReader.acquire();
			} catch (InterruptedException e) {
				System.out.println(e.toString());
	            System.out.flush();
			}
			Req r = new Req(si, iIndex, (int)System.currentTimeMillis() , 1 , 1);
			System.out.println("recv reader req from "+r.sClientIndex+" at "+(r.iReqTime-Server.iLaunchTime));
            System.out.flush();
			arrReaderReq.add(r);
			semaReader.release();
			try {
				semaFinished.acquire();
			} catch (InterruptedException e) {
				System.out.println(e.toString());
	            System.out.flush();
			}
			r.iFinishedIndex = Server.arrFinishedReq.size();
			Server.arrFinishedReq.add(r);
			semaFinished.release();
		}
		
		semaReq.release();
	}
	
	public void updateFinishedTime(int iIndex)	throws RemoteException {
		//System.out.println("updateFinishedTime("+iIndex+")");
		//System.out.flush();
		try {
			semaFinished.acquire();
		} catch (InterruptedException e) {
			System.out.println(e.toString());
            System.out.flush();
		}
		Req r = Server.arrFinishedReq.get(iIndex);
		r.iEndTime = (int)System.currentTimeMillis();
		Server.arrFinishedReq.set(iIndex, r);
		semaFinished.release();
		if(r.iType == 0){
			System.out.println("writer["+r.sClientIndex+"] finish at "+(r.iEndTime-Server.iLaunchTime));
			System.out.flush();
			semaObjAccess.release();
		}else{
			
			try {
				semaiReaders.acquire();
			} catch (InterruptedException e) {
				System.out.println(e.toString());
	            System.out.flush();
			}
			iReaders.decrementAndGet();
			if(iReaders.get() == 0){
				System.out.println("Reader["+r.sClientIndex+"] finish at "+(r.iEndTime-Server.iLaunchTime)+"(last reader)");
				semaObjAccess.release();
			}else
				System.out.println("Reader["+r.sClientIndex+"] finish at "+(r.iEndTime-Server.iLaunchTime));
			System.out.flush();
			semaiReaders.release();
		}
	}
	
	public void run() {
		
		while(arrClient.size() != Server.iReader+Server.iWriter){
			try {
				Thread.sleep(20);
			} catch (InterruptedException e) {
				System.out.println(e.toString());
	            System.out.flush();
			}
		}
		
		System.out.println("wait for all client done ~");
        System.out.flush();
		
		Server.iLaunchTime = (int)System.currentTimeMillis()+3000;
		Iterator<Notify> it = arrClient.iterator();
		while(it.hasNext()){
			Notify c = (Notify) it.next();
			try {
				c.setStartTime(Server.iLaunchTime);
			} catch (RemoteException e) {
				System.out.println(e.toString());
	            System.out.flush();
			}
		}
		
		System.out.println("notify for all client on : "+Server.iLaunchTime+" done");
        System.out.flush();
		
		while(!Server.bStopServer){
			
			try {
				semaReader.acquire();
				semaWriter.acquire();
			} catch (InterruptedException e) {
				System.out.println(e.toString());
	            System.out.flush();
			}
			
			if( arrReaderReq.size() == 0 && arrWriterReq.size() == 0 ){
				semaReader.release();
				semaWriter.release();
				continue;
			}else if( arrReaderReq.size() == 0 ){
				//System.out.println("case 2");
	            //System.out.flush();
				
				
				if( !semaObjAccess.tryAcquire() ){
					//System.out.println("case 2-obj is accessed");
		            //System.out.flush();
		            //semaReader.release();
					semaReader.release();
					semaWriter.release();
					continue;
				}
				
				outputQueue();
				Req r = arrWriterReq.remove(0);
				try {
					//System.out.println("case 2-1");
		            //System.out.flush();
		            r.iStartTime = (int)System.currentTimeMillis();
		            arrClient.get(r.iFromClientIndex).doOperation();
		            
		            System.out.println("notify writer["+r.sClientIndex+"] do oper at "+(r.iStartTime-Server.iLaunchTime));
		            System.out.flush();
				} catch (RemoteException e) {
					System.out.println(e.toString());
		            System.out.flush();
				}
				Server.iObjVal.set(Integer.parseInt(r.sClientIndex));
				r.iObjVal = Integer.parseInt(r.sClientIndex);
				
				try {
					arrClient.get(r.iFromClientIndex).setFinishedIndex(r.iFinishedIndex);
					Server.arrFinishedReq.set(r.iFinishedIndex, r);
				} catch (RemoteException e) {
					System.out.println(e.toString());
		            System.out.flush();
				}
				
				semaReader.release();
				semaWriter.release();
				
			}else if( arrWriterReq.size() == 0 ){
				//System.out.println("case 3");
	            //System.out.flush();
				
				try {
					semaiReaders.acquire();
				} catch (InterruptedException e) {
					System.out.println(e.toString());
		            System.out.flush();
				}
				
				if( iReaders.get() == 0 ){
					
					if( !semaObjAccess.tryAcquire() ){
						//System.out.println("case 2-obj is accessed by writer");
			            //System.out.flush();
						semaWriter.release();
			            semaReader.release();
			            semaiReaders.release();
						continue;
					}
				}
				
				outputQueue();
				int iCurrentTime = (int)System.currentTimeMillis();
				while(arrReaderReq.size() > 0){
					Req r = arrReaderReq.remove(0);
					
					try {
						r.iStartTime = iCurrentTime;
						arrClient.get(r.iFromClientIndex).doOperation();
						System.out.println("notify reader["+r.sClientIndex+"] do oper at "+(r.iStartTime-Server.iLaunchTime));
			            System.out.flush();
					} catch (RemoteException e) {
						System.out.println(e.toString());
			            System.out.flush();
					}
					
					r.iObjVal = Server.iObjVal.get();
					
					try {
						arrClient.get(r.iFromClientIndex).setFinishedIndex(r.iFinishedIndex);
						Server.arrFinishedReq.set(r.iFinishedIndex, r);
					} catch (RemoteException e) {
						System.out.println(e.toString());
			            System.out.flush();
					}
					
					iReaders.incrementAndGet();
				}
				
				semaiReaders.release();
				semaWriter.release();
				semaReader.release();
			}else{
				//System.out.println("case 4");
	            //System.out.flush();
				Req rr = arrReaderReq.get(0);
				Req rw = arrWriterReq.get(0);
				
				if( rr.iPriority > rw.iPriority ){
					//System.out.println("case 4-1");
		            //System.out.flush();
					
		            
		            try {
						semaiReaders.acquire();
					} catch (InterruptedException e) {
						System.out.println(e.toString());
			            System.out.flush();
					}
					
					if( iReaders.get() == 0 ){
						
						if( !semaObjAccess.tryAcquire() ){
							//System.out.println("case 4-1-obj is accessed by writer");
				            //System.out.flush();
				            semaReader.release();
				            semaWriter.release();
				            semaiReaders.release();
							continue;
						}
					}
					
					outputQueue();					
					int iTry = 2;
					int iCurrentTime = (int)System.currentTimeMillis();
					while(iTry > 0){
						if( arrReaderReq.size() <= 0 )
							break;
						
						iReaders.incrementAndGet();
						
						Req r = arrReaderReq.remove(0);
						
						try {
							r.iStartTime = iCurrentTime;
							arrClient.get(r.iFromClientIndex).doOperation();
							System.out.println("notify reader["+r.sClientIndex+"] do oper at "+(r.iStartTime-Server.iLaunchTime));
				            System.out.flush();
						} catch (RemoteException e) {
							System.out.println(e.toString());
				            System.out.flush();
						}
						
						r.iObjVal = Server.iObjVal.get();
						
						try {
							arrClient.get(r.iFromClientIndex).setFinishedIndex(r.iFinishedIndex);
							Server.arrFinishedReq.set(r.iFinishedIndex, r);
						} catch (RemoteException e) {
							System.out.println(e.toString());
				            System.out.flush();
						}
						
						iTry--;
					}
					
					semaWriter.release();
					semaiReaders.release();
					semaReader.release();
				}else{
					//System.out.println("case 4-2");
		            //System.out.flush();
					//semaReader.release();
		            
		            if( !semaObjAccess.tryAcquire() ){
						//System.out.println("case 4-2-obj is accessed");
			            //System.out.flush();
						semaWriter.release();
						semaReader.release();
						continue;
					}
		            
		            outputQueue();
					Req r = arrWriterReq.remove(0);
					try {
						r.iStartTime = (int)System.currentTimeMillis();
						arrClient.get(r.iFromClientIndex).doOperation();
						System.out.println("notify writer["+r.sClientIndex+"] do oper at "+(r.iStartTime-Server.iLaunchTime));
			            System.out.flush();
					} catch (RemoteException e) {
						System.out.println(e.toString());
			            System.out.flush();
					}
					Server.iObjVal.set(Integer.parseInt(r.sClientIndex));
					r.iObjVal = Server.iObjVal.get();
					
					try {
						arrClient.get(r.iFromClientIndex).setFinishedIndex(r.iFinishedIndex);
						Server.arrFinishedReq.set(r.iFinishedIndex, r);
					} catch (RemoteException e) {
						System.out.println(e.toString());
			            System.out.flush();
					}
					
					if(rr.iReqTime <= r.iReqTime){
						rr.incrementPri();
						arrReaderReq.set(0, rr);
					}
					semaReader.release();
					semaWriter.release();
				}
			}
		}
		
	}

	public int getCurrentTime() throws RemoteException {
		return (int)System.currentTimeMillis();
	}
}
