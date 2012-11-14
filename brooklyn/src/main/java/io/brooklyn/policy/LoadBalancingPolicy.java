package io.brooklyn.policy;

import com.hazelcast.actors.api.ActorRef;
import io.brooklyn.Entity;
import io.brooklyn.SoftwareProcessEntityStatus;
import io.brooklyn.attributes.Attribute;
import io.brooklyn.attributes.BasicAttributeRef;
import io.brooklyn.attributes.SensorEvent;


public class LoadBalancingPolicy extends Entity {

    public static final Attribute<ActorRef> CLUSTER = new Attribute<ActorRef>("cluster");

    public final BasicAttributeRef<ActorRef> cluster = newBasicAttributeRef(CLUSTER);

    public void receive(SensorEvent e) {
        if (!SoftwareProcessEntityStatus.FAILURE.equals(e.getNewValue())) {
            return;
        }

        System.out.println("Detected a machine on fire: " + e);
    }
}
