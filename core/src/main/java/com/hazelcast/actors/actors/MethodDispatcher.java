package com.hazelcast.actors.actors;

import com.hazelcast.actors.api.Actor;
import com.hazelcast.actors.api.exceptions.UnprocessedException;
import com.hazelcast.actors.utils.Util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static com.hazelcast.actors.utils.Util.notNull;
import static java.lang.String.format;
import static java.lang.reflect.Modifier.isAbstract;
import static java.lang.reflect.Modifier.isStatic;

public class MethodDispatcher {

    private final static ConcurrentMap<Class, ConcurrentMap<Class, Method>> receiveMethodMap = new ConcurrentHashMap<>();

    private final Class senderClass;

    public MethodDispatcher(Class senderClass) {
        this.senderClass = notNull(senderClass, "sender");
    }

    public void dispatch(Object target, Object msg, Object sender, MissingMethodHandler missingMethodHandler) throws Exception {
        notNull(target, "target");

        Class targetClass = target.getClass();

        Method receiveMethod = findReceiveMethod(targetClass, msg.getClass());
        if (receiveMethod == null) {
            missingMethodHandler.onUnhandledMessage(msg, sender);
            return;
        }

        try {
            if (receiveMethod.getParameterTypes().length == 2) {
                receiveMethod.invoke(target, msg, sender);
            } else {
                receiveMethod.invoke(target, msg);
            }
        } catch (IllegalAccessException e) {
            //This will not be thrown since we make the receiveMethod accessible
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw Util.handle(e);
        }
    }

    public Method findReceiveMethod(Class actorClass, Class messageClass) {
        ConcurrentMap<Class, Method> actorReceiveMethods = receiveMethodMap.get(actorClass);
        if (actorReceiveMethods == null) {
            actorReceiveMethods = new ConcurrentHashMap<>();
            ConcurrentMap<Class, Method> found = receiveMethodMap.putIfAbsent(actorClass, actorReceiveMethods);
            actorReceiveMethods = found == null ? actorReceiveMethods : found;
        }

        Method method = actorReceiveMethods.get(messageClass);
        if (method != null) {
            return method;
        }

        method = findBestMatch(actorClass, messageClass);
        if (method != null) {
            actorReceiveMethods.put(messageClass, method);
        }

        return method;
    }

    private Method findBestMatch(Class actorClass, Class messageClass) {
        Class clazz = actorClass;
        do {
            Method bestMatch = null;
            for (Method method : clazz.getDeclaredMethods()) {
                if (!method.getName().equals("receive")) {
                    continue;
                }

                Class<?>[] parameterTypes = method.getParameterTypes();
                if (parameterTypes.length == 0 || parameterTypes.length > 2) {
                    //the method is not usable since it doesn't have the right number of arguments.
                    continue;
                }

                Class receiveMessageType = parameterTypes[0];
                if (receiveMessageType.equals(messageClass)) {
                    //we have an exact match, so we can stop searching.
                    bestMatch = method;
                    break;
                }

                if (receiveMessageType.isAssignableFrom(messageClass)) {
                    if (bestMatch == null) {
                        bestMatch = method;
                    } else {
                        Class<?> bestReceiveMessageType = bestMatch.getParameterTypes()[0];
                        if (bestReceiveMessageType.isAssignableFrom(receiveMessageType)) {
                            bestMatch = method;
                        } else if (!receiveMessageType.isAssignableFrom(bestReceiveMessageType)) {
                            throw new UnprocessedException("Ambiguous " + bestMatch + " " + method + " for message " + messageClass);
                        }
                    }
                }
            }

            if (bestMatch != null) {
                checkValid(bestMatch);
                return bestMatch;
            }

            clazz = clazz.getSuperclass();
        } while (!DispatchingActor.class.equals(clazz));

        return null;
    }

    private void checkValid(Method receiveMethod) {
        if (receiveMethod.getParameterTypes().length == 2) {
            Class actualSenderType = receiveMethod.getParameterTypes()[1];
            if (!actualSenderType.isAssignableFrom(senderClass)) {
                throw new UnprocessedException(format("Receive method '%s' should have '%s' as second argument.",
                        receiveMethod, senderClass.getName()));
            }
        }

        if (!receiveMethod.getReturnType().equals(Void.TYPE)) {
            throw new UnprocessedException(format("Receive method '%s' can't have a return value.", receiveMethod));
        }

        if (isStatic(receiveMethod.getModifiers())) {
            throw new UnprocessedException(format("Receive method '%s' can't be static.", receiveMethod));
        }

        if (isAbstract(receiveMethod.getModifiers())) {
            throw new UnprocessedException(format("Receive method '%s' can't be abstract.", receiveMethod));
        }
    }
}
