package io.brooklyn.entity.application;

import io.brooklyn.entity.EntityConfig;

public class ApplicationConfig extends EntityConfig{

    public ApplicationConfig(Class<? extends Application> entityClazz) {
        super(entityClazz);
    }
}
