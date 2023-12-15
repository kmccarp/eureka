package com.netflix.discovery;

import com.netflix.discovery.junit.resource.DiscoveryClientResource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for DiscoveryClient stats reported when initial registry fetch fails.
 */
class DiscoveryClientStatsInitFailedTest extends BaseDiscoveryClientTester {

    @BeforeEach
    void setUp() throws Exception {
        setupProperties();
        populateRemoteRegistryAtStartup();
        setupDiscoveryClient();
    }

    @AfterEach
    void tearDown() throws Exception {
        shutdownDiscoveryClient();
        DiscoveryClientResource.clearDiscoveryClientConfig();
    }

    @Test
    void emptyInitLocalRegistrySize() throws Exception {
        assertTrue(client instanceof DiscoveryClient);
        DiscoveryClient clientImpl = (DiscoveryClient) client;
        assertEquals(0, clientImpl.getStats().initLocalRegistrySize());
    }

    @Test
    void initFailed() throws Exception {
        assertTrue(client instanceof DiscoveryClient);
        DiscoveryClient clientImpl = (DiscoveryClient) client;
        assertFalse(clientImpl.getStats().initSucceeded());
    }

}
