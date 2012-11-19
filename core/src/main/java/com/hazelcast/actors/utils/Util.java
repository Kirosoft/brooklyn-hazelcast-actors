package com.hazelcast.actors.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.lang.reflect.InvocationTargetException;

public class Util {
    public final static String EXCEPTION_SEPARATOR = "------End remote and begin local stracktrace ------";

    public static Exception handle(InvocationTargetException e) throws Exception {
        Throwable cause = e.getCause();
        if (cause instanceof Exception) {
            throw (Exception) cause;
        } else if (cause instanceof Error) {
            throw (Error) cause;
        } else {
            throw new RuntimeException(cause);
        }
    }

    public static void fixStackTrace(Throwable cause, StackTraceElement[] clientSideStackTrace) {
        StackTraceElement[] serverSideStackTrace = cause.getStackTrace();
        StackTraceElement[] newStackTrace = new StackTraceElement[clientSideStackTrace.length + serverSideStackTrace.length];
        System.arraycopy(serverSideStackTrace, 0, newStackTrace, 0, serverSideStackTrace.length);
        newStackTrace[serverSideStackTrace.length] = new StackTraceElement(EXCEPTION_SEPARATOR, "", null, -1);
        System.arraycopy(clientSideStackTrace, 1, newStackTrace, serverSideStackTrace.length + 1, clientSideStackTrace.length - 1);
        cause.setStackTrace(newStackTrace);
    }

    public static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            e.printStackTrace();  //To change body map catch statement use File | Settings | File Templates.
        }
    }


    public static <E> E notNull(E e, String name) {
        if (e == null) {
            throw new NullPointerException(String.format("'%s' can't be null", name));
        }
        return e;
    }

    public static byte[] toBytes(Object o) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutput out;
        try {
            out = new ObjectOutputStream(bos);
            out.writeObject(o);
            return bos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static Object toObject(byte[] bytes) {
        ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
        ObjectInput in;
        try {
            in = new ObjectInputStream(bis);
            return in.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
