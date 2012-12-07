package com.hazelcast.actors.impl.actorcontainers;

import com.hazelcast.actors.TestUtils;
import com.hazelcast.actors.actors.AbstractActor;
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
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ForkJoinPool;

public class AkkaPerformanceTest {
    private static ActorService.ActorRuntimeProxyImpl actorRuntime;
    private static HazelcastInstance hzInstance;
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

    @Test
    public void test_fiber_1() throws InterruptedException {
        final FiberScheduler fiberScheduler = new FiberScheduler();

        test(1, new ActorContainerFactory() {
            @Override
            public ActorContainer newContainer(ActorRecipe recipe, ActorRef actorRef, IMap<ActorRef, Set<ActorRef>> monitorMap) {
                return new com.hazelcast.actors.impl.actorcontainers.FiberActorContainer(recipe, actorRef, monitorMap, fiberScheduler);
            }
        });
    }

    @Test
    public void test_fiber_2() throws InterruptedException {
        final FiberScheduler fiberScheduler = new FiberScheduler();

        test(2, new ActorContainerFactory() {
            @Override
            public ActorContainer newContainer(ActorRecipe recipe, ActorRef actorRef, IMap<ActorRef, Set<ActorRef>> monitorMap) {
                return new com.hazelcast.actors.impl.actorcontainers.FiberActorContainer(recipe, actorRef, monitorMap, fiberScheduler);
            }
        });
    }

    @Test
    public void test_forkJoin_1() throws InterruptedException {
        final ForkJoinPool forkJoinPool = new ForkJoinPool();

        test(1, new ActorContainerFactory() {
            @Override
            public ActorContainer newContainer(ActorRecipe recipe, ActorRef actorRef, IMap<ActorRef, Set<ActorRef>> monitorMap) {
                return new ForkJoinPoolActorContainer(recipe, actorRef, monitorMap, forkJoinPool);
            }
        });
    }

    @Test
    public void test_threadPerActor_1() throws InterruptedException {
        test(1, new ActorContainerFactory() {
            @Override
            public ActorContainer newContainer(ActorRecipe recipe, ActorRef actorRef, IMap<ActorRef, Set<ActorRef>> monitorMap) {
                return new DedicatedThreadActorContainer(recipe, actorRef, monitorMap);
            }
        });
    }

    interface ActorContainerFactory {
        ActorContainer newContainer(ActorRecipe recipe, ActorRef actorRef, IMap<ActorRef, Set<ActorRef>> monitorMap);
    }

    public void test(int numberOfClients, ActorContainerFactory containerFactory) throws InterruptedException {
        int messageCount = 100 * 1000 * 1000;
        long totalMessageCount = numberOfClients * messageCount;

        ActorContainer[] destinations = new ActorContainer[numberOfClients];
        ActorRecipe<Destination> destinationRecipe = new ActorRecipe<>(Destination.class, 0);
        for (int k = 0; k < destinations.length; k++) {
            ActorRef ref = TestUtils.newRandomActorRef();
            ActorContainer container = containerFactory.newContainer(destinationRecipe, ref, monitorMap);
            container.activate(actorRuntime, nodeService, actorFactory);
            container.post(null, new Init(container));
            destinations[k] = container;
        }

        ActorContainer[] clients = new ActorContainer[numberOfClients];
        ActorRecipe<Client> clientRecipe = new ActorRecipe<>(Client.class, 0);
        for (int k = 0; k < clients.length; k++) {
            ActorRef ref = TestUtils.newRandomActorRef();
            ActorContainer container = containerFactory.newContainer(clientRecipe, ref, monitorMap);
            container.activate(actorRuntime, nodeService, actorFactory);
            container.post(null, new Init(container));
            clients[k] = container;
        }

        CountDownLatch latch = new CountDownLatch(numberOfClients);

        System.out.println("Starting Benchmark");
        long startTimeMs = System.currentTimeMillis();

        for (int k = 0; k < numberOfClients; k++) {
            ActorContainer client = clients[k];
            ActorContainer destination = destinations[k];
            client.post(null, new Run(destination, messageCount, latch));
        }

        latch.await();

        long durationMs = System.currentTimeMillis() - startTimeMs;
        System.out.println("Completed Benchmark");
        System.out.println("Total number of messages send: " + totalMessageCount);
        System.out.println("Total duration: " + durationMs + " ms");
        double performance = (1000d * totalMessageCount) / durationMs;
        System.out.println("Performance:" + String.format("%1$,.2f", performance) + " messages/second");
    }

    private final static class Run {
        private final ActorContainer target;
        private final int repeat;
        private final CountDownLatch countDownLatch;

        private Run(ActorContainer container, int count, CountDownLatch latch) {
            this.target = container;
            this.repeat = count;
            this.countDownLatch = latch;
        }
    }

    private final static class Init {
        private final ActorContainer container;

        private Init(ActorContainer container) {
            this.container = container;
        }
    }

    private static class Client extends AbstractActor {

        private ActorContainer self;
        private CountDownLatch latch;
        private long sent = 0L;
        private long received = 0L;
        private long repeat;
        private ActorContainer target;

        @Override
        public void receive(Object msg, ActorRef sender) throws Exception {
            if (msg instanceof Init) {
                Init init = (Init) msg;
                self = init.container;
            } else if (msg instanceof Run) {
                System.out.println("Run encountered");
                Run run = (Run) msg;
                latch = run.countDownLatch;
                repeat = run.repeat;
                int count = run.repeat;
                target = run.target;
                for (int k = 0; k < Math.min(1000, count); k++) {
                    target.post(null, self);
                    sent += 1;
                }
            } else {
                received += 1;
                if (sent < repeat) {
                    target.post(null, self);
                    sent += 1;
                } else if (received >= repeat) {
                    latch.countDown();
                    target.post(null, new Info());
                }

                if (sent % 1000000 == 0) {
                    System.out.println("Client at: " + sent);
                }
            }
        }
    }

    private static class Info {
    }

    private static class Destination implements Actor {
        private ActorContainer self;
        private long count;

        @Override
        public void receive(Object msg, ActorRef sender) throws Exception {
            if (msg instanceof Init) {
                self = ((Init) msg).container;
            } else if (msg instanceof Info) {
                // System.out.println("count:" + count + " destination:" + toString());
            } else {
                count++;
                ActorContainer s = (ActorContainer) msg;
                s.post(null, self);
            }
        }
    }
}
