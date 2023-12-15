package com.netflix.discovery;

import com.netflix.appinfo.AmazonInfo;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.config.ConfigurationManager;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author Nitesh Kant
 */
class InstanceRegionCheckerTest {

    @Test
    void defaults() throws Exception {
        PropertyBasedAzToRegionMapper azToRegionMapper = new PropertyBasedAzToRegionMapper(
                new DefaultEurekaClientConfig());
        InstanceRegionChecker checker = new InstanceRegionChecker(azToRegionMapper, "us-east-1");
        azToRegionMapper.setRegionsToFetch(new String[]{"us-east-1"});
        AmazonInfo dcInfo = AmazonInfo.Builder.newBuilder().addMetadata(AmazonInfo.MetaDataKey.availabilityZone,
                "us-east-1c").build();
        InstanceInfo instanceInfo = InstanceInfo.Builder.newBuilder().setAppName("app").setDataCenterInfo(dcInfo).build();
        String instanceRegion = checker.getInstanceRegion(instanceInfo);

        assertEquals("us-east-1", instanceRegion, "Invalid instance region.");
    }

    @Test
    void defaultOverride() throws Exception {
        ConfigurationManager.getConfigInstance().setProperty("eureka.us-east-1.availabilityZones", "abc,def");
        PropertyBasedAzToRegionMapper azToRegionMapper = new PropertyBasedAzToRegionMapper(new DefaultEurekaClientConfig());
        InstanceRegionChecker checker = new InstanceRegionChecker(azToRegionMapper, "us-east-1");
        azToRegionMapper.setRegionsToFetch(new String[]{"us-east-1"});
        AmazonInfo dcInfo = AmazonInfo.Builder.newBuilder().addMetadata(AmazonInfo.MetaDataKey.availabilityZone,
                "def").build();
        InstanceInfo instanceInfo = InstanceInfo.Builder.newBuilder().setAppName("app").setDataCenterInfo(
                dcInfo).build();
        String instanceRegion = checker.getInstanceRegion(instanceInfo);

        assertEquals("us-east-1", instanceRegion, "Invalid instance region.");
    }

    @Test
    void instanceWithNoAZ() throws Exception {
        ConfigurationManager.getConfigInstance().setProperty("eureka.us-east-1.availabilityZones", "abc,def");
        PropertyBasedAzToRegionMapper azToRegionMapper = new PropertyBasedAzToRegionMapper(new DefaultEurekaClientConfig());
        InstanceRegionChecker checker = new InstanceRegionChecker(azToRegionMapper, "us-east-1");
        azToRegionMapper.setRegionsToFetch(new String[]{"us-east-1"});
        AmazonInfo dcInfo = AmazonInfo.Builder.newBuilder().addMetadata(AmazonInfo.MetaDataKey.availabilityZone,
                "").build();
        InstanceInfo instanceInfo = InstanceInfo.Builder.newBuilder().setAppName("app").setDataCenterInfo(
                dcInfo).build();
        String instanceRegion = checker.getInstanceRegion(instanceInfo);

        assertNull(instanceRegion, "Invalid instance region.");
    }

    @Test
    void notMappedAZ() throws Exception {
        ConfigurationManager.getConfigInstance().setProperty("eureka.us-east-1.availabilityZones", "abc,def");
        PropertyBasedAzToRegionMapper azToRegionMapper = new PropertyBasedAzToRegionMapper(new DefaultEurekaClientConfig());
        InstanceRegionChecker checker = new InstanceRegionChecker(azToRegionMapper, "us-east-1");
        azToRegionMapper.setRegionsToFetch(new String[]{"us-east-1"});
        AmazonInfo dcInfo = AmazonInfo.Builder.newBuilder().addMetadata(AmazonInfo.MetaDataKey.availabilityZone,
                "us-east-1x").build();
        InstanceInfo instanceInfo = InstanceInfo.Builder.newBuilder().setAppName("abc").setDataCenterInfo(dcInfo).build();
        String instanceRegion = checker.getInstanceRegion(instanceInfo);

        assertEquals("us-east-1", instanceRegion, "Invalid instance region.");
    }

    @Test
    void notMappedAZNotFollowingFormat() throws Exception {
        ConfigurationManager.getConfigInstance().setProperty("eureka.us-east-1.availabilityZones", "abc,def");
        PropertyBasedAzToRegionMapper azToRegionMapper = new PropertyBasedAzToRegionMapper(new DefaultEurekaClientConfig());
        InstanceRegionChecker checker = new InstanceRegionChecker(azToRegionMapper, "us-east-1");
        azToRegionMapper.setRegionsToFetch(new String[]{"us-east-1"});
        AmazonInfo dcInfo = AmazonInfo.Builder.newBuilder().addMetadata(AmazonInfo.MetaDataKey.availabilityZone,
                "us-east-x").build();
        InstanceInfo instanceInfo = InstanceInfo.Builder.newBuilder().setAppName("abc").setDataCenterInfo(dcInfo).build();
        String instanceRegion = checker.getInstanceRegion(instanceInfo);

        assertNull(instanceRegion, "Invalid instance region.");
    }
}
