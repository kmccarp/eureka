package com.netflix.appinfo;

import com.netflix.discovery.util.InstanceInfoGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.netflix.appinfo.AmazonInfo.MetaDataKey.amiId;
import static com.netflix.appinfo.AmazonInfo.MetaDataKey.instanceId;
import static com.netflix.appinfo.AmazonInfo.MetaDataKey.localIpv4;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author David Liu
 */
class RefreshableAmazonInfoProviderTest {

    private InstanceInfo instanceInfo;

    @BeforeEach
    void setUp() {
        instanceInfo = InstanceInfoGenerator.takeOne();
    }

    @Test
    void amazonInfoNoUpdateIfEqual() {
        AmazonInfo oldInfo = (AmazonInfo) instanceInfo.getDataCenterInfo();

        AmazonInfo newInfo = copyAmazonInfo(instanceInfo);
        assertThat(RefreshableAmazonInfoProvider.shouldUpdate(newInfo, oldInfo), is(false));
    }

    @Test
    void amazonInfoNoUpdateIfEmpty() {
        AmazonInfo oldInfo = (AmazonInfo) instanceInfo.getDataCenterInfo();

        AmazonInfo newInfo = new AmazonInfo();
        assertThat(RefreshableAmazonInfoProvider.shouldUpdate(newInfo, oldInfo), is(false));
    }

    @Test
    void amazonInfoNoUpdateIfNoInstanceId() {
        AmazonInfo oldInfo = (AmazonInfo) instanceInfo.getDataCenterInfo();

        AmazonInfo newInfo = copyAmazonInfo(instanceInfo);
        newInfo.getMetadata().remove(instanceId.getName());
        assertThat(newInfo.getId(), is(nullValue()));
        assertThat(newInfo.get(instanceId), is(nullValue()));
        assertThat(CloudInstanceConfig.shouldUpdate(newInfo, oldInfo), is(false));

        newInfo.getMetadata().put(instanceId.getName(), "");
        assertThat(newInfo.getId(), is(""));
        assertThat(newInfo.get(instanceId), is(""));
        assertThat(RefreshableAmazonInfoProvider.shouldUpdate(newInfo, oldInfo), is(false));
    }

    @Test
    void amazonInfoNoUpdateIfNoLocalIpv4() {
        AmazonInfo oldInfo = (AmazonInfo) instanceInfo.getDataCenterInfo();

        AmazonInfo newInfo = copyAmazonInfo(instanceInfo);
        newInfo.getMetadata().remove(localIpv4.getName());
        assertThat(newInfo.get(localIpv4), is(nullValue()));
        assertThat(CloudInstanceConfig.shouldUpdate(newInfo, oldInfo), is(false));

        newInfo.getMetadata().put(localIpv4.getName(), "");
        assertThat(newInfo.get(localIpv4), is(""));
        assertThat(RefreshableAmazonInfoProvider.shouldUpdate(newInfo, oldInfo), is(false));
    }

    @Test
    void amazonInfoUpdatePositiveCase() {
        AmazonInfo oldInfo = (AmazonInfo) instanceInfo.getDataCenterInfo();

        AmazonInfo newInfo = copyAmazonInfo(instanceInfo);
        newInfo.getMetadata().remove(amiId.getName());
        assertThat(newInfo.getMetadata().size(), is(oldInfo.getMetadata().size() - 1));
        assertThat(RefreshableAmazonInfoProvider.shouldUpdate(newInfo, oldInfo), is(true));

        String newKey = "someNewKey";
        newInfo.getMetadata().put(newKey, "bar");
        assertThat(newInfo.getMetadata().size(), is(oldInfo.getMetadata().size()));
        assertThat(RefreshableAmazonInfoProvider.shouldUpdate(newInfo, oldInfo), is(true));
    }

    private static AmazonInfo copyAmazonInfo(InstanceInfo instanceInfo) {
        AmazonInfo currInfo = (AmazonInfo) instanceInfo.getDataCenterInfo();
        AmazonInfo copyInfo = new AmazonInfo();
        for (String key : currInfo.getMetadata().keySet()) {
            copyInfo.getMetadata().put(key, currInfo.getMetadata().get(key));
        }
        return copyInfo;
    }
}
