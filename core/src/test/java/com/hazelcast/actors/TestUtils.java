package com.hazelcast.actors;

import com.hazelcast.actors.api.ActorRef;
import com.hazelcast.actors.utils.Util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class TestUtils {

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

    public static ActorRef newRandomActorRef() {
        return new ActorRef(UUID.randomUUID().toString(), "Foo",1);
    }
}
