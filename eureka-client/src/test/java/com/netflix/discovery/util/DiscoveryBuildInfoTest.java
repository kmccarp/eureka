package com.netflix.discovery.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Tomasz Bak
 */
class DiscoveryBuildInfoTest {

    @Test
    void requestedManifestIsLocatedAndLoaded() throws Exception {
        DiscoveryBuildInfo buildInfo = new DiscoveryBuildInfo(ObjectMapper.class);
        assertThat(buildInfo.getBuildVersion().contains("version_unknown"), is(false));
    }
}