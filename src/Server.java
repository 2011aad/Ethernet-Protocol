/**
 * Created by Jian on 2015/8/13.
 */
public class Server implements Runnable {
    private static int ONUNUMBER;
    private int CYCLENUMBER;
    private static int counter = 0;
    private static double systime = 0;
    private static double[] idle_time = new double[ONUNUMBER];

    public Server(int ONUNUMBER, int CYCLENUMBER){
        this.ONUNUMBER = ONUNUMBER;
        this.CYCLENUMBER = CYCLENUMBER;
    }

    public void run(){
        while(counter<CYCLENUMBER){
            for(int i=0;i<ONUNUMBER;i++){
                while(!Simulation.read_report_flag());
                systime = Simulation.grant[i];
                Simulation.set_grant(i,calculate_next_grant(Simulation.report, i));
                Simulation.set_flag(i);
                Simulation.unset_report_flag();
            }
            counter++;
        }
    }

    public static double calculate_next_grant(int [] report, int i){
        double grant;
        double factor = Simulation.PACKETBIT/Simulation.RATE;

        if((systime + report[i]*factor + Simulation.RTT)>Simulation.grant[i-1]+report[i-1]*factor+Simulation.guard_time){
            grant = systime + report[i]*factor + Simulation.RTT;
            idle_time[i] += systime + report[i]*factor + Simulation.RTT - (Simulation.grant[i-1]+report[i-1]*factor+Simulation.guard_time);
        }

        else {
            grant = Simulation.grant[i-1]+report[i-1]*factor+Simulation.guard_time;
        }

        return grant;
    }
}
