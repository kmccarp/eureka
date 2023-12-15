package com.netflix.eureka;

import com.netflix.eureka.mock.MockRemoteEurekaServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;

/**
 * @author Nitesh Kant
 */
class RemoteRegionSoftDependencyTest extends AbstractTester {

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        doReturn(10).when(serverConfig).getWaitTimeInMsWhenSyncEmpty();
        doReturn(1).when(serverConfig).getRegistrySyncRetries();
        doReturn(1l).when(serverConfig).getRegistrySyncRetryWaitMs();
        registry.syncUp();
    }

    @Test
    void softDepRemoteDown() throws Exception {
        assertTrue(registry.shouldAllowAccess(false), "Registry access disallowed when remote region is down.");
        assertFalse(registry.shouldAllowAccess(true), "Registry access allowed when remote region is down.");
    }

    @Override
    protected MockRemoteEurekaServer newMockRemoteServer() {
        MockRemoteEurekaServer server = super.newMockRemoteServer();
        server.simulateNotReady(true);
        return server;
    }
}
