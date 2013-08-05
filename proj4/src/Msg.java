import java.io.Serializable;

public class Msg implements Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public int iHeader;
	/* 1 = send to client 1
	 * 2 = client 1 notify other clients
	 * 3 = request
	 * 4 = accept ack
	 * 5 = reject ack
	 * 6 = buffer ack
	 * 7 = send to client 1 req all done
	 * 8 = client 1 notify other clients to close
	*/
	public int iFromID;
	public String sSeqVec;
	
	public Msg(int fromid, int iheader, String ssv){
		this.iHeader = iheader;
		this.iFromID = fromid;
		
		this.sSeqVec = ssv;
	}
	
	public Msg(int fromid, int iheader)
	{
		this.iHeader = iheader;
		this.iFromID = fromid;
		
		this.sSeqVec = "";
	}
	
}
