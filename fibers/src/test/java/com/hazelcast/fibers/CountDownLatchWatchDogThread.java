package com.hazelcast.actors.fibers;

import com.hazelcast.actors.utils.Util;

import java.util.concurrent.CountDownLatch;

public class CountDownLatchWatchDogThread extends Thread {

    private CountDownLatch latch;

    public CountDownLatchWatchDogThread(CountDownLatch latch) {
        this.latch = latch;
    }

    public void run() {
        for (; ; ) {
            long count = latch.getCount();
            System.out.println("CountdownLatch is at:" + count);

            if (count == 0) {
                return;
            }

            Util.sleep(1000);
        }
    }
}
