package io.brooklyn.entity.policies;

import com.hazelcast.actors.api.ActorRef;
import io.brooklyn.attributes.AttributeType;
import io.brooklyn.attributes.ReferenceAttribute;
import io.brooklyn.attributes.SensorEvent;
import io.brooklyn.entity.EntityConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class LoadBalancingPolicy extends Policy {

    private static final Logger log = LoggerFactory.getLogger(LoadBalancingPolicy.class);


    public final ReferenceAttribute<ActorRef> cluster = newReferenceAttribute(Config.CLUSTER);

    private final Map<ActorRef, Double> performanceMap = new HashMap<>();

    public void receive(SensorEvent e) {


        //if (!SoftwareProcessEntityStatus.FAILURE.equals(e.getNewValue())) {
        //    return;
        //}

        //getActorRuntime().send(cluster, WebCluster.ScaleToMessage());

        if (log.isDebugEnabled()) log.debug("Detected a machine on fire: " + e);
    }

    public static class Config extends EntityConfig {

        public static final AttributeType<ActorRef> CLUSTER = new AttributeType<>("cluster");

        public Config() {
            super(LoadBalancingPolicy.class);
        }
    }
}
