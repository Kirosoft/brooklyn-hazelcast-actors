package io.brooklyn.web;

import com.hazelcast.actors.ActorRef;
import io.brooklyn.Entity;
import io.brooklyn.attributes.AttributeType;
import io.brooklyn.attributes.ListAttribute;

import java.io.Serializable;

public class WebCluster extends Entity {

    private ListAttribute<ActorRef> childrenAttribute = newListAttribute(new AttributeType<ActorRef>("childrenAttribute"));

    public void receive(ChildFailureMessage message, ActorRef sender) {
        System.out.println("WebCluster.BrokenChild@" + self());

        //remove the child.
        childrenAttribute.remove(message.tomcat);

        //create a new child.
        ActorRef tomcat = getActorRuntime().newActor(Tomcat.class);
        childrenAttribute.add(tomcat);
        getActorRuntime().send(self(), tomcat, new Tomcat.StartTomcatMessage("localhost", self()));
    }

    public void receive(SimulateTomcatFailure msg, ActorRef sender) {
        System.out.println("WebCluster.SimulateTomcatFailure@" + self());
        if (childrenAttribute.isEmpty()) return;

        //select a random tomcat machine to fail
        ActorRef randomFailedTomcat = childrenAttribute.get(0);
        getActorRuntime().send(self(), randomFailedTomcat, new Tomcat.TomcatFailureMessage());
    }

    public void receive(ScaleToMessage message, ActorRef sender) {
        System.out.println("WebCluster.ScaleToMessage@" + self());

        int delta = message.size - childrenAttribute.size();
        if (delta == 0) {
            //no change.
        } else if (delta > 0) {
            //we need to add machines.
            for (int k = 0; k < delta; k++) {
                ActorRef tomcat = getActorRuntime().newActor(Tomcat.class);
                childrenAttribute.add(tomcat);
                getActorRuntime().send(self(), tomcat, new Tomcat.StartTomcatMessage("localhost", self()));
            }
        } else {
            //we need to remove machines.
            for (int k = 0; k < -delta; k++) {
                ActorRef tomcat = childrenAttribute.removeFirst();
                getActorRuntime().send(self(), tomcat, new Tomcat.StopTomcatMessage());
            }
        }
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

    //scales the webcluster to a certain size.
    public static class ScaleToMessage implements Serializable {
        public final int size;

        public ScaleToMessage(int size) {
            if (size < 0) throw new IllegalArgumentException();
            this.size = size;
        }
    }
}
