package com.hazelcast.actors.service;

import com.hazelcast.actors.api.Actor;
import com.hazelcast.actors.api.ActorRecipe;
import com.hazelcast.actors.api.ActorRef;
import com.hazelcast.actors.api.ActorRuntime;
import com.hazelcast.actors.api.Actors;
import com.hazelcast.actors.utils.Util;
import com.hazelcast.config.ServiceConfig;
import com.hazelcast.core.IMap;
import com.hazelcast.logging.ILogger;
import com.hazelcast.partition.PartitionInfo;
import com.hazelcast.spi.Invocation;
import com.hazelcast.spi.ManagedService;
import com.hazelcast.spi.MigrationAwareService;
import com.hazelcast.spi.MigrationServiceEvent;
import com.hazelcast.spi.NodeService;
import com.hazelcast.spi.Operation;
import com.hazelcast.spi.RemoteService;
import com.hazelcast.spi.ServiceProxy;
import com.hazelcast.spi.impl.NodeServiceImpl;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static com.hazelcast.actors.utils.Util.notNull;

public class ActorService implements ManagedService, MigrationAwareService, RemoteService {

    public static final String NAME = "ActorService";

    private NodeService nodeService;
    private ExecutorService executor = Executors.newFixedThreadPool(10);
    private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(10);
    private ILogger logger;
    private ActorPartitionContainer[] partitionContainers;
    private ConcurrentMap<String, ActorRuntimeProxyImpl> actorSystems = new ConcurrentHashMap<String, ActorRuntimeProxyImpl>();
    private ActorServiceConfig actorConfig;

    @Override
    public void destroy() {
    }

    @Override
    public void init(NodeService nodeService, Properties properties) {
        this.nodeService = nodeService;
        this.logger = nodeService.getLogger(ActorService.class.getName());
        this.actorConfig = findActorServiceConfig();
        int partitionCount = nodeService.getPartitionCount();
        this.partitionContainers = new ActorPartitionContainer[partitionCount];
        for (int partitionId = 0; partitionId < partitionCount; partitionId++) {
            PartitionInfo partition = nodeService.getPartitionInfo(partitionId);
            this.partitionContainers[partitionId] = new ActorPartitionContainer(partition);
        }
    }

    private ActorServiceConfig findActorServiceConfig() {
        for (ServiceConfig config : nodeService.getConfig().getServicesConfig().getServiceConfigs()) {
            if (config.getName().equals(NAME)) {
                return (ActorServiceConfig) config;
            }
        }
        return null;
    }

    @Override
    public void beforeMigration(MigrationServiceEvent e) {
    }

    @Override
    public void commitMigration(MigrationServiceEvent e) {
    }

    @Override
    public void rollbackMigration(MigrationServiceEvent e) {
    }

    @Override
    public Operation prepareMigrationOperation(MigrationServiceEvent e) {
        if (e.getReplicaIndex() != 0) return null;

        //System.out.println("prepare:" + e);

        ActorPartitionContainer partitionContainer = partitionContainers[e.getPartitionId()];
        return partitionContainer.createMigrationOperation();
    }

    @Override
    public ServiceProxy createProxy(Object... params) {
        String id = (String) params[0];
        ActorRuntimeProxyImpl actorSystem = actorSystems.get(id);
        if (actorSystem == null) {
            actorSystem = new ActorRuntimeProxyImpl(id);
            ActorRuntimeProxyImpl found = actorSystems.put(id, actorSystem);
            actorSystem = found != null ? found : actorSystem;
        }
        return actorSystem;
    }

    @Override
    public Collection<ServiceProxy> getProxies() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    private class ActorPartitionContainer {

        private final PartitionInfo partition;
        private final ConcurrentMap<String, ActorPartition> partitionMap = new ConcurrentHashMap<String, ActorPartition>();

        public ActorPartitionContainer(PartitionInfo partition) {
            this.partition = partition;
        }

        public ActorPartition getPartition(String name) {
            ActorPartition actorPartition = partitionMap.get(name);
            if (actorPartition == null) {
                actorPartition = new ActorPartition(name, partition);
                ActorPartition found = partitionMap.putIfAbsent(name, actorPartition);
                actorPartition = found != null ? found : actorPartition;
            }
            return actorPartition;
        }

        public Operation createMigrationOperation() {
            ActorPartitionContent[] partitionChanges = new ActorPartitionContent[partitionMap.size()];
            int k = 0;
            for (Map.Entry<String, ActorPartition> entry : partitionMap.entrySet()) {
                partitionChanges[k] = entry.getValue().content();
                k++;
            }

            return new MigrationOperation(partition.getPartitionId(), partitionChanges);
        }

        public void applyChanges(ActorPartitionContent[] changes) {
            //To change body of created methods use File | Settings | File Templates.
        }
    }

