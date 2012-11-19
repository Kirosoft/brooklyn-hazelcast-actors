package io.brooklyn.example;

import com.hazelcast.actors.api.ActorRef;
import io.brooklyn.attributes.Attribute;
import io.brooklyn.attributes.BasicAttributeRef;
import io.brooklyn.entity.Entity;
import io.brooklyn.entity.web.WebCluster;

import java.io.Serializable;

public class ExampleWebApplication extends Entity {

    private final BasicAttributeRef<ActorRef> web = newBasicAttributeRef(new Attribute<ActorRef>("web"));

    public void receive(StartMessage msg) {
        ActorRef webcluster = getActorRuntime().newActor(WebCluster.class);
        web.set(webcluster);
        getActorRuntime().send(web.get(), new WebCluster.ScaleTo(1));
    }

    public void receive(WebCluster.ScaleTo msg, ActorRef sender) {
        getActorRuntime().send(sender, msg);
    }

    public void receive(WebCluster.SimulateTomcatFailure msg, ActorRef sender) {
        getActorRuntime().send(sender, msg);
    }

    public static class StartMessage implements Serializable {

    }
}
