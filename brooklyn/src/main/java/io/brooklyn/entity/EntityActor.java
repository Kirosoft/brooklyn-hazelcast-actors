package io.brooklyn.entity;

import com.hazelcast.actors.actors.AbstractActor;
import com.hazelcast.actors.actors.MethodDispatcher;
import com.hazelcast.actors.actors.MissingMethodHandler;
import com.hazelcast.actors.api.ActorRef;
import com.hazelcast.actors.api.Injected;
import io.brooklyn.ManagementContext;

public class EntityActor extends AbstractActor implements MissingMethodHandler<EntityReference> {

    private final static MethodDispatcher methodDispatcher = new MethodDispatcher(EntityReference.class);

    private Entity entity;

    @Injected
    ManagementContext managementContext;
    public EntityReference entityReference;

    @Override
    public void onActivation() throws Exception {
        entityReference = new EntityReference(self());
        EntityConfig entityConfig = (EntityConfig)getRecipe().getProperties().get("entityConfig");
        Class<? extends Entity> entityClass = entityConfig.getEntityClass();
        entity = entityClass.newInstance();
        entity.entityActor = this;
        entity.onActivation();
    }

    @Override
    public void onUnhandledMessage(Object msg, EntityReference sender) {
        entity.onUnhandledMessage(msg,sender);
    }

    @Override
    public void receive(Object msg, ActorRef sender) throws Exception {
        EntityReference entitySender = sender == null ? null : new EntityReference(sender);
        methodDispatcher.dispatch(entity, msg, entitySender, this);
    }
}
