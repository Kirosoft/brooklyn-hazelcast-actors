package io.brooklyn.example;

import com.hazelcast.actors.api.ActorRef;
import io.brooklyn.attributes.BasicAttributeRef;
import io.brooklyn.entity.Start;
import io.brooklyn.entity.application.Application;
import io.brooklyn.entity.web.WebCluster;
import io.brooklyn.entity.web.WebClusterConfig;
import io.brooklyn.locations.SshMachineLocation;

public class ExampleWebApplication extends Application {

    private final BasicAttributeRef<ActorRef> web = newBasicAttributeRef("web");

    public void receive(Start msg) {
        System.out.println("ExampleWebApplication:Start");
        location.set(msg.location);
        web.set(newEntity(new WebClusterConfig()));
        send(web, new Start(location));
        send(web, new WebCluster.ScaleTo(1));
        System.out.println("ExampleWebApplication:Started");
    }
}
