package com.netflix.discovery.guice;

import com.google.inject.AbstractModule;
import com.google.inject.Binding;
import com.google.inject.Key;
import com.google.inject.Scopes;
import com.netflix.appinfo.ApplicationInfoManager;
import com.netflix.appinfo.EurekaInstanceConfig;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.appinfo.providers.MyDataCenterInstanceConfigProvider;
import com.netflix.config.ConfigurationManager;
import com.netflix.discovery.DiscoveryClient;
import com.netflix.discovery.DiscoveryManager;
import com.netflix.discovery.EurekaClient;
import com.netflix.discovery.EurekaClientConfig;
import com.netflix.discovery.shared.transport.jersey.TransportClientFactories;
import com.netflix.discovery.shared.transport.jersey2.Jersey2TransportClientFactories;
import com.netflix.governator.InjectorBuilder;
import com.netflix.governator.LifecycleInjector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author David Liu
 */
class Jersey2EurekaModuleTest {

    private LifecycleInjector injector;

    @BeforeEach
    void setUp() throws Exception {
        ConfigurationManager.getConfigInstance().setProperty("eureka.region", "default");
        ConfigurationManager.getConfigInstance().setProperty("eureka.shouldFetchRegistry", "false");
        ConfigurationManager.getConfigInstance().setProperty("eureka.registration.enabled", "false");
        ConfigurationManager.getConfigInstance().setProperty("eureka.serviceUrl.default", "http://localhost:8080/eureka/v2");

        injector = InjectorBuilder
                .fromModule(new Jersey2EurekaModule())
                .overrideWith(new AbstractModule() {
                    @Override
                    protected void configure() {
                        // the default impl of EurekaInstanceConfig is CloudInstanceConfig, which we only want in an AWS
                        // environment. Here we override that by binding MyDataCenterInstanceConfig to EurekaInstanceConfig.
                        bind(EurekaInstanceConfig.class).toProvider(MyDataCenterInstanceConfigProvider.class).in(Scopes.SINGLETON);
                    }
                })
                .createInjector();
    }

    @AfterEach
    void tearDown() {
        if (injector != null) {
            injector.shutdown();
        }
        ConfigurationManager.getConfigInstance().clear();
    }

    @SuppressWarnings("deprecation")
    @Test
    void dI() {
        InstanceInfo instanceInfo = injector.getInstance(InstanceInfo.class);
        assertEquals(ApplicationInfoManager.getInstance().getInfo(), instanceInfo);

        EurekaClient eurekaClient = injector.getInstance(EurekaClient.class);
        DiscoveryClient discoveryClient = injector.getInstance(DiscoveryClient.class);

        assertEquals(DiscoveryManager.getInstance().getEurekaClient(), eurekaClient);
        assertEquals(DiscoveryManager.getInstance().getDiscoveryClient(), discoveryClient);
        assertEquals(eurekaClient, discoveryClient);

        EurekaClientConfig eurekaClientConfig = injector.getInstance(EurekaClientConfig.class);
        assertEquals(DiscoveryManager.getInstance().getEurekaClientConfig(), eurekaClientConfig);

        EurekaInstanceConfig eurekaInstanceConfig = injector.getInstance(EurekaInstanceConfig.class);
        assertEquals(DiscoveryManager.getInstance().getEurekaInstanceConfig(), eurekaInstanceConfig);

        Binding<TransportClientFactories> binding = injector.getExistingBinding(Key.get(TransportClientFactories.class));
        assertNotNull(binding);  // has a binding for jersey2

        TransportClientFactories transportClientFactories = injector.getInstance(TransportClientFactories.class);
        assertTrue(transportClientFactories instanceof Jersey2TransportClientFactories);
    }
}
