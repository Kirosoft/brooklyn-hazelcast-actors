package io.brooklyn.policy;

import com.hazelcast.actors.AbstractActor;
import com.hazelcast.actors.ActorRef;
import io.brooklyn.SensorEvent;

public class Policy extends AbstractActor {

   public void receive(SensorEvent e, ActorRef source){
        System.out.println("Sensor: "+e);
    }
}
