package com.netflix.appinfo;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;

import com.netflix.discovery.internal.util.AmazonInfoUtils;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mockStatic;


/**
 * @author David Liu
 */
class AmazonInfoTest {
    @Test
    void extractAccountId() throws Exception {
        String json = """
                {
                  "imageId" : "ami-someId",
                  "instanceType" : "m1.small",
                  "version" : "2000-00-00",
                  "architecture" : "x86_64",
                  "accountId" : "1111111111",
                  "instanceId" : "i-someId",
                  "billingProducts" : null,
                  "pendingTime" : "2000-00-00T00:00:00Z",
                  "availabilityZone" : "us-east-1c",
                  "region" : "us-east-1",
                  "kernelId" : "aki-someId",
                  "ramdiskId" : null,
                  "privateIp" : "1.1.1.1"
                }\
                """;

        InputStream inputStream = new ByteArrayInputStream(json.getBytes());
        String accountId = AmazonInfo.MetaDataKey.accountId.read(inputStream);

        assertEquals("1111111111", accountId);
    }

    @Test
    void extractMacs_SingleMac() throws Exception {
        String body = "0d:c2:9a:3c:18:2b";

        InputStream inputStream = new ByteArrayInputStream(body.getBytes());
        String macs = AmazonInfo.MetaDataKey.macs.read(inputStream);

        assertEquals("0d:c2:9a:3c:18:2b", macs);
    }

    @Test
    void extractMacs_MultipleMacs() throws Exception {
        String body = "0d:c2:9a:3c:18:2b\n4c:31:99:7e:26:d6";

        InputStream inputStream = new ByteArrayInputStream(body.getBytes());
        String macs = AmazonInfo.MetaDataKey.macs.read(inputStream);

        assertEquals("0d:c2:9a:3c:18:2b\n4c:31:99:7e:26:d6", macs);
    }

    @Test
    void extractPublicIPv4s_SingleAddress() throws Exception {
        String body = "10.0.0.1";

        InputStream inputStream = new ByteArrayInputStream(body.getBytes());
        String publicIPv4s = AmazonInfo.MetaDataKey.publicIpv4s.read(inputStream);

        assertEquals("10.0.0.1", publicIPv4s);
    }

    @Test
    void extractPublicIPv4s_MultipleAddresses() throws Exception {
        String body = "10.0.0.1\n10.0.0.2";

        InputStream inputStream = new ByteArrayInputStream(body.getBytes());
        String publicIPv4s = AmazonInfo.MetaDataKey.publicIpv4s.read(inputStream);

        assertEquals("10.0.0.1", publicIPv4s);
    }

    @Test
    void autoBuild() throws Exception {
        try (MockedStatic<AmazonInfoUtils> mockUtils = mockStatic(AmazonInfoUtils.class)) {
            mockUtils.when(
                    () -> AmazonInfoUtils.readEc2MetadataUrl(any(AmazonInfo.MetaDataKey.class), any(URL.class), anyInt(), anyInt())
            ).thenReturn(null);

            mockUtils.when(
                    () -> AmazonInfoUtils.readEc2MetadataUrl(any(AmazonInfo.MetaDataKey.class), any(URL.class), anyInt(), anyInt())
            ).thenReturn(null);

            URL macsUrl = AmazonInfo.MetaDataKey.macs.getURL(null, null);
            mockUtils.when(
                    () -> AmazonInfoUtils.readEc2MetadataUrl(eq(AmazonInfo.MetaDataKey.macs), eq(macsUrl), anyInt(), anyInt())
            ).thenReturn("0d:c2:9a:3c:18:2b\n4c:31:99:7e:26:d6");

            URL firstMacPublicIPv4sUrl = AmazonInfo.MetaDataKey.publicIpv4s.getURL(null, "0d:c2:9a:3c:18:2b");
            mockUtils.when(
                    () -> AmazonInfoUtils.readEc2MetadataUrl(eq(AmazonInfo.MetaDataKey.publicIpv4s), eq(firstMacPublicIPv4sUrl), anyInt(), anyInt())
            ).thenReturn(null);

            URL secondMacPublicIPv4sUrl = AmazonInfo.MetaDataKey.publicIpv4s.getURL(null, "4c:31:99:7e:26:d6");
            mockUtils.when(
                    () -> AmazonInfoUtils.readEc2MetadataUrl(eq(AmazonInfo.MetaDataKey.publicIpv4s), eq(secondMacPublicIPv4sUrl), anyInt(), anyInt())
            ).thenReturn("10.0.0.1");

            AmazonInfoConfig config = mock(AmazonInfoConfig.class);
            when(config.getNamespace()).thenReturn("test_namespace");
            when(config.getConnectTimeout()).thenReturn(10);
            when(config.getNumRetries()).thenReturn(1);
            when(config.getReadTimeout()).thenReturn(10);
            when(config.shouldLogAmazonMetadataErrors()).thenReturn(false);
            when(config.shouldValidateInstanceId()).thenReturn(false);
            when(config.shouldFailFastOnFirstLoad()).thenReturn(false);

            AmazonInfo info = AmazonInfo.Builder.newBuilder().withAmazonInfoConfig(config).autoBuild("test_namespace");

            assertEquals("10.0.0.1", info.get(AmazonInfo.MetaDataKey.publicIpv4s));
        }
    }
}
