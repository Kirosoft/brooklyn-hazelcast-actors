package com.hazelcast.actors;

import com.hazelcast.actors.actors.AbstractActor;
import com.hazelcast.actors.api.ActorRef;
import com.hazelcast.actors.api.MessageDeliveryFailure;
import com.hazelcast.actors.service.ActorService;
import com.hazelcast.actors.service.ActorServiceConfig;
import com.hazelcast.config.Config;
import com.hazelcast.config.Services;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;
import java.util.Vector;

import static com.hazelcast.actors.utils.Util.sleep;
import static org.junit.Assert.fail;

public class IntegrationTest {
    private static ActorService.ActorRuntimeProxyImpl actorRuntime;
    private static HazelcastInstance hzInstance;

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
    public void singleMessage() {
        ActorRef ref = actorRuntime.newActor(TestActor.class);
        Object msg = "foo";
        actorRuntime.send(ref, msg);

        assertReceived(ref, msg);
    }

    @Test
    public void multipleMessages() {
        ActorRef ref = actorRuntime.newActor(TestActor.class);
        Object msg1 = "1";
        Object msg2 = "2";
        actorRuntime.send(ref, msg1);
        actorRuntime.send(ref, msg2);

        assertReceived(ref, msg1);
        assertReceived(ref, msg2);
    }

    @Test
    public void monitoring() {
        ActorRef target = actorRuntime.newActor(TestActor.class);
        ActorRef monitor = actorRuntime.newActor(TestActor.class);
        actorRuntime.monitor(monitor, target);

        Exception ex = new Exception();
        actorRuntime.send(target, ex);

        assertReceived(monitor, new MessageDeliveryFailure(target, ex));
    }

    @Test
    public void whenMessageFailedSenderIsNotified() {
        ActorRef target = actorRuntime.newActor(TestActor.class);
        ActorRef monitor = actorRuntime.newActor(TestActor.class);

        Exception ex = new Exception();
        actorRuntime.send(monitor, target, ex);

        assertReceived(monitor, new MessageDeliveryFailure(target, ex));
    }

    private void assertReceived(ActorRef ref, Object msg) {
        TestActor actor = (TestActor) actorRuntime.getActor(ref);
        for (int k = 0; k < 60; k++) {
            System.out.println(actor.messages);
            if (actor.messages.contains(msg)) {
                return;
            }
            sleep(1000);
        }
        fail();
    }

    public static class TestActor extends AbstractActor {
        private List messages = new Vector();

        public void receive(Object msg, ActorRef sender) throws Exception {
            System.out.println(msg);
            messages.add(msg);
            if (msg instanceof Exception) {
                throw (Exception) msg;
            }
        }
    }
}
