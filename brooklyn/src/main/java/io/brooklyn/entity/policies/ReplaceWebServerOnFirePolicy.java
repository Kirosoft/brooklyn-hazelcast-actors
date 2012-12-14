package io.brooklyn.entity.policies;

import brooklyn.entity.basic.Lifecycle;
import io.brooklyn.attributes.AttributeType;
import io.brooklyn.attributes.ReferenceAttribute;
import io.brooklyn.attributes.SensorEvent;
import io.brooklyn.entity.EntityConfig;
import io.brooklyn.entity.EntityReference;
import io.brooklyn.entity.web.WebCluster;

/**
 * The ReplaceWebServerOnFirePolicy is a policy that automatically replaces a webserver within a WebCluster
 * when that webserver is on fire.
 */
public class ReplaceWebServerOnFirePolicy extends Policy {

    public final ReferenceAttribute<EntityReference> cluster = newReferenceAttribute(Config.CLUSTER);

    public void receive(SensorEvent e) {
        System.out.println(self() + ":ReplaceWebServerOnFirePolicy:" + e);

        if (!Lifecycle.ON_FIRE.equals(e.getNewValue())) {
            return;
        }

        EntityReference webServerOnFire = e.getSource();
        send(cluster, new WebCluster.ReplaceWebServer(webServerOnFire));
    }

    public static class Config extends EntityConfig {
        public static final AttributeType<EntityReference> CLUSTER = new AttributeType<>("cluster");

        public Config() {
            super(ReplaceWebServerOnFirePolicy.class);
        }

        public Config cluster(EntityReference cluster) {
            addProperty(CLUSTER, cluster);
            return this;
        }

        public Config cluster(ReferenceAttribute<EntityReference> cluster) {
            addProperty(CLUSTER, cluster.get());
            return this;
        }
    }
}
