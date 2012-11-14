package io.brooklyn.policy;

import com.hazelcast.actors.actors.ReflectiveActor;
import io.brooklyn.attributes.SensorEvent;

/**
 * The MemberDownPolicy needs to be registered to:
 * TODO: We need to add a working status to tomcat/software process and configure it.
 * TODO: Currently it will print the failed member, the question is what the action should be.
 *
 * In the WebCluster example, the webcluster already is listening to the status of the member, so what is the point
 * of having a policy? We could introduce an additional actor for clarity/reusability.
 */
public class MemberDownPolicy extends ReflectiveActor {

    public void receive(SensorEvent e) {
        System.out.println("Sensor: " + e);
    }
}
