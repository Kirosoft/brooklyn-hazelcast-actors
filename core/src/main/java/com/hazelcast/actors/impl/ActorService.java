package com.hazelcast.actors.impl;

import com.hazelcast.actors.api.*;
import com.hazelcast.actors.impl.actorcontainers.ActorContainer;
import com.hazelcast.actors.impl.actorcontainers.ActorContainerFactory;
import com.hazelcast.actors.impl.actorcontainers.ActorContainerFactoryFactory;
import com.hazelcast.actors.utils.Util;
import com.hazelcast.config.ServiceConfig;
import com.hazelcast.core.IMap;
import com.hazelcast.logging.ILogger;
import com.hazelcast.partition.PartitionInfo;
import com.hazelcast.spi.*;
import com.hazelcast.spi.impl.NodeServiceImpl;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.*;

import static com.hazelcast.actors.utils.Util.notNull;

public class ActorService implements ManagedService, MigrationAwareService, RemoteService {

    public static final String NAME = "ActorService";

    private final ConcurrentMap<String, ActorRuntimeProxyImpl> actorSystems = new ConcurrentHashMap<>();

    //TODO: These need to be pulled out; made configurable. For the time being it is good enough.
    private final ExecutorService offloadExecutor = Executors.newFixedThreadPool(16);
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(16);

    private NodeServiceImpl nodeService;

    private ILogger logger;
    private ActorPartitionContainer[] partitionContainers;
    private ActorServiceConfig actorConfig;
    private IMap<ActorRef, Set<ActorRef>> linksMap;
    private ActorFactory actorFactory;
    private ActorContainerFactoryFactory containerFactoryFactory;

    @Override
    public void init(NodeService nodeService, Properties properties) {
        this.nodeService = (NodeServiceImpl) nodeService;
        this.logger = nodeService.getLogger(ActorService.class.getName());
        this.actorConfig = findActorServiceConfig();
        this.actorFactory = actorConfig.getActorFactory();
        this.containerFactoryFactory = actorConfig.getActorContainerFactoryFactory();
        this.linksMap = ((NodeServiceImpl) nodeService).getNode().hazelcastInstance.getMap("linksMap");
        int partitionCount = nodeService.getPartitionCount();

        this.partitionContainers = new ActorPartitionContainer[partitionCount];

        for (int partitionId = 0; partitionId < partitionCount; partitionId++) {
            PartitionInfo partition = nodeService.getPartitionInfo(partitionId);
            this.partitionContainers[partitionId] = new ActorPartitionContainer(partition);
        }
    }

