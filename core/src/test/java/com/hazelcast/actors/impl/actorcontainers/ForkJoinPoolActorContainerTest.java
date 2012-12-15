package com.hazelcast.actors.impl.actorcontainers;

import com.hazelcast.actors.api.ActorRef;
import com.hazelcast.actors.impl.ActorRuntimeProxyImpl;
import com.hazelcast.actors.impl.ActorService;
import com.hazelcast.actors.impl.ActorServiceConfig;
import com.hazelcast.actors.impl.BasicActorFactory;
import com.hazelcast.config.Config;
import com.hazelcast.config.Services;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.spi.impl.NodeEngineImpl;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import java.util.Set;
import java.util.concurrent.ForkJoinPool;

import static org.junit.Assert.assertNotNull;

public class ForkJoinPoolActorContainerTest {

    private static ActorRuntimeProxyImpl actorRuntime;
    private static HazelcastInstance hzInstance;
    private static final ForkJoinPool executor = new ForkJoinPool(16, ForkJoinPool.defaultForkJoinWorkerThreadFactory, null, true);
    private static IMap<ActorRef, Set<ActorRef>> monitorMap;
    private static BasicActorFactory actorFactory;
    private static NodeEngineImpl nodeEngineImpl;

    @BeforeClass
    public static void setUp() {
        Config config = new Config();
        Services services = config.getServicesConfig();

        ActorServiceConfig actorServiceConfig = new ActorServiceConfig();
        actorServiceConfig.setEnabled(true);
        services.addServiceConfig(actorServiceConfig);

        hzInstance = Hazelcast.newHazelcastInstance(config);
        actorRuntime = (ActorRuntimeProxyImpl) hzInstance.getServiceProxy(ActorService.NAME, "foo");
        monitorMap = hzInstance.getMap("linkedMap");
        actorFactory = new BasicActorFactory();
        nodeEngineImpl = (NodeEngineImpl) actorRuntime.getNodeEngine();
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
        container.activate(actorRuntime, nodeEngine, actorFactory);

        assertNotNull(container.getActor());
    }

    @Test
    public void receivingMessage() throws InterruptedException {
        ActorRef actorRef = newRandomActorRef();
        ActorRecipe<TestActor> recipe = new ActorRecipe<>(TestActor.class, actorRef.getPartitionKey());
        ForkJoinPoolActorContainer<TestActor> container = new ForkJoinPoolActorContainer<>(recipe, actorRef, linkedMap, executor);
        TestActor actor = container.activate(actorRuntime, nodeEngine, actorFactory);

        Object msg = "foo";
        container.send(null, msg);

        actor.assertReceivesEventually(msg);
    }

    @Test
    public void receivingMultipleMessages() throws InterruptedException {
        ActorRef actorRef = newRandomActorRef();
        ActorRecipe<TestActor> recipe = new ActorRecipe<>(TestActor.class, actorRef.getPartitionKey());
        ForkJoinPoolActorContainer<TestActor> container = new ForkJoinPoolActorContainer<>(recipe, actorRef, linkedMap, executor);
        TestActor actor = container.activate(actorRuntime, nodeEngine, actorFactory);

        Object msg = "foo";
        container.send(null, msg);

        actor.assertReceivesEventually(msg);

        Object msg2 = "foo2";
        container.send(null, msg2);

        actor.assertReceivesEventually(msg2);
    }

    @Test
    public void receiveThrowsException() throws InterruptedException {
        ActorRef actorRef = newRandomActorRef();
        ActorRecipe<TestActor> recipe = new ActorRecipe<>(TestActor.class, actorRef.getPartitionKey());
        ForkJoinPoolActorContainer<TestActor> container = new ForkJoinPoolActorContainer<>(recipe, actorRef, linkedMap, executor);
        TestActor actor = container.activate(actorRuntime, nodeEngine, actorFactory);

        Exception msg = new Exception();
        container.send(null, msg);

        actor.assertReceivesEventually(msg);
    }

   */
}
