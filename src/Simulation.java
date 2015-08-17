/**
 * Created by Jian on 2015/8/13.
 */

import java.util.concurrent.Semaphore;

public class Simulation {
    public static final int ONUNUMBER = 32;
    public static final int CYCLENUMBER = 10000;
    public static final double RTT = 1e-4;
    public static final double guard_time = 1e-6;
    public static final double PACKETBIT = 64 * 8;
    public static final double RATE = 1e9;

    public static int[] report = new int[ONUNUMBER];

    public static Semaphore server_lock = new Semaphore(1);
    public static Semaphore[] customer_lock = new Semaphore[ONUNUMBER];
    public static double[] grant = new double[ONUNUMBER];             //grant time

    public static void main(String[] args) {
        Thread[] ONUThread = new Thread[ONUNUMBER];

        for(int i = 0;i < ONUNUMBER;i++){
            customer_lock[i] = new Semaphore(1);
            try {
                customer_lock[i].acquire();
            }catch (InterruptedException e){
                e.printStackTrace();
            }
        }

        Thread OLTThread = new Thread(new Server(ONUNUMBER,CYCLENUMBER));
        OLTThread.start();

        for(int i = 0;i < ONUNUMBER;i++){
            ONUThread[i] = new Thread(new Customer(i));
            ONUThread[i].start();
        }
    }

    public static void set_grant(int i,double t){grant[i] = t;}                  //Server
    public static double read_grant(int i){return grant[i];}                    //Customer
}
