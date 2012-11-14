package io.brooklyn.example;

import com.hazelcast.actors.api.ActorRecipe;
import com.hazelcast.actors.api.ActorRef;
import io.brooklyn.Application;
import io.brooklyn.attributes.Attribute;
import io.brooklyn.attributes.BasicAttributeRef;
import io.brooklyn.web.WebCluster;

import java.io.Serializable;

public class ExampleWebApplication extends Application {

    private final BasicAttributeRef<ActorRef> web = newBasicAttributeRef(new Attribute<ActorRef>("web"));

    @Override
    public void init(ActorRecipe actorRecipe) throws Exception {
        super.init(actorRecipe);

        ActorRef webcluster = getActorRuntime().newActor(WebCluster.class);
        web.set(webcluster);
    }

    public void receive(StartMessage msg) {
        getActorRuntime().send(web.get(), new WebCluster.ScaleToMessage(1));
    }

    public void receive(WebCluster.ScaleToMessage msg, ActorRef sender) {
        getActorRuntime().send(sender, msg);
    }

    public void receive(WebCluster.SimulateTomcatFailure msg, ActorRef sender) {
        getActorRuntime().send(sender, msg);
    }

    public static class StartMessage implements Serializable {

    }
}
