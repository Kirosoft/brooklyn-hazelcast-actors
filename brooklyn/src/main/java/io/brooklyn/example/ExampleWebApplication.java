package io.brooklyn.example;

import com.hazelcast.actors.actors.EchoActor;
import com.hazelcast.actors.api.ActorRef;
import io.brooklyn.attributes.BasicAttributeRef;
import io.brooklyn.entity.Start;
import io.brooklyn.entity.application.Application;
import io.brooklyn.entity.policies.ReplaceWebServerOnFirePolicy;
import io.brooklyn.entity.softwareprocess.SoftwareProcess;
import io.brooklyn.entity.web.Tomcat;
import io.brooklyn.entity.web.WebCluster;
import io.brooklyn.entity.web.WebClusterConfig;

public class ExampleWebApplication extends Application {

    private final BasicAttributeRef<ActorRef> webCluster = newBasicAttributeRef("webCluster");
    private final BasicAttributeRef<ActorRef> policy = newBasicAttributeRef("policy");


    public void receive(Start msg) {
        System.out.println(self() + ":ExampleWebApplication:Start");

        location.set(msg.location);

        webCluster.set(newEntity(new WebClusterConfig()));

        //create the policy and configures it with the webCluster.
        policy.set(newEntity(new ReplaceWebServerOnFirePolicy.Config().cluster(webCluster)));

        ActorRef echoer = getActorRuntime().newActor(EchoActor.class);

        //register the policy with the webCluster. It will be notified when a state change happens in the webservers of the cluster.
        send(webCluster, new WebCluster.WebServerPolicyRegistration(policy, SoftwareProcess.STATE));

        //lets print the result that are published on the average used heap
        send(webCluster, new WebCluster.WebServerPolicyRegistration(echoer, Tomcat.AVERAGE_USED_HEAP));

        send(webCluster, new Start(location));
        send(webCluster, new WebCluster.ScaleTo(1));
    }
}
