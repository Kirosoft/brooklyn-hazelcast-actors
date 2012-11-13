package io.brooklyn.activeobject;

import com.hazelcast.actors.actors.AbstractActor;
import com.hazelcast.actors.api.ActorRecipe;
import com.hazelcast.actors.api.ActorRef;

import java.lang.reflect.Method;

public class ActiveObjectActor extends AbstractActor {

    private AbstractActiveObject activeObject;

    @Override
    public void init(ActorRecipe recipe) throws Exception {
        super.init(recipe);

        try {

            System.out.println("init start");
            String clazz = (String) recipe.getProperties().get("activeObjectClass");
            activeObject = (AbstractActiveObject) Class.forName(clazz).newInstance();
            activeObject.setActorRef(self());
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    @Override
    public final void receive(Object msg, ActorRef sender) throws Exception {
        if (msg instanceof ActiveObjectMessage) {
            ActiveObjectMessage activeObjectMessage = (ActiveObjectMessage) msg;

            Method method = findActiveObjectMethod(activeObjectMessage);
            method.invoke(activeObject, activeObjectMessage.getArgs());
        }
    }

    protected Method findActiveObjectMethod(ActiveObjectMessage msg) {
        Class clazz = activeObject.getClass();
        do {
            for (Method method : clazz.getDeclaredMethods()) {
                boolean sameMethodName = method.getName().equals(msg.getMethodName());
                boolean sameArgCount = method.getParameterTypes().length == msg.getArgs().length;
                if (sameMethodName && sameArgCount) {
                    if (!method.getReturnType().equals(Void.TYPE)) {
                        throw new IllegalArgumentException("Only void method allowed " + method);
                    }
                    return method;
                }
            }
            clazz = clazz.getSuperclass();
        } while (clazz != null);

        return null;
    }
}
