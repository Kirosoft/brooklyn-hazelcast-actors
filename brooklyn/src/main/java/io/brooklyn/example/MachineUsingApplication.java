package io.brooklyn.example;

import brooklyn.config.BrooklynProperties;
import brooklyn.location.basic.SshMachineLocation;
import com.hazelcast.actors.api.ActorRef;
import com.hazelcast.actors.api.ActorRuntime;
import com.hazelcast.actors.impl.ActorService;
import com.hazelcast.actors.impl.ActorServiceConfig;
import com.hazelcast.actors.impl.BasicActorFactory;
import com.hazelcast.actors.utils.MutableMap;
import com.hazelcast.config.Config;
import com.hazelcast.config.Services;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import io.brooklyn.LocalManagementContext;
import io.brooklyn.attributes.BasicAttributeRef;
import io.brooklyn.entity.Start;
import io.brooklyn.entity.application.Application;
import io.brooklyn.entity.application.ApplicationConfig;
import io.brooklyn.entity.machines.Machine;
import io.brooklyn.entity.web.TomcatConfig;

import java.net.InetAddress;
import java.util.Map;

public class MachineUsingApplication extends Application {

    private final BasicAttributeRef<ActorRef> machine1 = newBasicAttributeRef("machine1", ActorRef.class);
    private final BasicAttributeRef<ActorRef> tomcat = newBasicAttributeRef("tomcat", ActorRef.class);

    public void receive(Start start) {
        Machine.MachineConfig machineConfig = new Machine.MachineConfig();
        machine1.set(spawnAndLink(machineConfig));

        TomcatConfig tomcatConfig = new TomcatConfig();

        //todo: we need to get notified
        send(machine1, new Machine.StartSoftwareProcess(tomcatConfig));
    }

    public static void main(String[] args) throws Exception {
        Config config = new Config();
        config.getNetworkConfig().getJoin().getMulticastConfig().setEnabled(true);
        Services services = config.getServicesConfig();

        LocalManagementContext managementContext = new LocalManagementContext();

        Map<String, Object> dependencies = MutableMap.map("managementContext", managementContext);
        ActorServiceConfig actorServiceConfig = new ActorServiceConfig();
        actorServiceConfig.setActorFactory(new BasicActorFactory(dependencies));
        services.addServiceConfig(actorServiceConfig);

        HazelcastInstance hzInstance = Hazelcast.newHazelcastInstance(config);

        ActorRuntime actorRuntime = (ActorRuntime) hzInstance.getServiceProxy(ActorService.NAME, "foo");
        managementContext.init(hzInstance, actorRuntime);

        BrooklynProperties brooklynProperties = BrooklynProperties.Factory.newDefault();

        Map props = MutableMap.map(
                "user", brooklynProperties.get("user.name"),
                "privateKeyFile", brooklynProperties.get("brooklyn.jclouds.private-key-file"),
                "address", InetAddress.getByName("127.0.0.1"));
        SshMachineLocation location = new SshMachineLocation(props);

        ApplicationConfig applicationConfig = new ApplicationConfig(Application.class);
        ActorRef application = managementContext.spawn(applicationConfig);
        actorRuntime.send(application, new Start(location));
    }
}
