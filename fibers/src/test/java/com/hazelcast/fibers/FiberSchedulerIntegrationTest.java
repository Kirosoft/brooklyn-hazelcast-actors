package com.hazelcast.actors.fibers;

import org.junit.Test;

import java.util.concurrent.CountDownLatch;

import static com.hazelcast.actors.TestUtils.assertCompletes;

public class FiberSchedulerIntegrationTest {

    @Test
    public void testSingleExecution() {
        FiberScheduler fiberScheduler = new FiberScheduler(1, false, 1);

        MockFiberContext fiberContext = new MockFiberContext();
        Fiber fiber = fiberScheduler.start(fiberContext);

        final CountDownLatch latch = new CountDownLatch(1);
        fiber.scheduleTask(new CountdownTask(latch));
        assertCompletes(latch);
    }

    @Test
    public void testSingleMultipleExecutions() {
        FiberScheduler fiberScheduler = new FiberScheduler(1, false, 1);

        MockFiberContext fiberContext = new MockFiberContext();
        Fiber fiber = fiberScheduler.start(fiberContext);

        int times = 10;
        final CountDownLatch latch = new CountDownLatch(times);
        for (int k = 0; k < times; k++) {
            fiber.scheduleTask(new CountdownTask(latch));
        }
        assertCompletes(latch);
    }

    @Test
    public void whenMultipleFibersForTheSameThread() {
        FiberScheduler fiberScheduler = new FiberScheduler(1, false, 1);

        MockFiberContext fiberContext1 = new MockFiberContext();
        Fiber fiber1 = fiberScheduler.start(fiberContext1);
        MockFiberContext fiberContext2 = new MockFiberContext();
        Fiber fiber2 = fiberScheduler.start(fiberContext2);

        int times = 10;
        final CountDownLatch latch1 = new CountDownLatch(times);
        final CountDownLatch latch2 = new CountDownLatch(times);
        for (int k = 0; k < times; k++) {
            fiber1.scheduleTask(new CountdownTask(latch1));
            fiber2.scheduleTask(new CountdownTask(latch2));
        }

        assertCompletes(latch1);
        assertCompletes(latch2);
    }

}
