package com.hazelcast.actors;

import com.hazelcast.actors.api.Actor;
import com.hazelcast.actors.api.ActorRef;
import com.hazelcast.actors.impl.ActorService;
import com.hazelcast.actors.impl.ActorServiceConfig;
import com.hazelcast.actors.utils.Util;
import com.hazelcast.config.Config;
import com.hazelcast.config.Services;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

public class StressTest {
    private static ActorService.ActorRuntimeProxyImpl actorRuntime;
    private static HazelcastInstance hzInstance;
    private static final AtomicInteger failureCount = new AtomicInteger();

    @BeforeClass
    public static void setUp() {
        Config config = new Config();
        Services services = config.getServicesConfig();

        ActorServiceConfig actorServiceConfig = new ActorServiceConfig();
        actorServiceConfig.setEnabled(true);
        services.addServiceConfig(actorServiceConfig);

        hzInstance = Hazelcast.newHazelcastInstance(config);
        actorRuntime = (ActorService.ActorRuntimeProxyImpl) hzInstance.getServiceProxy(ActorService.NAME, "foo");
    }

    @AfterClass
    public static void tearDown() {
        actorRuntime.destroy();
        Hazelcast.shutdownAll();
    }

    @Test
    public void test() {
        ActorRef ref = actorRuntime.newActor(DetectingActor.class);

        for (int k = 0; k < 10000; k++) {
            actorRuntime.send(ref, "");
        }

        Util.sleep(60000);
    }

    private static class DetectingActor implements Actor {
        private final AtomicInteger foo = new AtomicInteger();
        private final AtomicInteger count = new AtomicInteger();

        public DetectingActor() {
        }

        @Override
        public void receive(Object msg, ActorRef sender) throws Exception {
            if (foo.incrementAndGet() > 0) {
                failureCount.incrementAndGet();
            }

            Util.sleep(10);

            int c = count.incrementAndGet();

            if (c % 100 == 0) {
                System.out.println("at: " + c);
            }


            foo.decrementAndGet();
        }
    }
}