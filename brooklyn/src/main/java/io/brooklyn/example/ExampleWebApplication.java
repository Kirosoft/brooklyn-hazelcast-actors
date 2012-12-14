package io.brooklyn.example;

import com.hazelcast.actors.actors.EchoActor;
import com.hazelcast.actors.api.ActorRecipe;
import com.hazelcast.actors.api.ActorRef;
import io.brooklyn.attributes.RelationAttribute;
import io.brooklyn.entity.application.Application;
import io.brooklyn.entity.policies.ReplaceWebServerOnFirePolicy;
import io.brooklyn.entity.softwareprocess.SoftwareProcess;
import io.brooklyn.entity.web.Tomcat;
import io.brooklyn.entity.web.TomcatFactory;
import io.brooklyn.entity.web.WebCluster;
import io.brooklyn.entity.web.WebClusterConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExampleWebApplication extends Application {

    private static final Logger log = LoggerFactory.getLogger(ExampleWebApplication.class);

    private final RelationAttribute webCluster = newRelationAttribute("webCluster");
    private final RelationAttribute policy = newRelationAttribute("policy");
    private final RelationAttribute machine = newRelationAttribute("machine");

    public void receive(SoftwareProcess.Start msg) {
        if (log.isDebugEnabled()) log.debug(self() + ":ExampleWebApplication:Start");

        //create the webcluster; we configure it with a tomcatfactory.
        WebClusterConfig webclusterConfig = new WebClusterConfig().webserverFactory(new TomcatFactory());
        webCluster.start(webclusterConfig);

        //create the policy and configure it with the webCluster.
        policy.start(new ReplaceWebServerOnFirePolicy.Config().cluster(webCluster));
        //start the policy with the webCluster. It will be notified when a state change happens in the webservers of
        //the cluster.

        webCluster.send(new RelationsAttributeSubscription("webservers", policy, SoftwareProcess.STATE));

        //lets print the result that are published on the average used heap
        ActorRef echoer = getActorRuntime().spawnAndLink(self().toActorRef(), new ActorRecipe(EchoActor.class));
        webCluster.send(new RelationsAttributeSubscription("webservers", echoer, Tomcat.AVERAGE_USED_HEAP));

        webCluster.send(new SoftwareProcess.Start(msg.location));
        webCluster.send(new WebCluster.ScaleTo(1));
    }
}
