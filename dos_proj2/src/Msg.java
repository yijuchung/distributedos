import java.io.Serializable;
import java.util.ArrayList;

public class Msg implements Serializable{
	public int iId;
	// 1 = set up done, 2 = update
	public ArrayList<ProcTime> arrTime;
	
	public Msg(int id){
		this.iId = id;
	}
}
