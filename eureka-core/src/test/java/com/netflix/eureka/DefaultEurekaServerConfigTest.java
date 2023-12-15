package com.netflix.eureka;

import java.util.Map;
import java.util.Set;

import com.netflix.config.ConfigurationManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * @author Nitesh Kant
 */
class DefaultEurekaServerConfigTest {

    @Test
    void remoteRegionUrlsWithName2Regions() throws Exception {
        String region1 = "myregion1";
        String region1url = "http://local:888/eee";
        String region2 = "myregion2";
        String region2url = "http://local:888/eee";
        ConfigurationManager.getConfigInstance().setProperty("eureka.remoteRegionUrlsWithName", region1
                + ';' + region1url
                + ',' + region2
                + ';' + region2url);
        DefaultEurekaServerConfig config = new DefaultEurekaServerConfig();
        Map<String, String> remoteRegionUrlsWithName = config.getRemoteRegionUrlsWithName();

        assertEquals(2, remoteRegionUrlsWithName.size(), "Unexpected remote region url count.");
        assertTrue(remoteRegionUrlsWithName.containsKey(region1), "Remote region 1 not found.");
        assertTrue(remoteRegionUrlsWithName.containsKey(region2), "Remote region 2 not found.");
        assertEquals(region1url, remoteRegionUrlsWithName.get(region1), "Unexpected remote region 1 url.");
        assertEquals(region2url, remoteRegionUrlsWithName.get(region2), "Unexpected remote region 2 url.");

    }

    @Test
    void remoteRegionUrlsWithName1Region() throws Exception {
        String region1 = "myregion1";
        String region1url = "http://local:888/eee";
        ConfigurationManager.getConfigInstance().setProperty("eureka.remoteRegionUrlsWithName", region1
                + ';' + region1url);
        DefaultEurekaServerConfig config = new DefaultEurekaServerConfig();
        Map<String, String> remoteRegionUrlsWithName = config.getRemoteRegionUrlsWithName();

        assertEquals(1, remoteRegionUrlsWithName.size(), "Unexpected remote region url count.");
        assertTrue(remoteRegionUrlsWithName.containsKey(region1), "Remote region 1 not found.");
        assertEquals(region1url, remoteRegionUrlsWithName.get(region1), "Unexpected remote region 1 url.");

    }

    @Test
    void getGlobalAppWhiteList() throws Exception {
        String whitelistApp = "myapp";
        ConfigurationManager.getConfigInstance().setProperty("eureka.remoteRegion.global.appWhiteList", whitelistApp);
        DefaultEurekaServerConfig config = new DefaultEurekaServerConfig();
        Set<String> globalList = config.getRemoteRegionAppWhitelist(null);
        assertNotNull(globalList, "Global whitelist is null.");
        assertEquals(1, globalList.size(), "Global whitelist not as expected.");
        assertEquals(whitelistApp, globalList.iterator().next(), "Global whitelist not as expected.");
    }

    @Test
    void getRegionAppWhiteList() throws Exception {
        String globalWhiteListApp = "myapp";
        String regionWhiteListApp = "myapp";
        ConfigurationManager.getConfigInstance().setProperty("eureka.remoteRegion.global.appWhiteList", globalWhiteListApp);
        ConfigurationManager.getConfigInstance().setProperty("eureka.remoteRegion.region1.appWhiteList", regionWhiteListApp);
        DefaultEurekaServerConfig config = new DefaultEurekaServerConfig();
        Set<String> regionList = config.getRemoteRegionAppWhitelist(null);
        assertNotNull(regionList, "Region whitelist is null.");
        assertEquals(1, regionList.size(), "Region whitelist not as expected.");
        assertEquals(regionWhiteListApp, regionList.iterator().next(), "Region whitelist not as expected.");
    }
}
