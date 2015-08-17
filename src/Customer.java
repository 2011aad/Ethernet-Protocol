import java.util.LinkedList;
import java.util.Queue;

public class Customer implements Runnable {

    private final int SLEEP = 0, DOZE = 1;
    private final int THRESHOLD = 0; //阈值

    private final double offerload = 0.98;
    private double arrivalRate = offerload * Simulation.RATE / ((Simulation.PACKETBIT) * Simulation.ONUNUMBER);
    private final double wakeUpTime = 1.25e-4; //125μS

    private int identifier;
    private int currentState = 0; //初始在睡觉

    private double systemTime = 0;
    private double nextArrivalTime = exponential(arrivalRate); //下次到达时间
    
    private double totalDelay = 0;
    private int totalPacket = 0;

    private Queue<Packet> buffer = new LinkedList<Packet>();
    public Customer(int identifier) {this.identifier = identifier;}

    @Override
    public void run() {
        while (true) {
            try {
                Simulation.customer_lock[identifier].acquire();
                if(Simulation.grant[identifier] < -0.5) break;
                double grant = Simulation.grant[identifier] + Simulation.RTT / 2; //grant时间

                switch(currentState) {
                    case SLEEP: //在睡觉
                    {
                        if (buffer.size() >= THRESHOLD) currentState = DOZE;
                        while (systemTime < grant) { //推进系统时间

                            if (nextArrivalTime < grant) {
                                addPacket();
	                            if (buffer.size() == THRESHOLD) { //触发阈值
	                                if (grant > (systemTime + wakeUpTime)) currentState = DOZE; //grant时刻已经处于DOZE
	                                else { //grant时刻处于WAKEUP
	                                    grant = systemTime + wakeUpTime; //推进到WAKEUP结束时刻
	                                    currentState = SLEEP;
	                                }
	                            }
                            } else { //下次arrival时刻超过grant
                                systemTime = grant;
                                if (currentState == DOZE){
                                    Simulation.report[identifier] = buffer.size();      
                                }
                                if (currentState == SLEEP){
                                    Simulation.report[identifier] = 0;
                                }
                                break;
                            }  
                        }
                        break;
                    }
                    case DOZE: //在打瞌睡
                    {
                        while (systemTime < grant) {
                        	
                            if (nextArrivalTime < grant) {
                                addPacket();
                            } else {
                                systemTime = grant;
                                break;
                            } //推进到grant,开始离去
                        }

                        for (int i = 0; i < Simulation.report[identifier]; i++) { //传送上次报告包数
                            Packet tmp = buffer.remove();
                            tmp.setDepartureTime(systemTime + (i+1) * Simulation.PACKETBIT / Simulation.RATE);
                            totalDelay += tmp.getDepartureTime() - tmp.getArriveTime();
                            totalPacket++;
                        }

                        grant += Simulation.report[identifier] * Simulation.PACKETBIT / Simulation.RATE;
                        while (systemTime < grant) {//推进到发包结束以后
                            if (nextArrivalTime < grant) {
                                addPacket();
                            } else {
                                systemTime = grant;
                                break;
                            }
                        }
                        if (buffer.size() >= THRESHOLD) { //超过阈值,下次继续传输
                            Simulation.report[identifier] = buffer.size();
                            currentState = DOZE;
                        } else {
                            currentState = SLEEP; //否则，睡觉
                            Simulation.report[identifier] = 0;
                        }
                        break;
                    }
                }
            }catch (InterruptedException e){
                e.printStackTrace();
            }
            Simulation.server_lock.release();
        }
        
        if (identifier == 0){
	        System.out.print("ONU " + identifier + " stopped; ");
	        System.out.print("average delay: ");
	        System.out.println(totalDelay / totalPacket);
        }
        
    }

    public double exponential(double mean) {return (-(1 / mean) * Math.log(Math.random()));}

    private void addPacket(){
    	systemTime = nextArrivalTime; //更新系统时间
        Packet tmp = new Packet();
        tmp.setArriveTime(systemTime);
        buffer.add(tmp);
        nextArrivalTime = systemTime + exponential(arrivalRate); //更新下次到达时间
    }
}
