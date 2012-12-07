package com.hazelcast.fibers;


public interface FiberContext {

    /**
     * Stores a task to be scheduled later on.
     *
     * This method can be called concurrently.
     *
     * @param task the task to schedule. The task is allowed to be null.
     */
    void storeTask(Object task);

    /**
     * Takes a task to be processed. This task should not block since it only will be called if there is work
     * available.
     *
     * Only 1 thread at a time will call this method.
     *
     * @return the task to process (is allowed to be null).
     */
    Object takeTask();

    /**
     * Executes a single 'task'.
     *
     * Only 1 thread at a time will call this method.
     *
     * @param task the task to execute.
     */
    void executeTask(Object task);

    /**
     * Checks if there is any work available.
     *
     * This method should be threadsafe.
     *
     * @return true if there is work available, false otherwise.
     */
    boolean workAvailable();

    /**
     * Returns the name of this Fiber. The name can be used for identification purposes,
     * but is not required to be unique.
     *
     * This method should be threadsafe.
     *
     * @return the name of the Fiber.
     */
    String getFiberName();
}
