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
import io.brooklyn.entity.EntityConfig;
import io.brooklyn.entity.softwareprocess.SoftwareProcess;

import java.net.InetAddress;
import java.util.Map;

public class Main {

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

        //we spawn a new Application which will not be linked to any other actor; it will be a 'root' entity.
        ActorRef application = managementContext.spawn(new EntityConfig(ExampleWebApplication.class));
        actorRuntime.send(application, new SoftwareProcess.Start(location));
    }
}
