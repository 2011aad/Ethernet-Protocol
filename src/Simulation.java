/**
 * Created by Jian on 2015/8/13.
 */
public class Simulation {
    public static final int ONUNUMBER = 32;
    public static final int CYCLENUMBER = 100000;
    public static final double RTT = 200;
    public static final double guard_time = 1;
    public static final double PACKETBIT = 64 * 8;
    public static final double RATE = 1e9;

    public static int[] report = new int[ONUNUMBER];

    public static int flag = 0;
    public static boolean report_flag = true;  //Customer set flag after report, Server unset flag after read the report
    public static double[] grant = new double[ONUNUMBER];             //grant time

    public static void main(String[] args) {
        Thread[] ONUThread = new Thread[ONUNUMBER];
        Thread OLTThread = new Thread(new Server(ONUNUMBER,CYCLENUMBER));
        OLTThread.start();

        for(int i = 0;i < ONUNUMBER;i++){
            ONUThread[i] = new Thread(new Customer());
            ONUThread[i].start();
        }
    }

    public static void set_report_flag(){report_flag = true;}           //Customer
    public static boolean read_report_flag(){return report_flag;}       //Server
    public static void unset_report_flag(){report_flag = false;}        //sever
    public static void set_flag(int n){flag = n;}                       //Server
    public static int read_flag(){return flag;}                         //Customer
    public static void set_grant(int i,double t){grant[i] = t;}                  //Server
    public static double read_grant(int i){return grant[i];}                    //Customer
}
