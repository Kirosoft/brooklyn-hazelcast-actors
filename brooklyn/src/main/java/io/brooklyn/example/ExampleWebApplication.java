package io.brooklyn.example;

import com.hazelcast.actors.api.ActorRecipe;
import com.hazelcast.actors.api.ActorRef;
import io.brooklyn.Application;
import io.brooklyn.attributes.Attribute;
import io.brooklyn.attributes.BasicAttributeRef;
import io.brooklyn.web.WebCluster;

public class ExampleWebApplication extends Application {

    private final BasicAttributeRef<ActorRef> webClusterAttribute = newBasicAttributeRef(new Attribute<ActorRef>("web"));

    @Override
    public void init(ActorRecipe actorRecipe) {
        super.init(actorRecipe);

        ActorRef webcluster = getActorRuntime().newActor(WebCluster.class);
        webClusterAttribute.set(webcluster);

        //actorRuntime.send(webcluster, new WebCluster.ScaleToMessage(10));
        //actorRuntime.send(webcluster, new WebCluster.SimulateTomcatFailure());
    }

    //public vod

    public void receive(WebCluster.ScaleToMessage msg, ActorRef sender){
        getActorRuntime().send(sender, msg);
    }

    public void receive(WebCluster.SimulateTomcatFailure msg, ActorRef sender){
        getActorRuntime().send(sender, msg);
    }
}
