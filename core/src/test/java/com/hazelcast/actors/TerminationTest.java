package com.hazelcast.actors;

import com.hazelcast.actors.actors.DispatchingActor;
import com.hazelcast.actors.api.ActorRef;
import com.hazelcast.actors.impl.ActorService;
import com.hazelcast.actors.impl.ActorServiceConfig;
import com.hazelcast.actors.utils.Util;
import com.hazelcast.config.Config;
import com.hazelcast.config.Services;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.Serializable;
import java.util.List;
import java.util.Vector;

public class TerminationTest {
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

    @Test
    public void terminateRootActorWithoutChildren() {
        ActorRef ref = actorRuntime.newActor(TerminationActor.class);
        TerminationActor terminationActor = (TerminationActor) actorRuntime.getActor(ref);
        actorRuntime.exit(ref);
        terminationActor.assertTerminatedEventually();
    }

    @Test
    public void terminateRootActorWithChildren() {
        ActorRef ref = actorRuntime.newActor(TerminationActor.class);
        TerminationActor terminationActor = (TerminationActor) actorRuntime.getActor(ref);
        actorRuntime.send(ref,new CreateChild());

        actorRuntime.exit(ref);
        terminationActor.assertTerminatedEventually();

        TerminationActor child = (TerminationActor)actorRuntime.getActor(terminationActor.children.get(0));
        child.assertTerminatedEventually();
    }

    @Test
    public void terminateWhenAlreadyTerminated(){
        ActorRef ref = actorRuntime.newActor(TerminationActor.class);
        TerminationActor terminationActor = (TerminationActor) actorRuntime.getActor(ref);
        actorRuntime.exit(ref);
        terminationActor.assertTerminatedEventually();

        actorRuntime.exit(ref);
    }

    public static class CreateChild implements Serializable {
    }

    public static class TerminationActor extends DispatchingActor {
        private volatile boolean terminated = false;
        private final List<ActorRef> children = new Vector<>();

        public void receive(CreateChild msg) throws Exception {
            ActorRef child = spawnAndLink(TerminationActor.class);
            children.add(child);
        }

        @Override
        public void onExit() throws Exception {
            System.out.println(self()+" Terminated");
            terminated = true;
        }

        public void assertTerminatedEventually() {
            for (int k = 0; k < 60; k++) {
                if (terminated) {
                    return;
                }

                Util.sleep(1000);
            }

            Assert.fail("Failed to exit actor");
        }
    }
}