    private static class ActorPartitionContent implements Serializable {

    }

    private class ActorPartition {
        private final String name;
        private final PartitionInfo partition;
        private final ConcurrentMap<String, Future<ActorContainer>> actors = new ConcurrentHashMap<String, Future<ActorContainer>>();
        private final ActorRuntime actorRuntime;

        private ActorPartition(String name, PartitionInfo partition) {
            this.partition = partition;
            this.name = name;
            this.actorRuntime = (ActorRuntime) ActorService.this.createProxy(name);
        }

        public void post(ActorRef sender, String id, final Object message) throws InterruptedException {
            final Future<ActorContainer> future = actors.get(id);
            if (future == null) {
                throw new IllegalArgumentException("Actor " + id + " is not found");
            }

            try {
                final ActorContainer actorContainer = future.get();
                actorContainer.post(sender, message);
                executor.execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            actorContainer.processMessage();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                });
            } catch (ExecutionException e) {
                throw new RuntimeException(e);
            }
        }

        public ActorRef createActor(final ActorRecipe recipe) {
            final ActorRef ref = new ActorRef(UUID.randomUUID().toString(), recipe.getPartitionId());

            Future<ActorContainer> future = executor.submit(
                    new Callable<ActorContainer>() {
                        @Override
                        public ActorContainer call() {
                            try {
                                ActorContainer actorContainer = new ActorContainer(recipe, ref);
                                actorContainer.init(actorRuntime, (NodeServiceImpl) nodeService, actorConfig.getDependencies());
                                return actorContainer;
                            } catch (RuntimeException e) {
                                e.printStackTrace();
                                return null;
                            }
                        }
                    }
            );

            actors.put(ref.getId(), future);
            return ref;
        }

        public void terminate(final ActorRef target) throws InterruptedException {
            final Future<ActorContainer> future = actors.get(target.getId());
            if (future == null) {
                return;
            }

            try {
                final ActorContainer actorContainer = future.get();
                actorContainer.post(null, new Actors.TerminateActor());
                executor.execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            actorContainer.processMessage();
                            actors.remove(target.getId());
                            actorContainer.terminate();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
            } catch (ExecutionException e) {
                throw new RuntimeException(e);
            }
        }

        public ActorPartitionContent content() {
            return null;
        }
    }

    public class ActorRuntimeProxyImpl implements ActorRuntimeProxy {
        private final Random random = new Random();
        private final String name;

        private ActorRuntimeProxyImpl(String name) {
            this.name = name;
        }


        public Actor getActor(ActorRef actorRef) {
            ActorPartitionContainer container = partitionContainers[actorRef.getPartitionId()];
            for (int k = 0; k < 60; k++) {
                Future<ActorContainer> future = container.getPartition(name).actors.get(actorRef.getId());
                if (future == null) {
                    Util.sleep(1000);
                    break;
                }
                try {
                    return future.get().getActor();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                } catch (ExecutionException e) {
                    throw new RuntimeException(e);
                }
            }

            throw new RuntimeException("Actor with ref: "+actorRef+" was not found");
        }

        @Override
        public void monitor(ActorRef listener, ActorRef target) {
            notNull(listener, "listener");
            notNull(target, "target");
            IMap<ActorRef, Set<ActorRef>> monitorMap = ((NodeServiceImpl) nodeService).getNode().hazelcastInstance.getMap("monitorMap");
            monitorMap.lock(target);
            try {
                Set<ActorRef> listeners = monitorMap.get(target);
                if (listeners == null) {
                    listeners = new HashSet<ActorRef>();
                }
                listeners.add(listener);
                monitorMap.put(target, listeners);
            } finally {
                monitorMap.unlock(target);
            }
        }

        @Override
        public void repeat(final ActorRef ref, final Object msg, int delaysMs) {
            Runnable command = new Runnable() {
                @Override
                public void run() {
                    send(ref, msg);
                }
            };
            scheduler.scheduleWithFixedDelay(command, 0, delaysMs, TimeUnit.MILLISECONDS);
        }

        @Override
        public ActorRef newActor(Class<? extends Actor> actorClass, int partitionId) {
            notNull(actorClass, "actorClass");
            ActorRecipe recipe = new ActorRecipe(actorClass, partitionId);
            return newActor(recipe);
        }

        @Override
        public ActorRef newActor(Class<? extends Actor> actorClass) {
            notNull(actorClass, "actorClass");
            int partitionId = random.nextInt(nodeService.getPartitionCount());
            ActorRecipe recipe = new ActorRecipe(actorClass, partitionId);
            return newActor(recipe);
        }

        @Override
        public ActorRef newActor(Class<? extends Actor> actorClass, Map<String, Object> properties) {
            notNull(actorClass, "actorClass");
            int partitionId = random.nextInt(nodeService.getPartitionCount());
            ActorRecipe recipe = new ActorRecipe(actorClass, partitionId, properties);
            return newActor(recipe);
        }

        @Override
        public ActorRef newActor(ActorRecipe recipe) {
            notNull(recipe, "recipe");

            Operation createOperation = new CreateOperation(name, recipe);
            createOperation.setValidateTarget(true);
            createOperation.setServiceName(NAME);
            try {
                Invocation invocation = nodeService.createInvocationBuilder(NAME, createOperation, recipe.getPartitionId()).build();
                Future f = invocation.invoke();
                return (ActorRef) nodeService.toObject(f.get());
            } catch (RuntimeException e) {
                throw e;
            } catch (Throwable throwable) {
                throw new RuntimeException(throwable);
            }
        }

        @Override
        public void send(ActorRef sender, ActorRef destination, Object msg) {
            notNull(destination, "destination");
            notNull(msg, "msg");

            Operation sendOperation = new SendOperation(sender, name, destination.getId(), msg);
            sendOperation.setValidateTarget(true);
            sendOperation.setServiceName(NAME);
            try {
                Invocation invocation = nodeService.createInvocationBuilder(NAME, sendOperation, destination.getPartitionId()).build();
                Future f = invocation.invoke();
                f.get();
            } catch (RuntimeException e) {
                throw e;
            } catch (Throwable throwable) {
                throw new RuntimeException(throwable);
            }
        }

        @Override
        public void send(ActorRef destination, Object msg) {
            send(null, destination, msg);
        }

        @Override
        public void terminate(ActorRef target) {
            notNull(target, "target");

            Operation terminateOperation = new TerminateOperation(name, target);
            terminateOperation.setValidateTarget(true);
            terminateOperation.setServiceName(NAME);
            try {
                Invocation invocation = nodeService.createInvocationBuilder(NAME, terminateOperation, target.getPartitionId()).build();
                Future f = invocation.invoke();
                f.get();
            } catch (RuntimeException e) {
                throw e;
            } catch (Throwable throwable) {
                throw new RuntimeException(throwable);
            }
        }

        @Override
        public InstanceType getInstanceType() {
            return null;
        }

        @Override
        public void destroy() {
        }

        @Override
        public Object getId() {
            return name;
        }
    }

    private static class CreateOperation extends Operation {
        private String name;
        private ActorRecipe actorRecipe;

        public CreateOperation() {
        }

        private CreateOperation(String name, ActorRecipe actorRecipe) {
            this.name = name;
            this.actorRecipe = actorRecipe;
        }

        public void writeInternal(DataOutput out) throws IOException {
            out.writeUTF(name);
            byte[] recipeBytes = Util.toBytes(actorRecipe);
            out.writeInt(recipeBytes.length);
            out.write(recipeBytes);
        }

        public void readInternal(DataInput in) throws IOException {
            name = in.readUTF();
            int recipeBytesLength = in.readInt();
            byte[] recipeBytes = new byte[recipeBytesLength];
            in.readFully(recipeBytes);
            this.actorRecipe = (ActorRecipe) Util.toObject(recipeBytes);
        }

        @Override
        public void run() {
            ActorService actorService = (ActorService) getService();
            ActorPartitionContainer actorPartitionContainer = actorService.partitionContainers[getPartitionId()];
            ActorPartition actorPartition = actorPartitionContainer.getPartition(name);
            ActorRef ref = actorPartition.createActor(actorRecipe);
            getResponseHandler().sendResponse(actorService.nodeService.toData(ref));
        }
    }

    private static class TerminateOperation extends Operation {
        private String name;
        private ActorRef target;

        public TerminateOperation() {
        }

        private TerminateOperation(String name, ActorRef target) {
            this.name = name;
            this.target = target;
        }

        public void writeInternal(DataOutput out) throws IOException {
            out.writeUTF(name);
            byte[] targetBytes = Util.toBytes(target);
            out.writeInt(targetBytes.length);
            out.write(targetBytes);
        }

        public void readInternal(DataInput in) throws IOException {
            name = in.readUTF();
            int targetBytesLength = in.readInt();
            byte[] targetBytes = new byte[targetBytesLength];
            in.readFully(targetBytes);
            this.target = (ActorRef) Util.toObject(targetBytes);
        }

        @Override
        public void run() {
            ActorService actorService = (ActorService) getService();
            ActorPartitionContainer actorPartitionContainer = actorService.partitionContainers[getPartitionId()];
            ActorPartition actorPartition = actorPartitionContainer.getPartition(name);
            try {
                actorPartition.terminate(target);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            getResponseHandler().sendResponse(null);
        }
    }

    private static class SendOperation extends Operation {
        private String name;
        private String destinationId;
        private Object message;
        private ActorRef sender;

        public SendOperation() {
        }

        private SendOperation(ActorRef sender, String name, String destinationId, Object message) {
            this.destinationId = destinationId;
            this.message = message;
            this.name = name;
            this.sender = sender;
        }

        public void writeInternal(DataOutput out) throws IOException {
            out.writeUTF(name);
            out.writeUTF(destinationId);
            byte[] messageBytes = Util.toBytes(message);
            out.writeInt(messageBytes.length);
            out.write(messageBytes);

            if (sender == null) {
                out.writeInt(0);
            } else {
                byte[] senderBytes = Util.toBytes(sender);
                out.writeInt(senderBytes.length);
                out.write(senderBytes);
            }
        }

        public void readInternal(DataInput in) throws IOException {
            name = in.readUTF();
            destinationId = in.readUTF();

            int messageBytesLength = in.readInt();
            byte[] recipeBytes = new byte[messageBytesLength];
            in.readFully(recipeBytes);
            this.message = Util.toObject(recipeBytes);

            int senderBytesLength = in.readInt();
            if (senderBytesLength > 0) {
                byte[] senderBytes = new byte[senderBytesLength];
                in.readFully(senderBytes);
                this.sender = (ActorRef) Util.toObject(senderBytes);
            }
        }

        @Override
        public void run() {
            ActorService actorService = (ActorService) getService();
            ActorPartitionContainer moopPartitionContainer = actorService.partitionContainers[getPartitionId()];
            ActorPartition actorPartition = moopPartitionContainer.getPartition(name);
            try {
                actorPartition.post(sender, destinationId, message);
                getResponseHandler().sendResponse(null);
            } catch (Exception e) {
                getResponseHandler().sendResponse(e);
            }
        }
    }

    static class MigrationOperation extends Operation {

        private ActorPartitionContent[] changes;

        public MigrationOperation() {
        }

        public MigrationOperation(int partitionId, ActorPartitionContent[] changes) {
            setPartitionId(partitionId);
            this.changes = changes;
        }

        public String getServiceName() {
            return NAME;
        }

        @Override
        protected void readInternal(DataInput in) throws IOException {
            int length = in.readInt();
            byte[] bytes = new byte[length];
            in.readFully(bytes);
            changes = (ActorPartitionContent[]) Util.toObject(bytes);
        }

        @Override
        protected void writeInternal(DataOutput out) throws IOException {
            byte[] bytes = Util.toBytes(changes);
            out.writeInt(bytes.length);
            out.write(bytes);
        }

        @Override
        public void run() {
            ActorService moopService = (ActorService) getService();
            ActorPartitionContainer container = moopService.partitionContainers[getPartitionId()];
            container.applyChanges(changes);
        }
    }
}
