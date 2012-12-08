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
import java.util.concurrent.ForkJoinPool;

import static com.hazelcast.actors.TestUtils.newRandomActorRef;
import static org.junit.Assert.assertNotNull;

public class ForkJoinPoolActorContainerTest {

    private static ActorService.ActorRuntimeProxyImpl actorRuntime;
    private static HazelcastInstance hzInstance;
    private static final ForkJoinPool executor = new ForkJoinPool(16, ForkJoinPool.defaultForkJoinWorkerThreadFactory, null, true);
    private static IMap<ActorRef, Set<ActorRef>> monitorMap;
    private static BasicActorFactory actorFactory;
    private static NodeServiceImpl nodeService;

    @BeforeClass
    public static void setUp() {
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
    }

    @AfterClass
    public static void tearDown() {
        actorRuntime.destroy();
        Hazelcast.shutdownAll();
        executor.shutdown();
    }
       /*
    @Test
    public void activate() {
        ActorRef actorRef = newRandomActorRef();
        ActorRecipe<TestActor> recipe = new ActorRecipe<>(TestActor.class, actorRef.getPartitionKey());
        ForkJoinPoolActorContainer container = new ForkJoinPoolActorContainer<>(recipe, actorRef, linkedMap, executor);
        container.activate(actorRuntime, nodeService, actorFactory);

        assertNotNull(container.getActor());
    }

    @Test
    public void receivingMessage() throws InterruptedException {
        ActorRef actorRef = newRandomActorRef();
        ActorRecipe<TestActor> recipe = new ActorRecipe<>(TestActor.class, actorRef.getPartitionKey());
        ForkJoinPoolActorContainer<TestActor> container = new ForkJoinPoolActorContainer<>(recipe, actorRef, linkedMap, executor);
        TestActor actor = container.activate(actorRuntime, nodeService, actorFactory);

        Object msg = "foo";
        container.post(null, msg);

        actor.assertReceivesEventually(msg);
    }

    @Test
    public void receivingMultipleMessages() throws InterruptedException {
        ActorRef actorRef = newRandomActorRef();
        ActorRecipe<TestActor> recipe = new ActorRecipe<>(TestActor.class, actorRef.getPartitionKey());
        ForkJoinPoolActorContainer<TestActor> container = new ForkJoinPoolActorContainer<>(recipe, actorRef, linkedMap, executor);
        TestActor actor = container.activate(actorRuntime, nodeService, actorFactory);

        Object msg = "foo";
        container.post(null, msg);

        actor.assertReceivesEventually(msg);

        Object msg2 = "foo2";
        container.post(null, msg2);

        actor.assertReceivesEventually(msg2);
    }

    @Test
    public void receiveThrowsException() throws InterruptedException {
        ActorRef actorRef = newRandomActorRef();
        ActorRecipe<TestActor> recipe = new ActorRecipe<>(TestActor.class, actorRef.getPartitionKey());
        ForkJoinPoolActorContainer<TestActor> container = new ForkJoinPoolActorContainer<>(recipe, actorRef, linkedMap, executor);
        TestActor actor = container.activate(actorRuntime, nodeService, actorFactory);

        Exception msg = new Exception();
        container.post(null, msg);

        actor.assertReceivesEventually(msg);
    }

   */
}
