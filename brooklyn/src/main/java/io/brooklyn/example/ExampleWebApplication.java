package io.brooklyn.example;

import com.hazelcast.actors.actors.EchoActor;
import com.hazelcast.actors.api.ActorRecipe;
import com.hazelcast.actors.api.ActorRef;
import io.brooklyn.attributes.ReferenceAttribute;
import io.brooklyn.entity.EntityReference;
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

    private final ReferenceAttribute<EntityReference> webCluster = newReferenceAttribute("webCluster");
    private final ReferenceAttribute<EntityReference> policy = newReferenceAttribute("policy");
    private final ReferenceAttribute<EntityReference> machine = newReferenceAttribute("machine");

    public void receive(SoftwareProcess.Start msg) {
        if (log.isDebugEnabled()) log.debug(self() + ":ExampleWebApplication:Start");

        //create the webcluster; we configure it with a tomcatfactory.
        WebClusterConfig webclusterConfig = new WebClusterConfig().webserverFactory(new TomcatFactory());
        webCluster.set(spawnAndLink(webclusterConfig));

        //create the policy and configure it with the webCluster.
        policy.set(spawnAndLink(new ReplaceWebServerOnFirePolicy.Config().cluster(webCluster)));
        //start the policy with the webCluster. It will be notified when a state change happens in the webservers of
        //the cluster.
        send(webCluster, new RelationsAttributeSubscription("webservers", policy, SoftwareProcess.STATE));

        //lets print the result that are published on the average used heap
        ActorRef echoer = getActorRuntime().spawnAndLink(self().toActorRef(), new ActorRecipe(EchoActor.class));
        send(webCluster, new RelationsAttributeSubscription("webservers", echoer, Tomcat.AVERAGE_USED_HEAP));

        send(webCluster, new SoftwareProcess.Start(msg.location));
        send(webCluster, new WebCluster.ScaleTo(1));
    }
}
