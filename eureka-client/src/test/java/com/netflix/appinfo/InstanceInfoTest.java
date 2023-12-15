package com.netflix.appinfo;

import com.netflix.appinfo.InstanceInfo.Builder;
import com.netflix.appinfo.InstanceInfo.PortType;
import com.netflix.config.ConcurrentCompositeConfiguration;
import com.netflix.config.ConfigurationManager;
import com.netflix.discovery.util.InstanceInfoGenerator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static com.netflix.appinfo.InstanceInfo.Builder.newBuilder;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

/**
 * Created by jzarfoss on 2/12/14.
 */
class InstanceInfoTest {
    @AfterEach
    void tearDown() throws Exception {
        ((ConcurrentCompositeConfiguration) ConfigurationManager.getConfigInstance()).clearOverrideProperty("NETFLIX_APP_GROUP");
        ((ConcurrentCompositeConfiguration) ConfigurationManager.getConfigInstance()).clearOverrideProperty("eureka.appGroup");
    }

    // contrived test to check copy constructor and verify behavior of builder for InstanceInfo

    @Test
    void copyConstructor() {

        DataCenterInfo myDCI = new DataCenterInfo() {

            public DataCenterInfo.Name getName() {
                return DataCenterInfo.Name.MyOwn;
            }

        };


        InstanceInfo smallII1 = newBuilder().setAppName("test").setDataCenterInfo(myDCI).build();
        InstanceInfo smallII2 = new InstanceInfo(smallII1);

        assertNotSame(smallII1, smallII2);
        assertEquals(smallII1, smallII2);


        InstanceInfo fullII1 = newBuilder().setMetadata(null)
                .setOverriddenStatus(InstanceInfo.InstanceStatus.UNKNOWN)
                .setHostName("localhost")
                .setSecureVIPAddress("testSecureVIP:22")
                .setStatus(InstanceInfo.InstanceStatus.UNKNOWN)
                .setStatusPageUrl("relative", "explicit/relative")
                .setVIPAddress("testVIP:21")
                .setAppName("test").setASGName("testASG").setDataCenterInfo(myDCI)
                .setHealthCheckUrls("relative", "explicit/relative", "secureExplicit/relative")
                .setHomePageUrl("relativeHP", "explicitHP/relativeHP")
                .setIPAddr("127.0.0.1")
                .setPort(21).setSecurePort(22).build();

        InstanceInfo fullII2 = new InstanceInfo(fullII1);

        assertNotSame(fullII1, fullII2);
        assertEquals(fullII1, fullII2);
    }

    @Test
    void appGroupNameSystemProp() throws Exception {
        String appGroup = "testAppGroupSystemProp";
        ((ConcurrentCompositeConfiguration) ConfigurationManager.getConfigInstance()).setOverrideProperty("NETFLIX_APP_GROUP",
                appGroup);
        MyDataCenterInstanceConfig config = new MyDataCenterInstanceConfig();
        assertEquals(appGroup, config.getAppGroupName(), "Unexpected app group name");
    }

    @Test
    void appGroupName() throws Exception {
        String appGroup = "testAppGroup";
        ((ConcurrentCompositeConfiguration) ConfigurationManager.getConfigInstance()).setOverrideProperty("eureka.appGroup",
                appGroup);
        MyDataCenterInstanceConfig config = new MyDataCenterInstanceConfig();
        assertEquals(appGroup, config.getAppGroupName(), "Unexpected app group name");
    }

    @Test
    void healthCheckSetContainsValidUrlEntries() throws Exception {
        Builder builder = newBuilder()
                .setAppName("test")
                .setNamespace("eureka.")
                .setHostName("localhost")
                .setPort(80)
                .setSecurePort(443)
                .enablePort(PortType.SECURE, true);

        // No health check URLs
        InstanceInfo noHealtcheckInstanceInfo = builder.build();
        assertThat(noHealtcheckInstanceInfo.getHealthCheckUrls().size(), is(equalTo(0)));

        // Now when health check is defined
        InstanceInfo instanceInfo = builder
                .setHealthCheckUrls("/healthcheck", "http://${eureka.hostname}/healthcheck", "https://${eureka.hostname}/healthcheck")
                .build();
        assertThat(instanceInfo.getHealthCheckUrls().size(), is(equalTo(2)));
    }

    @Test
    void nullUrlEntries() throws Exception {
        Builder builder = newBuilder()
                .setAppName("test")
                .setNamespace("eureka.")
                .setHostName("localhost")
                .setPort(80)
                .setSecurePort(443)
                .setHealthCheckUrls(null, null, null)
                .setStatusPageUrl(null, null)
                .setHomePageUrl(null, null)
                .enablePort(PortType.SECURE, true);

        // No URLs for healthcheck , status , homepage
        InstanceInfo noHealtcheckInstanceInfo = builder.build();
        assertThat(noHealtcheckInstanceInfo.getHealthCheckUrls().size(), is(equalTo(0)));
        assertThat(noHealtcheckInstanceInfo.getStatusPageUrl(), nullValue());
        assertThat(noHealtcheckInstanceInfo.getHomePageUrl(), nullValue());
    }

    @Test
    void getIdWithInstanceIdUsed() {
        InstanceInfo baseline = InstanceInfoGenerator.takeOne();
        String dataCenterInfoId = ((UniqueIdentifier) baseline.getDataCenterInfo()).getId();
        assertThat(baseline.getInstanceId(), is(baseline.getId()));
        assertThat(dataCenterInfoId, is(baseline.getId()));

        String customInstanceId = "someId";
        InstanceInfo instanceInfo = new InstanceInfo.Builder(baseline).setInstanceId(customInstanceId).build();
        dataCenterInfoId = ((UniqueIdentifier) instanceInfo.getDataCenterInfo()).getId();
        assertThat(instanceInfo.getInstanceId(), is(instanceInfo.getId()));
        assertThat(customInstanceId, is(instanceInfo.getId()));
        assertThat(dataCenterInfoId, is(not(baseline.getId())));
    }

    // test case for backwards compatibility
    @Test
    void getIdWithInstanceIdNotUsed() {
        InstanceInfo baseline = InstanceInfoGenerator.takeOne();
        // override the sid with ""
        InstanceInfo instanceInfo1 = new InstanceInfo.Builder(baseline).setInstanceId("").build();
        String dataCenterInfoId = ((UniqueIdentifier) baseline.getDataCenterInfo()).getId();
        assertThat(instanceInfo1.getInstanceId().isEmpty(), is(true));
        assertThat(instanceInfo1.getInstanceId(), is(not(instanceInfo1.getId())));
        assertThat(dataCenterInfoId, is(instanceInfo1.getId()));

        // override the sid with null
        InstanceInfo instanceInfo2 = new InstanceInfo.Builder(baseline).setInstanceId(null).build();
        dataCenterInfoId = ((UniqueIdentifier) baseline.getDataCenterInfo()).getId();
        assertThat(instanceInfo2.getInstanceId(), is(nullValue()));
        assertThat(instanceInfo2.getInstanceId(), is(not(instanceInfo2.getId())));
        assertThat(dataCenterInfoId, is(instanceInfo2.getId()));
    }
}
