import java.util.TimerTask;
import java.util.concurrent.atomic.*;

public class TimeofProc extends TimerTask {
	
	public int iClockSpeed;
	public static AtomicInteger iCurClock;
	
	public TimeofProc(int ic){
		this.iClockSpeed = ic;
		this.iCurClock.set(0);
	}
	
	public void run() {
		iCurClock.addAndGet(iClockSpeed);
	}
}
