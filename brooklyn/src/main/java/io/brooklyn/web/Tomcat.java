package io.brooklyn.web;

import com.hazelcast.actors.ActorRecipe;
import com.hazelcast.actors.ActorRef;
import io.brooklyn.SoftwareProcessDriver;
import io.brooklyn.SoftwareProcessEntity;
import io.brooklyn.attributes.AttributeType;
import io.brooklyn.attributes.BasicAttribute;

import java.io.Serializable;

public class Tomcat extends SoftwareProcessEntity<TomcatDriver> {

    protected final BasicAttribute<Integer> portAttribute = newBasicAttribute(new AttributeType<Integer>("port", 8080));
    protected final BasicAttribute<String> stateAttribute = newBasicAttribute(new AttributeType<String>("state", "stopped"));
    protected final BasicAttribute<ActorRef> clusterAttribute = newBasicAttribute(new AttributeType<ActorRef>("cluster"));
    protected final BasicAttribute<Long> usedHeapAttribute = newBasicAttribute(new AttributeType<Long>("usedHeap"));
    protected final BasicAttribute<Long> maxHeapAttribute = newBasicAttribute(new AttributeType<Long>("maxHeap"));

    @Override
    public Class<? extends SoftwareProcessDriver> getDriverClass() {
        return TomcatDriver.class;
    }

    @Override
    public void init(ActorRecipe actorRecipe) {
        super.init(actorRecipe);

        getActorRuntime().repeat(self(),new Tomcat.MeasureHeap(),1000);
    }

    public void receive(UndeployMessage msg, ActorRef sender) {
        System.out.println("Undeploy");
        getDriver().undeploy();
    }

    public void receive(DeployMessage msg, ActorRef sender) {
        System.out.println("Deploying:" + msg.url);
        //todo: would be best to offload the work of the potentially long copy action
        getDriver().deploy();
    }

    public void receive(StartTomcatMessage msg, ActorRef sender) {
        System.out.println("StartTomcat");

        clusterAttribute.set(msg.cluster);
        locationAttribute.set(msg.location);

        try {
            stateAttribute.set("Starting");
            getDriver().install();
            getDriver().customize();
            getDriver().launch();
            stateAttribute.set("Started");
        } catch (Exception e) {
            e.printStackTrace();
            stateAttribute.set("On fire");
        }
    }

    public void receive(StopTomcatMessage msg, ActorRef sender) {
        System.out.println("StopTomcat");
        try {
            stateAttribute.set("Stopping");
            getDriver().stop();
            stateAttribute.set("Stopped");
        } catch (Exception e) {
            e.printStackTrace();
            stateAttribute.set("On fire");
        }
    }

    public void receive(TomcatFailureMessage msg, ActorRef sender) {
        System.out.println("TomcatFailure at: " + self());
        ActorRef cluster = clusterAttribute.get();
        if (cluster != null) {
            getActorRuntime().send(cluster, new WebCluster.ChildFailureMessage(self()));
        }
    }

    public void receive(MeasureHeap msg, ActorRef sender){
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        usedHeapAttribute.set(maxMemory - runtime.freeMemory());
        maxHeapAttribute.set(maxMemory);
    }

    public static class TomcatFailureMessage implements Serializable {
    }

    public static class StopTomcatMessage implements Serializable {
    }

    public static class MeasureHeap implements Serializable{

    }

    public static class StartTomcatMessage extends StartMessage {
        public final ActorRef cluster;

        public StartTomcatMessage(String location) {
            this(location, null);
        }

        public StartTomcatMessage(String location, ActorRef cluster) {
            super(location);
            this.cluster = cluster;
        }
    }

    public static class UndeployMessage implements Serializable {
    }

    public static class DeployMessage implements Serializable {
        public final String url;

        public DeployMessage(String url) {
            this.url = url;
        }
    }
}
