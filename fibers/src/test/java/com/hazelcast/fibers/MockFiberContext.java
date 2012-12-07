package com.hazelcast.actors.fibers;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
* Created with IntelliJ IDEA.
* User: alarmnummer
* Date: 12/1/12
* Time: 7:58 PM
* To change this template use File | Settings | File Templates.
*/
class MockFiberContext implements FiberContext {
    private final BlockingQueue taskQueue = new LinkedBlockingQueue();

    @Override
    public void storeTask(Object task) {
        try {
            taskQueue.put(task);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Object takeTask() {
        try {
            return taskQueue.take();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void executeTask(Object task) {
        ((Runnable) task).run();
    }

    @Override
    public boolean workAvailable() {
        return taskQueue.size() > 0;
    }

    @Override
    public String getFiberName() {
        return "Fiber";
    }
}
