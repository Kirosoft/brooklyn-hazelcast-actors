package io.brooklyn;

import com.hazelcast.actors.ActorRef;
import com.hazelcast.actors.ActorRuntime;
import com.hazelcast.actors.ActorService;
import com.hazelcast.actors.ActorServiceConfig;
import com.hazelcast.actors.EchoActor;
import com.hazelcast.config.Config;
import com.hazelcast.config.Services;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;

public class Main {

    public static void main(String[] args) throws Exception {
        Config config = new Config();
        config.getNetworkConfig().getJoin().getMulticastConfig().setEnabled(true);
        Services services = config.getServicesConfig();

        ActorServiceConfig actorServiceConfig = new ActorServiceConfig();
        ManagementContext managementContext = new LocalManagementContext();
        actorServiceConfig.addDependency("managementContext", managementContext);
        actorServiceConfig.setName(ActorService.NAME);
        actorServiceConfig.setEnabled(true);
        actorServiceConfig.setClassName(ActorService.class.getName());
        services.addServiceConfig(actorServiceConfig);

        HazelcastInstance hzInstance = Hazelcast.newHazelcastInstance(config);

        ActorRuntime actorRuntime = (ActorRuntime) hzInstance.getServiceProxy(ActorService.NAME, "foo");
        //ActorApplication actorApplication = new ActorApplication(new Context(), actorRuntime);
        //actorApplication.startServer();

        ActorRef echoer = actorRuntime.newActor(EchoActor.class);
        System.out.println(echoer);

        //ActorRef tomcat = actorRuntime.newActor(Tomcat.class, map("port", 8080, "ssl", false));
        //actorRuntime.send(tomcat, new Tomcat.StartTomcatMessage("localhost"));
        //actorRuntime.send(tomcat, new Tomcat.DeployMessage("foo.war"));

        //ActorRef policy = actorRuntime.newActor(Policy.class);
        //actorRuntime.send(tomcat, new Entity.SubscribeMessage(policy, "maxHeap"));
        //actorRuntime.send(tomcat, new Entity.SubscribeMessage(policy, "usedHeap"));

        //ActorRef application = actorRuntime.newActor(ExampleWebApplication.class);
        //actorRuntime.send(application, new WebCluster.ScaleToMessage(10));
        //actorRuntime.send(application, new Tomcat.DeployMessage("foo.war"));
        //actorRuntime.send(application, new WebCluster.SimulateTomcatFailure());
    }
}
