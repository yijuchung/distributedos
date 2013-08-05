import java.util.TimerTask;
import java.util.concurrent.atomic.*;

public class TimeofProc extends TimerTask {
	
	public int iClockSpeed;
	public static AtomicInteger iOriginalClock;
	public static AtomicInteger iCurClock;
	
	public TimeofProc(int ic){
		this.iClockSpeed = ic;
		TimeofProc.iCurClock = new AtomicInteger(0);
		TimeofProc.iOriginalClock = new AtomicInteger(0);
		//TimeofProc.iCurClock.set(0);
		//TimeofProc.iOriginalClock.set(0);
	}
	
	public void run() {
		iCurClock.addAndGet(iClockSpeed);
		iOriginalClock.addAndGet(iClockSpeed);
	}
}