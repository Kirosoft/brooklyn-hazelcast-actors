package com.hazelcast.actors.impl;

import com.hazelcast.actors.api.Actor;
import com.hazelcast.actors.api.ActorFactory;
import com.hazelcast.actors.api.ActorRecipe;
import com.hazelcast.actors.api.ActorRef;
import com.hazelcast.actors.api.ActorRuntime;
import com.hazelcast.actors.api.Actors;
import com.hazelcast.actors.impl.actorcontainers.ActorContainer;
import com.hazelcast.actors.impl.actorcontainers.ActorContainerFactory;
import com.hazelcast.actors.utils.MutableMap;
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

import static com.hazelcast.actors.utils.Util.notNull;

public class ActorService implements ManagedService, MigrationAwareService, RemoteService {

    public static final String NAME = "ActorService";

    private NodeService nodeService;
    private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(10);
    private ILogger logger;
    private ActorPartitionContainer[] partitionContainers;
    private final ConcurrentMap<String, ActorRuntimeProxyImpl> actorSystems = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newScheduledThreadPool(10);
    private ActorServiceConfig actorConfig;
    private IMap<ActorRef, Set<ActorRef>> monitorMap;
    private ActorFactory actorFactory;
    private ActorContainerFactory actorContainerFactory;

    @Override
    public void destroy() {
    }

    @Override
    public void init(NodeService nodeService, Properties properties) {
        this.nodeService = nodeService;
        this.logger = nodeService.getLogger(ActorService.class.getName());
        this.actorConfig = findActorServiceConfig();
        this.actorFactory = actorConfig.getActorFactory();
        this.actorContainerFactory = actorConfig.getActorContainerFactory();
        this.actorContainerFactory.init(monitorMap);
        this.monitorMap = ((NodeServiceImpl) nodeService).getNode().hazelcastInstance.getMap("monitorMap");
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
        //TODO:
        return null;
    }

    private class ActorPartitionContainer {

        private final PartitionInfo partition;
        private final ConcurrentMap<String, ActorPartition> partitionMap = new ConcurrentHashMap<String, ActorPartition>();

        public ActorPartitionContainer(PartitionInfo partition) {
            this.partition = partition;
        }

        private ActorPartition getPartition(String name) {
            ActorPartition actorPartition = partitionMap.get(name);
            if (actorPartition == null) {
                actorPartition = new ActorPartition(name, partition);
                ActorPartition found = partitionMap.putIfAbsent(name, actorPartition);
                actorPartition = found != null ? found : actorPartition;
            }
            return actorPartition;
        }

        private Operation createMigrationOperation() {
            ActorPartitionContent[] partitionChanges = new ActorPartitionContent[partitionMap.size()];
            int k = 0;
            for (Map.Entry<String, ActorPartition> entry : partitionMap.entrySet()) {
                partitionChanges[k] = entry.getValue().content();
                k++;
            }

            return new MigrationOperation(partition.getPartitionId(), partitionChanges);
        }

        public void applyChanges(ActorPartitionContent[] changes) {
            //todo
        }
    }

    private static class ActorPartitionContent implements Serializable {

    }

    private class ActorPartition {
        private final String name;
        private final PartitionInfo partition;
        private final ConcurrentMap<String, ActorContainer> actorContainerMap = new ConcurrentHashMap<>();
        private final ActorRuntime actorRuntime;

        private ActorPartition(String name, PartitionInfo partition) {
            this.partition = partition;
            this.name = name;
            this.actorRuntime = (ActorRuntime) ActorService.this.createProxy(name);
        }

        public void post(ActorRef sender, String id, final Object message) throws InterruptedException {
            final ActorContainer actorContainer = actorContainerMap.get(id);
            if (actorContainer == null) {
                throw new IllegalArgumentException("Actor " + id + " is not found");
            }

            actorContainer.post(sender, message);
        }

        public ActorRef createActor(final ActorRecipe recipe) throws Exception {
            final ActorRef ref = new ActorRef(UUID.randomUUID().toString(), recipe.getPartitionId());

            Future<ActorContainer> future = executor.submit(
                    new Callable<ActorContainer>() {
                        @Override
                        public ActorContainer call() {
                            try {
                                ActorContainer actorContainer = actorContainerFactory.newContainer(ref, recipe);
                                actorContainer.activate(actorRuntime, (NodeServiceImpl) nodeService, actorFactory);
                                return actorContainer;
                            } catch (Exception e) {
                                e.printStackTrace();
                                throw e;
                            }
                        }
                    }
            );


            try {
                ActorContainer container = future.get();
                actorContainerMap.put(ref.getId(), container);
                return ref;
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } catch (ExecutionException e) {
                Throwable cause = e.getCause();
                if (cause instanceof Exception) {
                    throw (Exception) cause;
                } else {
                    throw new RuntimeException(e);
                }
            }
        }

