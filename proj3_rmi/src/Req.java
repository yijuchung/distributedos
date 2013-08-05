
public class Req implements Comparable<Req> {
	public int iType;
	// iType: 0 writer, 1 reader
	
	public int iFromClientIndex;
	public String sClientIndex;
	public int iFinishedIndex;
	public int iReqTime;
	public int iStartTime;
	public int iEndTime;
	public int iPriority;
	public int iObjVal;
	
	public Req(String si, int fc, int rt, int t, int pri){
		this.sClientIndex = si;
		this.iFromClientIndex = fc;
		this.iReqTime = rt;
		this.iType = t;
		this.iPriority = pri;
	}
	
	public int incrementPri(){
		this.iPriority++;
		return this.iPriority;
	}

	public int compareTo(Req o) {
		if (this.iStartTime == o.iStartTime)
            return 0;
        else if ((this.iStartTime) > o.iStartTime)
            return 1;
        else
            return -1;
	}
}
