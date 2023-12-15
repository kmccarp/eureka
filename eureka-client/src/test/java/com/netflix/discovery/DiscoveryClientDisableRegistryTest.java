package com.netflix.discovery;

import java.util.UUID;

import com.netflix.appinfo.ApplicationInfoManager;

import static org.junit.jupiter.api.Assertions.assertFalse;
import com.netflix.appinfo.DataCenterInfo;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.appinfo.MyDataCenterInstanceConfig;
import com.netflix.config.ConfigurationManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Nitesh Kant
 */
class DiscoveryClientDisableRegistryTest {

    private EurekaClient client;
    private MockRemoteEurekaServer mockLocalEurekaServer;

    @BeforeEach
    void setUp() throws Exception {
        mockLocalEurekaServer = new MockRemoteEurekaServer();
        mockLocalEurekaServer.start();

        ConfigurationManager.getConfigInstance().setProperty("eureka.registration.enabled", "false");
        ConfigurationManager.getConfigInstance().setProperty("eureka.shouldFetchRegistry", "false");
        ConfigurationManager.getConfigInstance().setProperty("eureka.serviceUrl.default",
                "http://localhost:" + mockLocalEurekaServer.getPort() +
                        MockRemoteEurekaServer.EUREKA_API_BASE_PATH);

        InstanceInfo.Builder builder = InstanceInfo.Builder.newBuilder();
        builder.setIPAddr("10.10.101.00");
        builder.setHostName("Hosttt");
        builder.setAppName("EurekaTestApp-" + UUID.randomUUID());
        builder.setDataCenterInfo(new DataCenterInfo() {
            @Override
            public Name getName() {
                return Name.MyOwn;
            }
        });

        ApplicationInfoManager applicationInfoManager = new ApplicationInfoManager(new MyDataCenterInstanceConfig(), builder.build());
        client = new DiscoveryClient(applicationInfoManager, new DefaultEurekaClientConfig());
    }

    @Test
    void disableFetchRegistry() throws Exception {
        assertFalse(mockLocalEurekaServer.isSentRegistry(),
                "Registry fetch disabled but eureka server recieved a registry fetch.");
    }
}
