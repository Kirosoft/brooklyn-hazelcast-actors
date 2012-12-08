package com.hazelcast.actors.impl.actorcontainers;

import com.hazelcast.actors.TestActor;
import com.hazelcast.actors.api.ActorRecipe;
import com.hazelcast.actors.api.ActorRef;
import com.hazelcast.actors.impl.ActorService;
import com.hazelcast.actors.impl.ActorServiceConfig;
import com.hazelcast.actors.impl.BasicActorFactory;
import com.hazelcast.config.Config;
import com.hazelcast.config.Services;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.spi.impl.NodeServiceImpl;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.hazelcast.actors.TestUtils.newRandomActorRef;

public class ThreadPoolExecutorActorContainerTest {

    private static ActorService.ActorRuntimeProxyImpl actorRuntime;
    private static HazelcastInstance hzInstance;
    private static IMap<ActorRef, Set<ActorRef>> monitorMap;
    private static BasicActorFactory actorFactory;
    private static NodeServiceImpl nodeService;
    private static ExecutorService executor;

    @BeforeClass
    public static void beforeClass() {
        Config config = new Config();
        Services services = config.getServicesConfig();

        ActorServiceConfig actorServiceConfig = new ActorServiceConfig();
        actorServiceConfig.setEnabled(true);
        services.addServiceConfig(actorServiceConfig);

        hzInstance = Hazelcast.newHazelcastInstance(config);
        actorRuntime = (ActorService.ActorRuntimeProxyImpl) hzInstance.getServiceProxy(ActorService.NAME, "foo");
        monitorMap = hzInstance.getMap("linkedMap");
        actorFactory = new BasicActorFactory();
        nodeService = (NodeServiceImpl) actorRuntime.getNodeService();
        executor = Executors.newSingleThreadExecutor();
    }

    @AfterClass
    public static void afterClass() {
        Hazelcast.shutdownAll();
    }

    /*
    @Test
    public void exit() throws Exception {
        ActorRef actorRef = newRandomActorRef();
        ActorRecipe<TestActor> recipe = new ActorRecipe<>(TestActor.class, actorRef.getPartitionKey());

        ThreadPoolExecutorActorContainer<TestActor> container = new ThreadPoolExecutorActorContainer<>(recipe, actorRef, executor, linkedMap);
        TestActor actor = container.activate(actorRuntime, nodeService, actorFactory);
        container.exit();
    } */
}
