package com.hazelcast.actors;

import com.hazelcast.actors.api.ActorRecipe;
import com.hazelcast.actors.api.ActorRef;
import org.junit.Ignore;
import org.junit.Test;

import static com.hazelcast.actors.TestUtils.assertValidActorRef;
import static org.junit.Assert.assertNotNull;

public class SpawnTest extends AbstractTest {

    @Test(expected = NullPointerException.class)
    public void whenNullRecipe_thenNpe() {
        actorRuntime.spawn(null);
    }

    @Test
    public void whenNullPartitionKey_thenPartitionIsAssigned() {
        ActorRecipe recipe = new ActorRecipe(TestActor.class, null);
        ActorRef ref = actorRuntime.spawn(recipe);
        assertValidActorRef(ref);

        TestActor testActor = (TestActor) actorRuntime.getActor(ref);
        assertNotNull(testActor);
    }

    @Test
    public void whenSuccess() {
        Object partitionKey = "foo";
        ActorRecipe recipe = new ActorRecipe(TestActor.class, partitionKey);
        ActorRef ref = actorRuntime.spawn(recipe);
        assertValidActorRef(ref);

        TestActor testActor = (TestActor) actorRuntime.getActor(ref);
        assertNotNull(testActor);
    }



    @Test
    @Ignore
    public void whenSpawningFails() {

    }
}
