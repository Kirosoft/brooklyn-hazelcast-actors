package io.brooklyn.entity.policies;

import brooklyn.entity.basic.Lifecycle;
import com.hazelcast.actors.api.ActorRef;
import io.brooklyn.attributes.Attribute;
import io.brooklyn.attributes.BasicAttributeRef;
import io.brooklyn.attributes.SensorEvent;
import io.brooklyn.entity.EntityConfig;
import io.brooklyn.entity.web.WebCluster;

/**
 * The ReplaceWebServerOnFirePolicy is a policy that automatically replaces a webserver within a WebCluster
 * when that webserver is on fire.
 */
public class ReplaceWebServerOnFirePolicy extends Policy {

    public final BasicAttributeRef<ActorRef> cluster = newBasicAttributeRef(Config.CLUSTER);

    public void receive(SensorEvent e) {
        System.out.println(self() + ":ReplaceWebServerOnFirePolicy:" + e);

        if (!Lifecycle.ON_FIRE.equals(e.getNewValue())) {
            return;
        }

        ActorRef webServerOnFire = e.getSource();
        send(cluster, new WebCluster.ReplaceWebServer(webServerOnFire));
    }

    public static class Config extends EntityConfig {
        public static final Attribute<ActorRef> CLUSTER = new Attribute<>("cluster");

        public Config() {
            super(ReplaceWebServerOnFirePolicy.class);
        }

        public Config cluster(ActorRef cluster) {
            addProperty(CLUSTER, cluster);
            return this;
        }

        public Config cluster(BasicAttributeRef<ActorRef> cluster) {
            addProperty(CLUSTER, cluster.get());
            return this;
        }
    }
}
