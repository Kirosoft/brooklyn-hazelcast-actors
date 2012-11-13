package io.brooklyn.policy;

import com.hazelcast.actors.actors.ReflectiveActor;
import io.brooklyn.attributes.SensorEvent;

public class Policy extends ReflectiveActor {

    public void receive(SensorEvent e) {
        System.out.println("Sensor: " + e);
    }
}
