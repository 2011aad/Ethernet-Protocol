import java.util.LinkedList;
import java.util.Queue;

public class Customer implements Runnable {

    private final int SLEEP = 0, DOZE = 1;
    private final int THRESHOLD = 0; //��ֵ

    private final double offerload = 0.98;
    private double arrivalRate = offerload * Simulation.RATE / ((Simulation.PACKETBIT) * Simulation.ONUNUMBER);
    private final double wakeUpTime = 1.25e-4; //125��S

    private int identifier;
    private int currentState = 0; //��ʼ��˯��

    private double systemTime = 0;
    private double nextArrivalTime = exponential(arrivalRate); //�´ε���ʱ��
    
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
                double grant = Simulation.grant[identifier] + Simulation.RTT / 2; //grantʱ��

                switch(currentState) {
                    case SLEEP: //��˯��
                    {
                        if (buffer.size() >= THRESHOLD) currentState = DOZE;
                        while (systemTime < grant) { //�ƽ�ϵͳʱ��

                            if (nextArrivalTime < grant) {
                                addPacket();
	                            if (buffer.size() == THRESHOLD) { //������ֵ
	                                if (grant > (systemTime + wakeUpTime)) currentState = DOZE; //grantʱ���Ѿ�����DOZE
	                                else { //grantʱ�̴���WAKEUP
	                                    grant = systemTime + wakeUpTime; //�ƽ���WAKEUP����ʱ��
	                                    currentState = SLEEP;
	                                }
	                            }
                            } else { //�´�arrivalʱ�̳���grant
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
                    case DOZE: //�ڴ��˯
                    {
                        while (systemTime < grant) {
                        	
                            if (nextArrivalTime < grant) {
                                addPacket();
                            } else {
                                systemTime = grant;
                                break;
                            } //�ƽ���grant,��ʼ��ȥ
                        }

                        for (int i = 0; i < Simulation.report[identifier]; i++) { //�����ϴα������
                            Packet tmp = buffer.remove();
                            tmp.setDepartureTime(systemTime + (i+1) * Simulation.PACKETBIT / Simulation.RATE);
                            totalDelay += tmp.getDepartureTime() - tmp.getArriveTime();
                            totalPacket++;
                        }

                        grant += Simulation.report[identifier] * Simulation.PACKETBIT / Simulation.RATE;
                        while (systemTime < grant) {//�ƽ������������Ժ�
                            if (nextArrivalTime < grant) {
                                addPacket();
                            } else {
                                systemTime = grant;
                                break;
                            }
                        }
                        if (buffer.size() >= THRESHOLD) { //������ֵ,�´μ�������
                            Simulation.report[identifier] = buffer.size();
                            currentState = DOZE;
                        } else {
                            currentState = SLEEP; //����˯��
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
    	systemTime = nextArrivalTime; //����ϵͳʱ��
        Packet tmp = new Packet();
        tmp.setArriveTime(systemTime);
        buffer.add(tmp);
        nextArrivalTime = systemTime + exponential(arrivalRate); //�����´ε���ʱ��
    }
}
