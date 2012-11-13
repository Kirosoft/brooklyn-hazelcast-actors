package io.brooklyn.activeobject;

import java.io.Serializable;

import static com.hazelcast.actors.utils.Util.notNull;

public class ActiveObjectMessage implements Serializable {
    private final String methodName;
    private final Object[] args;

    public ActiveObjectMessage(String methodName, Object... args) {
        this.args = notNull(args, "args");
        this.methodName = notNull(methodName, "methodName");
    }

    public Object[] getArgs() {
        return args;
    }

    public String getMethodName() {
        return methodName;
    }
}
