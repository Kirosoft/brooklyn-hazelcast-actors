package example.hazelcast.moop;

import com.hazelcast.logging.ILogger;
import com.hazelcast.nio.Data;
import com.hazelcast.partition.PartitionInfo;
import com.hazelcast.spi.Invocation;
import com.hazelcast.spi.ManagedService;
import com.hazelcast.spi.MigrationAwareService;
import com.hazelcast.spi.MigrationServiceEvent;
import com.hazelcast.spi.NodeService;
import com.hazelcast.spi.Operation;
import com.hazelcast.spi.RemoteService;
import com.hazelcast.spi.ServiceProxy;
import com.hazelcast.util.Util;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.Serializable;
import java.util.Collection;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Future;

public class MoopService implements ManagedService, MigrationAwareService, RemoteService {

    public static final String NAME = "MoopService";

    private final ConcurrentMap<String, MoopServiceProxy> proxies = new ConcurrentHashMap();
    private NodeService nodeService;
    private ILogger logger;
    private MoopPartitionContainer[] partitionContainers;

    public MoopService() {
        System.out.println("MoopService created");
    }

    @Override
    public void destroy() {
        //    System.out.println("Destroy");
    }

    @Override
    public void init(NodeService nodeService, Properties properties) {
        this.nodeService = nodeService;
        this.logger = nodeService.getLogger(MoopService.class.getName());

        int partitionCount = nodeService.getPartitionCount();
        this.partitionContainers = new MoopPartitionContainer[partitionCount];
        for (int partitionId = 0; partitionId < partitionCount; partitionId++) {
            PartitionInfo partition = nodeService.getPartitionInfo(partitionId);
            this.partitionContainers[partitionId] = new MoopPartitionContainer(partition);
        }
    }

    @Override
    public void beforeMigration(MigrationServiceEvent e) {
        if (e.getReplicaIndex() != 0) return;

        //    System.out.println("before: " + e);
    }

    @Override
    public Operation prepareMigrationOperation(MigrationServiceEvent e) {
        if (e.getReplicaIndex() != 0) return null;

        //System.out.println("prepare:" + e);

        MoopPartitionContainer partitionContainer = partitionContainers[e.getPartitionId()];
        return partitionContainer.createMigrationOperation();
    }

    private static class MoopPartitionContent implements Serializable {
        private final Map<String, String> records;
        private final String name;

        public MoopPartitionContent(String name, ConcurrentMap<String, String> records) {
            this.records = records;
            this.name = name;
        }
    }

    @Override
    public void commitMigration(MigrationServiceEvent e) {
        if (e.getReplicaIndex() != 0) return;

        boolean isDestination = e.getMigrationEndpoint() == MigrationServiceEvent.MigrationEndpoint.DESTINATION;
        boolean isMove = e.getMigrationType() == MigrationServiceEvent.MigrationType.MOVE;

        if (isDestination) {
            //    System.out.println("adding partition: " + e.getPartitionId());
        } else {
            //    System.out.println("removing partition: " + e.getPartitionId());
        }
    }

    @Override
    public void rollbackMigration(MigrationServiceEvent e) {
        //System.out.println("rollbackMigration");
    }

