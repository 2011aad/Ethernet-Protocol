
import java.util.concurrent.Semaphore;

public class Simulation {
    public static final int ONUNUMBER = 32;
    public static final int CYCLENUMBER = 1000;
    public static final int WAKEUPTHRESHOLD = 10;
    public static final int SLEEPTHRESHOLD = 1;
     
    public static final double RTT = 0;
    public static final double GUARDTIME = 1e-6;
    public static final double WAKEUPTIME = 1.25e-4;
    public static final double PACKETBIT = 64 * 8;
    public static final double RATE = 1e9;
    
    public static double OFFERLOAD = 0.3;
    public static double ARRIVALRATE = 0;

    public static int[] report;
    public static double[] grant; //grant time

    public static Semaphore server_lock;
    public static Semaphore[] customer_lock = new Semaphore[ONUNUMBER];
    
    public static void main(String[] args) {
    	Thread[] ONUThread;
    	Thread OLTThread;
    	
    	for (OFFERLOAD = 0.1; OFFERLOAD < 1; OFFERLOAD += 0.1){
            grant = new double[ONUNUMBER];
            report = new int[ONUNUMBER];

            ARRIVALRATE = OFFERLOAD * RATE / (PACKETBIT * ONUNUMBER);

            for(int i = 0; i < ONUNUMBER; i++){
                customer_lock[i] = new Semaphore(1);
                try {
                    customer_lock[i].acquire();
                }catch (InterruptedException e){}
            }
            server_lock = new Semaphore(1);

            ONUThread = new Thread[ONUNUMBER];
            OLTThread = new Thread(new Server(ONUNUMBER,CYCLENUMBER));
            OLTThread.start();

            for(int i = 0; i < ONUNUMBER; i++){
                ONUThread[i] = new Thread(new Customer(i));
                ONUThread[i].start();
            }

            try {
                OLTThread.join();
            }catch (InterruptedException e){}

			for(int i=0;i<ONUNUMBER;i++){
				try {
					ONUThread[i].join();
				}catch (InterruptedException e){}
			}
    	}
    }

    public static void set_grant(int i,double t){grant[i] = t;}                 //Server
    public static double read_grant(int i){return grant[i];}                    //Customer
}
