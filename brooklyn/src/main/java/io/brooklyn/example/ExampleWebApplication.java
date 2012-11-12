package io.brooklyn.example;

import com.hazelcast.actors.ActorRecipe;
import com.hazelcast.actors.ActorRef;
import io.brooklyn.Application;
import io.brooklyn.attributes.AttributeType;
import io.brooklyn.attributes.BasicAttribute;
import io.brooklyn.web.WebCluster;

public class ExampleWebApplication extends Application {

    private final BasicAttribute<ActorRef> webClusterAttribute = newBasicAttribute(new AttributeType<ActorRef>("web"));

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
