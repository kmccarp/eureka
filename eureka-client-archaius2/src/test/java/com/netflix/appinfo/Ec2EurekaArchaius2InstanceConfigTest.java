package com.netflix.appinfo;

import com.netflix.archaius.config.MapConfig;
import com.netflix.discovery.util.InstanceInfoGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static com.netflix.appinfo.AmazonInfo.MetaDataKey.ipv6;
import static com.netflix.appinfo.AmazonInfo.MetaDataKey.localIpv4;
import static com.netflix.appinfo.AmazonInfo.MetaDataKey.publicHostname;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author David Liu
 */
class Ec2EurekaArchaius2InstanceConfigTest {
    private Ec2EurekaArchaius2InstanceConfig config;
    private String dummyDefault = "dummyDefault";
    private InstanceInfo instanceInfo;

    @BeforeEach
    void setUp() {
        instanceInfo = InstanceInfoGenerator.takeOne();
    }

    @Test
    void resolveDefaultAddress() {
        AmazonInfo info = (AmazonInfo) instanceInfo.getDataCenterInfo();
        config = createConfig(info);
        assertThat(config.resolveDefaultAddress(false), is(info.get(publicHostname)));

        info.getMetadata().remove(publicHostname.getName());
        config = createConfig(info);
        assertThat(config.resolveDefaultAddress(false), is(info.get(localIpv4)));

        info.getMetadata().remove(localIpv4.getName());
        config = createConfig(info);
        assertThat(config.resolveDefaultAddress(false), is(info.get(ipv6)));

        info.getMetadata().remove(ipv6.getName());
        config = createConfig(info);
        assertThat(config.resolveDefaultAddress(false), is(dummyDefault));
    }

    private Ec2EurekaArchaius2InstanceConfig createConfig(AmazonInfo info) {

        return new Ec2EurekaArchaius2InstanceConfig(MapConfig.from(Collections.<String, String>emptyMap()), info) {
            @Override
            public String[] getDefaultAddressResolutionOrder() {
                return new String[] {
                        publicHostname.name(),
                        localIpv4.name(),
                        ipv6.name()
                };
            }

            @Override
            public String getHostName(boolean refresh) {
                return dummyDefault;
            }
        };
    }
}

