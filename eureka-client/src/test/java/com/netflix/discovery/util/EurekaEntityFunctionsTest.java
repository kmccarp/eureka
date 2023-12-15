/*
 * Copyright 2019 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netflix.discovery.util;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.shared.Application;
import com.netflix.discovery.shared.Applications;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EurekaEntityFunctionsTest {

    private Application createSingleInstanceApp(
            String appId, String instanceId,
            InstanceInfo.ActionType actionType) {
        InstanceInfo instanceInfo = Mockito.mock(InstanceInfo.class);
        Mockito.when(instanceInfo.getId()).thenReturn(instanceId);
        Mockito.when(instanceInfo.getAppName()).thenReturn(instanceId);
        Mockito.when(instanceInfo.getStatus())
                .thenReturn(InstanceInfo.InstanceStatus.UP);
        Mockito.when(instanceInfo.getActionType()).thenReturn(actionType);
        Application application = new Application(appId);
        application.addInstance(instanceInfo);
        return application;
    }

    private Applications createApplications(Application... applications) {
        return new Applications("appsHashCode",
                1559658285l, new ArrayList<>(Arrays.asList(applications)));
    }

    @Test
    void selectApplicationNamesIfNotNullReturnNameString() {
        Applications applications = createApplications(new Application("foo"),
                new Application("bar"), new Application("baz"));

        HashSet<String> strings =
                new HashSet<>(Arrays.asList("baz", "bar", "foo"));
        assertEquals(strings,
                EurekaEntityFunctions.selectApplicationNames(applications));
    }

    @Test
    void selectInstancesMappedByIdIfNotNullReturnMapOfInstances() {
        Application application = createSingleInstanceApp("foo", "foo",
                InstanceInfo.ActionType.ADDED);
        HashMap<String, InstanceInfo> hashMap = new HashMap<>();
        hashMap.put("foo", application.getByInstanceId("foo"));
        assertEquals(hashMap,
                EurekaEntityFunctions.selectInstancesMappedById(application));
    }

    @Test
    void selectInstanceIfInstanceExistsReturnSelectedInstance() {
        Application application = createSingleInstanceApp("foo", "foo",
                InstanceInfo.ActionType.ADDED);
        Applications applications = createApplications(application);

        assertNull(EurekaEntityFunctions
                .selectInstance(new Applications(), "foo"));
        assertNull(EurekaEntityFunctions
                .selectInstance(new Applications(), "foo", "foo"));

        assertEquals(application.getByInstanceId("foo"),
                EurekaEntityFunctions.selectInstance(applications, "foo"));
        assertEquals(application.getByInstanceId("foo"),
                EurekaEntityFunctions.selectInstance(applications, "foo", "foo"));
    }

    @Test
    void takeFirstIfNotNullReturnFirstInstance() {
        Application application = createSingleInstanceApp("foo", "foo",
                InstanceInfo.ActionType.ADDED);
        Applications applications = createApplications(application);
        applications.addApplication(application);

        assertNull(EurekaEntityFunctions.takeFirst(new Applications()));
        assertEquals(application.getByInstanceId("foo"),
                EurekaEntityFunctions.takeFirst(applications));
    }

    @Test
    void selectAllIfNotNullReturnAllInstances() {
        Application application = createSingleInstanceApp("foo", "foo",
                InstanceInfo.ActionType.ADDED);
        Applications applications = createApplications(application);
        applications.addApplication(application);
        assertEquals(new ArrayList<>(Arrays.asList(
                application.getByInstanceId("foo"),
                application.getByInstanceId("foo"))),
                EurekaEntityFunctions.selectAll(applications));
    }

    @Test
    void toApplicationMapIfNotNullReturnMapOfApplication() {
        Application application = createSingleInstanceApp("foo", "foo",
                InstanceInfo.ActionType.ADDED);
        assertEquals(1, EurekaEntityFunctions.toApplicationMap(
                new ArrayList<>(Arrays.asList(
                        application.getByInstanceId("foo")))).size());
    }

    @Test
    void toApplicationsIfNotNullReturnApplicationsFromMapOfApplication() {
        HashMap<String, Application> hashMap = new HashMap<>();
        hashMap.put("foo", new Application("foo"));
        hashMap.put("bar", new Application("bar"));
        hashMap.put("baz", new Application("baz"));

        Applications applications = createApplications(new Application("foo"),
                new Application("bar"), new Application("baz"));

        assertEquals(applications.size(),
                EurekaEntityFunctions.toApplications(hashMap).size());
    }

    @Test
    void toApplicationsIfNotNullReturnApplicationsFromInstances() {
        InstanceInfo instanceInfo1 = createSingleInstanceApp("foo", "foo",
                InstanceInfo.ActionType.ADDED).getByInstanceId("foo");
        InstanceInfo instanceInfo2 = createSingleInstanceApp("bar", "bar",
                InstanceInfo.ActionType.ADDED).getByInstanceId("bar");
        InstanceInfo instanceInfo3 = createSingleInstanceApp("baz", "baz",
                InstanceInfo.ActionType.ADDED).getByInstanceId("baz");
        assertEquals(3, EurekaEntityFunctions.toApplications(
                instanceInfo1, instanceInfo2, instanceInfo3).size());
    }

    @Test
    void copyApplicationsIfNotNullReturnApplications() {
        Application application1 = createSingleInstanceApp("foo", "foo",
                InstanceInfo.ActionType.ADDED);
        Application application2 = createSingleInstanceApp("bar", "bar",
                InstanceInfo.ActionType.ADDED);
        Applications applications = createApplications();
        applications.addApplication(application1);
        applications.addApplication(application2);
        assertEquals(2,
                EurekaEntityFunctions.copyApplications(applications).size());
    }

    @Test
    void copyApplicationIfNotNullReturnApplication() {
        Application application = createSingleInstanceApp("foo", "foo",
                InstanceInfo.ActionType.ADDED);
        assertEquals(1,
                EurekaEntityFunctions.copyApplication(application).size());
    }

    @Test
    void copyInstancesIfNotNullReturnCollectionOfInstanceInfo() {
        Application application = createSingleInstanceApp("foo", "foo",
                InstanceInfo.ActionType.ADDED);
        assertEquals(1,
                EurekaEntityFunctions.copyInstances(
                        new ArrayList<>(Arrays.asList(
                                application.getByInstanceId("foo"))),
                        InstanceInfo.ActionType.ADDED).size());
    }

    @Test
    void mergeApplicationsIfNotNullAndHasAppNameReturnApplications() {
        Application application = createSingleInstanceApp("foo", "foo",
                InstanceInfo.ActionType.ADDED);
        Applications applications = createApplications(application);
        assertEquals(1, EurekaEntityFunctions.mergeApplications(
                applications, applications).size());
    }

    @Test
    void mergeApplicationsIfNotNullAndDoesNotHaveAppNameReturnApplications() {
        Application application1 = createSingleInstanceApp("foo", "foo",
                InstanceInfo.ActionType.ADDED);
        Applications applications1 = createApplications(application1);

        Application application2 = createSingleInstanceApp("bar", "bar",
                InstanceInfo.ActionType.ADDED);
        Applications applications2 = createApplications(application2);

        assertEquals(2, EurekaEntityFunctions.mergeApplications(
                applications1, applications2).size());
    }

    @Test
    void mergeApplicationIfActionTypeAddedReturnApplication() {
        Application application = createSingleInstanceApp("foo", "foo",
                InstanceInfo.ActionType.ADDED);
        assertEquals(application.getInstances(),
                EurekaEntityFunctions.mergeApplication(
                        application, application).getInstances());
    }

    @Test
    void mergeApplicationIfActionTypeModifiedReturnApplication() {
        Application application = createSingleInstanceApp("foo", "foo",
                InstanceInfo.ActionType.MODIFIED);
        assertEquals(application.getInstances(),
                EurekaEntityFunctions.mergeApplication(
                        application, application).getInstances());
    }

    @Test
    void mergeApplicationIfActionTypeDeletedReturnApplication() {
        Application application = createSingleInstanceApp("foo", "foo",
                InstanceInfo.ActionType.DELETED);

        assertNotEquals(application.getInstances(),
                EurekaEntityFunctions.mergeApplication(
                        application, application).getInstances());
    }

    @Test
    void updateMetaIfNotNullReturnApplications() {
        Application application = createSingleInstanceApp("foo", "foo",
                InstanceInfo.ActionType.ADDED);
        Applications applications = createApplications(application);
        assertEquals(1l,
                (long) EurekaEntityFunctions.updateMeta(applications)
                        .getVersion());
    }

    @Test
    void countInstancesIfApplicationsHasInstancesReturnSize() {
        Application application = createSingleInstanceApp("foo", "foo",
                InstanceInfo.ActionType.ADDED);
        Applications applications = createApplications(application);
        assertEquals(1,
                EurekaEntityFunctions.countInstances(applications));
    }

    @Test
    void comparatorByAppNameAndIdIfNotNullReturnInt() {
        InstanceInfo instanceInfo1 = Mockito.mock(InstanceInfo.class);
        InstanceInfo instanceInfo2 = Mockito.mock(InstanceInfo.class);
        InstanceInfo instanceInfo3 = createSingleInstanceApp("foo", "foo",
                InstanceInfo.ActionType.ADDED).getByInstanceId("foo");
        InstanceInfo instanceInfo4 = createSingleInstanceApp("bar", "bar",
                InstanceInfo.ActionType.ADDED).getByInstanceId("bar");

        assertTrue(EurekaEntityFunctions.comparatorByAppNameAndId()
                .compare(instanceInfo1, instanceInfo2) > 0);
        assertTrue(EurekaEntityFunctions.comparatorByAppNameAndId()
                .compare(instanceInfo3, instanceInfo2) > 0);
        assertTrue(EurekaEntityFunctions.comparatorByAppNameAndId()
                .compare(instanceInfo1, instanceInfo3) < 0);
        assertTrue(EurekaEntityFunctions.comparatorByAppNameAndId()
                .compare(instanceInfo3, instanceInfo4) > 0);
        assertEquals(0, EurekaEntityFunctions.comparatorByAppNameAndId()
                .compare(instanceInfo3, instanceInfo3));
    }
}
