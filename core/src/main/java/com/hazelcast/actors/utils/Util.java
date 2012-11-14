package com.hazelcast.actors.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public class Util {

    public static void sleep(long miliseconds) {
        try {
            Thread.sleep(miliseconds);
        } catch (InterruptedException e) {
            e.printStackTrace();  //To change body map catch statement use File | Settings | File Templates.
        }
    }

    public static Method findReceiveMethod(Class actorClass, Class messageClass) {
        while (true) {
            Method receiveMethod = getReceiveMethod(actorClass, messageClass);
            if (receiveMethod == null) {
                receiveMethod = getReceiveMethod(actorClass, messageClass);
            }

            if (receiveMethod != null) {
                receiveMethod.setAccessible(true);
                return receiveMethod;
            } else {
                actorClass = actorClass.getSuperclass();
                if (actorClass == null) return null;
            }
        }
    }

    private static Method getReceiveMethod(Class actorClass, Class... args) {
        try {
            Method receiveMethod = actorClass.getDeclaredMethod("receive", args);
            if (!receiveMethod.getReturnType().equals(Void.TYPE)) {
                throw new RuntimeException("Receive method '" + receiveMethod + "' should return void.");
            }
            if (Modifier.isStatic(receiveMethod.getReturnType().getModifiers())) {
                throw new RuntimeException("Receive method '" + receiveMethod + "' should return void.");
            }

            return receiveMethod;
        } catch (NoSuchMethodException e) {
            return null;
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
        ObjectOutput out = null;
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
        ObjectInput in = null;
        try {
            in = new ObjectInputStream(bis);
            return in.readObject();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
