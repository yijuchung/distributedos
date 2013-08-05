import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.net.*;
import java.util.*;
import java.util.concurrent.atomic.*;

public class Start {
	
	public static int iTotalProcess;
	public static int iExecTimes;
	public static int iSleepTime;
	public static int iSelf;
	public static int iLeft;
	public static int iRight;
	public static int iParent;
	public static ArrayList<String> arrAddProc;
	public static ArrayList<ProcTime> arrProcTime;
	public static ArrayList<ArrayList<ProcTime>> arrTotalProcTime;
	public static ArrayList<Integer> arrProcClock;
	
	public static int iClockSpeed;
	public static AtomicInteger iOriginalClock;
	public static AtomicInteger iCurClock;
	public static boolean bClockStop = false;
	public static boolean bClockPause = false;
	
	public static ServerSocket sSelf;
	public static int iPort = 46972;
	public static boolean bStopTime = false;
	public static Socket sLeft = null;
	public static Socket sRight = null;
	public static Socket sPar = null;
	
	public static ObjectInputStream ParIn = null;
	public static ObjectOutputStream ParOut = null;
	public static ObjectInputStream LeftIn = null;
	public static ObjectOutputStream LeftOut = null;
	public static ObjectInputStream RightIn = null;
	public static ObjectOutputStream RightOut = null;
	
	public static void sentToPar(Msg mTemp) throws UnknownHostException, IOException{
		sPar = new Socket(arrAddProc.get(iParent),iPort);
		ParOut = new ObjectOutputStream(sPar.getOutputStream());
    	ParIn = new ObjectInputStream(sPar.getInputStream());
    	
    	ParOut.writeObject(mTemp);
    	ParOut.flush();
    	
    	ParOut.close();
    	ParIn.close();
    	sPar.close();
	}
	
	public static void sentToLeft(Msg mTemp) throws UnknownHostException, IOException{
		sLeft = new Socket(arrAddProc.get(iLeft),iPort);
		LeftOut = new ObjectOutputStream(sLeft.getOutputStream());
		LeftIn = new ObjectInputStream(sLeft.getInputStream());
    	
		LeftOut.writeObject(mTemp);
		LeftOut.flush();
		
    	LeftOut.close();
    	LeftIn.close();
    	sPar.close();
	}
	
	public static void sentToRight(Msg mTemp) throws UnknownHostException, IOException{
		sRight = new Socket(arrAddProc.get(iRight),iPort);
		RightOut = new ObjectOutputStream(sRight.getOutputStream());
		RightIn = new ObjectInputStream(sRight.getInputStream());
    	
		RightOut.writeObject(mTemp);
    	RightOut.flush();
		
		RightOut.close();
    	RightIn.close();
    	sRight.close();
	}
	
	public static Msg readFromPar() throws UnknownHostException, IOException, ClassNotFoundException{
		sPar = new Socket(arrAddProc.get(iParent),iPort);
		ParOut = new ObjectOutputStream(sPar.getOutputStream());
    	ParIn = new ObjectInputStream(sPar.getInputStream());
    	
    	Msg mTemp = (Msg)ParIn.readObject();
    	
    	ParOut.close();
    	ParIn.close();
    	sPar.close();
    	
    	return mTemp;
	}
	
	public static Msg readFromLeft() throws UnknownHostException, IOException, ClassNotFoundException{
		sLeft = new Socket(arrAddProc.get(iLeft),iPort);
		LeftOut = new ObjectOutputStream(sLeft.getOutputStream());
    	LeftIn = new ObjectInputStream(sLeft.getInputStream());
    	
    	Msg mTemp = (Msg)LeftIn.readObject();
    	
    	LeftOut.close();
    	LeftIn.close();
    	sLeft.close();
    	
    	return mTemp;
	}
	
	public static Msg readFromRight() throws UnknownHostException, IOException, ClassNotFoundException{
		sRight = new Socket(arrAddProc.get(iRight),iPort);
		RightOut = new ObjectOutputStream(sRight.getOutputStream());
    	RightIn = new ObjectInputStream(sRight.getInputStream());
    	
    	Msg mTemp = (Msg)RightIn.readObject();
    	
    	RightOut.close();
    	RightIn.close();
    	sRight.close();
    	
    	return mTemp;
	}

