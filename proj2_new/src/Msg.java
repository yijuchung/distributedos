import java.io.Serializable;

public class Msg implements Serializable{
	public int iId;
	public int iTimeStamp;
	// 1 = set up done, 2 = update
	//public static ArrayList<UpdateTime> arrUpdate;
	public String sUpdate;
	
	public Msg(int id){
		this.iId = id;
		this.iTimeStamp = 0;
		this.sUpdate = "";
		//arrUpdate = new ArrayList<UpdateTime>(); 
	}
}