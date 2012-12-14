package com.hazelcast.actors.actors;

import com.hazelcast.actors.api.Actor;
import com.hazelcast.actors.api.ActorRef;
import com.hazelcast.actors.api.exceptions.UnprocessedException;
import com.hazelcast.actors.utils.Util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static java.lang.String.format;
import static java.lang.reflect.Modifier.isAbstract;
import static java.lang.reflect.Modifier.isStatic;

/**
 * The ReflectiveActor is an AbstractActor that uses reflection to dispatch to right receive method. So a user
 * doesn't need to dispatch on the correct message type. For every type map message you are interested in,
 * create a receive method.
 * <p/>
 * The DispatchingActor stores the looked up receive method in a cache.
 * <p/>
 * Receiving an 'Object' is not possible. Use the AbstractActor if you want a single receive method that receives
 * all messages.
 * <p/>
 * Receiving on subtypes is allowed. E.g. you could create a receive method that accepts a List, but send it an
 * LinkedList. If Multiple receive methods match, then the one with the most strict types will be selected. If multiple
 * method are most strict (e.g. when using interfaces) then the ambiguity is detected and an exception is thrown.
 * <p/>
 * If you are not interested in the sender, you can create receive(SomeType msg). Otherwise you create a
 * receive(SomeType msg, ActorRef ref) method.
 * <p/>
 * Receive methods are not allowed to be static.
 * <p/>
 * Receive methods are not allowed to return a value.
 * <p/>
 * Receive methods are allowed to throw Exceptions (checked and unchecked).
 *
 * @author Peter Veentjer.
 */
public class DispatchingActor extends AbstractActor implements MissingMethodHandler<ActorRef>{

    private final static MethodDispatcher dispatcher = new MethodDispatcher(ActorRef.class);

    /**
     * Override this method if you want to execute a certain action when the message is not handled
     * by any map the receive methods.
     * <p/>
     * By default an UnprocessedException is thrown.
     *
     * @param msg
     * @param sender
     */
    public void onUnhandledMessage(Object msg, ActorRef sender) {
        String id = sender == null ? "unknown" : sender.getId();
        throw new UnprocessedException("No receive method found on actor.class: " + getClass().getName() +
                " for message.class:" + msg.getClass().getName() + " send by: " + id);
    }

    public final void receive(Object msg) {
        throw new RuntimeException("Should never be called");
    }

    @Override
    public final void receive(Object msg, ActorRef sender) throws Exception {
        //needed to prevent looping.
        if (msg.getClass().equals(Object.class)) {
            throw new UnprocessedException(
                    format("No receive method available for 'Object.class' on '%s'. " +
                            "If you want to receive all messages, use an AbstractActor", getClass().getName()));
        }

        dispatcher.dispatch(this,msg,sender,this);
    }
}
