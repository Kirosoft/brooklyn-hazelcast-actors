package com.hazelcast.actors;

import com.hazelcast.actors.api.ActorRef;
import com.hazelcast.actors.utils.Util;
import com.hazelcast.core.Instance;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

public class TestUtils {

    public static void destroySilently(Instance instance) {
        if (instance == null) return;

        try {
            instance.destroy();
        } catch (RuntimeException e) {
      }
    }

    public static void assertInstanceOf(Class expectedClass, Object o) {
        if (o == null) {
            return;
        }

        assertTrue(o.getClass().isAssignableFrom(expectedClass));
    }

    public static void assertExceptionContainsLocalSeparator(Throwable throwable) {
        assertNotNull(throwable);

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        String stacktrace = sw.toString();

        assertTrue("stacktrace does not contains exception separator\n" + stacktrace, stacktrace.contains(Util.EXCEPTION_SEPARATOR));
    }

    public static void assertCompletes(CountDownLatch latch) {
        try {
            boolean completed = latch.await(1, TimeUnit.MINUTES);
            assertTrue("CountdownLatch failed to countdown to zero in the given timeout", completed);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public static void assertValidActorRef(ActorRef ref) {
        assertNotNull(ref);
        assertNotNull(ref.getPartitionKey());
        assertTrue("partitionId should be >=0", ref.getPartitionId() >= 0);
    }

    public static void assertContains(Set<ActorRef> actual, ActorRef... expected) {
        assertEquals(actual.size(), expected.length);
        for (ActorRef ref : expected) {
            assertTrue(actual.contains(ref));
        }
    }

    public static ActorRef newRandomActorRef() {
        return new ActorRef(UUID.randomUUID().toString(), "Foo", 1);
    }
}
