package com.netflix.discovery;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for DiscoveryClient stats reported when initial registry fetch succeeds.
 */
class DiscoveryClientStatsTest extends AbstractDiscoveryClientTester {

    @Test
    void nonEmptyInitLocalRegistrySize() throws Exception {
        assertTrue(client instanceof DiscoveryClient);
        DiscoveryClient clientImpl = (DiscoveryClient) client;
        assertEquals(createLocalApps().size(), clientImpl.getStats().initLocalRegistrySize());
    }

    @Test
    void initSucceeded() throws Exception {
        assertTrue(client instanceof DiscoveryClient);
        DiscoveryClient clientImpl = (DiscoveryClient) client;
        assertTrue(clientImpl.getStats().initSucceeded());
    }

}
