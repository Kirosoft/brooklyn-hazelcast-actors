package io.brooklyn.entity.web;

import com.hazelcast.actors.api.ActorRef;
import com.hazelcast.actors.api.MessageDeliveryFailure;
import io.brooklyn.attributes.BasicAttributeRef;
import io.brooklyn.attributes.IntAttributeRef;
import io.brooklyn.attributes.ListAttributeRef;
import io.brooklyn.entity.Entity;
import io.brooklyn.entity.EntityConfig;
import io.brooklyn.entity.Start;
import io.brooklyn.entity.policies.LoadBalancingPolicyConfig;
import io.brooklyn.entity.softwareprocess.SoftwareProcess;
import io.brooklyn.locations.Location;
import io.brooklyn.locations.SshMachineLocation;

import java.io.Serializable;

public class WebCluster extends Entity {

    private final ListAttributeRef<ActorRef> children = newListAttributeRef("children", ActorRef.class);
    private final BasicAttributeRef<ActorRef> loadBalancingPolicy = newBasicAttributeRef("loadBalancingPolicy", ActorRef.class);
    private final IntAttributeRef nextHttpPort = newIntAttributeRef("nextHttpPort", 8085);
    private final IntAttributeRef nextJmxPort = newIntAttributeRef("nextJmxPort", 20001);
    private final IntAttributeRef nextShutdownPort = newIntAttributeRef("nextShutdownPort", 9001);
    public final BasicAttributeRef<Location> location = newBasicAttributeRef("location");

    @Override
    public void activate() throws Exception {
        System.out.println("WebCluster:Activate");

        super.activate();

    }

    public void receive(Start start){
        System.out.println("WebCluster:Start");
        location.set(start.location);
        System.out.println("WebCluster:Complete");
    }

    public void receive(ScaleTo scaleTo) {
        System.out.println("WebCluster.ScaleToMessage@" + self());

        //todo: this code should be moved to the activate method. The problem is that Hazelcast currently runs into a
        //deadlock while doing hazelcast operations in the activate method.
        if (loadBalancingPolicy.isNull()) {
            EntityConfig lbPolicyConfig = new LoadBalancingPolicyConfig()
                    .addProperty(LoadBalancingPolicyConfig.CLUSTER, self());
            loadBalancingPolicy.set(newEntity(lbPolicyConfig));
        }

        int delta = scaleTo.size - children.size();
        if (delta == 0) {
            //no change.
        } else if (delta > 0) {
            //we need to add machines.
            for (int k = 0; k < delta; k++) {
                TomcatConfig tomcatConfig = new TomcatConfig()
                        .httpPort(nextHttpPort.getAndInc())
                        .jmxPort(nextJmxPort.getAndInc())
                        .shutdownPort(nextShutdownPort.getAndInc());
                ActorRef webServer = newEntity(tomcatConfig);
                children.add(webServer);

                //hook the loadBalancingPolicy up to the newly created webServer.
                subscribeToAttribute(loadBalancingPolicy, webServer, SoftwareProcess.STATE);

                //lets start webServer.
                send(webServer, new Start(location));
            }
        } else {
            //we need to remove machines.
            for (int k = 0; k < -delta; k++) {
                ActorRef webServer = children.removeFirst();
                send(webServer, new SoftwareProcess.Stop());
            }
        }
    }

    public void receive(MessageDeliveryFailure messageDeliveryFailure) {
        //todo: react on child processing failures
    }

    //scales the webcluster to a certain size.
    public static class ScaleTo implements Serializable {
        public final int size;

        public ScaleTo(int size) {
            if (size < 0) throw new IllegalArgumentException();
            this.size = size;
        }
    }
}
