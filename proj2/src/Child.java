import java.net.*;
import java.io.*;
import java.util.Iterator;

public class Child implements Runnable {
	public int iBoolInx;
	public Socket sSocket;
	
	public Child(Socket sk, int inx)
	{
		this.sSocket = sk;
		this.iBoolInx = inx;
	}
	
	public void run() {
		
		ObjectInputStream in = null;
		ObjectOutputStream out = null;
		
		try {
			in = new ObjectInputStream(sSocket.getInputStream());
			out = new ObjectOutputStream(sSocket.getOutputStream());
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		// fetch info from child
		Msg mTemp = null;
		try {
			while((mTemp = (Msg)in.readObject()) != null){
				if(mTemp.iId == 1)
					Comm.bChild.set(iBoolInx, true);
				else{
					// update
					int iCur = TimeofProc.iCurClock.get();
					int iOri = TimeofProc.iOriginalClock.get();
					int iUpTime = Math.max(iCur+1, mTemp.iTimeStamp);
					TimeofProc.iCurClock.set(iUpTime);
					
					Iterator<ProcTime> itr = mTemp.arrTime.iterator();
					while(itr.hasNext()){
						ProcTime pt = (ProcTime)itr.next();
						Start.arrTimeProc.set(pt.iProcId, pt);
					}
					
					ProcTime ptPar = new ProcTime(iOri,iCur,Start.iSelf);
					Start.arrTimeProc.set(Start.iSelf, ptPar);
					
					Comm.bChild.set(iBoolInx, true);
					
					// childs all done
					if(Comm.bChild.get(0) && Comm.bChild.get(1)){
						
						Msg mTempPar = new Msg(2);
						mTempPar.iTimeStamp = iCur;
						mTemp.arrTime.add(ptPar);
						mTempPar.arrTime = mTemp.arrTime;
						
						Comm.ParOut.writeObject(mTempPar);
						Comm.ParOut.flush();
						Comm.ParOut.reset();
					}
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
