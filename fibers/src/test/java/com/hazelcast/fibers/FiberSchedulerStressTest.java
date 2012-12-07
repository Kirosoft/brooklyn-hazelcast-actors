package com.hazelcast.actors.fibers;

import org.junit.Before;
import org.junit.Test;

import java.util.Random;
import java.util.concurrent.CountDownLatch;

import static com.hazelcast.actors.TestUtils.assertCompletes;

public class FiberSchedulerStressTest {

    private FiberScheduler scheduler;
    private Random random;

    @Before
    public void setUp() {
        scheduler = new FiberScheduler(256, false, 100);
        random = new Random();
    }

    /**
     * This stress test is very difficult for the scheduler since there are not hot fibers. Fibers all over
     * requests for tasks to be processed.
     */
    @Test
    public void test() {
        int fiberCount = 1000 * 1000;

        System.out.println("Creating fibers");
        int messageCount = 50 * 1000 * 1000;
        CountDownLatch latch = new CountDownLatch(messageCount);
        Fiber[] fibers = new Fiber[fiberCount];
        for (int k = 0; k < fiberCount; k++) {
            fibers[k] = scheduler.start(new MockFiberContext());
        }
        System.out.println("Finished creating fibers");

        System.out.println("Start sending messages");
        CountdownTask task = new CountdownTask(latch);
        new CountDownLatchWatchDogThread(latch).start();
        for (int k = 0; k < messageCount; k++) {
            Fiber fiber = fibers[random.nextInt(fibers.length)];
            fiber.scheduleTask(task);
        }
        System.out.println("Completes sending messages");

        assertCompletes(latch);
    }
}
