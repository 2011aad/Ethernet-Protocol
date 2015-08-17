public class Server implements Runnable {
    private static int ONUNUMBER;
    private int CYCLENUMBER;
    private static int counter = 0;
    private static double systime = 0;
    private static double[] idle_time;
    private static double tmp;

    public Server(int ONUNUMBER, int CYCLENUMBER){
        this.ONUNUMBER = ONUNUMBER;
        this.CYCLENUMBER = CYCLENUMBER;
        idle_time = new double[ONUNUMBER];
    }

    public void run(){
        while(counter<CYCLENUMBER){
            System.out.println("Cycle " + counter);
            for(int i=0;i<ONUNUMBER;i++) {
                try {
                	Simulation.server_lock.acquire();
                    if(counter!=0)Simulation.set_grant((i-1+ONUNUMBER)%ONUNUMBER, tmp);
                    systime = Simulation.grant[i];
                    tmp = calculate_next_grant(Simulation.report, i);
                    Simulation.customer_lock[i].release();
                }catch (InterruptedException e){
                    e.printStackTrace();
                }

            }
            counter++;
        }

        for(int i=0;i<ONUNUMBER;i++) {
            Simulation.set_grant(i, -1);
            Simulation.customer_lock[i].release();
        }
    }

    public static double calculate_next_grant(int [] report, int i){
        double grant;
        double factor = Simulation.PACKETBIT/Simulation.RATE;

        if((systime + report[i]*factor + Simulation.RTT)>(Simulation.grant[(i-1+ONUNUMBER)%ONUNUMBER]+report[(i-1+ONUNUMBER)%ONUNUMBER]*factor+Simulation.guard_time)){
            grant = systime + report[i]*factor + Simulation.RTT;
            //idle_time[i] += (systime + report[i]*factor + Simulation.RTT - (Simulation.grant[(i-1+ONUNUMBER)%ONUNUMBER]+report[(i-1+ONUNUMBER)%ONUNUMBER]*factor+Simulation.guard_time));
        }

        else {
            grant = Simulation.grant[(i-1+ONUNUMBER)%ONUNUMBER]+report[(i-1+ONUNUMBER)%ONUNUMBER]*factor+Simulation.guard_time;
        }
        return grant;
    }
}
