import java.io.*;
import java.util.Timer;
import java.util.Random;
import java.util.ArrayList;

public class Start {
	
	public static int iClockSpeed;
	
	public static int iSelf;
	public static int iLeft;
	public static int iRight;
	public static int iParent;
	public static int iTotalProcess;
	public static int iExecTimes;
	public static int iSleepTime;
	public static ArrayList<String> arrAddProc;
	public static int iCurrentTime;
	public static ArrayList<ProcTime> arrTime;
	public static int iPort = 8973;
	public static ArrayList<ArrayList<ProcTime>> arrTotTime;

	public static void main(String[] args) throws IOException, InterruptedException {

		FileReader fr = new FileReader("system.properties");
		
		BufferedReader br = new BufferedReader(fr);
		
		String sInput;
		String sDel = "[=]";
		
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
				
		br.close();
		fr.close();
		// end of read file
		
		// who am I
		if(args.length != 0)
		{
			iSelf = Integer.parseInt(args[0]);
		}else
		{
			// I am root
			iSelf = 0;
		}
		// --------
		Random r = new Random();
		iClockSpeed = r.nextInt(10)+1;
		Thread cThread = new Thread(new Comm());
		
		if(iSelf == 0)
		{
			// I'm root
			iParent = -1;
			iLeft = 1;
			iRight = 2;
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
		cThread.start();
		
		Timer tThread = new Timer();
		tThread.schedule(new TimeofProc(iClockSpeed), 1);
		
		cThread.join();
	}

}
