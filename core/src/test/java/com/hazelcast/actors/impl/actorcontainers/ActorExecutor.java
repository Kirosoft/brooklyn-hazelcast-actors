package com.hazelcast.actors.impl.actorcontainers;

import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class ActorExecutor {

    private final WorkerThread[] threads;
    private final WorkIndicator root;
    private final Semaphore semaphore = new Semaphore(0);

    public ActorExecutor() {
        threads = new WorkerThread[16];
        for (int k = 0; k < threads.length; k++) {
            threads[k] = new WorkerThread();
            threads[k].start();
        }

        root = buildTree(0, 16);
    }

    public WorkIndicator buildTree(int depth, int maxDepth) {
        WorkIndicator node = new WorkIndicator();
        if (depth < maxDepth) {
            WorkIndicator left = buildTree(depth + 1, maxDepth);
            left.parent = node;
            node.left = left;
            WorkIndicator right = buildTree(depth + 1, maxDepth);
            right.parent = node;
            node.right = right;
        }
        return node;
    }

    public WorkIndicator register(ActorExecutorActorContainer container) {
        WorkIndicator workIndicator = root;
        WorkIndicator targetNode = null;

        ThreadLocalRandom random = ThreadLocalRandom.current();

        while (targetNode == null) {
            boolean isLeaf = workIndicator.left == null;
            if (isLeaf) {
                targetNode = workIndicator;
            } else {
                if (random.nextBoolean()) {
                    workIndicator = workIndicator.left;
                } else {
                    workIndicator = workIndicator.right;
                }
            }
        }

        for (; ; ) {
            ActorExecutorActorContainer[] oldIndicators = workIndicator.containers.get();
            ActorExecutorActorContainer[] newIndicators;
            if (oldIndicators == null) {
                newIndicators = new ActorExecutorActorContainer[]{container};
            } else {
                newIndicators = new ActorExecutorActorContainer[oldIndicators.length + 1];
                System.arraycopy(oldIndicators, 0, newIndicators, 0, oldIndicators.length);
                newIndicators[oldIndicators.length] = container;
            }

            if (workIndicator.containers.compareAndSet(oldIndicators, newIndicators)) {
                break;
            }
        }

        return workIndicator;
    }

    public class WorkIndicator {
        private WorkIndicator parent;
        private WorkIndicator left;
        private WorkIndicator right;
        private AtomicReference<ActorExecutorActorContainer[]> containers;
        private AtomicInteger workPendingCounter = new AtomicInteger();
        private AtomicBoolean processingLock = new AtomicBoolean();

        public void signal() {
            int oldCount = workPendingCounter.incrementAndGet();
            if (oldCount > 0) {
                return;
            }

            WorkIndicator workIndicator = parent;
            for (; ; ) {
                if (workIndicator == null) {
                    semaphore.release();
                } else {
                    if (!workIndicator.workPendingCounter.compareAndSet(0, 1)) {

                    } else {
                        workIndicator = workIndicator.parent;
                    }
                }
            }
        }

        private void run() {
            if (!processingLock.compareAndSet(false, true)) {
                return;
            }

            try {
              //  container.process();
                workPendingCounter.decrementAndGet();
            } finally {
                processingLock.set(false);
            }
        }
    }

    private class WorkerThread extends Thread {

        private int random;

        public WorkerThread() {
            super("WorkerThread");
        }

        public void run() {
            try {
                for (; ; ) {
                    semaphore.acquire();

                    WorkIndicator workIndicator = findNodeWithPendingWork();
                    workIndicator.run();


                    //release work if needed.
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        private WorkIndicator findNodeWithPendingWork() {
            WorkIndicator node = root;
            WorkIndicator targetNode = null;
            while (targetNode == null) {
                boolean isLeaf = node.left == null;
                if (isLeaf) {
                    targetNode = node;
                } else {
                    //in the future you want to randomize left/right selection to realize some fairness.
                    if (node.left.workPendingCounter.get() > 0) {
                        node = node.left;
                    } else {
                        node = node.right;
                    }
                }
            }

            return targetNode;
        }

    }
}
