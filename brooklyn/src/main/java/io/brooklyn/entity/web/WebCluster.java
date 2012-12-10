package io.brooklyn.entity.web;

import com.hazelcast.actors.api.ActorRef;
import io.brooklyn.AbstractMessage;
import io.brooklyn.entity.Group;
import io.brooklyn.entity.Stop;
import io.brooklyn.entity.softwareprocess.SoftwareProcess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.hazelcast.actors.utils.Util.notNull;

public class WebCluster extends Group {

    private static final Logger log = LoggerFactory.getLogger(Tomcat.class);


    @Override
    public void onActivation() throws Exception {
        if (log.isDebugEnabled())log.debug(self() + ":WebCluster:Activate");
        super.onActivation();
    }

    public void receive(SoftwareProcess.Start start) {
        if (log.isDebugEnabled()) log.debug(self() + ":WebCluster:Start");
        location.set(start.location);
        if (log.isDebugEnabled()) log.debug(self() + ":WebCluster:Start Complete");
    }

    public void receive(ScaleTo scaleTo) {
        if (log.isDebugEnabled()) log.debug(self() + ":WebCluster:ScaleTo");

        int delta = scaleTo.size - children.size();
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
            ActorRef webServer = children.removeFirst();
            send(webServer, new Stop());
        }
    }

    private void scaleUp(int delta) {
        for (int k = 0; k < delta; k++) {
            //todo: we want to use a factory here. We do not want to be tied to tomcat
            //todo: we are linking to the webserver; the question is if we want to do that.
            ActorRef webServer = spawnAndLink(new TomcatConfig());
            addChild(webServer);

            //lets start webServer.
            send(webServer, new SoftwareProcess.Start(location.get()));
        }
    }

    public void receive(ReplaceWebServer replaceWebServer) {
        if (log.isDebugEnabled()) log.debug(self() + ":WebCluster:ReplaceWebServer");

        boolean found = children.remove(replaceWebServer.webServer);
        if (!found) {
            return;
        }
        send(replaceWebServer.webServer, new Stop());
        scaleUp(children.size() + 1);
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
        public final ActorRef webServer;

        public ReplaceWebServer(ActorRef webServer) {
            this.webServer = notNull(webServer, "webServer");
        }
    }
}
