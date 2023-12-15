package com.netflix.discovery;

import static com.netflix.discovery.shared.transport.EurekaHttpResponse.anEurekaHttpResponse;
import static com.netflix.discovery.util.EurekaEntityFunctions.toApplications;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import java.io.IOException;

import jakarta.ws.rs.core.MediaType;

import org.junit.Rule;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.junit.resource.DiscoveryClientResource;
import com.netflix.discovery.shared.Applications;
import com.netflix.discovery.shared.transport.EurekaHttpClient;
import com.netflix.discovery.shared.transport.EurekaHttpResponse;
import com.netflix.discovery.shared.transport.SimpleEurekaHttpServer;

public class EurekaEventListenerTest {
    private static final EurekaHttpClient requestHandler = mock(EurekaHttpClient.class);
    private static SimpleEurekaHttpServer eurekaHttpServer;

    @Rule
    public DiscoveryClientResource discoveryClientResource = DiscoveryClientResource.newBuilder()
            .withRegistration(true)
            .withRegistryFetch(true)
            .connectWith(eurekaHttpServer)
            .build();

    /**
     * Share server stub by all tests.
     */
    @BeforeAll
    static void setUpClass() throws IOException {
        eurekaHttpServer = new SimpleEurekaHttpServer(requestHandler);
    }

    @AfterAll
    static void tearDownClass() throws Exception {
        if (eurekaHttpServer != null) {
            eurekaHttpServer.shutdown();
        }
    }

    @BeforeEach
    void setUp() throws Exception {
        reset(requestHandler);
        when(requestHandler.register(any(InstanceInfo.class))).thenReturn(EurekaHttpResponse.status(204));
        when(requestHandler.cancel(anyString(), anyString())).thenReturn(EurekaHttpResponse.status(200));
        when(requestHandler.getDelta()).thenReturn(
                anEurekaHttpResponse(200, new Applications()).type(MediaType.APPLICATION_JSON_TYPE).build()
        );
    }

    static class CapturingEurekaEventListener implements EurekaEventListener {
        private volatile EurekaEvent event;
        
        @Override
        public void onEvent(EurekaEvent event) {
            this.event = event;
        }
    }

    @Test
    void cacheRefreshEvent() throws Exception {
        CapturingEurekaEventListener listener = new CapturingEurekaEventListener();

        Applications initialApps = toApplications(discoveryClientResource.getMyInstanceInfo());
        when(requestHandler.getApplications()).thenReturn(
                anEurekaHttpResponse(200, initialApps).type(MediaType.APPLICATION_JSON_TYPE).build()
        );
        DiscoveryClient client = (DiscoveryClient) discoveryClientResource.getClient();
        client.registerEventListener(listener);
        client.refreshRegistry();

        assertNotNull(listener.event);
        assertThat(listener.event, is(instanceOf(CacheRefreshedEvent.class)));
    }
}
