package io.brooklyn.entity.web;

import com.hazelcast.actors.api.ActorRef;
import com.hazelcast.actors.api.MessageDeliveryFailure;
import io.brooklyn.attributes.BasicAttributeRef;
import io.brooklyn.attributes.IntAttributeRef;
import io.brooklyn.attributes.ListAttributeRef;
import io.brooklyn.entity.Entity;
import io.brooklyn.entity.EntityConfig;
import io.brooklyn.entity.policies.LoadBalancingPolicyConfig;

import java.io.Serializable;

public class WebCluster extends Entity {

    private final ListAttributeRef<ActorRef> children = newListAttributeRef("children", ActorRef.class);
    private final BasicAttributeRef<ActorRef> loadBalancingPolicy = newBasicAttributeRef("loadBalancingPolicy", ActorRef.class);
    private final IntAttributeRef nextHttpPort = newIntAttributeRef("nextHttpPort", 8085);
    private final IntAttributeRef nextJmxPort = newIntAttributeRef("nextJmxPort", 20001);
    private final IntAttributeRef nextShutdownPort = newIntAttributeRef("nextShutdownPort", 9001);

    @Override
    public void activate() throws Exception {
        super.activate();

        EntityConfig lbPolicyConfig = new LoadBalancingPolicyConfig()
                .addProperty(LoadBalancingPolicyConfig.CLUSTER, self());

        loadBalancingPolicy.set(newEntity(lbPolicyConfig));
    }

    public void receive(ChildFailure message) {
        System.out.println("WebCluster.BrokenChild@" + self());

        //remove the child.
        children.remove(message.tomcat);

        receive(new ScaleTo(children.size() + 1));
    }

    public void receive(SimulateTomcatFailure msg) {
        System.out.println("WebCluster.SimulateTomcatFailure@" + self());
        if (children.isEmpty()) return;

        //select a random tomcat machine to fail
        ActorRef randomFailedTomcat = children.get(0);
        send(randomFailedTomcat, new Tomcat.TomcatFailure());
    }

    public void receive(ScaleTo scaleTo) {
        System.out.println("WebCluster.ScaleToMessage@" + self());

        int delta = scaleTo.size - children.size();
        if (delta == 0) {
            //no change.
        } else if (delta > 0) {
            //we need to add machines.
            for (int k = 0; k < delta; k++) {
                TomcatConfig tomcatConfig = new TomcatConfig()
                        .httpPort(nextHttpPort.getAndInc())
                        .jmxPort(nextJmxPort.getAndInc())
                        .shutdownPort(nextShutdownPort.getAndInc())
                        .cluster(self());
                ActorRef tomcat = newEntity(tomcatConfig);
                children.add(tomcat);

                //hook the loadBalancingPolicy up to the newly created tomcat.
                subscribeToAttribute(loadBalancingPolicy, tomcat, Tomcat.STATE);

                //lets start tomcat.
                send(tomcat, new Tomcat.StartTomcat("localhost"));
            }
        } else {
            //we need to remove machines.
            for (int k = 0; k < -delta; k++) {
                ActorRef tomcat = children.removeFirst();
                send(tomcat, new Tomcat.StopTomcat());
            }
        }
    }

    public void receive(MessageDeliveryFailure messageDeliveryFailure) {
        //todo: react on child processing failures
    }

    //signals the webcluster that it should simulate a failure.
    public static class SimulateTomcatFailure implements Serializable {
    }

    //send by a child when it is in trouble.
    public static class ChildFailure implements Serializable {
        public final ActorRef tomcat;

        public ChildFailure(ActorRef tomcat) {
            this.tomcat = tomcat;
        }
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
