package com.hazelcast.fibers;

/**
 * The idea behind a caterpillerqueue is that there is reduced contention on the head on tail be limiting ordering
 * guarantees. Normally there is a total ordering in the messages on a queue, but with the caterpillerqueue there
 * only is an ordering between items posted by the same 'entity' (we need to figure out what this 'entity' means).
 * Very likely it is the actor.
 *
 * The CaterPillerQueue contains a continuous chain of 'plates'. As soon as a plate is used (either filler or empties)
 * the next plate is selected. When work is taken, a complete 'plate' can be 'removed', instead of one by one. This should
 * reduce contention on the tail of the queue.
 *
 * On the head of the queue, puts by different entities can happen concurrently since we don't care about a strict ordering.
 */
public class CaterpillarQueue {
}
