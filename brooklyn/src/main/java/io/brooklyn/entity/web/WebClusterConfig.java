package io.brooklyn.entity.web;

import io.brooklyn.attributes.AttributeType;
import io.brooklyn.entity.EntityConfig;
import io.brooklyn.entity.EntityReference;

import java.util.HashMap;
import java.util.Map;

public class WebClusterConfig extends EntityConfig {

    public static final AttributeType<? extends Map<AttributeType, EntityReference>> POLICIES =
            new AttributeType<>("httpPort", new HashMap<AttributeType, EntityReference>());
    public static final AttributeType<? extends WebServerFactory> FACTORY =
            new AttributeType<>("webserverFactory", new TomcatFactory());

    public WebClusterConfig() {
        super(WebCluster.class);
    }

    public WebClusterConfig addPolicy(AttributeType attributeType, EntityReference policy) {
        Map<AttributeType, EntityReference> policies = (Map<AttributeType, EntityReference>) getProperty(POLICIES);
        if (policies == null) {
            policies = new HashMap<>();
            policies.put(attributeType, policy);
        }
        this.addProperty(POLICIES, policies);
        return this;
    }

    public WebClusterConfig webserverFactory(WebServerFactory webServerFactory) {
        this.addProperty(FACTORY, webServerFactory);
        return this;
    }
}
