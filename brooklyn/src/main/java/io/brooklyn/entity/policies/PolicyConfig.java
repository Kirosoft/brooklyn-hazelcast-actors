package io.brooklyn.entity.policies;

import io.brooklyn.entity.EntityConfig;

import java.util.Map;

public class PolicyConfig<P extends Policy> extends EntityConfig<P> {
    public PolicyConfig(Class<P> policyClass) {
        super(policyClass);
    }

    public PolicyConfig(Class<P> policyClass, Map<String, Object> properties) {
        super(policyClass, properties);
    }
}
