package com.netflix.eureka.registry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.appinfo.InstanceInfo.InstanceStatus;
import com.netflix.discovery.shared.Application;
import com.netflix.discovery.shared.Applications;
import com.netflix.eureka.AbstractTester;
import com.netflix.eureka.registry.AbstractInstanceRegistry.CircularQueue;
import com.netflix.eureka.registry.AbstractInstanceRegistry.EvictionTask;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

/**
 * @author Nitesh Kant
 */
class InstanceRegistryTest extends AbstractTester {

    @Test
    void softDepRemoteUp() throws Exception {
        assertTrue(registry.shouldAllowAccess(false), "Registry access disallowed when remote region is UP.");
        assertTrue(registry.shouldAllowAccess(true), "Registry access disallowed when remote region is UP.");
    }

    @Test
    void getAppsFromAllRemoteRegions() throws Exception {
        Applications apps = registry.getApplicationsFromAllRemoteRegions();
        List<Application> registeredApplications = apps.getRegisteredApplications();
        assertEquals(1, registeredApplications.size(), "Apps size from remote regions do not match");
        Application app = registeredApplications.iterator().next();
        assertEquals(REMOTE_REGION_APP_NAME, app.getName(), "Added app did not return from remote registry");
        assertEquals(1, app.getInstances().size(), "Returned app did not have the instance");
    }

    @Test
    void getAppsDeltaFromAllRemoteRegions() throws Exception {
        registerInstanceLocally(createLocalInstance(LOCAL_REGION_INSTANCE_2_HOSTNAME)); /// local delta
        waitForDeltaToBeRetrieved();
        Applications appDelta = registry.getApplicationDeltasFromMultipleRegions(null);
        List<Application> registeredApplications = appDelta.getRegisteredApplications();
        assertEquals(2, registeredApplications.size(), "Apps size from remote regions do not match");
        Application localApplication = null;
        Application remApplication = null;
        for (Application registeredApplication : registeredApplications) {
            if (registeredApplication.getName().equalsIgnoreCase(LOCAL_REGION_APP_NAME)) {
                localApplication = registeredApplication;
            }
            if (registeredApplication.getName().equalsIgnoreCase(REMOTE_REGION_APP_NAME)) {
                remApplication = registeredApplication;
            }
        }
        assertNotNull(localApplication, "Did not find local registry app in delta.");
        assertEquals(1,
                localApplication.getInstances().size(),
                "Local registry app instance count in delta not as expected.");
        assertNotNull(remApplication, "Did not find remote registry app in delta");
        assertEquals(1,
                remApplication.getInstances().size(),
                "Remote registry app instance count  in delta not as expected.");
    }

    @Test
    void appsHashCodeAfterRefresh() throws InterruptedException {
        assertEquals("UP_1_", registry.getApplicationsFromAllRemoteRegions().getAppsHashCode());

        registerInstanceLocally(createLocalInstance(LOCAL_REGION_INSTANCE_2_HOSTNAME));
        waitForDeltaToBeRetrieved();

        assertEquals("UP_2_", registry.getApplicationsFromAllRemoteRegions().getAppsHashCode());
    }

    private void waitForDeltaToBeRetrieved() throws InterruptedException {
        int count = 0;
        System.out.println("Sleeping up to 35 seconds to let the remote registry fetch delta.");
        while (count++ < 35 && !mockRemoteEurekaServer.isSentDelta()) {
            Thread.sleep(1000);
        }
        if (!mockRemoteEurekaServer.isSentDelta()) {
            System.out.println("Waited for 35 seconds but remote server did not send delta");
        }
        // Wait 2 seconds more to be sure the delta was processed
        Thread.sleep(2000);
    }

