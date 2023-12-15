package com.netflix.eureka.cluster;

import java.util.Collections;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.util.InstanceInfoGenerator;
import com.netflix.eureka.cluster.TestableInstanceReplicationTask.ProcessingState;
import com.netflix.eureka.registry.PeerAwareInstanceRegistryImpl.Action;
import com.netflix.eureka.util.batcher.TaskProcessor.ProcessingResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.netflix.eureka.cluster.TestableInstanceReplicationTask.aReplicationTask;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Tomasz Bak
 */
class ReplicationTaskProcessorTest {

    private final TestableHttpReplicationClient replicationClient = new TestableHttpReplicationClient();

    private ReplicationTaskProcessor replicationTaskProcessor;

    @BeforeEach
    void setUp() throws Exception {
        replicationTaskProcessor = new ReplicationTaskProcessor("peerId#test", replicationClient);
    }

    @Test
    void nonBatchableTaskExecution() throws Exception {
        TestableInstanceReplicationTask task = aReplicationTask().withAction(Action.Heartbeat).withReplyStatusCode(200).build();
        ProcessingResult status = replicationTaskProcessor.process(task);
        assertThat(status, is(ProcessingResult.Success));
    }

    @Test
    void nonBatchableTaskCongestionFailureHandling() throws Exception {
        TestableInstanceReplicationTask task = aReplicationTask().withAction(Action.Heartbeat).withReplyStatusCode(503).build();
        ProcessingResult status = replicationTaskProcessor.process(task);
        assertThat(status, is(ProcessingResult.Congestion));
        assertThat(task.getProcessingState(), is(ProcessingState.Pending));
    }

    @Test
    void nonBatchableTaskNetworkFailureHandling() throws Exception {
        TestableInstanceReplicationTask task = aReplicationTask().withAction(Action.Heartbeat).withNetworkFailures(1).build();
        ProcessingResult status = replicationTaskProcessor.process(task);
        assertThat(status, is(ProcessingResult.TransientError));
        assertThat(task.getProcessingState(), is(ProcessingState.Pending));
    }

    @Test
    void nonBatchableTaskPermanentFailureHandling() throws Exception {
        TestableInstanceReplicationTask task = aReplicationTask().withAction(Action.Heartbeat).withReplyStatusCode(406).build();
        ProcessingResult status = replicationTaskProcessor.process(task);
        assertThat(status, is(ProcessingResult.PermanentError));
        assertThat(task.getProcessingState(), is(ProcessingState.Failed));
    }

    @Test
    void batchableTaskListExecution() throws Exception {
        TestableInstanceReplicationTask task = aReplicationTask().build();

        replicationClient.withBatchReply(200);
        replicationClient.withNetworkStatusCode(200);
        ProcessingResult status = replicationTaskProcessor.process(Collections.<ReplicationTask>singletonList(task));

        assertThat(status, is(ProcessingResult.Success));
        assertThat(task.getProcessingState(), is(ProcessingState.Finished));
    }

    @Test
    void batchableTaskCongestionFailureHandling() throws Exception {
        TestableInstanceReplicationTask task = aReplicationTask().build();

        replicationClient.withNetworkStatusCode(503);
        ProcessingResult status = replicationTaskProcessor.process(Collections.<ReplicationTask>singletonList(task));

        assertThat(status, is(ProcessingResult.Congestion));
        assertThat(task.getProcessingState(), is(ProcessingState.Pending));
    }

    @Test
    void batchableTaskNetworkReadTimeOutHandling() throws Exception {
        TestableInstanceReplicationTask task = aReplicationTask().build();

        replicationClient.withReadtimeOut(1);
        ProcessingResult status = replicationTaskProcessor.process(Collections.<ReplicationTask>singletonList(task));

        assertThat(status, is(ProcessingResult.Congestion));
        assertThat(task.getProcessingState(), is(ProcessingState.Pending));
    }


    @Test
    void batchableTaskNetworkFailureHandling() throws Exception {
        TestableInstanceReplicationTask task = aReplicationTask().build();

        replicationClient.withNetworkError(1);
        ProcessingResult status = replicationTaskProcessor.process(Collections.<ReplicationTask>singletonList(task));

        assertThat(status, is(ProcessingResult.TransientError));
        assertThat(task.getProcessingState(), is(ProcessingState.Pending));
    }

    @Test
    void batchableTaskPermanentFailureHandling() throws Exception {
        TestableInstanceReplicationTask task = aReplicationTask().build();
        InstanceInfo instanceInfoFromPeer = InstanceInfoGenerator.takeOne();

        replicationClient.withNetworkStatusCode(200);
        replicationClient.withBatchReply(400);
        replicationClient.withInstanceInfo(instanceInfoFromPeer);
        ProcessingResult status = replicationTaskProcessor.process(Collections.<ReplicationTask>singletonList(task));

        assertThat(status, is(ProcessingResult.Success));
        assertThat(task.getProcessingState(), is(ProcessingState.Failed));
    }
}