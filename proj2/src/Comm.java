import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;

public class Comm implements Runnable {

	private ServerSocket sSelf;
	public static Socket sPar;
	public static Socket sLeft;
	public static Socket sRight;
	public static ObjectInputStream ParIn = null;
	public static ObjectOutputStream ParOut = null;
	public static ObjectInputStream LeftIn = null;
	public static ObjectOutputStream LeftOut = null;
	public static ObjectInputStream RightIn = null;
	public static ObjectOutputStream RightOut = null;
	
	public boolean bServerOut = false;
	public static ArrayList<Boolean> bChild;
	
	public void Log(String s) throws IOException{
		Start.dos.writeUTF(s);
		Start.dos.flush();
	}
	
	public Comm(){
		
        try {
        	sSelf = new ServerSocket(Start.iPort);
        	Log("server start:"+sSelf.getLocalSocketAddress());
 
        } catch (java.io.IOException e) {
            System.out.println("socket start error !!");
        }
	}
	
	public void run(){
		// connect with parent
		sPar = null;
		
		if( Start.iParent >= 0 )
		{
			try {
				Log("Connet to "+Start.arrAddProc.get(Start.iParent)+":"+Integer.toString(Start.iPort));
				
				sPar = new Socket(Start.arrAddProc.get(Start.iParent),Start.iPort);
				
				Log("optain stream");
				//sPar.connect(isa, 10000);
				ParIn = new ObjectInputStream(sPar.getInputStream());
				ParOut = new ObjectOutputStream(sPar.getOutputStream());
				
				Log("connected");
	        } catch (java.io.IOException e) {
	            System.out.println("Socket connet to parent error !!");
	        }
		}
		
		//System.out.println("1");
		// start children process and connect
		Thread tLeft = null;
		Thread tRight = null;
		bChild = new ArrayList<Boolean>();
		bChild.add(false);
		bChild.add(false);
		
		if( Start.iLeft > 0 ){
			String path = System.getProperty("user.dir");
			//System.out.println(path);
			try {
				//System.out.println("ssh "+Start.arrAddProc.get(Start.iLeft)+" cd " + path + "/proj2/; java Start "+Start.iLeft);
				Runtime.getRuntime().exec("ssh -o StrictHostKeyChecking=no "+Start.arrAddProc.get(Start.iLeft)+" cd " + path + "; java Start "+Start.iLeft);
				Runtime.getRuntime().exec("ssh -o StrictHostKeyChecking=no "+Start.arrAddProc.get(Start.iRight)+" cd " + path + "; java Start "+Start.iRight);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			int iCurChild = 0;
			while(iCurChild < 2)
			{
				if(iCurChild == 0)
				{
					try {
						Log("wait for child"+Integer.toString(iCurChild));
						
						sLeft = sSelf.accept();
						LeftIn = new ObjectInputStream(sLeft.getInputStream());
						LeftOut = new ObjectOutputStream(sLeft.getOutputStream());
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					tLeft = new Thread(new Child(sLeft, 0));
					tLeft.start();
				}
				else
				{
					try {
						Log("wait for child"+Integer.toString(iCurChild));
						
						sRight = sSelf.accept();
						RightIn = new ObjectInputStream(sRight.getInputStream());
						RightOut = new ObjectOutputStream(sRight.getOutputStream());
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					tRight = new Thread(new Child(sRight, 1));
					tRight.start();
				}
				iCurChild++;
			}
		}
		
		//System.out.println("2");
		// wait setup done
		if( Start.iParent >= 0 && Start.iTotalProcess/2 <= Start.iSelf && Start.iSelf < Start.iTotalProcess )
		{
			// leaf
			Msg mDone = new Msg(1);
			try {
				ParOut.writeObject(mDone);
				ParOut.flush();
				ParOut.reset();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}else if( Start.iParent >= 0 )
		{
			// internal
			while(true){
				if(bChild.get(0) && bChild.get(1))
					break;
			}
			bChild.set(0, false);
			bChild.set(1, false);
			Msg mDone = new Msg(1);
			try {
				ParOut.writeObject(mDone);
				ParOut.flush();
				ParOut.reset();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}else
		{
			// root
			while(true){
				if(bChild.get(0) && bChild.get(1))
					break;
			}
			bChild.set(0, false);
			bChild.set(1, false);
			try {
				// all setup
				Thread.sleep(Start.iSleepTime);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			Msg mTemp = new Msg(2);
			int iCur = TimeofProc.iCurClock.get();
			int iOri = TimeofProc.iOriginalClock.get();
			mTemp.iTimeStamp = iCur;
			ProcTime pt = new ProcTime(iOri,iCur,Start.iSelf);
			
			Start.arrTimeProc.set(Start.iSelf, pt);
			mTemp.arrTime.add(pt);
			
			try {
				LeftOut.writeObject(mTemp);
				LeftOut.flush();
				LeftOut.reset();
				RightOut.writeObject(mTemp);
				RightOut.flush();
				RightOut.reset();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		// main loop for tcp
		//System.out.println("3");
		int iCurTimes = 0;
		while(!bServerOut){
			if( Start.iParent >= 0 && Start.iTotalProcess/2 <= Start.iSelf && Start.iSelf < Start.iTotalProcess)
			{ // leaf behavior
				try {
					Msg mTemp = (Msg)ParIn.readObject();
					
					if(mTemp.iId == 2){
						//Start.arrTimeProc = mTemp.arrTime;
						int iCur = TimeofProc.iCurClock.get();
						int iUpTime = Math.max(iCur+1, mTemp.iTimeStamp);
						TimeofProc.iCurClock.set(iUpTime);
						int iOri = TimeofProc.iOriginalClock.get();
						
						Iterator<ProcTime> itr = mTemp.arrTime.iterator();
						while(itr.hasNext()){
							ProcTime pt = (ProcTime)itr.next();
							Start.arrTimeProc.set(pt.iProcId, pt);
						}
						
						ProcTime ptPar = new ProcTime(iOri,iCur,Start.iSelf);
						Start.arrTimeProc.set(Start.iSelf, ptPar);
						
						Msg mTempPar = new Msg(2);
						mTempPar.iTimeStamp = iCur;
						//mTemp.arrTime.add(ptPar);
						mTempPar.arrTime.add(ptPar);
						
						ParOut.writeObject(mTempPar);
						ParOut.flush();
						ParOut.reset();
					}
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (ClassNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}else if(Start.iParent >= 0)
			{ // internal behavior
				try {
					Msg mTemp = (Msg)ParIn.readObject();
					
					if(mTemp.iId == 2){
						//Start.arrTimeProc = mTemp.arrTime;
						int iCur = TimeofProc.iCurClock.get();
						int iUpTime = Math.max(iCur+1, mTemp.iTimeStamp);
						TimeofProc.iCurClock.set(iUpTime);
						int iOri = TimeofProc.iOriginalClock.get();
						
						Iterator<ProcTime> itr = mTemp.arrTime.iterator();
						while(itr.hasNext()){
							ProcTime pt = (ProcTime)itr.next();
							Start.arrTimeProc.set(pt.iProcId, pt);
						}
						
						ProcTime ptPar = new ProcTime(iOri,iCur,Start.iSelf);
						Start.arrTimeProc.set(Start.iSelf, ptPar);
						
						Msg mTempChi = new Msg(2);
						mTempChi.iTimeStamp = iCur;
						mTemp.arrTime.add(ptPar);
						mTempChi.arrTime = mTemp.arrTime;
						
						LeftOut.writeObject(mTempChi);
						LeftOut.flush();
						LeftOut.reset();
						RightOut.writeObject(mTempChi);
						RightOut.flush();
						RightOut.reset();
					}
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (ClassNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}else
			{ // root behavior
				if(bChild.get(0) && bChild.get(1))
				{ // one run finish
					Start.arrRunTimeProc.add(Start.arrTimeProc);
					iCurTimes++;
					
					if(iCurTimes == Start.iExecTimes)
					{
						bServerOut = true;
						break;
					}
					
					bChild.set(0, false);
					bChild.set(1, false);
					
					try {
						Thread.sleep(Start.iSleepTime);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
					Msg mTemp = new Msg(2);
					int iCur = TimeofProc.iCurClock.get();
					int iOri = TimeofProc.iOriginalClock.get();
					mTemp.iTimeStamp = iCur;
					ProcTime pt = new ProcTime(iOri,iCur,Start.iSelf);
					
					Start.arrTimeProc.set(Start.iSelf, pt);
					mTemp.arrTime.add(pt);
					
					try {
						LeftOut.writeObject(mTemp);
						LeftOut.flush();
						LeftOut.reset();
						RightOut.writeObject(mTemp);
						RightOut.flush();
						RightOut.reset();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
				}

			}
			
		}
		// end
		
		try {
			if(sPar != null)
				sPar.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		sPar = null;
		try {
			sSelf.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		sSelf = null;
	}
}