    @Test
    void getAppsFromLocalRegionOnly() throws Exception {
        registerInstanceLocally(createLocalInstance(LOCAL_REGION_INSTANCE_1_HOSTNAME));

        Applications apps = registry.getApplicationsFromLocalRegionOnly();
        List<Application> registeredApplications = apps.getRegisteredApplications();
        assertEquals(1, registeredApplications.size(), "Apps size from local region do not match");
        Application app = registeredApplications.iterator().next();
        assertEquals(LOCAL_REGION_APP_NAME, app.getName(), "Added app did not return from local registry");
        assertEquals(1, app.getInstances().size(), "Returned app did not have the instance");
    }

    @Test
    void getAppsFromBothRegions() throws Exception {
        registerInstanceLocally(createRemoteInstance(LOCAL_REGION_INSTANCE_2_HOSTNAME));
        registerInstanceLocally(createLocalInstance(LOCAL_REGION_INSTANCE_1_HOSTNAME));

        Applications apps = registry.getApplicationsFromAllRemoteRegions();
        List<Application> registeredApplications = apps.getRegisteredApplications();
        assertEquals(2, registeredApplications.size(), "Apps size from both regions do not match");
        Application locaApplication = null;
        Application remApplication = null;
        for (Application registeredApplication : registeredApplications) {
            if (registeredApplication.getName().equalsIgnoreCase(LOCAL_REGION_APP_NAME)) {
                locaApplication = registeredApplication;
            }
            if (registeredApplication.getName().equalsIgnoreCase(REMOTE_REGION_APP_NAME)) {
                remApplication = registeredApplication;
            }
        }
        assertNotNull(locaApplication, "Did not find local registry app");
        assertEquals(1,
                locaApplication.getInstances().size(),
                "Local registry app instance count not as expected.");
        assertNotNull(remApplication, "Did not find remote registry app");
        assertEquals(2,
                remApplication.getInstances().size(),
                "Remote registry app instance count not as expected.");

    }

    @Test
    void statusOverrideSetAndRemoval() throws Exception {
        InstanceInfo seed = createLocalInstance(LOCAL_REGION_INSTANCE_1_HOSTNAME);
        seed.setLastDirtyTimestamp(100l);

        // Regular registration first
        InstanceInfo myInstance1 = new InstanceInfo(seed);
        registerInstanceLocally(myInstance1);
        verifyLocalInstanceStatus(myInstance1.getId(), InstanceStatus.UP);

        // Override status
        boolean statusResult = registry.statusUpdate(LOCAL_REGION_APP_NAME, seed.getId(), InstanceStatus.OUT_OF_SERVICE, "0", false);
        assertThat("Couldn't override instance status", statusResult, is(true));
        verifyLocalInstanceStatus(seed.getId(), InstanceStatus.OUT_OF_SERVICE);

        // Register again with status UP to verify that the override is still in place even if the dirtytimestamp is higher
        InstanceInfo myInstance2 = new InstanceInfo(seed);  // clone to avoid object state in this test
        myInstance2.setLastDirtyTimestamp(200l);  // use a later 'client side' dirty timestamp
        registry.register(myInstance2, 10000000, false);
        verifyLocalInstanceStatus(seed.getId(), InstanceStatus.OUT_OF_SERVICE);

        // Now remove override
        statusResult = registry.deleteStatusOverride(LOCAL_REGION_APP_NAME, seed.getId(), InstanceStatus.DOWN, "0", false);
        assertThat("Couldn't remove status override", statusResult, is(true));
        verifyLocalInstanceStatus(seed.getId(), InstanceStatus.DOWN);

        // Register again with status UP after the override deletion, keeping myInstance2's dirtyTimestamp (== no client side change)
        InstanceInfo myInstance3 = new InstanceInfo(seed);  // clone to avoid object state in this test
        myInstance3.setLastDirtyTimestamp(200l);  // use a later 'client side' dirty timestamp
        registry.register(myInstance3, 10000000, false);
        verifyLocalInstanceStatus(seed.getId(), InstanceStatus.UP);
    }

