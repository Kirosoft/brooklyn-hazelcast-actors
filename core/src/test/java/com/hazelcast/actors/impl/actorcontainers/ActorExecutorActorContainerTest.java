package com.hazelcast.actors.impl.actorcontainers;

import com.hazelcast.actors.TestActor;
import com.hazelcast.actors.api.ActorRecipe;
import com.hazelcast.actors.api.ActorRef;
import com.hazelcast.actors.impl.ActorService;
import com.hazelcast.actors.impl.ActorServiceConfig;
import com.hazelcast.actors.impl.BasicActorFactory;
import com.hazelcast.config.Config;
import com.hazelcast.config.Services;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.spi.impl.NodeServiceImpl;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Set;

import static com.hazelcast.actors.TestUtils.newRandomActorRef;

public class ActorExecutorActorContainerTest {
    private static ActorService.ActorRuntimeProxyImpl actorRuntime;
    private static HazelcastInstance hzInstance;
    private static IMap<ActorRef, Set<ActorRef>> monitorMap;
    private static BasicActorFactory actorFactory;
    private static NodeServiceImpl nodeService;
    private ActorExecutor executor;

    @BeforeClass
    public static void beforeClass() {
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

    @Before
    public void before() {
        executor = new ActorExecutor();
    }

    @Ignore
    @Test
    public void test() throws InterruptedException {
        ActorRef actorRef = newRandomActorRef();
        ActorRecipe<TestActor> recipe = new ActorRecipe<>(TestActor.class, actorRef.getPartitionId());

        ActorExecutorActorContainer<TestActor> container = new ActorExecutorActorContainer<>(recipe, actorRef, monitorMap);
        TestActor actor = container.activate(actorRuntime, nodeService, actorFactory);

        String msg = "foo";
        container.post(null, msg);
        actor.assertReceivesEventually(msg);
    }
}
