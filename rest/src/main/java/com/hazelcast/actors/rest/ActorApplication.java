package com.hazelcast.actors.rest;

import com.hazelcast.actors.ActorRef;
import com.hazelcast.actors.ActorRuntime;
import org.restlet.Application;
import org.restlet.Component;
import org.restlet.Context;
import org.restlet.Restlet;
import org.restlet.data.Form;
import org.restlet.data.Parameter;
import org.restlet.data.Protocol;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;
import org.restlet.routing.Router;

public class ActorApplication extends Application {

    //nasty hack
    private static ActorRuntime actorRuntime;

    public ActorApplication(Context context, ActorRuntime actorRuntime) {
        super(context);
        this.actorRuntime = actorRuntime;
    }

    public Restlet createInboundRoot() {
        Router router = new Router(this.getContext());
        router.attach("/actor", ActorsResource.class);
        router.attach("/actor/{actor_id}", ActorResource.class);
        //router.attach("/race/{race_id}/runner", RaceRunnersResource.class);
        //router.attach("/race/{race_id}/runner/{runner_id}", RaceRunnerResource.class);
        return router;
    }

    public static class ActorResource extends ServerResource {

        String actorId;
        Integer partitionId;
        Object msg;

        @Override
        protected void doInit() throws ResourceException {
            this.actorId = (String) getRequestAttributes().get("actor_id");
            Form form = getRequest().getResourceRef().getQueryAsForm();
            Parameter p = form.getFirst("partition_id");
            partitionId = Integer.parseInt(p.getValue());
            msg = form.getFirst("msg").getValue();
        }

        @Post
        public String doPost() {
            ActorRef actorRef = new ActorRef(actorId, partitionId);
            actorRuntime.send(actorRef, msg);
            return "actor resource:" + actorId;
        }

        @Get
        public String doGet() {
            return "actor resource:" + actorId;
        }
    }


    public void startServer() throws Exception {
        Component component = new Component();
        component.getServers().add(Protocol.HTTP, 8182);
        component.getDefaultHost().attach("/peter", this);
        component.start();
    }
}