    @Override
    public MoopProxy createProxy(Object... params) {
        //TODO: A signal should be send to all MoopService instances that a moop should be created.
        String id = (String) params[0];
        MoopServiceProxy proxy = proxies.get(id);
        if (proxy == null) {
            proxy = new MoopServiceProxy(id);
            MoopServiceProxy foundProxy = proxies.putIfAbsent(id, proxy);
            proxy = foundProxy == null ? proxy : foundProxy;

            try {
                nodeService.invokeOnAllPartitions(NAME, new CreateOperation(id));
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        return proxy;
    }

    @Override
    public Collection<ServiceProxy> getProxies() {
        System.out.println("getProxies");
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    class MoopServiceProxy implements MoopProxy {

        private final String name;

        MoopServiceProxy(String name) {
            this.name = name;
        }

        @Override
        public void destroy() {
            try {
                nodeService.invokeOnAllPartitions(NAME, new DestroyOperation(name));
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public InstanceType getInstanceType() {
            return null;
        }

        @Override
        public Object getId() {
            return name;
        }

        @Override
        public int localSize() {
            int size = 0;
            for (MoopPartitionContainer c : MoopService.this.partitionContainers) {
                MoopPartition moopPartition = c.getPartition(name);
                if (moopPartition != null) {
                    size += moopPartition.records.size();
                }
            }
            return size;
        }

        @Override
        public String get(String key) {
            if (key == null) {
                throw new NullPointerException("key can't be null");
            }

            Operation getOperation = new GetOperation(name, key);
            Data keyData = nodeService.toData(key);
            int partitionId = nodeService.getPartitionId(keyData);
            //System.out.println("partition:"+partitionId);
            getOperation.setValidateTarget(true);
            getOperation.setServiceName(NAME);
            try {
                Invocation invocation = nodeService.createInvocationBuilder(NAME, getOperation, partitionId).build();
                Future f = invocation.invoke();
                return (String) MoopService.this.nodeService.toObject(f.get());
            } catch (RuntimeException e) {
                throw e;
            } catch (Throwable throwable) {
                throw new RuntimeException(throwable);
            }
        }

        @Override
        public int size() {
            Operation operation = new SizeOperation(name);

            operation.setValidateTarget(true);
            operation.setServiceName(NAME);
            try {
                Map<Integer, Object> result = nodeService.invokeOnAllPartitions(NAME, operation);
                int size = 0;
                for (Object s : result.values()) {
                    size += (Integer) MoopService.this.nodeService.toObject(s);
                }
                return size;
            } catch (RuntimeException e) {
                throw e;
            } catch (Throwable throwable) {
                throw new RuntimeException(throwable);
            }
        }

        @Override
        public void put(String key, String value) {
            if (key == null) {
                throw new NullPointerException("key can't be null");
            }

            //System.out.println("put(" + key + "," + value + ")");

            Operation getOperation = new PutOperation(name, key, value);
            Data keyData = nodeService.toData(key);
            int partitionId = nodeService.getPartitionId(keyData);

            getOperation.setValidateTarget(true);
            getOperation.setServiceName(NAME);
            try {
                Invocation invocation = nodeService.createInvocationBuilder(NAME, getOperation, partitionId).build();
                Future f = invocation.invoke();
                f.get();
            } catch (Throwable throwable) {
                throw new RuntimeException(throwable);
            }
        }

        @Override
        public String toString() {
            return "Moop(name=" + name + ")";
        }
    }

    private static class MoopPartition {
        private final String name;
        private final ConcurrentMap<String, String> records = new ConcurrentHashMap<String, String>();

        private MoopPartition(String name) {
            this.name = name;
        }

        public MoopPartitionContent content() {
            return new MoopPartitionContent(name, records);
        }
    }

    private static class MoopPartitionContainer {

        private final ConcurrentMap<String, MoopPartition> map = new ConcurrentHashMap<String, MoopPartition>();
        private final PartitionInfo partition;

        public MoopPartitionContainer(PartitionInfo partition) {
            this.partition = partition;
        }

        public void destroyPartition(String name) {
            map.remove(name);
        }

        public MoopPartition getPartition(String name) {
            return map.get(name);
        }

        public MoopMigrationOperation createMigrationOperation() {
            MoopPartitionContent[] partitionChanges = new MoopPartitionContent[map.size()];
            int k = 0;
            for (Map.Entry<String, MoopPartition> entry : map.entrySet()) {
                partitionChanges[k] = entry.getValue().content();
                k++;
            }

            return new MoopMigrationOperation(partition.getPartitionId(), partitionChanges);
        }

        public void applyChanges(MoopPartitionContent[] changes) {
            for (MoopPartitionContent partitionContent : changes) {
                getOrCreatePartition(partitionContent.name).records.putAll(partitionContent.records);
            }
        }

        public MoopPartition getOrCreatePartition(String name) {
            MoopPartition partition = map.get(name);
            if (partition == null) {
                partition = new MoopPartition(name);
                MoopPartition found = map.putIfAbsent(name, partition);
                partition = found != null ? found : partition;
            }

            return partition;
        }
    }

    static class MoopMigrationOperation extends Operation {

        private MoopPartitionContent[] changes;

        public MoopMigrationOperation() {
        }

        public MoopMigrationOperation(int partitionId, MoopPartitionContent[] changes) {
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
            changes = (MoopPartitionContent[]) Util.toObject(bytes);
        }

        @Override
        protected void writeInternal(DataOutput out) throws IOException {
            byte[] bytes = Util.toBytes(changes);
            out.writeInt(bytes.length);
            out.write(bytes);
        }

        @Override
        public void run() {
            MoopService moopService = (MoopService) getService();
            MoopPartitionContainer container = moopService.partitionContainers[getPartitionId()];
            container.applyChanges(changes);
        }
    }

    private static class GetOperation extends Operation {
        private String key;
        private String name;

        public GetOperation() {
        }

        private GetOperation(String name, String key) {
            this.key = key;
            this.name = name;
        }

        public void writeInternal(DataOutput out) throws IOException {
            out.writeUTF(name);
            out.writeUTF(key);
        }

        public void readInternal(DataInput in) throws IOException {
            name = in.readUTF();
            key = in.readUTF();
        }

        @Override
        public void run() {
            MoopService moopService = (MoopService) getService();
            MoopPartitionContainer moopPartitionContainer = moopService.partitionContainers[getPartitionId()];
            MoopPartition moopPartition = moopPartitionContainer.getPartition(name);
            String value = moopPartition.records.get(key);
            getResponseHandler().sendResponse(getNodeService().toData(value));
        }
    }

    private static class SizeOperation extends Operation {
        private String name;

        public SizeOperation() {
        }

        private SizeOperation(String name) {
            this.name = name;
        }

        public void writeInternal(DataOutput out) throws IOException {
            out.writeUTF(name);
        }

        public void readInternal(DataInput in) throws IOException {
            name = in.readUTF();
        }

        @Override
        public void run() {
            MoopService moopService = (MoopService) getService();
            MoopPartitionContainer moopPartitionContainer = moopService.partitionContainers[getPartitionId()];
            MoopPartition moopPartition = moopPartitionContainer.getPartition(name);
            int size = moopPartition.records.size();
            getResponseHandler().sendResponse(getNodeService().toData(size));
        }
    }

    private static class PutOperation extends Operation {
        private String key;
        private String name;
        private String value;

        public PutOperation() {
        }

        private PutOperation(String name, String key, String value) {
            this.key = key;
            this.name = name;
            this.value = value;
        }

        public void writeInternal(DataOutput out) throws IOException {
            out.writeUTF(name);
            out.writeUTF(key);
            out.writeUTF(value);
        }

        public void readInternal(DataInput in) throws IOException {
            name = in.readUTF();
            key = in.readUTF();
            value = in.readUTF();
        }

        @Override
        public void run() {
            MoopService moopService = (MoopService) getService();
            MoopPartitionContainer moopPartitionContainer = moopService.partitionContainers[getPartitionId()];
            MoopPartition moopPartition = moopPartitionContainer.getPartition(name);
            moopPartition.records.put(key, value);
            getResponseHandler().sendResponse(null);
        }
    }

    private static class CreateOperation extends Operation {
        private String name;

        public CreateOperation() {
        }

        private CreateOperation(String name) {
            this.name = name;
        }

        public void writeInternal(DataOutput out) throws IOException {
            out.writeUTF(name);
        }

        public void readInternal(DataInput in) throws IOException {
            name = in.readUTF();
        }

        @Override
        public void run() {
            MoopService moopService = (MoopService) getService();
            MoopPartitionContainer container = moopService.partitionContainers[getPartitionId()];
            container.getOrCreatePartition(name);
            getResponseHandler().sendResponse(null);
        }
    }

    private static class DestroyOperation extends Operation {
        private String name;

        public DestroyOperation() {
        }

        private DestroyOperation(String name) {
            this.name = name;
        }

        public void writeInternal(DataOutput out) throws IOException {
            out.writeUTF(name);
        }

        public void readInternal(DataInput in) throws IOException {
            name = in.readUTF();
        }

        @Override
        public void run() {
            MoopService moopService = (MoopService) getService();
            MoopPartitionContainer container = moopService.partitionContainers[getPartitionId()];
            container.destroyPartition(name);
            getResponseHandler().sendResponse(null);
        }
    }
}
