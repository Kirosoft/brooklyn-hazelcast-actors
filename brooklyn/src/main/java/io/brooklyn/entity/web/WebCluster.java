package io.brooklyn.entity.web;

import com.hazelcast.actors.actors.EchoActor;
import com.hazelcast.actors.api.ActorRef;
import com.hazelcast.examples.TestApp;
import io.brooklyn.attributes.BasicAttributeRef;
import io.brooklyn.attributes.ListAttributeRef;
import io.brooklyn.entity.Entity;
import io.brooklyn.entity.EntityConfig;
import io.brooklyn.entity.Start;
import io.brooklyn.entity.policies.LoadBalancingPolicyConfig;
import io.brooklyn.entity.softwareprocess.SoftwareProcess;
import io.brooklyn.example.Echoer;

import java.io.Serializable;

public class WebCluster extends Entity {

    private final ListAttributeRef<ActorRef> children = newListAttributeRef("children", ActorRef.class);
    private final BasicAttributeRef<ActorRef> loadBalancingPolicy = newBasicAttributeRef("loadBalancingPolicy", ActorRef.class);

    @Override
    public void activate() throws Exception {
        System.out.println(self() + ":WebCluster:Activate");
        super.activate();
    }

    public void receive(Start start) {
        System.out.println(self() + ":WebCluster:Start");
        location.set(start.location);

        EntityConfig lbPolicyConfig = new LoadBalancingPolicyConfig()
                .addProperty(LoadBalancingPolicyConfig.CLUSTER, self());

        loadBalancingPolicy.set(newEntity(lbPolicyConfig));
        send(loadBalancingPolicy, start);

        System.out.println(self() + ":WebCluster:Start Complete");
    }

    public void receive(ScaleTo scaleTo) {
        System.out.println(self() + ":WebCluster.ScaleTo");

        int delta = scaleTo.size - children.size();
        if (delta == 0) {
            //no change.
        } else if (delta > 0) {
            //we need to add machines.
            for (int k = 0; k < delta; k++) {
                //todo: we want to use a factory here. We do not want to be tied to tomcat.
                ActorRef webServer = newEntity(new TomcatConfig());
                children.add(webServer);

                //hook the loadBalancingPolicy up to the newly created webServer.
                subscribeToAttribute(loadBalancingPolicy, webServer, SoftwareProcess.STATE);

                ActorRef echo = getActorRuntime().newActor(EchoActor.class);
                subscribeToAttribute(echo, webServer, TomcatConfig.USED_HEAP);

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

        System.out.println(self() + ":WebCluster.ScaleTo Complete");
    }

    //scales the webcluster to a certain size. This will be send by the loadBalancingPolicy.
    public static class ScaleTo implements Serializable {
        public final int size;

        public ScaleTo(int size) {
            if (size < 0) throw new IllegalArgumentException();
            this.size = size;
        }
    }
}
