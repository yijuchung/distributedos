
public class ACKtable {
	public int [] ack;
	public int iTotal;
	
	public ACKtable(int iSize){
		this.iTotal = iSize;
		this.ack = new int[iTotal];
	}
	
	public synchronized void printACK(){
		System.out.print("ACK = ");
		for(int i = 0;i<this.iTotal;i++){
			System.out.print(this.ack[i]+" ");
		}
		System.out.print("\n");
		System.out.flush();
	}
	
	public synchronized boolean isRecv(int index){
		return (this.ack[index]==1);
	}
	
	public synchronized void setRecv(int index){
		this.ack[index] = 1;
	}
	
	public synchronized void reset(){
		for(int i = 0;i<this.iTotal;i++){
			this.ack[i] = 0;
		}
	}
	
	public synchronized boolean checkAll(){
		boolean bCheck = true;
		for(int i = 0;i<this.iTotal;i++){
			if(this.ack[i] == 0){
				bCheck = false;
				break;
			}
		}
		
		return bCheck;
	}
}
