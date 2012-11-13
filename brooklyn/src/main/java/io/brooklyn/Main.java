package io.brooklyn;

import com.hazelcast.actors.actors.EchoActor;
import com.hazelcast.actors.api.ActorRef;
import com.hazelcast.actors.api.ActorRuntime;
import com.hazelcast.actors.service.ActorService;
import com.hazelcast.actors.service.ActorServiceConfig;
import com.hazelcast.config.Config;
import com.hazelcast.config.Services;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import io.brooklyn.web.Tomcat;

import static com.hazelcast.actors.utils.MutableMap.map;

public class Main {

    public static void main(String[] args) throws Exception {
        Config config = new Config();
        config.getNetworkConfig().getJoin().getMulticastConfig().setEnabled(true);
        Services services = config.getServicesConfig();

        ActorServiceConfig actorServiceConfig = new ActorServiceConfig();
        LocalManagementContext managementContext = new LocalManagementContext();
        actorServiceConfig.addDependency("managementContext", managementContext);
        services.addServiceConfig(actorServiceConfig);

        HazelcastInstance hzInstance = Hazelcast.newHazelcastInstance(config);

        ActorRuntime actorRuntime = (ActorRuntime) hzInstance.getServiceProxy(ActorService.NAME, "foo");
        managementContext.init(hzInstance, actorRuntime);
        //ActorApplication actorApplication = new ActorApplication(new Context(), actorRuntime);
        //actorApplication.startServer();

        //ActorRef echoer = actorRuntime.newActor(EchoActor.class);
        //System.out.println(echoer);


        ActorRef tomcat = actorRuntime.newActor(Tomcat.class, map("httpPort", 8085, "jmxPort", 20001,"shutdownPort", 9001));
        actorRuntime.send(tomcat, new Tomcat.StartTomcatMessage("localhost"));

        ActorRef tomcat1 = actorRuntime.newActor(Tomcat.class, map("httpPort", 8086, "jmxPort", 30000,"shutdownPort", 9002));
        actorRuntime.send(tomcat1, new Tomcat.StartTomcatMessage("localhost"));

        //ActorRef tomcat2 = actorRuntime.newActor(Tomcat.class, map("httPort", 8087, "jmxPort", 10002));
        //actorRuntime.send(tomcat2, new Tomcat.StartTomcatMessage("localhost"));


        ActorRef echor = actorRuntime.newActor(EchoActor.class);
        managementContext.subscribe(echor, tomcat, Tomcat.MAX_HEAP);
        managementContext.subscribe(echor, tomcat, Tomcat.USED_HEAP);
        managementContext.subscribe(echor, tomcat1, Tomcat.MAX_HEAP);
        managementContext.subscribe(echor, tomcat1, Tomcat.USED_HEAP);
        //managementContext.subscribe(echor, tomcat2, Tomcat.MAX_HEAP);
        //managementContext.subscribe(echor, tomcat2, Tomcat.USED_HEAP);

        //  actorRuntime.send(tomcat, new Tomcat.DeployMessage("foo.war"));

        //ActorRef policy = actorRuntime.newActor(Policy.class);
        //actorRuntime.send(tomcat, new Entity.SubscribeMessage(policy, "maxHeap"));
        //actorRuntime.send(tomcat, new Entity.SubscribeMessage(policy, "usedHeap"));

        //ActorRef application = actorRuntime.newActor(ExampleWebApplication.class);
        //actorRuntime.send(application, new WebCluster.ScaleToMessage(10));
        //actorRuntime.send(application, new Tomcat.DeployMessage("foo.war"));
        //actorRuntime.send(application, new WebCluster.SimulateTomcatFailure());
    }
}
