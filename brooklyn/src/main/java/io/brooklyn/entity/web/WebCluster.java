package io.brooklyn.entity.web;

import com.hazelcast.actors.api.ActorRef;
import io.brooklyn.AbstractMessage;
import io.brooklyn.attributes.Attribute;
import io.brooklyn.attributes.BasicAttributeRef;
import io.brooklyn.attributes.ListAttributeRef;
import io.brooklyn.entity.PlatformComponent;
import io.brooklyn.entity.Start;
import io.brooklyn.entity.Stop;

import static com.hazelcast.actors.utils.Util.notNull;

public class WebCluster extends PlatformComponent {

    private final ListAttributeRef<ActorRef> children = newListAttributeRef("children", ActorRef.class);
    private final ListAttributeRef<WebServerPolicyRegistration> policyRegistrations =
            newListAttributeRef("policyRegistrations", WebServerPolicyRegistration.class);

    @Override
    public void activate() throws Exception {
        System.out.println(self() + ":WebCluster:Activate");
        super.activate();
    }

    public void receive(Start start) {
        System.out.println(self() + ":WebCluster:Start");
        location.set(start.location);
        System.out.println(self() + ":WebCluster:Start Complete");
    }

    public void receive(ScaleTo scaleTo) {
        System.out.println(self() + ":WebCluster:ScaleTo");

        int delta = scaleTo.size - children.size();
        if (delta == 0) {
            //no change.
        } else if (delta > 0) {
            //we need to add machines.
            for (int k = 0; k < delta; k++) {
                //todo: we want to use a factory here. We do not want to be tied to tomcat.
                ActorRef webServer = newEntity(new TomcatConfig());
                children.add(webServer);

                //hook the newly created webServer up to the registered policies.
                for (WebServerPolicyRegistration webServerPolicyRegistration : policyRegistrations) {
                    subscribeToAttribute(webServerPolicyRegistration.subscriber, webServer, webServerPolicyRegistration.attribute);
                }

                //lets start webServer.
                send(webServer, new Start(location));
            }
        } else {
            //we need to remove machines.
            for (int k = 0; k < -delta; k++) {
                ActorRef webServer = children.removeFirst();
                send(webServer, new Stop());
            }
        }

        System.out.println(self() + ":WebCluster:ScaleTo Complete");
    }

    public void receive(ReplaceWebServer replaceWebServer) {
        System.out.println(self() + ":WebCluster:ReplaceWebServer");

        boolean found = children.remove(replaceWebServer.webServer);
        if (!found) {
            return;
        }
        send(replaceWebServer.webServer, new Stop());
        receive(new ScaleTo(1));
    }

    public void receive(WebServerPolicyRegistration webServerPolicyRegistration) {
        System.out.println(self() + ":WebCluster:PolicyRegistration");

        for (ActorRef webServer : children) {
            subscribeToAttribute(webServerPolicyRegistration.subscriber, webServer, webServerPolicyRegistration.attribute);
        }
        policyRegistrations.add(webServerPolicyRegistration);
    }

    //scales the webcluster to a certain size. This will be send by the replaceMemberOnFirePolicy.
    public static class ScaleTo extends AbstractMessage {
        public final int size;

        public ScaleTo(int size) {
            if (size < 0) throw new IllegalArgumentException();
            this.size = size;
        }
    }

    public static class WebServerPolicyRegistration extends AbstractMessage {
        public final Attribute attribute;
        public final ActorRef subscriber;

        public WebServerPolicyRegistration(ActorRef subscriber, Attribute attribute) {
            this.attribute = notNull(attribute, "attribute");
            this.subscriber = notNull(subscriber, "subscriber");
        }

        public WebServerPolicyRegistration(BasicAttributeRef<ActorRef> subscriber, Attribute attribute) {
            this(subscriber.get(), attribute);
        }
    }

    public static class ReplaceWebServer extends AbstractMessage {
        public final ActorRef webServer;

        public ReplaceWebServer(ActorRef webServer) {
            this.webServer = notNull(webServer, "webServer");
        }
    }
}
