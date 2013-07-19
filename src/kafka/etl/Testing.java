package kafka.etl;

public class Testing extends Thread {

	public void run(){
		System.out.println("iii");
		//while(true){
			
		//}
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		System.out.print("hello");
		Testing t = new Testing();
		t.start();
	}

}
