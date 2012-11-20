package io.brooklyn.example;

import com.hazelcast.actors.api.ActorRef;
import io.brooklyn.attributes.Attribute;
import io.brooklyn.attributes.BasicAttributeRef;
import io.brooklyn.entity.Entity;
import io.brooklyn.entity.softwareprocess.SoftwareProcessConfig;
import io.brooklyn.entity.web.WebCluster;
import io.brooklyn.entity.web.WebClusterConfig;

import java.io.Serializable;

public class ExampleWebApplication extends Entity {

    private final BasicAttributeRef<ActorRef> web = newBasicAttributeRef("web");

    public void receive(StartMessage msg) {
        web.set(newEntity(new WebClusterConfig()));
        send(web, new WebCluster.ScaleTo(1));
    }

    public void receive(WebCluster.ScaleTo msg, ActorRef sender) {
        send(sender, msg);
    }

    public void receive(WebCluster.SimulateTomcatFailure msg, ActorRef sender) {
        send(sender, msg);
    }

    public static class StartMessage implements Serializable {

    }
}