	@SuppressWarnings({ "unchecked", "null" })
	public static void main(String[] args){
		
		FileReader fr = null;
		try {
			fr = new FileReader("system.properties");
		} catch (FileNotFoundException e) {
			System.out.println(e.getMessage());
		    System.out.flush();
		}
		
		BufferedReader br = new BufferedReader(fr);
		
		String sInput;
		String sDel = "[=]";
		
		try {
			while( (sInput = br.readLine()) != null )
			{
				String[] sCom = sInput.split(sDel);
				sCom[1] = sCom[1].trim();
				
				switch( sCom[0].charAt(0) )
				{
				case 'n':
					iTotalProcess = Integer.parseInt(sCom[1]);
					arrAddProc = new ArrayList<String>();
					break;
				case 'M':
					iExecTimes = Integer.parseInt(sCom[1]);
					break;
				case 'S':
					iSleepTime = Integer.parseInt(sCom[1]);
					break;
				default :
					arrAddProc.add(sCom[1].trim());
				}
			}
		} catch (NumberFormatException e) {
			System.out.println(e.getMessage());
		    System.out.flush();
		} catch (IOException e) {
			System.out.println(e.getMessage());
		    System.out.flush();
		}
				
		try {
			br.close();
			fr.close();
		} catch (IOException e) {
			System.out.println(e.getMessage());
		    System.out.flush();
		}
		
		if(args.length != 0){
			// not main
			iSelf = Integer.parseInt(args[0]);
		}else{
			String path = System.getProperty("user.dir");
			Process tRoot = null;
			try {
				tRoot = Runtime.getRuntime().exec("ssh -o StrictHostKeyChecking=no "+Start.arrAddProc.get(0)+" cd " + path + "; java Start 0");
			} catch (IOException e) {
				System.out.println(e.getMessage());
    		    System.out.flush();
			}
			try {
				tRoot.waitFor();
			} catch (InterruptedException e) {
				System.out.println(e.getMessage());
    		    System.out.flush();
			}
			return;
		}
		
		PrintStream console = System.out;
	    PrintStream out = null;
	    
	    
		try {
			out = new PrintStream(new BufferedOutputStream(new FileOutputStream(Integer.toString(iSelf)+"_event.log")));
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		}
	    System.setOut(out);
	    
	    arrProcTime = new ArrayList<ProcTime>();
	    int iCurProc = 0;
	    while(iCurProc < iTotalProcess){
	    	arrProcTime.add(new ProcTime(0,0));
	    	iCurProc++;
	    }
	    
	    //System.out.println("start server :"+Integer.toString(iSelf)+":"+Integer.toString(iPort));
	    //System.out.flush();
	    try{
	    	sSelf = new ServerSocket(iPort);
	    }catch(IOException e){
	    	System.out.println(e.getMessage());
		    System.out.flush();
	    }
	    System.out.println("start server :"+Integer.toString(iSelf)+":"+Integer.toString(iPort)+" success");
	    System.out.flush();
	    //sSelf.setSoTimeout(5000);
	    
	    if(iSelf == 0)
		{
			// I'm root
			iParent = -1;
			iLeft = 1;
			iRight = 2;
			arrTotalProcTime = new ArrayList<ArrayList<ProcTime>>();
		}else if(iTotalProcess/2 <= iSelf && iSelf < iTotalProcess)
		{
			// I'm leaf
			iParent = (iSelf-1)/2;
			iLeft = -1;
			iRight = -1;
		}else
		{
			// I'm internal node
			iParent = (iSelf-1)/2;
			iLeft = 2*iSelf+1;
			iRight = 2*iSelf+2;
		}
	    
	    System.out.flush();
	    Random r = new Random();
	    iClockSpeed = r.nextInt(10)+1;
	    
	    if(iParent != -1){
	    	try{
	    	sPar = new Socket(arrAddProc.get(iParent),iPort);
	    	sPar.setKeepAlive(true);
	    	sPar.setTcpNoDelay(true);
	    	System.out.println("parent:"+Integer.toString(iParent)+" connected");
	    	//sPar.close();
	    	ParOut = new ObjectOutputStream(sPar.getOutputStream());
	    	ParIn = new ObjectInputStream(sPar.getInputStream());
	    	}catch(IOException e){
    	    	System.out.println(e.getMessage());
    		    System.out.flush();
    	    }
	    }
	    
	    if(iLeft > 0){
	    	String path = System.getProperty("user.dir");
	    	
	    	try {
				Runtime.getRuntime().exec("ssh -o StrictHostKeyChecking=no "+arrAddProc.get(iLeft)+" cd " + path + "; java Start "+iLeft);
				Runtime.getRuntime().exec("ssh -o StrictHostKeyChecking=no "+arrAddProc.get(iRight)+" cd " + path + "; java Start "+iRight);
			} catch (IOException e1) {
				System.out.println(e1.getMessage());
    		    System.out.flush();
			}
	    	
	    	int iCurChild = 0;
			while(iCurChild < 2){
				System.out.println("wait for child:"+Integer.toString(iCurChild));
				
				if(iCurChild == 0){
					try{
					sLeft = sSelf.accept();
					sLeft.setKeepAlive(true);
					sLeft.setTcpNoDelay(true);
					//sLeft.close();
					System.out.println("child:"+Integer.toString(iCurChild)+" connected from "+sLeft.getInetAddress());
					
					LeftOut = new ObjectOutputStream(sLeft.getOutputStream());
					LeftIn = new ObjectInputStream(sLeft.getInputStream());
					}catch(IOException e){
		    	    	System.out.println(e.getMessage());
		    		    System.out.flush();
		    	    }
				}else{
					try{
					sRight = sSelf.accept();
					sRight.setKeepAlive(true);
					sRight.setTcpNoDelay(true);
					//sRight.close();
					System.out.println("child:"+Integer.toString(iCurChild)+" connected from "+sRight.getInetAddress());
					
					RightOut = new ObjectOutputStream(sRight.getOutputStream());
					RightIn = new ObjectInputStream(sRight.getInputStream());
					}catch(IOException e){
		    	    	System.out.println(e.getMessage());
		    		    System.out.flush();
		    	    }
				}
				
				iCurChild++;
			}
			
			iCurChild = 0;
			Msg mTempPar = new Msg(1);
			
			while(iCurChild < 2){
				System.out.println("wait for child "+Integer.toString(iCurChild)+" setup !!");
				Msg mTempChi = new Msg(1);
				
				if(iCurChild == 0){
					try {
						LeftIn = new ObjectInputStream(sLeft.getInputStream());
						mTempChi = (Msg)LeftIn.readObject();
						//LeftIn.close();
					} catch (ClassNotFoundException e) {
						System.out.println(e.getMessage());
		    		    System.out.flush();
					} catch (IOException e) {
						System.out.println(e.getMessage());
		    		    System.out.flush();
					}
					//mTempChi = readFromLeft();
					if(mTempChi.iId != 1)
						System.out.println("first packet is not 1 !!");
					System.out.println(mTempChi.sUpdate);
			    	System.out.flush();
					mTempPar.sUpdate = mTempChi.sUpdate;
				}else{
					try{
						RightIn = new ObjectInputStream(sRight.getInputStream());
						mTempChi = (Msg)RightIn.readObject();
						//RightIn.close();
					} catch (ClassNotFoundException e) {
						System.out.println(e.getMessage());
		    		    System.out.flush();
					} catch(IOException e){
		    	    	System.out.println(e.getMessage());
		    		    System.out.flush();
		    	    }
					//mTempChi = readFromRight(); 
							
					if(mTempChi.iId != 1)
						System.out.println("first packet is not 1 !!");
					
					System.out.println(mTempChi.sUpdate);
			    	System.out.flush();
					mTempPar.sUpdate += mTempChi.sUpdate;
				}
				iCurChild++;
			}
			
			System.out.println("all child setup done !!");
			
			if(iSelf != 0){
				System.out.println("send to parent that setup done !!");
				mTempPar.sUpdate += iSelf+":"+iClockSpeed+",";
				System.out.println(iSelf+":"+iClockSpeed);
		    	System.out.flush();
				//sentToPar(mTemp);
				try{
					ParOut = new ObjectOutputStream(sPar.getOutputStream());
					//ParOut.flush();
					ParOut.writeObject(mTempPar);
		    		//ParOut.flush();
		    		//ParOut.close();
				}catch(IOException e){
	    	    	System.out.println(e.getMessage());
	    		    System.out.flush();
	    	    }
			}else{
				
				arrProcClock = new ArrayList<Integer>();
				System.out.println("recv update string :"+mTempPar.sUpdate);
				
				for(int i = 0;i<iTotalProcess;i++)
					arrProcClock.add(0);
				
				String [] sUpArray = mTempPar.sUpdate.split(",");
				//System.out.println("recv update string :"+mTempChi.sUpdate);
				
				arrProcClock.set(0,iClockSpeed);
				
				for(int i = 0;i<sUpArray.length;i++){
					String [] sUpdate = sUpArray[i].split(":");
					int iId = Integer.parseInt(sUpdate[0]);
					int iClo = Integer.parseInt(sUpdate[1]);
					
					arrProcClock.set(iId,iClo);
				}
				
				System.out.println("all setup done !!");
			}
	    }else{
	    	System.out.println("leaf setup done, send to parent !!");
	    	Msg mTemp = new Msg(1);
	    	mTemp.sUpdate = iSelf+":"+iClockSpeed+",";
	    	System.out.println(iSelf+":"+iClockSpeed);
	    	System.out.flush();
	    	//sentToPar(mTemp);
	    	try{
	    		ParOut = new ObjectOutputStream(sPar.getOutputStream());
	    		//ParOut.flush();
	    		ParOut.writeObject(mTemp);
	    		//ParOut.flush();
	    		//ParOut.close();
	    	}catch(IOException e){
    	    	System.out.println(e.getMessage());
    		    System.out.flush();
    	    }
	    }
	    
	    
		System.out.println("ClockSpeed:"+Integer.toString(iClockSpeed)+"/ms");
		System.out.flush();
		
		iCurClock = new AtomicInteger(0);
	    iOriginalClock = new AtomicInteger(0);
		
		//Thread tThread = new Thread(new TimeofProc());
	    //tThread.start();
	    
	    Timer tThread = new Timer();
	    TimeofProc tTime = new TimeofProc();
		tThread.schedule(tTime,0 ,(long)1);
	    
	    if(iSelf == 0){
			try {
				Thread.sleep(iSleepTime);
			} catch (InterruptedException e) {
				System.out.println(e.getMessage());
    		    System.out.flush();
			}
	    }
	    
	    System.out.println("begin tcp loop !!");
    	System.out.flush();
    	
		// tcp loop
	    boolean bServerOut = false;
	    int iCurExec = 0;
	    while(!bServerOut){
	    	int iCurChild = 0;
	    	
	    	if( iParent >= 0 && iTotalProcess/2 <= iSelf && iSelf < iTotalProcess )
			{ // leaf
	    		System.out.println("leaf !!");
	    	    System.out.flush();
	    		//ParOut.close();
	    		//ParIn.close();
	    		//ParOut = new ObjectOutputStream(sPar.getOutputStream());
		    	//ParIn = new ObjectInputStream(sPar.getInputStream());
		    	
	    		System.out.println("wait parent "+Integer.toString(iParent));
	    		System.out.flush();
	    		
	    		Msg mTemp = null;
	    		try{
	    			//System.out.println(ParIn.available());
	    		    //System.out.flush();
	    		    ParIn = new ObjectInputStream(sPar.getInputStream());
	    			mTemp = (Msg)ParIn.readObject();
	    			//ParIn.close();
	    		} catch (ClassNotFoundException e) {
					System.out.println("class error "+e.getMessage());
	    		    System.out.flush();
				} catch(IOException e){
	    	    	System.out.println("io exception "+e.getMessage());
	    		    System.out.flush();
	    		    break;
	    	    }
	    		System.out.println("get parent "+Integer.toString(iParent)+" string:"+mTemp.sUpdate);
	    		System.out.flush();
	    		
	    		if(mTemp.iId != 2){
					System.out.println("first packet is not 2 !!");
					System.out.flush();
	    		}
	    		//tThread.cancel();
	    		bClockPause = true;
	    		int iCurTime = iCurClock.get();
				int iSelfOriTime = iOriginalClock.get();
				int iUpTime = Math.max(mTemp.iTimeStamp, iCurTime+1);
				iCurClock.set(iUpTime);
				bClockPause = false;
				//tThread.schedule(tTime, (long)1);
				
				mTemp.iTimeStamp = iUpTime;
				mTemp.sUpdate = iSelf+":"+iSelfOriTime+":"+mTemp.iTimeStamp+",";
				System.out.println("Self original "+iSelfOriTime+", timestamp of packet "+mTemp.iTimeStamp+" clock update to "+iUpTime);
				System.out.flush();
				//sentToPar(mTemp);
				try{
					ParOut = new ObjectOutputStream(sPar.getOutputStream());
					//ParOut.flush();
					ParOut.writeObject(mTemp);
				} catch(IOException e){
	    	    	System.out.println(e.getMessage());
	    		    System.out.flush();
	    	    }
				
				if(sPar.isClosed())
					bServerOut = true;
			}else if( iParent >= 0 )
			{ // internal
				System.out.println("internal !!");
	    	    System.out.flush();
				//ParOut.close();
	    		//ParIn.close();
	    		//ParOut = new ObjectOutputStream(sPar.getOutputStream());
		    	//ParIn = new ObjectInputStream(sPar.getInputStream());
				
				System.out.println("wait parent "+Integer.toString(iParent));
				System.out.flush();
				
				Msg mTemp = null;
				try{
	    		    ParIn = new ObjectInputStream(sPar.getInputStream());
	    		    mTemp = (Msg)ParIn.readObject();
	    		    //ParIn.close();
				} catch (ClassNotFoundException e) {
					System.out.println(e.getMessage());
	    		    System.out.flush();
				} catch(IOException e){
	    	    	System.out.println(e.getMessage());
	    		    System.out.flush();
	    	    }
				if(mTemp.iId != 2){
					System.out.println("first packet is not 2 !!");
					System.out.flush();
	    		}
				
				//tThread.cancel();
				bClockPause = true;
				int iCurTime = iCurClock.get();
				int iOriTime = iOriginalClock.get();
				int iUpTime = Math.max(mTemp.iTimeStamp, iCurTime+1);
				iCurClock.set(iUpTime);
				bClockPause = false;
				//tThread.schedule(tTime, (long)1);
				mTemp.iTimeStamp = iUpTime;
				mTemp.sUpdate = iSelf+":"+iOriTime+":"+mTemp.iTimeStamp+",";
				
				//LeftOut.close();
				//LeftIn.close();
				//LeftOut = new ObjectOutputStream(sLeft.getOutputStream());
				//LeftIn = new ObjectInputStream(sLeft.getInputStream());
				
				//LeftOut.writeObject(mTemp);
				//LeftOut.flush();
				
				try{
					LeftOut = new ObjectOutputStream(sLeft.getOutputStream());
					//LeftOut.flush();
					LeftOut.writeObject(mTemp);
					//LeftOut.flush();
					//LeftOut.close();
				}catch(IOException e){
	    	    	System.out.println(e.getMessage());
	    		    System.out.flush();
	    	    }	
				
				//RightOut.close();
				//RightIn.close();
				//RightOut = new ObjectOutputStream(sRight.getOutputStream());
				//RightIn = new ObjectInputStream(sRight.getInputStream());
				
				try{
					RightOut = new ObjectOutputStream(sRight.getOutputStream());
					//RightOut.flush();
					RightOut.writeObject(mTemp);
					//RightOut.flush();
					//RightOut.close();
				}catch(IOException e){
	    	    	System.out.println(e.getMessage());
	    		    System.out.flush();
	    	    }				
				
				Msg mTempPar = new Msg(2);
				iCurChild = 0;
				
				while(iCurChild < 2){
					System.out.println("wait for child "+Integer.toString(iCurChild)+" update !!");
					System.out.flush();
					Msg mTempChi = null;
					if(iCurChild == 0){
						
						try{
							LeftIn = new ObjectInputStream(sLeft.getInputStream());
							mTempChi = (Msg)LeftIn.readObject();
							//LeftIn.close();
						} catch (ClassNotFoundException e) {
							System.out.println(e.getMessage());
			    		    System.out.flush();
						} catch(IOException e){
			    	    	System.out.println(e.getMessage());
			    		    System.out.flush();
			    	    }
						
						if(mTempChi.iId != 2)
							System.out.println("first packet is not 2 !!");
						
						mTempPar.sUpdate += mTempChi.sUpdate;
						
					}else{
						
						try{
							RightIn = new ObjectInputStream(sRight.getInputStream());
							mTempChi = (Msg)RightIn.readObject();
							//RightIn.close();
						} catch (ClassNotFoundException e) {
							System.out.println(e.getMessage());
			    		    System.out.flush();
						} catch(IOException e){
			    	    	System.out.println(e.getMessage());
			    		    System.out.flush();
			    	    }
						
						if(mTempChi.iId != 2)
							System.out.println("first packet is not 2 !!");
						
						mTempPar.sUpdate += mTempChi.sUpdate;
					}
					//tThread.cancel();
					bClockPause = true;
					iCurTime = iCurClock.get();
					bClockPause = false;
					//tThread.schedule(tTime, (long)1);
					//iOriTime = TimeofProc.iOriginalClock.get();
					iUpTime = Math.max(mTempChi.iTimeStamp, iCurTime+1);
					
					iCurChild++;
				}
				
				//tThread.cancel();
				bClockPause = true;
				iCurClock.set(iUpTime);
				int iSelfOriTime = iOriginalClock.get();
				bClockPause = false;
				//tThread.schedule(tTime, (long)1);
				mTempPar.iTimeStamp = iUpTime;
				mTempPar.sUpdate += iSelf+":"+iSelfOriTime+":"+mTemp.iTimeStamp+",";
								
				//ParOut.close();
	    		//ParIn.close();
	    		//ParOut = new ObjectOutputStream(sPar.getOutputStream());
		    	//ParIn = new ObjectInputStream(sPar.getInputStream());
				
				System.out.println("send to parent "+Integer.toString(iParent)+" update !!!");
				System.out.flush();
				
				try{
					ParOut = new ObjectOutputStream(sPar.getOutputStream());
					//ParOut.flush();
					ParOut.writeObject(mTempPar);
					//ParOut.flush();
					//ParOut.close();
				}catch(IOException e){
	    	    	System.out.println(e.getMessage());
	    		    System.out.flush();
	    	    }
				
				if(sPar.isClosed())
					bServerOut = true;
			}else
			{ // root
				System.out.println("run "+Integer.toString(iCurExec)+" start !!");
				System.out.flush();
				
				Msg mTemp = new Msg(2);
				
				//tThread.cancel();
				bClockPause = true;
				mTemp.iTimeStamp = iCurClock.get();
				int iSelfOriTime = iOriginalClock.get();
				bClockPause = false;
				//tThread.schedule(tTime, (long)1);
				//mTemp.arrUpdate.add(new UpdateTime(iSelf,TimeofProc.iOriginalClock.get(),mTemp.iTimeStamp));
				mTemp.sUpdate = iSelf+":"+iSelfOriTime+":"+mTemp.iTimeStamp+",";
				
				System.out.println("first packet, original "+iSelfOriTime+", time stamp "+mTemp.iTimeStamp);
				System.out.flush();

				System.out.println("send to left child ");
				System.out.flush();
				
				try{
					LeftOut = new ObjectOutputStream(sLeft.getOutputStream());
					//LeftOut.flush();
					LeftOut.writeObject(mTemp);
					//LeftOut.flush();
					//LeftOut.close();
				}catch(IOException e){
	    	    	System.out.println(e.getMessage());
	    		    System.out.flush();
	    	    }
				
				System.out.println("send to right child");
				System.out.flush();
				
				try{
					RightOut = new ObjectOutputStream(sRight.getOutputStream());
					//RightOut.flush();
					RightOut.writeObject(mTemp);
					//RightOut.flush();
					//RightOut.close();
				}catch(IOException e){
	    	    	System.out.println(e.getMessage());
	    		    System.out.flush();
	    	    }
				
				while(iCurChild < 2){
					System.out.println("wait for child "+Integer.toString(iCurChild)+" update !!");
					System.out.flush();
					Msg mTempChi = null;
					if(iCurChild == 0){
						
						try{
							LeftIn = new ObjectInputStream(sLeft.getInputStream());
							mTempChi = (Msg)LeftIn.readObject();
							//LeftIn.close();
						} catch (ClassNotFoundException e) {
							System.out.println(e.getMessage());
			    		    System.out.flush();
						} catch(IOException e){
			    	    	System.out.println(e.getMessage());
			    		    System.out.flush();
			    	    }
						
						if(mTempChi.iId != 2)
							System.out.println("first packet is not 2 !!");
					}else{
						
						try{
							RightIn = new ObjectInputStream(sRight.getInputStream());
							mTempChi = (Msg)RightIn.readObject();
							//RightIn.close();
						} catch (ClassNotFoundException e) {
							System.out.println(e.getMessage());
			    		    System.out.flush();
						} catch(IOException e){
			    	    	System.out.println(e.getMessage());
			    		    System.out.flush();
			    	    }
						
						if(mTempChi.iId != 2)
							System.out.println("first packet is not 2 !!");
					}
					
					//tThread.cancel();
					bClockPause = true;
					int iCurTime = iCurClock.get();
					int iOriTime = iOriginalClock.get();
					int iUpTime = Math.max(mTempChi.iTimeStamp, iCurTime+1);
					iCurClock.set(iUpTime);
					bClockPause = false;
					//tThread.schedule(tTime, (long)1);
					arrProcTime.set(iSelf, new ProcTime(iOriTime,iUpTime));
					
					String [] sUpArray = mTempChi.sUpdate.split(",");
					//System.out.println("recv update string :"+mTempChi.sUpdate);
					
					for(int i = 0;i<sUpArray.length;i++){
						String [] sUpdate = sUpArray[i].split(":");
						int iId = Integer.parseInt(sUpdate[0]);
						int iOri = Integer.parseInt(sUpdate[1]);
						int iUp = Integer.parseInt(sUpdate[2]);
						
						System.out.println("recv update time from "+iId+" original "+iOri+" update "+iUp);
						
						arrProcTime.set(iId, new ProcTime(iOri,iUp));
					}
					
					//Iterator<UpdateTime> itr = mTempChi.arrUpdate.iterator();
					
					/*
					while(itr.hasNext()){
						UpdateTime ut = itr.next();
						if(ut.iProcID != iSelf)
							arrProcTime.set(ut.iProcID, new ProcTime(ut.iOriTime,ut.iUpdateTime));
					}*/
					
					iCurChild++;
				}
				
				iCurExec++;
				System.out.println("run "+Integer.toString(iCurExec)+" finished !!");
				System.out.flush();
				arrTotalProcTime.add((ArrayList<ProcTime>)arrProcTime.clone());
				
				if(iCurExec == iExecTimes)
					bServerOut = true;
				else{
					try {
						Thread.sleep(iSleepTime);
					} catch (InterruptedException e) {
						System.out.println(e.getMessage());
		    		    System.out.flush();
					}
				}
			}
	    }
	    // finalize
	    
	    tThread.cancel();
	    bClockStop = true;
	    /*
	    try {
			tThread.join();
		} catch (InterruptedException e) {
			System.out.println(e.getMessage());
		    System.out.flush();
		}
		*/
	    System.out.println("stop time thread !!");
    	System.out.flush();
	    
	    try {
			sSelf.close();
		} catch (IOException e) {
			System.out.println(e.getMessage());
		    System.out.flush();
		}
	    
	    if(sLeft != null){
	    	try {
				LeftOut.close();
				RightOut.close();
		    	LeftIn.close();
		    	RightIn.close();
		    	sLeft.close();
		    	sRight.close();
			} catch (IOException e) {
				System.out.println(e.getMessage());
    		    System.out.flush();
			}
	    }
	    
	    if(sPar != null){
	    	try {
				ParOut.close();
				ParIn.close();
		    	sPar.close();
			} catch (IOException e) {
				System.out.println(e.getMessage());
    		    System.out.flush();
			}
	    }
	    
	    out.close();
		System.setOut(console);
	    
	    if(iSelf == 0){
	    	
	    	console = System.out;
		    out = null;
		    
		    
			try {
				out = new PrintStream(new BufferedOutputStream(new FileOutputStream("output.log")));
			} catch (FileNotFoundException e1) {
				e1.printStackTrace();
			}
		    System.setOut(out);
		    
	    	System.out.println("Increment Rates:");
	    	
	    	for(int j = 0;j<iTotalProcess;j++){
		    	int iClo = arrProcClock.get(j);
		    	System.out.println("P"+Integer.toString(j+1)+":"+Integer.toString(iClo));
		    }
	    	
	    	
		    for(int i = 0;i<iExecTimes;i++){
		    	System.out.println("Clocks After run "+Integer.toString(i+1)+":");
		    	for(int j = 0;j<iTotalProcess;j++){
		    		ProcTime pt = arrTotalProcTime.get(i).get(j);
		    		System.out.println("P"+Integer.toString(j+1)+" Updated:"+Integer.toString(pt.iUpTime));
		    		System.out.println("P"+Integer.toString(j+1)+" Original:"+Integer.toString(pt.iOriTime));
		    	}
		    }
		    
		    out.close();
			System.setOut(console);
	    }
	}

}
