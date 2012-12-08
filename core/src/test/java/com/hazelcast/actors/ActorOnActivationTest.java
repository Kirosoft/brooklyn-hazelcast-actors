package com.hazelcast.actors;

import com.hazelcast.actors.actors.AbstractActor;
import com.hazelcast.actors.api.ActorRecipe;
import com.hazelcast.actors.api.ActorRef;
import com.hazelcast.core.IMap;
import org.junit.AfterClass;
import org.junit.Ignore;
import org.junit.Test;

import java.util.UUID;

import static com.hazelcast.actors.TestUtils.destroySilently;
import static org.junit.Assert.assertTrue;

/**
 * A test that verifies that calling Hazelcast objects from the onActivation method of the Actor doesn't run into
 * a Hazelcast caused deadlock.
 */
public class ActorOnActivationTest extends AbstractTest {

    private static IMap<Object, Object> someMap;

    @AfterClass
    public static void afterClass() {
        destroySilently(someMap);
        someMap = null;
    }

    //this test is ignored, because currently it leads to the expected deadlock.
    @Ignore
    @Test
    public void test() {
        someMap = hzInstance.getMap("somemap"+ UUID.randomUUID().toString());

        ActorRef ref = actorRuntime.spawn(new ActorRecipe(InitActor.class));
        InitActor actor = (InitActor) actorRuntime.getActor(ref);
        assertTrue(actor.onActivationCalled);
    }

    public static class InitActor extends AbstractActor {
        private volatile boolean onActivationCalled = false;

        @Override
        public void onActivation() throws Exception {
            super.onActivation();

            System.out.println(Thread.currentThread().getName());

            for (int k = 0; k < 100; k++) {
                String key = "foo" + k;
                System.out.println(key);
                someMap.put(key, k);
                someMap.get(key);
            }
            onActivationCalled = true;
        }

        @Override
        public void receive(Object msg, ActorRef sender) throws Exception {
        }
    }
}
