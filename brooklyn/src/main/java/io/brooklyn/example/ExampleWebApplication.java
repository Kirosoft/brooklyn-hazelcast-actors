package io.brooklyn.example;

import com.hazelcast.actors.actors.EchoActor;
import com.hazelcast.actors.api.ActorRef;
import io.brooklyn.attributes.BasicAttributeRef;
import io.brooklyn.entity.application.Application;
import io.brooklyn.entity.policies.ReplaceWebServerOnFirePolicy;
import io.brooklyn.entity.softwareprocess.SoftwareProcess;
import io.brooklyn.entity.web.Tomcat;
import io.brooklyn.entity.web.WebCluster;
import io.brooklyn.entity.web.WebClusterConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExampleWebApplication extends Application {

    private static final Logger log = LoggerFactory.getLogger(ExampleWebApplication.class);

    private final BasicAttributeRef<ActorRef> webCluster = newBasicAttributeRef("webCluster");
    private final BasicAttributeRef<ActorRef> policy = newBasicAttributeRef("policy");
    private final BasicAttributeRef<ActorRef> machine = newBasicAttributeRef("machine");

    public void receive(SoftwareProcess.Start msg) {
        if (log.isDebugEnabled()) log.debug(self() + ":ExampleWebApplication:Start");


        webCluster.set(spawnAndLink(new WebClusterConfig()));

        //create the policy and configure it with the webCluster.
        policy.set(spawnAndLink(new ReplaceWebServerOnFirePolicy.Config().cluster(webCluster)));
        //start the policy with the webCluster. It will be notified when a state change happens in the webservers of the cluster.
        send(webCluster, new WebCluster.ChildAttributeRegistration(policy, SoftwareProcess.STATE));

        //lets print then result that are published on the average used heap
        ActorRef echoer = spawnAndLink(EchoActor.class);
        send(webCluster, new WebCluster.ChildAttributeRegistration(echoer, Tomcat.AVERAGE_USED_HEAP));

        send(webCluster, new SoftwareProcess.Start(msg.location));
        send(webCluster, new WebCluster.ScaleTo(1));
    }
}
