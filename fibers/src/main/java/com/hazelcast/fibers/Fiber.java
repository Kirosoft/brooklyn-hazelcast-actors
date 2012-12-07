package com.hazelcast.fibers;

/**
 * A Fiber is a lightweight threading construction. Many fibers can make use of the same thread, and over time
 * the same fiber can be executed by different threads. The Fiber always is executed by at most 1 thread at any
 * given moment. The idea is that there can be many fibers (potentially millions) sharing a limited number of
 * threads for execution.
 *
 * Fibers can be used for 'actor' based situations where there is some kind of queue where pending work is stored.
 * And where processing work from that queue is done on a fiber. This processing should be kept at short as possible,
 * so one should not block till that mailbox receives a message. But in such a case, the fiber should be signalled
 * that there is work to do, and the FiberScheduler schedules the fiber to be executed at some moment in time. When it
 * is executed, since there is work on the queue, processing that message will not block (unless a blocking operation
 * within that fiber is executed).
 *
 */
public interface Fiber {

    /**
     * Signals the fiber that there is work to do.
     */
    void scheduleTask(Object work);

    /**
     * Stops the Fiber from being scheduled. This method can safely called of the Fiber already is stopped.
     */
    void stop();

    /**
     * Returns the name of the Fiber.
     *
     * @return the name of the Fiber.
     */
    String getName();
}