        public void terminate(final ActorRef target) throws InterruptedException {
            ActorContainer actorContainer = actorContainerMap.get(target.getId());
            if (actorContainer == null) {
                return;
            }

            actorContainer.post(null, new Actors.Terminate());
            throw new RuntimeException();
            /*
            forkJoinPool.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        actorContainer.processMessage(forkJoinPool);
                        actors.remove(target.getId());
                        actorContainer.terminate();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });*/

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

        public NodeService getNodeService() {
            return nodeService;
        }

        public Actor getActor(ActorRef actorRef) {
            ActorPartitionContainer actorPartitionContainer = partitionContainers[actorRef.getPartitionId()];
            if (actorPartitionContainer == null) {
                throw new NullPointerException("No actor with actorRef:" + actorRef.getId() + " found");
            }

            ActorContainer actorContainer = actorPartitionContainer.getPartition(name).actorContainerMap.get(actorRef.getId());
            return actorContainer.getActor();
        }

        @Override
        public void monitor(ActorRef monitor, ActorRef subject) {
            notNull(monitor, "monitor");
            notNull(subject, "subject");

            monitorMap.lock(subject);
            try {
                Set<ActorRef> monitorsForSubject = monitorMap.get(subject);
                if (monitorsForSubject == null) {
                    monitorsForSubject = new HashSet<>();
                }
                monitorsForSubject.add(monitor);
                monitorMap.put(subject, monitorsForSubject);
            } finally {
                monitorMap.unlock(subject);
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
            return newActor(actorClass, partitionId, MutableMap.map());
        }

        @Override
        public ActorRef newActor(Class<? extends Actor> actorClass, int partitionId, Map<String, Object> config) {
            notNull(actorClass, "actorClass");
            if (partitionId == -1) {
                partitionId = random.nextInt(nodeService.getPartitionCount());
            }
            ActorRecipe recipe = new ActorRecipe(actorClass, partitionId, config);
            return newActor(recipe);
        }

        @Override
        public ActorRef newActor(Class<? extends Actor> actorClass) {
            return newActor(actorClass, -1);
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
                Object result = nodeService.toObject(f.get());
                if (result instanceof Throwable) {
                    Throwable throwable = (Throwable) result;
                    StackTraceElement[] clientSideStackTrace = Thread.currentThread().getStackTrace();
                    Util.fixStackTrace(throwable, clientSideStackTrace);
                    if (throwable instanceof RuntimeException) {
                        throw (RuntimeException) throwable;
                    } else {
                        throw new RuntimeException(throwable);
                    }
                } else {
                    return (ActorRef) result;
                }
            } catch (RuntimeException e) {
                throw e;
            } catch (Throwable throwable) {
                throw new RuntimeException(throwable);
            }
        }

        @Override
        public void send(ActorRef sender, Collection<ActorRef> destinations, Object msg) {
            notNull(destinations, "destinations");
            notNull(msg, "msg");

            if (destinations.isEmpty()) {
                return;
            }

            for (ActorRef destination : destinations) {
                send(sender, destination, msg);
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
                Object result = f.get();
                if (result instanceof Throwable) {
                    Throwable throwable = (Throwable) result;
                    StackTraceElement[] clientSideStackTrace = Thread.currentThread().getStackTrace();
                    Util.fixStackTrace(throwable, clientSideStackTrace);
                    if (throwable instanceof RuntimeException) {
                        throw (RuntimeException) throwable;
                    } else {
                        throw new RuntimeException(throwable);
                    }
                }
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
            //todo:
        }

        @Override
        public Object getId() {
            return name;
        }
    }

    private static class CreateOperation extends Operation {
        private String name;
        private ActorRecipe actorRecipe;

        private CreateOperation() {
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
            try {
                ActorRef ref = actorPartition.createActor(actorRecipe);
                getResponseHandler().sendResponse(actorService.nodeService.toData(ref));
            } catch (Exception e) {
                getResponseHandler().sendResponse(actorService.nodeService.toData(e));
            }
        }
    }

    private static class TerminateOperation extends Operation {
        private String name;
        private ActorRef target;

        private TerminateOperation() {
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

        private SendOperation() {
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
