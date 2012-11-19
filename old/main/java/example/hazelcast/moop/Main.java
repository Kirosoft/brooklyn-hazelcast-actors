package example.hazelcast.moop;

import com.hazelcast.config.Config;
import com.hazelcast.config.CustomServiceConfig;
import com.hazelcast.config.Services;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;

public class Main {

    public static void main(String[] args) throws Exception {
        Config config = new Config();
        config.getNetworkConfig().getJoin().getMulticastConfig().setEnabled(true);
        Services services = config.getServicesConfig();

        CustomServiceConfig moopServiceConfig = new CustomServiceConfig();
        moopServiceConfig.setName(MoopService.NAME);
        moopServiceConfig.setEnabled(true);
        moopServiceConfig.setClassName(MoopService.class.getName());
        services.addServiceConfig(moopServiceConfig);

        HazelcastInstance hzInstance = Hazelcast.newHazelcastInstance(config);

        Moop moop = (Moop) hzInstance.getServiceProxy(MoopService.NAME, "foo");

        HazelcastInstance slaveHzInstance = Hazelcast.newHazelcastInstance(config);

        Thread.sleep(10000);


        int count = 1000;
        for (int k = 0; k < count; k++) {
            moop.put("key" + k, "value" + k);
        }


        for (int k = 0; k < count; k++) {
            System.out.println(moop.get("key" + k));
        }

        System.out.println("size:" + moop.size());
        System.out.println("localSize:" + moop.localSize());
    }
}
