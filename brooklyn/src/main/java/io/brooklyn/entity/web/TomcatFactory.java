package io.brooklyn.entity.web;

import io.brooklyn.entity.Entity;
import io.brooklyn.entity.EntityReference;

public class TomcatFactory implements WebServerFactory {

    @Override
    public EntityReference newWebServer(Entity owner) {
        TomcatConfig tomcatConfig = new TomcatConfig();
        tomcatConfig.addProperties(owner);
        return owner.spawnAndLink(tomcatConfig);
    }
}
