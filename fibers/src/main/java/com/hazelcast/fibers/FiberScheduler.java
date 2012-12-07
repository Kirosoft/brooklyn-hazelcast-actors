package com.hazelcast.fibers;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Assigning an WorkIndicator to a threadpool.
 * <p/>
 * Assigning execute:
 * There are 2 options. When a new WorkSignaller is created, it is immediately assigned to a worker. When the WorkerThread
 * has completed processing execute for the WorkProvider, it can either remove the WorkSignaller from the WorkerThread, or
 * not. If it is removed, than the WorkSignallers assigned to a WorkerThread very likely have execute to do.  Stealing execute
 * is simple, just take a WorkSignaller from this pool. Balancing WorkerThread also is relatively simple, just calculate
 * the average size; steal execute from rich WorkerThreads and give it to poor WorkerThreads.
 * <p/>
 * <p/>
 * thoughts about fairness:
 * if the workers get execute from the central execute queue only when they have no owned execute-indicators, the workindicators
 * in this workqueue will not be processed. So tasks here should not be picked up when the worker thread has nothing to do.
 * But should be assigned to a worker thread much quicker.
 * <p/>
 * If a worker would have a pool of workindicators, and he executor would make sure each execute indicator has a roughly
 * equal size pool, then the execute can be processed reasonably fair.
 * <p/>
 * <p/>
 * todo:
 * idea: caller runs. can be based on depth.
 * idea:
 */
public class FiberScheduler {

    private final static AtomicLong ID_GENERATOR = new AtomicLong(1);

    private final FiberWorkerThread[] threads;
    private final int throughput;
    private final boolean callerRunsOptimization;
    private final Object CONSUMED = new Object();

    public FiberScheduler() {
        this(16, true, 20);
    }

    public FiberScheduler(int threadCount, boolean callerRunsOptimization, int throughput) {
        if (threadCount < 0) throw new IllegalArgumentException("threadCount can't be smaller than 0");
        if (throughput < 1) throw new IllegalArgumentException("throughput can't be smaller than 1");

        this.callerRunsOptimization = callerRunsOptimization;
        this.throughput = throughput;
        threads = new FiberWorkerThread[threadCount];
        for (int k = 0; k < threads.length; k++) {
            threads[k] = new FiberWorkerThread();
            threads[k].start();
        }
    }

    public Fiber start(FiberContext fiberTask) {
        return new FiberImpl(notNull(fiberTask, "fiberContext"));
    }

    private FiberWorkerThread findWorkerThread() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        int threadIndex = random.nextInt(threads.length);
        return threads[threadIndex];
    }

    private class FiberImpl implements Fiber {
        private final AtomicReference<FiberWorkerThread> ownerRef = new AtomicReference<>();
        private final com.hazelcast.actors.fibers.FiberContext fiberContext;

        private FiberImpl(com.hazelcast.actors.fibers.FiberContext fiberContext) {
            this.fiberContext = fiberContext;
        }

        @Override
        public String getName() {
            return fiberContext.getFiberName();
        }

        @Override
        public void scheduleTask(Object task) {
            final FiberWorkerThread currentOwner = ownerRef.get();
            final Thread currentThread = Thread.currentThread();

            if (currentOwner == currentThread) {
                //the task is scheduled from the same thread as the thread that currently owns this fiber. We can't
                //use the caller-runs optimization here, since it could lead to an interleaved processing of tasks.
                fiberContext.storeTask(task);
                return;
            }

            if (currentOwner != null) {
                //the fiber already is scheduled, so lets store the task.

                fiberContext.storeTask(task);

                //lets make sure that the fiber still is scheduled. If it is, then we are finished since it will be
                //the owner his responsibility to execute the task.
                if (ownerRef.get() != null) return;

                //we stored the task, but the fiber isn't scheduled. So lets fall through so this fiber gets scheduled.
                task = CONSUMED;
            }

            //the fiber currently isn't scheduled.
            if (callerRunsOptimization && task != CONSUMED && currentThread instanceof FiberWorkerThread) {
                final boolean scheduled = ownerRef.compareAndSet(null, (FiberWorkerThread) currentThread);

                if (scheduled) {
                    try {
                        fiberContext.executeTask(task);
                    } finally {
                        ownerRef.set(null);
                    }

                    //if there is no work available after we unschedule the fiber, we are done.
                    if (!fiberContext.workAvailable()) return;
                    //since there is work available, we still need to schedule the fiber. We don't need to
                    //execute this task since it already is processed.
                    task = CONSUMED;
                    //we didn't manage to do the caller runs optimization because another fiberworkerthread claimed
                } else {
                    //somebody else has claimed ownership.
                    fiberContext.storeTask(task);

                    //lets make sure that the fiber still is scheduled. If it is, then we are finished since it will be
                    //the owner his responsibility to execute the task.
                    if (ownerRef.get() != null) return;

                    //we stored the task, but the fiber isn't scheduled. So lets fall through so this fiber gets scheduled.
                    task = CONSUMED;
                }
            }

            //this is the last resort, we need to rely on scheduling
            if (task != CONSUMED) fiberContext.storeTask(task);

            final FiberWorkerThread newOwner = findWorkerThread();
            final boolean claimed = ownerRef.compareAndSet(null, newOwner);
            if (claimed) {
                try {
                    newOwner.localWorkPile.put(this);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        @Override
        public void stop() {
            throw new UnsupportedOperationException();
        }
    }

    private final class FiberWorkerThread extends Thread {

        //TODO: This queue is a huge performance hog.
        private final BlockingQueue<FiberImpl> localWorkPile = new LinkedBlockingQueue<>();

        public FiberWorkerThread() {
            super("FiberWorkerThread-" + ID_GENERATOR.getAndIncrement());
        }

        public void run() {
            try {
                for (; ; ) {
                    FiberImpl fiber = localWorkPile.take();

                    for (int k = 0; k < throughput; k++) {
                        com.hazelcast.actors.fibers.FiberContext fiberContext = fiber.fiberContext;
                        Object task = fiberContext.takeTask();
                        fiberContext.executeTask(task);
                        if (!fiberContext.workAvailable()) {
                            break;
                        }
                    }

                    if (fiber.fiberContext.workAvailable()) {
                        localWorkPile.add(fiber);
                    } else {
                        fiber.ownerRef.set(null);
                        //todo: it could be that work has been added, after we have unscheduld this fiber. We need to deal with this situation.
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
}