    @Override
    public void destroy() {
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
    public ServiceProxy getProxy(Object... params) {
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
        private final PartitionInfo partition;
        private final ConcurrentMap<String, ActorContainer> actorContainerMap = new ConcurrentHashMap<>();
        private final ActorRuntime actorRuntime;
        private final ActorContainerFactory containerFactory;

        private ActorPartition(String name, PartitionInfo partition) {
            this.partition = partition;
            this.actorRuntime = (ActorRuntime) ActorService.this.getProxy(name);
            this.containerFactory = containerFactoryFactory.newFactory(actorFactory, actorRuntime, linksMap, nodeService);
        }

        public void post(ActorRef sender, String id, final Object message) throws InterruptedException {
            final ActorContainer actorContainer = actorContainerMap.get(id);

            if (actorContainer == null) {
                throw new IllegalArgumentException("Actor " + id + " is not found");
            }

            actorContainer.post(sender, message);
        }

        public ActorRef createActor(final ActorRecipe recipe, final Object partitionKey, final int partitionId) throws Exception {
            final ActorRef ref = new ActorRef(UUID.randomUUID().toString(), partitionKey, partitionId);

            //we need to offload the actual creation/activation of the actor, since in Hazelcast it isn't allowed to call
            //a Hazelcast structure from the spi.
            Future<ActorContainer> future = offloadExecutor.submit(
                    new Callable<ActorContainer>() {
                        @Override
                        public ActorContainer call() throws Exception {
                            try {
                                ActorContainer container = containerFactory.newContainer(ref, recipe);
                                container.activate();
                                return container;
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

        public void exit(final ActorRef target) throws InterruptedException {
            final ActorContainer actorContainer = actorContainerMap.remove(target.getId());
            if (actorContainer == null) {
                return;
            }

            try {
                actorContainer.exit();
            } catch (Exception e) {
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

        public NodeService getNodeService() {
            return nodeService;
        }

        public Actor getActor(ActorRef actorRef) {
            int partitionId = actorRef.getPartitionId();
            ActorPartitionContainer actorPartitionContainer = partitionContainers[partitionId];
            if (actorPartitionContainer == null) {
                throw new NullPointerException("No actor with actorRef:" + actorRef.getId() + " found");
            }

            ActorContainer actorContainer = actorPartitionContainer.getPartition(name).actorContainerMap.get(actorRef.getId());
            return actorContainer.getActor();
        }

        @Override
        public void link(ActorRef ref1, ActorRef ref2) {
            notNull(ref1, "link");
            notNull(ref2, "subject");

            if(ref1.equals(ref2)){
                throw new IllegalArgumentException("Can't create a self link");
            }

            linkTo(ref1, ref2);
            linkTo(ref2, ref1);
        }

        private void linkTo(ActorRef monitor, ActorRef subject) {
            linksMap.lock(subject);
            try {
                Set<ActorRef> links = linksMap.get(subject);
                Set<ActorRef> newLinks = new HashSet<>();
                if (links != null) {
                    newLinks.addAll(links);
                }
                newLinks.add(monitor);
                linksMap.put(subject, newLinks);
            } finally {
                linksMap.unlock(subject);
            }
        }

        @Override
        public void notify(final ActorRef destination, final Object notification, int delaysMs) {
            Runnable command = new Runnable() {
                @Override
                public void run() {
                    send(destination, notification);
                }
            };
            scheduler.scheduleWithFixedDelay(command, 0, delaysMs, TimeUnit.MILLISECONDS);
        }

        @Override
        public ActorRef spawnAndLink(ActorRef listener, ActorRecipe recipe) {
            notNull(recipe, "recipe");

            Object partitionKey = recipe.getPartitionKey();
            //if there is no PartitionKey assigned to the recipe, it means that the caller doesn't care
            //where the actor is going to run. So lets pick a partition randomly.
            if (partitionKey == null) {
                partitionKey = random.nextInt();
            }

            Operation createOperation = new CreateOperation(name, recipe, partitionKey);
            createOperation.setValidateTarget(true);
            createOperation.setServiceName(NAME);
            try {
                int partitionId = nodeService.getPartitionId(nodeService.toData(partitionKey));
                Invocation invocation = nodeService.createInvocationBuilder(NAME, createOperation, partitionId).build();
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
                    ActorRef child = (ActorRef) result;
                    if (listener != null) {
                        link(child, listener);
                    }
                    return child;
                }
            } catch (RuntimeException e) {
                throw e;
            } catch (Throwable throwable) {
                throw new RuntimeException(throwable);
            }
        }


        @Override
        public ActorRef spawn(ActorRecipe recipe) {
            return spawnAndLink(null, recipe);
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
        public void exit(ActorRef target) {
            notNull(target, "target");

            Operation op = new ExitOperation(name, target);
            op.setValidateTarget(true);
            op.setServiceName(NAME);
            try {
                Invocation invocation = nodeService.createInvocationBuilder(NAME, op, target.getPartitionId()).build();
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
        private Object partitionKey;

        private CreateOperation() {
        }

        private CreateOperation(String name, ActorRecipe actorRecipe, Object partitionKey) {
            this.name = name;
            this.actorRecipe = actorRecipe;
            this.partitionKey = partitionKey;
        }

        public void writeInternal(DataOutput out) throws IOException {
            out.writeUTF(name);

            byte[] recipeBytes = Util.toBytes(actorRecipe);
            out.writeInt(recipeBytes.length);
            out.write(recipeBytes);

            byte[] partitionKeyBytes = Util.toBytes(partitionKey);
            out.writeInt(partitionKeyBytes.length);
            out.write(partitionKeyBytes);
        }

        public void readInternal(DataInput in) throws IOException {
            name = in.readUTF();
            int recipeBytesLength = in.readInt();

            byte[] recipeBytes = new byte[recipeBytesLength];
            in.readFully(recipeBytes);
            this.actorRecipe = (ActorRecipe) Util.toObject(recipeBytes);

            byte[] partitionKeyBytes = new byte[in.readInt()];
            in.readFully(partitionKeyBytes);
            this.partitionKey = Util.toObject(partitionKeyBytes);
        }

        @Override
        public void run() {
            ActorService actorService = (ActorService) getService();
            ActorPartitionContainer actorPartitionContainer = actorService.partitionContainers[getPartitionId()];
            ActorPartition actorPartition = actorPartitionContainer.getPartition(name);
            try {
                ActorRef ref = actorPartition.createActor(actorRecipe, partitionKey, getPartitionId());
                getResponseHandler().sendResponse(actorService.nodeService.toData(ref));
            } catch (Exception e) {
                getResponseHandler().sendResponse(actorService.nodeService.toData(e));
            }
        }
    }

    private static class ExitOperation extends Operation {
        private String name;
        private ActorRef target;

        private ExitOperation() {
        }

        private ExitOperation(String name, ActorRef target) {
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
                actorPartition.exit(target);
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
