package example.hazelcast.brooklyn.policy;

import example.hazelcast.actors.AbstractActor;
import example.hazelcast.actors.ActorRef;
import example.hazelcast.brooklyn.SensorEvent;

public class Policy extends AbstractActor {

   public void receive(SensorEvent e, ActorRef source){
        System.out.println("Sensor: "+e);
    }
}
