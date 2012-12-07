package io.brooklyn.entity;

import com.hazelcast.actors.api.ActorRef;
import io.brooklyn.AbstractMessage;
import io.brooklyn.attributes.Attribute;
import io.brooklyn.attributes.BasicAttributeRef;
import io.brooklyn.attributes.ListAttributeRef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.hazelcast.actors.utils.Util.notNull;

public class Group extends PlatformComponent {
    private static final Logger log = LoggerFactory.getLogger(Group.class);

    protected final ListAttributeRef<ActorRef> children = newListAttributeRef("children", ActorRef.class);
    protected final ListAttributeRef<ChildAttributeRegistration> childAttributeRegistrations =
            newListAttributeRef("childAttributeRegistrations", ChildAttributeRegistration.class);

    public void addChild(ActorRef child) {
        children.add(child);

        //hook the newly created webServer up to the registered policies.
        for (ChildAttributeRegistration childAttributeRegistration : childAttributeRegistrations) {
            subscribeToAttribute(childAttributeRegistration.subscriber, child, childAttributeRegistration.attribute);
        }
    }

    public void receive(ChildAttributeRegistration registration) {
        if (log.isDebugEnabled()) log.debug(self() + ":Group:ChildPolicyRegistration");

        for (ActorRef child : children) {
            subscribeToAttribute(registration.subscriber, child, registration.attribute);
        }
        childAttributeRegistrations.add(registration);
    }

    public static class ChildAttributeRegistration extends AbstractMessage {
        public final Attribute attribute;
        public final ActorRef subscriber;

        public ChildAttributeRegistration(ActorRef subscriber, Attribute attribute) {
            this.attribute = notNull(attribute, "attribute");
            this.subscriber = notNull(subscriber, "subscriber");
        }

        public ChildAttributeRegistration(BasicAttributeRef<ActorRef> subscriber, Attribute attribute) {
            this(subscriber.get(), attribute);
        }
    }
}
