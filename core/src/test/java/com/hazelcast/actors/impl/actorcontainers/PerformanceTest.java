package com.hazelcast.actors.impl.actorcontainers;

import com.hazelcast.actors.CountingActor;
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
import org.junit.Ignore;
import org.junit.Test;

import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;

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
    @Ignore
    public void testThreadPerActor() throws InterruptedException {
        ActorRef actorRef = newRandomActorRef();
        ActorRecipe<CountingActor> recipe = new ActorRecipe<>(CountingActor.class, actorRef.getPartitionId());
        DedicatedThreadActorContainer<CountingActor> container = new DedicatedThreadActorContainer<>(recipe, actorRef, monitorMap);
        container.activate(actorRuntime, nodeService, actorFactory);

        test(container);
    }

    @Test
    @Ignore
    public void testCallerRuns() throws InterruptedException {
        ActorRef actorRef = newRandomActorRef();
        ActorRecipe<CountingActor> recipe = new ActorRecipe<>(CountingActor.class, actorRef.getPartitionId());
        CallerRunsActorContainer<CountingActor> container = new CallerRunsActorContainer<>(recipe, actorRef, monitorMap);
        container.activate(actorRuntime, nodeService, actorFactory);

        test(container);
    }

    @Test
    @Ignore
    public void testThreadPoolExecutor() throws InterruptedException {
        ActorRef actorRef = newRandomActorRef();
        ActorRecipe<CountingActor> recipe = new ActorRecipe<>(CountingActor.class, actorRef.getPartitionId());
        ExecutorService executor = Executors.newFixedThreadPool(10);
        ThreadPoolExecutorActorContainer<CountingActor> container = new ThreadPoolExecutorActorContainer<>(recipe, actorRef, executor, monitorMap);
        container.activate(actorRuntime, nodeService, actorFactory);

        test(container);
    }

    @Test
    @Ignore
    public void testForkJoinExecutor() throws InterruptedException, ExecutionException {
        ActorRef actorRef = newRandomActorRef();
        ActorRecipe<CountingActor> recipe = new ActorRecipe<>(CountingActor.class, actorRef.getPartitionId());
        final ForkJoinPoolActorContainer<CountingActor> container = new ForkJoinPoolActorContainer<>(recipe, actorRef, forkJoinPool, monitorMap);

        container.activate(actorRuntime, nodeService, actorFactory);

        Future f = forkJoinPool.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    test(container);
                } catch (InterruptedException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                }
            }
        });
        f.get();
    }

    public void test(ActorContainer<CountingActor> actorContainer) throws InterruptedException {
        int messageCount = 200 * 1000 * 1000;

        String msg = "nonsense";
        CountingActor countingActor = actorContainer.getActor();

        System.out.println("Starting Benchmark");
        long startTimeMs = System.currentTimeMillis();
        for (int k = 0; k < messageCount; k++) {
            actorContainer.post(null, msg);
        }

        countingActor.assertCountEventually(messageCount);

        long durationMs = System.currentTimeMillis() - startTimeMs;
        System.out.println("Completed Benchmark");

        double performance = (1000d * messageCount) / durationMs;
        System.out.println("Performance:" + String.format("%1$,.2f", performance));
    }
}
