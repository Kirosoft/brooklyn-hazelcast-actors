package io.brooklyn.web;

import com.hazelcast.actors.api.ActorRef;
import com.hazelcast.actors.api.MessageDeliveryFailure;
import com.hazelcast.actors.utils.MutableMap;
import io.brooklyn.Entity;
import io.brooklyn.attributes.Attribute;
import io.brooklyn.attributes.BasicAttributeRef;
import io.brooklyn.attributes.ListAttributeRef;
import io.brooklyn.policy.LoadBalancingPolicy;

import java.io.Serializable;

public class WebCluster extends Entity {

    private ListAttributeRef<ActorRef> children = newListAttribute(new Attribute<ActorRef>("children"));
    private BasicAttributeRef<ActorRef> loadBalancingPolicy = newBasicAttributeRef(new Attribute<ActorRef>("loadBalancingPolicy"));

    @Override
    public void activate() throws Exception {
        super.activate();

        loadBalancingPolicy.set(getActorRuntime().newActor(
                LoadBalancingPolicy.class,
                MutableMap.map(LoadBalancingPolicy.CLUSTER.getName(), self())));
    }

    public void receive(ChildFailureMessage message) {
        System.out.println("WebCluster.BrokenChild@" + self());

        //remove the child.
        children.remove(message.tomcat);

        receive(new ScaleToMessage(children.size() + 1));
    }

    public void receive(SimulateTomcatFailure msg) {
        System.out.println("WebCluster.SimulateTomcatFailure@" + self());
        if (children.isEmpty()) return;

        //select a random tomcat machine to fail
        ActorRef randomFailedTomcat = children.get(0);
        getActorRuntime().send(self(), randomFailedTomcat, new Tomcat.TomcatFailureMessage());
    }

    public void receive(ScaleToMessage message) {
        System.out.println("WebCluster.ScaleToMessage@" + self());

        int delta = message.size - children.size();
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
                getActorRuntime().send(self(), tomcat, new Tomcat.StartTomcatMessage("localhost", self()));
            }
        } else {
            //we need to remove machines.
            for (int k = 0; k < -delta; k++) {
                ActorRef tomcat = children.removeFirst();
                getActorRuntime().send(self(), tomcat, new Tomcat.StopTomcatMessage());
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
    public static class ChildFailureMessage implements Serializable {
        public final ActorRef tomcat;

        public ChildFailureMessage(ActorRef tomcat) {
            this.tomcat = tomcat;
        }
    }

    public static class ReplaceMessage implements Serializable{

    }

    //scales the webcluster to a certain size.
    public static class ScaleToMessage implements Serializable {
        public final int size;

        public ScaleToMessage(int size) {
            if (size < 0) throw new IllegalArgumentException();
            this.size = size;
        }
    }
}
