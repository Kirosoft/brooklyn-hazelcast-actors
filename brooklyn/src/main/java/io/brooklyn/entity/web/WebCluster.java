package io.brooklyn.entity.web;

import com.hazelcast.actors.api.ActorRef;
import com.hazelcast.actors.api.MessageDeliveryFailure;
import com.hazelcast.actors.utils.MutableMap;
import io.brooklyn.attributes.BasicAttributeRef;
import io.brooklyn.attributes.ListAttributeRef;
import io.brooklyn.entity.Entity;
import io.brooklyn.policy.LoadBalancingPolicy;

import java.io.Serializable;

public class WebCluster extends Entity {

    private ListAttributeRef<ActorRef> children = newListAttributeRef("children", ActorRef.class);
    private BasicAttributeRef<ActorRef> loadBalancingPolicy = newBasicAttributeRef("loadBalancingPolicy", ActorRef.class);

    @Override
    public void activate() throws Exception {
        super.activate();

        loadBalancingPolicy.set(getActorRuntime().newActor(
                LoadBalancingPolicy.class,
                MutableMap.map(LoadBalancingPolicy.CLUSTER.getName(), self())));
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
        getActorRuntime().send(self(), randomFailedTomcat, new Tomcat.TomcatFailure());
    }

    public void receive(ScaleTo scaleTo) {
        System.out.println("WebCluster.ScaleToMessage@" + self());

        int delta = scaleTo.size - children.size();
        if (delta == 0) {
            //no change.
        } else if (delta > 0) {
            //we need to add machines.
            for (int k = 0; k < delta; k++) {
                ActorRef tomcat = getActorRuntime().newActor(Tomcat.class);
                children.add(tomcat);

                //subscribeToAttribute the loadbalancingPolicy to the status field of tomcat.
                getManagementContext().subscribeToAttribute(loadBalancingPolicy.get(), tomcat, Tomcat.STATE);

                //lets start tomcat.
                getActorRuntime().send(self(), tomcat, new Tomcat.StartTomcat("localhost", self()));
            }
        } else {
            //we need to remove machines.
            for (int k = 0; k < -delta; k++) {
                ActorRef tomcat = children.removeFirst();
                getActorRuntime().send(self(), tomcat, new Tomcat.StopTomcat());
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
