package io.brooklyn.policy;

import com.hazelcast.actors.actors.DispatchingActor;
import io.brooklyn.attributes.SensorEvent;

public class LamePolicy extends DispatchingActor {

    public void receive(SensorEvent e) {
        System.out.println("Sensor: " + e);
    }
}
