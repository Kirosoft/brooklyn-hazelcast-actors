package com.hazelcast.actors.actors;

import com.hazelcast.actors.api.ActorRef;
import com.hazelcast.actors.api.UnprocessedException;
import com.hazelcast.actors.utils.Util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * The ReflectiveActor is an AbstractActor that uses reflection to find the right receive method. So a user
 * doesnt' need to dispatch on the correct message type. For every type of message you are interested in,
 * create a receive method.
 *
 * @author Peter Veentjer.
 */
public class ReflectiveActor extends AbstractActor {

    public void receive(Callback callback) {
        callback.run(this);
    }

    /**
     * Override this method if you want to execute a certain action when the message is not handled
     * by any of the receive methods.
     *
     * @param msg
     * @param sender
     */
    public void onUnhandledMessage(Object msg, ActorRef sender) {
        throw new UnprocessedException("No receive method found on actor.class: " + getClass().getName() +
                " for message.class:" + msg.getClass().getName()+" send by: "+sender.getId());
    }

    @Override
    public final void receive(Object msg, ActorRef sender) throws Exception {
        Method receiveMethod = Util.findReceiveMethod(getClass(), msg.getClass());
        if (receiveMethod == null) {
            onUnhandledMessage(msg, sender);
            return;
        }

        try {
            receiveMethod.invoke(this, msg, sender);
        } catch (IllegalAccessException e) {
            //This will not be thrown since we make the receiveMethod accessible
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getTargetException();
            if (cause instanceof Exception) {
                throw (Exception) cause;
            } else if (cause instanceof Error) {
                throw (Error) cause;
            } else {
                throw new RuntimeException(e);
            }
        }
    }
}
