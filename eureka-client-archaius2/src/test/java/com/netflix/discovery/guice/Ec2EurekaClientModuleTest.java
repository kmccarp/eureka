package com.netflix.discovery.guice;

import com.netflix.appinfo.AmazonInfo;
import com.netflix.appinfo.ApplicationInfoManager;
import com.netflix.appinfo.DataCenterInfo;
import com.netflix.appinfo.Ec2EurekaArchaius2InstanceConfig;
import com.netflix.appinfo.EurekaInstanceConfig;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.appinfo.providers.Archaius2VipAddressResolver;
import com.netflix.appinfo.providers.VipAddressResolver;
import com.netflix.archaius.config.MapConfig;
import com.netflix.archaius.guice.ArchaiusModule;
import com.netflix.discovery.DiscoveryClient;
import com.netflix.discovery.DiscoveryManager;
import com.netflix.discovery.EurekaClient;
import com.netflix.discovery.EurekaClientConfig;
import com.netflix.governator.InjectorBuilder;
import com.netflix.governator.LifecycleInjector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author David Liu
 */
class Ec2EurekaClientModuleTest {

    private LifecycleInjector injector;

    @BeforeEach
    void setUp() throws Exception {
        injector = InjectorBuilder
                .fromModules(
                        new ArchaiusModule() {
                            @Override
                            protected void configureArchaius() {
                                bindApplicationConfigurationOverride().toInstance(
                                        MapConfig.builder()
                                                .put("eureka.region", "default")
                                                .put("eureka.shouldFetchRegistry", "false")
                                                .put("eureka.registration.enabled", "false")
                                                .put("eureka.serviceUrl.default", "http://localhost:8080/eureka/v2")
                                                .put("eureka.vipAddress", "some-thing")
                                                .put("eureka.validateInstanceId", "false")
                                                .put("eureka.mt.num_retries", 0)
                                                .put("eureka.mt.connect_timeout", 1000)
                                                .put("eureka.shouldInitAsEc2", true)
                                                 // this override is required to force EC2 env as out tests may not
                                                 // be executed in EC2
                                                .put("eureka.instanceDeploymentEnvironment", "ec2")
                                                .build()
                                );
                            }
                        },
                        new EurekaClientModule()
                )
                .createInjector();
    }

    @AfterEach
    void tearDown() {
        if (injector != null) {
            injector.shutdown();
        }
    }

    @SuppressWarnings("deprecation")
    @Test
    void dI() {
        InstanceInfo instanceInfo = injector.getInstance(InstanceInfo.class);
        assertEquals(ApplicationInfoManager.getInstance().getInfo(), instanceInfo);

        VipAddressResolver vipAddressResolver = injector.getInstance(VipAddressResolver.class);
        assertTrue(vipAddressResolver instanceof Archaius2VipAddressResolver);

        EurekaClient eurekaClient = injector.getInstance(EurekaClient.class);
        DiscoveryClient discoveryClient = injector.getInstance(DiscoveryClient.class);

        assertEquals(DiscoveryManager.getInstance().getEurekaClient(), eurekaClient);
        assertEquals(DiscoveryManager.getInstance().getDiscoveryClient(), discoveryClient);
        assertEquals(eurekaClient, discoveryClient);

        EurekaClientConfig eurekaClientConfig = injector.getInstance(EurekaClientConfig.class);
        assertEquals(DiscoveryManager.getInstance().getEurekaClientConfig(), eurekaClientConfig);

        EurekaInstanceConfig eurekaInstanceConfig = injector.getInstance(EurekaInstanceConfig.class);
        assertEquals(DiscoveryManager.getInstance().getEurekaInstanceConfig(), eurekaInstanceConfig);
        assertTrue(eurekaInstanceConfig instanceof Ec2EurekaArchaius2InstanceConfig);

        ApplicationInfoManager applicationInfoManager = injector.getInstance(ApplicationInfoManager.class);
        InstanceInfo myInfo = applicationInfoManager.getInfo();
        assertTrue(myInfo.getDataCenterInfo() instanceof AmazonInfo);
        assertEquals(DataCenterInfo.Name.Amazon, myInfo.getDataCenterInfo().getName());
    }
}