    @Test
    void statusOverrideWithRenewAppliedToAReplica() throws Exception {
        InstanceInfo seed = createLocalInstance(LOCAL_REGION_INSTANCE_1_HOSTNAME);
        seed.setLastDirtyTimestamp(100l);

        // Regular registration first
        InstanceInfo myInstance1 = new InstanceInfo(seed);
        registerInstanceLocally(myInstance1);
        verifyLocalInstanceStatus(myInstance1.getId(), InstanceStatus.UP);

        // Override status
        boolean statusResult = registry.statusUpdate(LOCAL_REGION_APP_NAME, seed.getId(), InstanceStatus.OUT_OF_SERVICE, "0", false);
        assertThat("Couldn't override instance status", statusResult, is(true));
        verifyLocalInstanceStatus(seed.getId(), InstanceStatus.OUT_OF_SERVICE);

        // Send a renew to ensure timestamps are consistent even with the override in existence.
        // To do this, we get hold of the registry local InstanceInfo and reset its status to before an override
        // has been applied
        //  (this is to simulate a case in a replica server where the override has been replicated, but not yet
        //   applied to the local InstanceInfo)
        InstanceInfo registeredInstance = registry.getInstanceByAppAndId(seed.getAppName(), seed.getId());
        registeredInstance.setStatusWithoutDirty(InstanceStatus.UP);
        verifyLocalInstanceStatus(seed.getId(), InstanceStatus.UP);
        registry.renew(seed.getAppName(), seed.getId(), false);
        verifyLocalInstanceStatus(seed.getId(), InstanceStatus.OUT_OF_SERVICE);

        // Now remove override
        statusResult = registry.deleteStatusOverride(LOCAL_REGION_APP_NAME, seed.getId(), InstanceStatus.DOWN, "0", false);
        assertThat("Couldn't remove status override", statusResult, is(true));
        verifyLocalInstanceStatus(seed.getId(), InstanceStatus.DOWN);

        // Register again with status UP after the override deletion, keeping myInstance2's dirtyTimestamp (== no client side change)
        InstanceInfo myInstance3 = new InstanceInfo(seed);  // clone to avoid object state in this test
        myInstance3.setLastDirtyTimestamp(200l);  // use a later 'client side' dirty timestamp
        registry.register(myInstance3, 10000000, false);
        verifyLocalInstanceStatus(seed.getId(), InstanceStatus.UP);
    }

    @Test
    void statusOverrideStartingStatus() throws Exception {
        // Regular registration first
        InstanceInfo myInstance = createLocalInstance(LOCAL_REGION_INSTANCE_1_HOSTNAME);
        registerInstanceLocally(myInstance);
        verifyLocalInstanceStatus(myInstance.getId(), InstanceStatus.UP);

        // Override status
        boolean statusResult = registry.statusUpdate(LOCAL_REGION_APP_NAME, myInstance.getId(), InstanceStatus.OUT_OF_SERVICE, "0", false);
        assertThat("Couldn't override instance status", statusResult, is(true));
        verifyLocalInstanceStatus(myInstance.getId(), InstanceStatus.OUT_OF_SERVICE);

        // If we are not UP or OUT_OF_SERVICE, the OUT_OF_SERVICE override does not apply. It gets trumped by the current
        // status (STARTING or DOWN). Here we test with STARTING.
        myInstance = createLocalStartingInstance(LOCAL_REGION_INSTANCE_1_HOSTNAME);
        registerInstanceLocally(myInstance);
        verifyLocalInstanceStatus(myInstance.getId(), InstanceStatus.STARTING);
    }

    @Test
    void statusOverrideWithExistingLeaseUp() throws Exception {
        // Without an override we expect to get the existing UP lease when we re-register with OUT_OF_SERVICE.
        // First, we are "up".
        InstanceInfo myInstance = createLocalInstance(LOCAL_REGION_INSTANCE_1_HOSTNAME);
        registerInstanceLocally(myInstance);
        verifyLocalInstanceStatus(myInstance.getId(), InstanceStatus.UP);

        // Then, we re-register with "out of service".
        InstanceInfo sameInstance = createLocalOutOfServiceInstance(LOCAL_REGION_INSTANCE_1_HOSTNAME);
        registry.register(sameInstance, 10000000, false);
        verifyLocalInstanceStatus(myInstance.getId(), InstanceStatus.UP);

        // Let's try again. We shouldn't see a difference.
        sameInstance = createLocalOutOfServiceInstance(LOCAL_REGION_INSTANCE_1_HOSTNAME);
        registry.register(sameInstance, 10000000, false);
        verifyLocalInstanceStatus(myInstance.getId(), InstanceStatus.UP);
    }

