import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.*;
import java.io.*;
import java.util.ArrayList;

public class Comm implements Runnable {

	private ServerSocket sSelf;
	public Socket sLeft;
	public Socket sRight;
	public boolean bServerOut = false;
	public static ArrayList<Boolean> bChild;
	
	public Comm(){
		
        try {
        	sSelf = new ServerSocket(Start.iPort);
 
        } catch (java.io.IOException e) {
            System.out.println("socket start error !!");
        }
	}
	
	@SuppressWarnings("null")
	public void run(){
		// connect with parent
		Socket sPar = null;
		ObjectInputStream ParIn = null;
		ObjectOutputStream ParOut = null;
		
		if( Start.iParent > 0 )
		{
			sPar = new Socket();
			InetSocketAddress isa = new InetSocketAddress(Start.arrAddProc.get(Start.iParent),Start.iPort);
			try {
				sPar.connect(isa, 10000);
	        } catch (java.io.IOException e) {
	            System.out.println("Socket connet to parent error !!");
	        }
			
			try {
				ParIn = new ObjectInputStream(sPar.getInputStream());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}else
		{
			Start.arrTotTime = new ArrayList<ArrayList<ProcTime>>();
		}
	
		// start children process and connect
		Thread tLeft = null;
		Thread tRight = null;
		bChild = new ArrayList<Boolean>();
		bChild.add(false);
		bChild.add(false);
		
		if( Start.iLeft > 0 ){
			String path = System.getProperty("user.dir");
			try {
				Runtime.getRuntime().exec("ssh "+Start.arrAddProc.get(Start.iLeft)+" cd " + path + "/proj2/; java Start "+Start.iLeft);
				Runtime.getRuntime().exec("ssh "+Start.arrAddProc.get(Start.iRight)+" cd " + path + "/proj2/; java Start "+Start.iRight);
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
						sLeft = sSelf.accept();
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
						sRight = sSelf.accept();
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
		
		// setup done && is leaf
		if( Start.iParent > 0 && Start.iTotalProcess/2 <= Start.iSelf && Start.iSelf < Start.iTotalProcess )
		{
			Msg mDone = new Msg(1);
			try {
				ParOut.writeObject(mDone);
				ParOut.flush();
				ParOut.reset();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}else if( Start.iParent > 0 )
		{
			while(true){
				if(bChild.get(0) && bChild.get(1))
					break;
			}
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
			while(true){
				if(bChild.get(0) && bChild.get(1))
					break;
			}
			try {
				Thread.sleep(Start.iSleepTime);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		// main loop for tcp
		int iCurTimes = 0;
		while(!bServerOut){
			if( Start.iParent > 0 && Start.iTotalProcess/2 <= Start.iSelf && Start.iSelf < Start.iTotalProcess)
			{ // leaf behavior
				
			}else if(Start.iParent > 0)
			{ // internal behavior
				
			}else
			{ // root behavior
				try {
					Msg mTemp = (Msg)ParIn.readObject();
					if(mTemp.iId == 2){
						Start.arrTime = mTemp.arrTime;
						// final update
						Start.arrTotTime.add(Start.arrTime);
						// flush re-send
						Start.arrTime.clear();
						
						iCurTimes++;
						if(iCurTimes == Start.iExecTimes){
							bServerOut = true;
						}
						
						Thread.sleep(Start.iSleepTime);
						
						int iCurCk = TimeofProc.iCurClock.get();
						Start.arrTime.add(new ProcTime(iCurCk,iCurCk,Start.iSelf));
						
						
					}
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (ClassNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
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
