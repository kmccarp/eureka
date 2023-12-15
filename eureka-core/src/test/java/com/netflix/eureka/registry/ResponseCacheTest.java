package com.netflix.eureka.registry;

import com.netflix.appinfo.EurekaAccept;
import com.netflix.discovery.DefaultEurekaClientConfig;
import com.netflix.eureka.AbstractTester;
import com.netflix.eureka.DefaultEurekaServerConfig;
import com.netflix.eureka.EurekaServerConfig;
import com.netflix.eureka.Version;
import com.netflix.eureka.resources.DefaultServerCodecs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

/**
 * @author Nitesh Kant
 */
class ResponseCacheTest extends AbstractTester {

    private static final String REMOTE_REGION = "myremote";

    private PeerAwareInstanceRegistry testRegistry;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        // create a new registry that is sync'ed up with the default registry in the AbstractTester,
        // but disable transparent fetch to the remote for gets
        EurekaServerConfig serverConfig = spy(new DefaultEurekaServerConfig());
        doReturn(true).when(serverConfig).disableTransparentFallbackToOtherRegion();

        testRegistry = new PeerAwareInstanceRegistryImpl(
                serverConfig,
                new DefaultEurekaClientConfig(),
                new DefaultServerCodecs(serverConfig),
                client
        );
        testRegistry.init(serverContext.getPeerEurekaNodes());
        testRegistry.syncUp();
    }

    @Test
    void invalidate() throws Exception {
        ResponseCacheImpl cache = (ResponseCacheImpl) testRegistry.getResponseCache();
        Key key = new Key(Key.EntityType.Application, REMOTE_REGION_APP_NAME,
                Key.KeyType.JSON, Version.V1, EurekaAccept.full);
        String response = cache.get(key, false);
        assertNotNull(response, "Cache get returned null.");

        testRegistry.cancel(REMOTE_REGION_APP_NAME, REMOTE_REGION_INSTANCE_1_HOSTNAME, true);
        assertNull(cache.get(key, true), "Cache after invalidate did not return null for write view.");
    }

    @Test
    void invalidateWithRemoteRegion() throws Exception {
        ResponseCacheImpl cache = (ResponseCacheImpl) testRegistry.getResponseCache();
        Key key = new Key(
                Key.EntityType.Application,
                REMOTE_REGION_APP_NAME,
                Key.KeyType.JSON, Version.V1, EurekaAccept.full, new String[]{REMOTE_REGION}
        );

        assertNotNull(cache.get(key, false), "Cache get returned null.");

        testRegistry.cancel(REMOTE_REGION_APP_NAME, REMOTE_REGION_INSTANCE_1_HOSTNAME, true);
        assertNull(cache.get(key), "Cache after invalidate did not return null.");
    }

    @Test
    void invalidateWithMultipleRemoteRegions() throws Exception {
        ResponseCacheImpl cache = (ResponseCacheImpl) testRegistry.getResponseCache();
        Key key1 = new Key(
                Key.EntityType.Application,
                REMOTE_REGION_APP_NAME,
                Key.KeyType.JSON, Version.V1, EurekaAccept.full, new String[]{REMOTE_REGION, "myregion2"}
        );
        Key key2 = new Key(
                Key.EntityType.Application,
                REMOTE_REGION_APP_NAME,
                Key.KeyType.JSON, Version.V1, EurekaAccept.full, new String[]{REMOTE_REGION}
        );

        assertNotNull(cache.get(key1, false), "Cache get returned null.");
        assertNotNull(cache.get(key2, false), "Cache get returned null.");

        testRegistry.cancel(REMOTE_REGION_APP_NAME, REMOTE_REGION_INSTANCE_1_HOSTNAME, true);

        assertNull(cache.get(key1, true), "Cache after invalidate did not return null.");
        assertNull(cache.get(key2, true), "Cache after invalidate did not return null.");
    }
}
