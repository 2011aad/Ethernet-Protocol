import java.util.LinkedList;
import java.util.Queue;

import javax.print.attribute.standard.Finishings;

public class Customer implements Runnable {
	
	private int identifier;
    private final int SLEEP = 0, DOZE = 1;
    private int currentState = 0; //��ʼ��˯��

    private double systemTime = 0;
    private double lastSystemTime = 0; //�ϸ�ϵͳʱ��
    private double nextArrivalTime = exponential(Simulation.ARRIVALRATE); //�´ε���ʱ��

    //ͳ��delay
    private double totalDelay = 0;
    private int totalPacket = 0;
    
    //ͳ�Ƹ���״̬ʱ��
    private double activeTime = 0, sleepTime = 0, dozeTime = 0;
    private int wakeUpTimes;

    private Queue<Packet> buffer = new LinkedList<Packet>();
    public Customer(int identifier) {this.identifier = identifier;}

    @Override
    public void run() {
    	
    	double offerLoad = Simulation.ARRIVALRATE;
        while (true) {
            try {
            	
            	Simulation.customer_lock[identifier].acquire();
            	
//            	if (identifier == 0) {
//            		//System.out.println(offerLoad);
//            		System.out.println(Simulation.grant[identifier] + " ");
//            	}
            	
            	
                if(Simulation.grant[identifier] < -0.5) break;
                
                lastSystemTime = systemTime;
             
                double grant = Simulation.grant[identifier] + Simulation.RTT / 2; //grantʱ��
                
                if (grant < systemTime) Simulation.report[identifier] = 0;
                else {           
	                switch(currentState) {
	                    case SLEEP: //˯��
	                    {
	                    	boolean isWakeUp = false;
	                    	double wakeUpPoint = 0;
	                        if (buffer.size() >= Simulation.WAKEUPTHRESHOLD) currentState = DOZE; //˵����һ��grant��WakeUp�ڼ�
	                        
	                        while (systemTime < grant) { //�ƽ�ϵͳʱ��
	                            if (nextArrivalTime < grant) {
	                                addPacket(); 
		                            if (buffer.size() == Simulation.WAKEUPTHRESHOLD) { //������ֵ
		                            	isWakeUp = true;
		                            	wakeUpPoint = systemTime;
		                            	wakeUpTimes ++;
		                                if (grant > (systemTime + Simulation.WAKEUPTIME)) currentState = DOZE; //grantʱ���Ѿ�����DOZE
		                                
		                                else { //grantʱ�̴���WAKEUP
		                                    grant = systemTime + Simulation.WAKEUPTIME; //�ƽ���WAKEUP����ʱ��
		                                    currentState = SLEEP;
		                                }
		                            }
	                            } else { //�´�arrivalʱ�̳���grant                         	
	                            	if (isWakeUp) {                      		
	                            		sleepTime += wakeUpPoint - lastSystemTime;
	                            		if (grant > (wakeUpPoint + Simulation.WAKEUPTIME)) dozeTime += grant - (wakeUpPoint + Simulation.WAKEUPTIME);
	                            	}
	                            	else { //û������
	                            		if (currentState == SLEEP) sleepTime += grant - lastSystemTime;
	                            		if (currentState == DOZE) dozeTime += grant - lastSystemTime;
	                            	}                          	
	                                systemTime = grant;
	                                if (currentState == DOZE) Simulation.report[identifier] = buffer.size();      
	                                if (currentState == SLEEP) Simulation.report[identifier] = 0;
	                                break;
	                            }  
	                        }
	                        break;
	                    }
	                    case DOZE: //�˯
	                    {
	                        while (systemTime < grant) {
	                            if (nextArrivalTime < grant) {
	                                addPacket();
	                            } else {
	                                systemTime = grant;
	                                break;
	                            } //�ƽ���grant,��ʼ��ȥ
	                        }
	                        dozeTime += grant - lastSystemTime;
	                        //System.out.println("A: " + (grant - lastSystemTime));
	
	                        for (int i = 0; i < Simulation.report[identifier]; i++) { //�����ϴα������
	                            Packet tmp = buffer.remove();
	                            tmp.setDepartureTime(systemTime + (i+1) * Simulation.PACKETBIT / Simulation.RATE);
	                            totalDelay += tmp.getDepartureTime() - tmp.getArriveTime();
	                            totalPacket++;
	                        }
	                        grant += Simulation.report[identifier] * Simulation.PACKETBIT / Simulation.RATE;
	                        activeTime += Simulation.report[identifier] * Simulation.PACKETBIT / Simulation.RATE;
	                        
	                        while (systemTime < grant) {//�ƽ������������Ժ�
	                            if (nextArrivalTime < grant) addPacket();
	                            else {
	                                systemTime = grant;
	                                break;
	                            }
	                        }
	                        if (buffer.size() >= Simulation.SLEEPTHRESHOLD) { //������ֵ,�´μ�������
	                            Simulation.report[identifier] = buffer.size();
	                            currentState = DOZE;
	                        } else {
	                            currentState = SLEEP; //����˯��
	                            Simulation.report[identifier] = 0;
	                        }
	                        break;
	                    }
	                }
                }
                //System.out.println("system time: " + systemTime);
            }catch (InterruptedException e){
                e.printStackTrace();
            }
            Simulation.server_lock.release();
        }   
        if (identifier == 0){
        	System.out.println("OfferLoad: " + offerLoad);
 	        System.out.print("Average delay: ");
 	        System.out.println(totalDelay / totalPacket);
 	        System.out.println("Total system time: " + systemTime);
 	        System.out.println("Sleep time: " + sleepTime);
 	        System.out.println("Active time: " + activeTime);
 	        System.out.println("wakeUpTimes: " + wakeUpTimes);
 	        System.out.println("WakeUp time: " + wakeUpTimes * Simulation.WAKEUPTIME);
 	        System.out.println("Doze time: " + dozeTime);
 	        System.out.println("SleepTime + ActiveTime + DozeTime + WakeUpTimes: " + (sleepTime + activeTime + dozeTime + (wakeUpTimes) * Simulation.WAKEUPTIME)); 
 	        System.out.println();
        }
    }
    
    private double exponential(double mean) {return (-(1 / mean) * Math.log(Math.random()));}

    private void addPacket(){	
    	systemTime = nextArrivalTime; //����ϵͳʱ��
        Packet tmp = new Packet();
        tmp.setArriveTime(systemTime);
        buffer.add(tmp);
        nextArrivalTime = systemTime + exponential(Simulation.ARRIVALRATE); //�����´ε���ʱ��
    }
}
