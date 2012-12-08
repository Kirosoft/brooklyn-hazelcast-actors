package com.hazelcast.actors;

import com.hazelcast.actors.api.ActorRecipe;
import com.hazelcast.actors.api.ActorRef;
import org.junit.Ignore;
import org.junit.Test;

public class SendTest extends AbstractTest {

    @Test
    public void noSender_whenSendingSingleMessage() {
        ActorRef target = actorRuntime.spawn(new ActorRecipe(TestActor.class, "foo"));
        TestActor testActor = (TestActor) actorRuntime.getActor(target);
        Object msg = "foo";
        actorRuntime.send(target, msg);

        testActor.assertReceivesEventually(msg,null);
    }

    @Test
    public void noSender_sendMultipleMessages() {
        ActorRef target = actorRuntime.spawn(new ActorRecipe(TestActor.class, "foo"));
        TestActor testActor = (TestActor) actorRuntime.getActor(target);

        Object msg1 = "1";
        Object msg2 = "2";
        actorRuntime.send(target, msg1);
        actorRuntime.send(target, msg2);

        testActor.assertReceivesEventually(msg1,null);
        testActor.assertReceivesEventually(msg2,null);
    }

    @Test
    public void sender_sendMultipleMessages() {
        ActorRef target = actorRuntime.spawn(new ActorRecipe(TestActor.class, "foo"));
        ActorRef sender = actorRuntime.spawn(new ActorRecipe(TestActor.class, "foo"));

        TestActor testActor = (TestActor) actorRuntime.getActor(target);

        Object msg1 = "1";
        Object msg2 = "2";
        actorRuntime.send(sender, target, msg1);
        actorRuntime.send(sender, target, msg2);

        testActor.assertReceivesEventually(msg1,sender);
        testActor.assertReceivesEventually(msg2,sender);
    }

    @Test
    @Ignore
    public void sendMessageToNoneExistingActor() {

    }


}
