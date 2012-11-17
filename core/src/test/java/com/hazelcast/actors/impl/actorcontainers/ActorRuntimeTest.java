package com.hazelcast.actors.impl.actorcontainers;

import com.hazelcast.actors.ActorWithBrokenActivate;
import com.hazelcast.actors.ActorWithBrokenConstructor;
import com.hazelcast.actors.SomeException;
import com.hazelcast.actors.TestActor;
import com.hazelcast.actors.TestUtils;
import com.hazelcast.actors.api.ActorRef;
import com.hazelcast.actors.api.exceptions.ActorInstantiationException;
import com.hazelcast.actors.impl.ActorService;
import com.hazelcast.actors.impl.ActorServiceConfig;
import com.hazelcast.actors.impl.BasicActorFactory;
import com.hazelcast.config.Config;
import com.hazelcast.config.Services;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


public class ActorRuntimeTest {

    private static ActorService.ActorRuntimeProxyImpl actorRuntime;
    private static HazelcastInstance hzInstance;

    @BeforeClass
    public static void setUp() {
        Config config = new Config();
        Services services = config.getServicesConfig();

        ActorServiceConfig actorServiceConfig = new ActorServiceConfig();
        actorServiceConfig.setActorFactory(new BasicActorFactory());
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
    public void newActor_whenNoPartitionPreference() {
        ActorRef testActor = actorRuntime.newActor(TestActor.class, -1);
        assertTrue("partition id should be equal or larger than 0, but was: " + testActor.getPartitionId(), testActor.getPartitionId() >= 0);
    }

    @Test
    public void newActor_whenBrokenConstructor() {
        try {
            actorRuntime.newActor(ActorWithBrokenConstructor.class);
            fail();
        } catch (ActorInstantiationException e) {
            TestUtils.assertInstanceOf(ActorInstantiationException.class,e);
            TestUtils.assertExceptionContainsLocalSeparator(e);
        }
    }

    @Test
    public void newActor_whenFailingInitialize() {
        try {
            actorRuntime.newActor(ActorWithBrokenActivate.class);
            fail();
        } catch (ActorInstantiationException e) {
            TestUtils.assertInstanceOf(ActorInstantiationException.class,e);
            TestUtils.assertExceptionContainsLocalSeparator(e);
        }
    }

}
