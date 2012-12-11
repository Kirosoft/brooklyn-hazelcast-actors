package io.brooklyn.entity.machines;


import brooklyn.location.Location;
import com.hazelcast.actors.api.ActorRef;
import io.brooklyn.AbstractMessage;
import io.brooklyn.attributes.ReferenceAttribute;
import io.brooklyn.attributes.ListAttribute;
import io.brooklyn.entity.Entity;
import io.brooklyn.entity.EntityConfig;
import io.brooklyn.entity.softwareprocess.SoftwareProcess;
import io.brooklyn.entity.softwareprocess.SoftwareProcessConfig;

import java.util.Map;

/**
 * Represents a 'machine'. It could be an operating system instance running on bare metal, but it could also be a
 * virtualized instance running in the cloud.
 * <p/>
 * In the existing Brooklyn, this would be the SshMachineLocation.
 * <p/>
 * A Machine is the parent of SoftwareProcesses; so if the machine actor terminates, also the software processes
 * exit. So if you want to start a SoftwareProcess on a machine, send the {@link StartSoftwareProcess} message
 * to the machine. This message contains a {@link SoftwareProcessConfig}.
 *
 * @author Peter Veentjer.
 */
public class Machine extends Entity {

    private final ListAttribute<ActorRef> softwareProcesses = newListAttribute("softwareProcesses", ActorRef.class);
    private final ReferenceAttribute<Location> location = newReferenceAttribute("location", Location.class);

    public void receive(StartMachine start) {
        location.set(start.location);
    }

    public static class StartMachine extends AbstractMessage {
        private final Location location;

        public StartMachine(Location location) {
            this.location = location;
        }
    }

    public void receive(StartSoftwareProcess startSoftwareProcess) {
        ActorRef process = spawnAndLink(startSoftwareProcess.softwareProcessConfig);
        softwareProcesses.add(process);
        //todo: machine in the future will be passed
        send(process, new SoftwareProcess.Start(location.get()));
    }

    public static class StartSoftwareProcess extends AbstractMessage {
        private final SoftwareProcessConfig softwareProcessConfig;

        public StartSoftwareProcess(SoftwareProcessConfig softwareProcessConfig) {
            this.softwareProcessConfig = softwareProcessConfig;
        }
    }

     public static class MachineConfig extends EntityConfig {

        public MachineConfig() {
            super(Machine.class);
        }

        public MachineConfig(Map<String, Object> properties) {
            super(Machine.class, properties);
        }
    }
}

