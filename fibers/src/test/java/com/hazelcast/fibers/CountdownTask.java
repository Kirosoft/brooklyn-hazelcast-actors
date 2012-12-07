package com.hazelcast.actors.fibers;

import java.util.concurrent.CountDownLatch;

public class CountdownTask implements Runnable {
    private final CountDownLatch latch;

    public CountdownTask(CountDownLatch latch) {
        this.latch = latch;
    }

    @Override
    public void run() {
        latch.countDown();
    }
}