    @Test
    void statusOverrideWithExistingLeaseOutOfService() throws Exception {
        // Without an override we expect to get the existing OUT_OF_SERVICE lease when we re-register with UP.
        // First, we are "out of service".
        InstanceInfo myInstance = createLocalOutOfServiceInstance(LOCAL_REGION_INSTANCE_1_HOSTNAME);
        registerInstanceLocally(myInstance);
        verifyLocalInstanceStatus(myInstance.getId(), InstanceStatus.OUT_OF_SERVICE);

        // Then, we re-register with "UP".
        InstanceInfo sameInstance = createLocalInstance(LOCAL_REGION_INSTANCE_1_HOSTNAME);
        registry.register(sameInstance, 10000000, false);
        verifyLocalInstanceStatus(myInstance.getId(), InstanceStatus.OUT_OF_SERVICE);

        // Let's try again. We shouldn't see a difference.
        sameInstance = createLocalInstance(LOCAL_REGION_INSTANCE_1_HOSTNAME);
        registry.register(sameInstance, 10000000, false);
        verifyLocalInstanceStatus(myInstance.getId(), InstanceStatus.OUT_OF_SERVICE);
    }

    @Test
    void evictionTaskCompensationTime() throws Exception {
        long evictionTaskPeriodNanos = serverConfig.getEvictionIntervalTimerInMs() * 1000000;

        AbstractInstanceRegistry.EvictionTask testTask = spy(registry.new EvictionTask());

        when(testTask.getCurrentTimeNano())
                .thenReturn(1l)  // less than the period
                .thenReturn(1l + evictionTaskPeriodNanos)  // exactly 1 period
                .thenReturn(1l + evictionTaskPeriodNanos*2 + 10000000l)  // 10ms longer than 1 period
                .thenReturn(1l + evictionTaskPeriodNanos*3 - 1l);  // less than 1 period

        assertThat(testTask.getCompensationTimeMs(), is(0l));
        assertThat(testTask.getCompensationTimeMs(), is(0l));
        assertThat(testTask.getCompensationTimeMs(), is(10l));
        assertThat(testTask.getCompensationTimeMs(), is(0l));
    }

    @Test
    void circularQueue() throws Exception {
        CircularQueue<Integer> queue = new CircularQueue<>(5);

        assertEquals(0, queue.size());
        assertNull(queue.peek());
        assertEquals(Collections.emptyList(), new ArrayList<>(queue));

        queue.add(1);
        queue.add(2);
        queue.add(3);
        queue.add(4);

        assertEquals(4, queue.size());
        assertEquals(Arrays.asList(1, 2, 3, 4), new ArrayList<>(queue));

        queue.offer(5);

        assertEquals(5, queue.size());
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), new ArrayList<>(queue));

        queue.offer(6);

        assertEquals(5, queue.size());
        assertEquals(Arrays.asList(2, 3, 4, 5, 6), new ArrayList<>(queue));

        Integer poll = queue.poll();

        assertEquals(4, queue.size());
        assertEquals((Object) 2, poll);
        assertEquals(Arrays.asList(3, 4, 5, 6), new ArrayList<>(queue));

        queue.add(7);

        assertEquals(5, queue.size());
        assertEquals(Arrays.asList(3, 4, 5, 6, 7), new ArrayList<>(queue));

        queue.clear();

        assertEquals(0, queue.size());
        assertEquals(Collections.emptyList(), new ArrayList<>(queue));
    }
}
