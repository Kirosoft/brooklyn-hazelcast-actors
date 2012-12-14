package io.brooklyn.entity.web;

import io.brooklyn.AbstractMessage;
import io.brooklyn.attributes.ReferenceAttribute;
import io.brooklyn.attributes.RelationsAttribute;
import io.brooklyn.entity.EntityReference;
import io.brooklyn.entity.PlatformComponent;
import io.brooklyn.entity.Stop;
import io.brooklyn.entity.softwareprocess.SoftwareProcess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.hazelcast.actors.utils.Util.notNull;

public class WebCluster extends PlatformComponent {

    private static final Logger log = LoggerFactory.getLogger(WebCluster.class);

    protected final ReferenceAttribute<? extends WebServerFactory> webserverFactory =
            newReferenceAttribute("webserverFactory", new TomcatFactory());
    protected final RelationsAttribute webservers = newRelationsAttribute("webservers");

    @Override
    public void onActivation() throws Exception {
        if (log.isDebugEnabled()) log.debug(self() + ":WebCluster:Activate");
        super.onActivation();
    }

    public void receive(SoftwareProcess.Start start) {
        if (log.isDebugEnabled()) log.debug(self() + ":WebCluster:Start");
        location.set(start.location);
        if (log.isDebugEnabled()) log.debug(self() + ":WebCluster:Start Complete");
    }

    public void receive(ScaleTo scaleTo) {
        if (log.isDebugEnabled()) log.debug(self() + ":WebCluster:" + scaleTo);

        int delta = scaleTo.size - webservers.size();
        if (delta == 0) {
            //no change.
        } else if (delta > 0) {
            scaleUp(delta);
        } else {
            scaleDown(-delta);
        }

        if (log.isDebugEnabled()) log.debug(self() + ":WebCluster:ScaleTo Complete");
    }

    private void scaleDown(int delta) {
        for (int k = 0; k < delta; k++) {
            EntityReference webServer = webservers.removeFirst();
            send(webServer, new Stop());
        }
    }

    private void scaleUp(int delta) {
        WebServerFactory factory = webserverFactory.get();
        for (int k = 0; k < delta; k++) {
            EntityReference webServer = factory.newWebServer(this);
            webservers.add(webServer);

            //lets start webServer.
            send(webServer, new SoftwareProcess.Start(location.get()));
        }
    }

    public void receive(ReplaceWebServer replaceWebServer) {
        if (log.isDebugEnabled()) log.debug(self() + ":WebCluster:ReplaceWebServer");

        boolean found = webservers.remove(replaceWebServer.webServer);
        if (!found) {
            return;
        }
        send(replaceWebServer.webServer, new Stop());
        scaleUp(webservers.size() + 1);
    }

    //scales the webcluster to a certain size. This will be send by the replaceMemberOnFirePolicy.
    public static class ScaleTo extends AbstractMessage {
        public final int size;

        public ScaleTo(int size) {
            if (size < 0) throw new IllegalArgumentException();
            this.size = size;
        }
    }

    public static class ReplaceWebServer extends AbstractMessage {
        public final EntityReference webServer;

        public ReplaceWebServer(EntityReference webServer) {
            this.webServer = notNull(webServer, "webServer");
        }
    }
}
