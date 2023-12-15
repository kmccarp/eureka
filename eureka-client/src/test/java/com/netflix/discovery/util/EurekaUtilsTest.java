package com.netflix.discovery.util;

import com.netflix.appinfo.AmazonInfo;
import com.netflix.appinfo.DataCenterInfo;
import com.netflix.appinfo.InstanceInfo;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author David Liu
 */
class EurekaUtilsTest {
    @Test
    void isInEc2() {
        InstanceInfo instanceInfo1 = new InstanceInfo.Builder(InstanceInfoGenerator.takeOne())
                .setDataCenterInfo(new DataCenterInfo() {
                    @Override
                    public Name getName() {
                        return Name.MyOwn;
                    }
                })
                .build();

        assertFalse(EurekaUtils.isInEc2(instanceInfo1));

        InstanceInfo instanceInfo2 = InstanceInfoGenerator.takeOne();
        assertTrue(EurekaUtils.isInEc2(instanceInfo2));
    }

    @Test
    void isInVpc() {
        InstanceInfo instanceInfo1 = new InstanceInfo.Builder(InstanceInfoGenerator.takeOne())
                .setDataCenterInfo(new DataCenterInfo() {
                    @Override
                    public Name getName() {
                        return Name.MyOwn;
                    }
                })
                .build();

        assertFalse(EurekaUtils.isInVpc(instanceInfo1));

        InstanceInfo instanceInfo2 = InstanceInfoGenerator.takeOne();
        assertFalse(EurekaUtils.isInVpc(instanceInfo2));

        InstanceInfo instanceInfo3 = InstanceInfoGenerator.takeOne();
        ((AmazonInfo) instanceInfo3.getDataCenterInfo()).getMetadata()
                .put(AmazonInfo.MetaDataKey.vpcId.getName(), "vpc-123456");

        assertTrue(EurekaUtils.isInVpc(instanceInfo3));
    }
}
