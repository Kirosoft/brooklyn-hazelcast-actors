package com.hazelcast.actors.impl.actorcontainers;

import com.hazelcast.actors.*;
import com.hazelcast.actors.api.ActorRecipe;
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
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


public class ActorRuntimeTest extends AbstractTest{

    @Ignore
    @Test
    public void newActor_whenNoPartitionPreference() {
        ActorRef testActor = actorRuntime.spawn(new ActorRecipe(TestActor.class, -1));
        assertTrue("partition id should be equal or larger than 0, but was: " + testActor.getPartitionId(), testActor.getPartitionId() >= 0);
    }

    @Test
    public void newActor_whenBrokenConstructor() {
        try {
            actorRuntime.spawn(new ActorRecipe(ActorWithBrokenConstructor.class));
            fail();
        } catch (ActorInstantiationException e) {
            TestUtils.assertInstanceOf(ActorInstantiationException.class, e);
            TestUtils.assertExceptionContainsLocalSeparator(e);
        }
    }

    @Test
    public void newActor_whenFailingInitialize() {
        try {
            actorRuntime.spawn(new ActorRecipe(ActorWithBrokenActivate.class));
            fail();
        } catch (ActorInstantiationException e) {
            TestUtils.assertInstanceOf(ActorInstantiationException.class, e);
            TestUtils.assertExceptionContainsLocalSeparator(e);
        }
    }

}
