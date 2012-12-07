package com.hazelcast.actors.impl.actorcontainers;

import com.hazelcast.actors.CountingActor;
import com.hazelcast.actors.api.Actor;
import com.hazelcast.actors.api.ActorRecipe;
import com.hazelcast.actors.api.ActorRef;
import com.hazelcast.actors.impl.ActorService;
import com.hazelcast.actors.impl.ActorServiceConfig;
import com.hazelcast.actors.impl.BasicActorFactory;
import com.hazelcast.actors.fibers.FiberScheduler;
import com.hazelcast.config.Config;
import com.hazelcast.config.Services;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.spi.impl.NodeServiceImpl;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;

import static com.hazelcast.actors.TestUtils.newRandomActorRef;

public class PerformanceTest {

    private static ActorService.ActorRuntimeProxyImpl actorRuntime;
    private static HazelcastInstance hzInstance;
    private static final ForkJoinPool forkJoinPool = new ForkJoinPool(16, ForkJoinPool.defaultForkJoinWorkerThreadFactory, null, true);
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
        monitorMap = hzInstance.getMap("monitorMap");
        actorFactory = new BasicActorFactory();
        nodeService = (NodeServiceImpl) actorRuntime.getNodeService();
    }

    @AfterClass
    public static void tearDown() {
        actorRuntime.destroy();
        Hazelcast.shutdownAll();
    }

    @Test
    // @Ignore
    public void testThreadPerActor() throws InterruptedException {
//        ActorRef actorRef = newRandomActorRef();
//        ActorRecipe<CountingActor> recipe = new ActorRecipe<>(CountingActor.class, actorRef.getPartitionId());
//
//        DedicatedThreadActorContainer<CountingActor> container = new DedicatedThreadActorContainer<>(recipe, actorRef, monitorMap);
//        container.activate(actorRuntime, nodeService, actorFactory);
//
//        test(container);
    }

    @Test
    // @Ignore
    public void testCallerRuns() throws InterruptedException {
//        ActorRef actorRef = newRandomActorRef();
//        ActorRecipe<CountingActor> recipe = new ActorRecipe<>(CountingActor.class, actorRef.getPartitionId());
//        CallerRunsActorContainer<CountingActor> container = new CallerRunsActorContainer<>(recipe, actorRef, monitorMap);
//        container.activate(actorRuntime, nodeService, actorFactory);
//
//        test(container);
    }

    @Test
    @Ignore
    public void testThreadPoolExecutor() throws InterruptedException {
//        ActorRef actorRef = newRandomActorRef();
//        ActorRecipe<CountingActor> recipe = new ActorRecipe<>(CountingActor.class, actorRef.getPartitionId());
//        ExecutorService executor = Executors.newFixedThreadPool(10);
//        ThreadPoolExecutorActorContainer<CountingActor> container = new ThreadPoolExecutorActorContainer<>(recipe, actorRef, executor, monitorMap);
//        container.activate(actorRuntime, nodeService, actorFactory);
//
//        test(container);
    }

    @Test
    public void testFiberScheduler() throws InterruptedException {
        ActorRef countingActorRef = newRandomActorRef();
        ActorRecipe<CountingActor> countingRecipe = new ActorRecipe<>(CountingActor.class, countingActorRef.getPartitionId());

        ActorRef postingActorRef = newRandomActorRef();
        ActorRecipe<PostingActor> postingRecipe = new ActorRecipe<>(PostingActor.class, postingActorRef.getPartitionId());

        FiberScheduler fiberScheduler = new FiberScheduler();

        FiberActorContainer<CountingActor> countingContainer = new FiberActorContainer<>(countingRecipe, countingActorRef, monitorMap, fiberScheduler);
        FiberActorContainer<PostingActor> postContainer = new FiberActorContainer<>(postingRecipe, postingActorRef, monitorMap, fiberScheduler);

        postContainer.activate(actorRuntime, nodeService, actorFactory);
        countingContainer.activate(actorRuntime, nodeService, actorFactory);

        test(postContainer, countingContainer);
    }

    @Test
   // @Ignore
    public void testForkJoinExecutor() throws InterruptedException, ExecutionException {
        ActorRef countingActorRef = newRandomActorRef();
        ActorRecipe<CountingActor> countingRecipe = new ActorRecipe<>(CountingActor.class, countingActorRef.getPartitionId());

        ActorRef postingActorRef = newRandomActorRef();
        ActorRecipe<PostingActor> postingRecipe = new ActorRecipe<>(PostingActor.class, postingActorRef.getPartitionId());

        ForkJoinPool forkJoinPool = new ForkJoinPool();

        ForkJoinPoolActorContainer<CountingActor> countingContainer = new ForkJoinPoolActorContainer<>(countingRecipe, countingActorRef, monitorMap, forkJoinPool);
        ForkJoinPoolActorContainer<PostingActor> postContainer = new ForkJoinPoolActorContainer<>(postingRecipe, postingActorRef, monitorMap, forkJoinPool);

        postContainer.activate(actorRuntime, nodeService, actorFactory);
        countingContainer.activate(actorRuntime, nodeService, actorFactory);

        test(postContainer, countingContainer);
    }

    public void test(ActorContainer<PostingActor> postingContainer, ActorContainer<CountingActor> countingContainer) throws InterruptedException {
        int messageCount = 200000000;// * 1000 * 1000;

        String msg = "nonsense";
        CountingActor countingActor = countingContainer.getActor();

        System.out.println("Starting Benchmark");
        long startTimeMs = System.currentTimeMillis();

        postingContainer.post(null, new PostMessage(msg, countingContainer, messageCount));
        countingActor.assertCountEventually(messageCount);

        long durationMs = System.currentTimeMillis() - startTimeMs;
        System.out.println("Completed Benchmark");

        double performance = (1000d * messageCount) / durationMs;
        System.out.println("Performance:" + String.format("%1$,.2f", performance) + " messages/second");
    }

    private final static class PostMessage {
        private final Object msg;
        private final ActorContainer container;
        private final int count;

        private PostMessage(Object msg, ActorContainer container, int count) {
            this.msg = msg;
            this.container = container;
            this.count = count;
        }
    }

    private static class PostingActor implements Actor {

        @Override
        public void receive(Object msg, ActorRef sender) throws Exception {
            PostMessage postMessage = (PostMessage) msg;
            int count = postMessage.count;
            Object message = postMessage.msg;
            ActorContainer target = postMessage.container;
            for (int k = 0; k < count; k++) {
                target.post(null, message);
            }
        }
    }
}
