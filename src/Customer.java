/**
 * Created by Jian on 2015/8/13.
 */
import java.util.LinkedList;
import java.util.Queue;

public class Customer implements Runnable {

    private final int SLEEP = 0;
    private final int DOZE = 1;
    private final int THRESHOLD = 50; //��ֵ

    private final double offload = 0.8;
    private final double wakeUpTime = 1.25e-4; //125��S

    private int identifier,currentState = 0,lastReport = 0;

    private double arrivalRate = offload * Simulation.RATE / (Simulation.PACKETBIT) / Simulation.ONUNUMBER;

    private double systemTime = 0;
    private double nextArrivalTime = exponential(arrivalRate); //�´ε���ʱ��

    private double totalDelay = 0;
    private int totalPacket = 0;

    private Queue<Packet> buffer = new LinkedList<Packet>();

    public Customer(int identifier){
        this.identifier = identifier;
    }

    @Override
    public void run() {
        while (Simulation.grant[identifier] > -0.5) {
            try {
                Simulation.customer_lock[identifier].acquire();
                double grant = Simulation.grant[identifier] + Simulation.RTT / 2; //grantʱ��

                switch(currentState) {
                    case SLEEP: //��˯��
                    {
                        if (buffer.size() > THRESHOLD) currentState = DOZE;
                        while (systemTime < grant) {//ϵͳʱ�� С�� grantʱ�䣬ʱ���ƽ�

                            if (nextArrivalTime < grant) {

                                systemTime = nextArrivalTime; //����ϵͳʱ��
                                addPacket();
                                nextArrivalTime = systemTime + exponential(arrivalRate); //�����´ε���ʱ��

//                                if (identifier == 0) {
//                                    System.out.println("System time: " + systemTime);
//                                    System.out.println("buffer: " + buffer.size());
//                                }

                                if (buffer.size() == THRESHOLD) { //������ֵ

                                    if (grant > (systemTime + wakeUpTime)) { // ���grantʱ���Ѿ�����DOZE��
                                        currentState = DOZE;
                                    } else { //���grantʱ�̴���WAKEUPʱ��
                                        //System.out.println("zzq");
                                        grant = systemTime + wakeUpTime; //�ƽ���WAKEUP����ʱ��
                                        currentState = SLEEP;
                                    }
                                }

                            } else {
                                systemTime = grant;
                                if (currentState == DOZE){

                                    Simulation.report[identifier] = buffer.size();
                                    lastReport = buffer.size();
                                }
                                if (currentState == SLEEP){
                                    Simulation.report[identifier] = 0;
                                }
                                break;
                            }
                        }

                        if (identifier == 0) {
                            System.out.println("Report number: " + Simulation.report[identifier]);
                            System.out.println("Current state: " + currentState);
                        }
                        break;

                    }
                    case DOZE: //�ڴ��˯
                    {
                        while (systemTime < grant) {//ϵͳʱ�� С�� grantʱ�䣬ʱ���ƽ�

                            if (nextArrivalTime < grant) {

                                systemTime = nextArrivalTime; //����ϵͳʱ��
                                addPacket();
                                nextArrivalTime = systemTime + exponential(arrivalRate); //�����´ε���ʱ��

//                                if (identifier == 0) {
//                                    System.out.println("System time: " + systemTime);
//                                    System.out.println("buffer: " + buffer.size());
//                                }
                            } else {
                                systemTime = grant;
                                break;
                            } //�����ƽ���grant��
                        }

                        //if (identifier == 0) System.out.println("lastReport: " + lastReport);
                        //if (identifier == 0) System.out.println("before: " + buffer.size());

                        for (int i = 1; i <= lastReport; i++) { //�����ϴα������

                            Packet tmp = buffer.remove();
                            tmp.setDepartureTime(systemTime + i * Simulation.PACKETBIT / Simulation.RATE);
                            totalDelay += tmp.getDepartureTime() - tmp.getArriveTime();
                            totalPacket++;
                        }

                        if (identifier == 0) System.out.println("after: " + buffer.size());

                        grant += lastReport * Simulation.PACKETBIT / Simulation.RATE;

                        while (systemTime < grant) {//ϵͳʱ�� С�� grantʱ�䣬ʱ���ƽ�

                            if (nextArrivalTime < grant) {

                                systemTime = nextArrivalTime; //����ϵͳʱ��
                                addPacket();
                                nextArrivalTime = systemTime + exponential(arrivalRate); //�����´ε���ʱ��

//                                if (identifier == 0) {
//                                    System.out.println("System time: " + systemTime);
//                                    System.out.println("buffer: " + buffer.size());
//                                }
                            } else {
                                systemTime = grant;
                                break;
                            } //�����ƽ�������������


                            if (buffer.size() >= THRESHOLD) { //������ֵ

                                Simulation.report[identifier] = buffer.size();
                                lastReport = buffer.size();
                                currentState = DOZE;
                            } else {
                                currentState = SLEEP;
                                Simulation.report[identifier] = 0;
                            }
                        }
                        if (identifier == 0) {
                            System.out.println("Report number: " + Simulation.report[identifier]);
                            System.out.println("Current state: " + currentState);
                        }
                        break;
                    }
                }
            }catch (InterruptedException e){
                e.printStackTrace();
            }
            //System.out.println("release server lock!");
            Simulation.server_lock.release();

        }
        if (identifier == 0) {
            System.out.println("ONU " + identifier + " stopped");
            System.out.println("ONU identifier: " + identifier);
            System.out.print("average delay: ");
            System.out.println(totalDelay / totalPacket);
        }
    }

    public double exponential(double mean){
        return (-(1 / mean) * Math.log(Math.random()));
    }

    private void addPacket(){
        Packet tmp = new Packet();
        tmp.setArriveTime(systemTime);
        buffer.add(tmp);
    }

}
