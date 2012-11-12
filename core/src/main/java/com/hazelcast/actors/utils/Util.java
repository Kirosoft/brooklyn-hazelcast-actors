package com.hazelcast.actors.utils;

import com.hazelcast.actors.api.ActorRef;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.lang.reflect.Method;

public class Util {

    public static Method findReceiveMethod(Class actorClass, Class messageClass) {
        while (true) {
            try {
                Method receiveMethod = actorClass.getDeclaredMethod("receive", messageClass, ActorRef.class);
                receiveMethod.setAccessible(true);
                return receiveMethod;
            } catch (NoSuchMethodException e) {
                actorClass = actorClass.getSuperclass();
                if (actorClass == null) {
                    return null;
                }
            }
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
