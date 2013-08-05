/*
public class TimeofProc implements Runnable{
	
	public void run() {
		int iCurMS = (int)System.currentTimeMillis();
		int iPastMS = 0;
		
		while(!Start.bClockStop){
			if(!Start.bClockPause && (iCurMS-iPastMS) > 1){
				Start.iCurClock.addAndGet(Start.iClockSpeed);
				Start.iOriginalClock.addAndGet(Start.iClockSpeed);
				iPastMS = iCurMS; 
			}
			iCurMS = (int)System.currentTimeMillis();
		}
	}
}
*/
import java.util.TimerTask;

public class TimeofProc extends TimerTask {
	
	public void run() {
		if(!Start.bClockPause){
			Start.iCurClock.addAndGet(Start.iClockSpeed);
			Start.iOriginalClock.addAndGet(Start.iClockSpeed);
		}
	}
}