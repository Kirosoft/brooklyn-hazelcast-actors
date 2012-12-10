package com.hazelcast.actors;

import com.hazelcast.actors.api.ActorRecipe;
import com.hazelcast.actors.api.ActorRef;
import org.junit.Ignore;
import org.junit.Test;

import static com.hazelcast.actors.TestUtils.assertContains;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

public class LinkTest extends AbstractTest {

    @Test(expected = NullPointerException.class)
    public void whenRef1IsNull_thenNpe() {
        ActorRef ref = actorRuntime.spawn(new ActorRecipe(TestActor.class));
        actorRuntime.link(null, ref);
    }

    @Test(expected = NullPointerException.class)
    public void whenRef2IsNull_thenNpe() {
        ActorRef ref = actorRuntime.spawn(new ActorRecipe(TestActor.class));
        actorRuntime.link(ref, null);
    }

    @Test
    public void whenLinkDoesntExistYet() {
        ActorRef ref1 = actorRuntime.spawn(new ActorRecipe(TestActor.class));
        ActorRef ref2 = actorRuntime.spawn(new ActorRecipe(TestActor.class));

        actorRuntime.link(ref1, ref2);
        assertContains(linksMap.get(ref1), ref2);
        assertContains(linksMap.get(ref2), ref1);
    }

    @Test
    public void whenAlreadyLinkedToAnotherActor() {
        ActorRef ref1 = actorRuntime.spawn(new ActorRecipe(TestActor.class));
        ActorRef ref2 = actorRuntime.spawn(new ActorRecipe(TestActor.class));
        ActorRef ref3 = actorRuntime.spawn(new ActorRecipe(TestActor.class));

        actorRuntime.link(ref1, ref2);
        actorRuntime.link(ref1, ref3);
        assertContains(linksMap.get(ref1), ref2, ref3);
        assertContains(linksMap.get(ref2), ref1);
        assertContains(linksMap.get(ref3), ref1);
    }

    @Test
    public void whenLinkAlreadyExists() {
        ActorRef ref1 = actorRuntime.spawn(new ActorRecipe(TestActor.class));
        ActorRef ref2 = actorRuntime.spawn(new ActorRecipe(TestActor.class));

        actorRuntime.link(ref1, ref2);
        //this is the duplicate link
        actorRuntime.link(ref1, ref2);

        assertContains(linksMap.get(ref1), ref2);
        assertContains(linksMap.get(ref2), ref1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void whenLinkToSelf() {
        ActorRef ref1 = actorRuntime.spawn(new ActorRecipe(TestActor.class));
        actorRuntime.link(ref1, ref1);
    }

    @Test
    @Ignore
    public void whenLinkingToDeadProcess() {

    }

    @Test
    public void whenLinkingToNonExistingProcess_ref1DoesNotExist_thenIllegalArgumentException() {
        ActorRef ref1 = TestUtils.newRandomActorRef();
        ActorRef ref2 = actorRuntime.spawn(new ActorRecipe(TestActor.class));
        whenLinkingToNonExistingProcess_thenIllegalArgumentException(ref1, ref2);
    }


    @Test
    public void whenLinkingToNonExistingProcess_ref2DoesNotExist_thenIllegalArgumentException() {
        ActorRef ref1 = actorRuntime.spawn(new ActorRecipe(TestActor.class));
        ActorRef ref2 = TestUtils.newRandomActorRef();
        whenLinkingToNonExistingProcess_thenIllegalArgumentException(ref1, ref2);
    }

    public void whenLinkingToNonExistingProcess_thenIllegalArgumentException(ActorRef ref1, ActorRef ref2) {
        try {
            actorRuntime.link(ref1, ref2);
            fail();
        } catch (IllegalArgumentException expected) {
        }

        assertFalse(linksMap.containsKey(ref1));
        assertFalse(linksMap.containsKey(ref2));
    }
}
