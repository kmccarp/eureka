package com.netflix.discovery;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import com.google.common.util.concurrent.ListeningExecutorService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TimedSupervisorTaskTest {

    private static final int EXP_BACK_OFF_BOUND = 10;

    private ScheduledExecutorService scheduler;
    private ListeningExecutorService helperExecutor;

    private ThreadPoolExecutor executor;
    private AtomicLong testTaskStartCounter;
    private AtomicLong testTaskSuccessfulCounter;
    private AtomicLong testTaskInterruptedCounter;

    private AtomicLong maxConcurrentTestTasks;
    private AtomicLong testTaskCounter;

    @BeforeEach
    void setUp() {
        scheduler = Executors.newScheduledThreadPool(4,
                new ThreadFactoryBuilder()
                        .setNameFormat("DiscoveryClient-%d")
                        .setDaemon(true)
                        .build());

        helperExecutor = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(10));

        executor = new ThreadPoolExecutor(
                1,  // corePoolSize
                3,  // maxPoolSize
                0,  // keepAliveTime
                TimeUnit.SECONDS,
                new SynchronousQueue<Runnable>());  // use direct handoff

        testTaskStartCounter = new AtomicLong(0);
        testTaskSuccessfulCounter = new AtomicLong(0);
        testTaskInterruptedCounter = new AtomicLong(0);

        maxConcurrentTestTasks = new AtomicLong(0);
        testTaskCounter = new AtomicLong(0);
    }

    @AfterEach
    void tearDown() {
        if (executor != null) {
            executor.shutdownNow();
        }

        if (helperExecutor != null) {
            helperExecutor.shutdownNow();
        }

        if (scheduler != null) {
            scheduler.shutdownNow();
        }
    }

    @Test
    void supervisorTaskDefaultSingleTestTaskHappyCase() throws Exception {
        // testTask should never timeout
        TestTask testTask = new TestTask(1, false);
        TimedSupervisorTask supervisorTask = new TimedSupervisorTask("test", scheduler, executor, 5, TimeUnit.SECONDS, EXP_BACK_OFF_BOUND, testTask);

        helperExecutor.submit(supervisorTask).get();

        assertEquals(1, maxConcurrentTestTasks.get());
        assertEquals(0, testTaskCounter.get());

        assertEquals(1, testTaskStartCounter.get());
        assertEquals(1, testTaskSuccessfulCounter.get());
        assertEquals(0, testTaskInterruptedCounter.get());
    }

    @Test
    void supervisorTaskCancelsTimedOutTask() throws Exception {
        // testTask will always timeout
        TestTask testTask = new TestTask(5, false);
        TimedSupervisorTask supervisorTask = new TimedSupervisorTask("test", scheduler, executor, 1, TimeUnit.SECONDS, EXP_BACK_OFF_BOUND, testTask);

        helperExecutor.submit(supervisorTask).get();
        Thread.sleep(500);  // wait a little bit for the subtask interrupt handler

        assertEquals(1, maxConcurrentTestTasks.get());
        assertEquals(1, testTaskCounter.get());

        assertEquals(1, testTaskStartCounter.get());
        assertEquals(0, testTaskSuccessfulCounter.get());
        assertEquals(1, testTaskInterruptedCounter.get());
    }

    @Test
    void supervisorRejectNewTasksIfThreadPoolIsFullForIncompleteTasks() throws Exception {
        // testTask should always timeout
        TestTask testTask = new TestTask(4, true);
        TimedSupervisorTask supervisorTask = new TimedSupervisorTask("test", scheduler, executor, 1, TimeUnit.MILLISECONDS, EXP_BACK_OFF_BOUND, testTask);

        scheduler.schedule(supervisorTask, 0, TimeUnit.SECONDS);

        Thread.sleep(500);  // wait a little bit for the subtask interrupt handlers

        assertEquals(3, maxConcurrentTestTasks.get());
        assertEquals(3, testTaskCounter.get());

        assertEquals(3, testTaskStartCounter.get());
        assertEquals(0, testTaskSuccessfulCounter.get());
        assertEquals(0, testTaskInterruptedCounter.get());
    }

    @Test
    void supervisorTaskAsPeriodicScheduledJobHappyCase() throws Exception {
        // testTask should never timeout
        TestTask testTask = new TestTask(1, false);
        TimedSupervisorTask supervisorTask = new TimedSupervisorTask("test", scheduler, executor, 4, TimeUnit.SECONDS, EXP_BACK_OFF_BOUND, testTask);

        scheduler.schedule(supervisorTask, 0, TimeUnit.SECONDS);
        Thread.sleep(5000);  // let the scheduler run for long enough for some results

        assertEquals(1, maxConcurrentTestTasks.get());
        assertEquals(0, testTaskCounter.get());

        assertEquals(0, testTaskInterruptedCounter.get());
    }

    @Test
    void supervisorTaskAsPeriodicScheduledJobTestTaskTimingOut() throws Exception {
        // testTask should always timeout
        TestTask testTask = new TestTask(5, false);
        TimedSupervisorTask supervisorTask = new TimedSupervisorTask("test", scheduler, executor, 2, TimeUnit.SECONDS, EXP_BACK_OFF_BOUND, testTask);

        scheduler.schedule(supervisorTask, 0, TimeUnit.SECONDS);
        Thread.sleep(5000);  // let the scheduler run for long enough for some results

        assertEquals(1, maxConcurrentTestTasks.get());
        assertTrue(0 != testTaskCounter.get());  // tasks are been cancelled


        assertEquals(0, testTaskSuccessfulCounter.get());
    }

    private class TestTask implements Runnable {
        private final int runTimeSecs;
        private final boolean blockInterrupt;

        public TestTask(int runTimeSecs, boolean blockInterrupt) {
            this.runTimeSecs = runTimeSecs;
            this.blockInterrupt = blockInterrupt;
        }

        public void run() {
            testTaskStartCounter.incrementAndGet();
            try {
                testTaskCounter.incrementAndGet();

                synchronized (maxConcurrentTestTasks) {
                    int activeCount = executor.getActiveCount();
                    if (maxConcurrentTestTasks.get() < activeCount) {
                        maxConcurrentTestTasks.set(activeCount);
                    }
                }

                long endTime = System.currentTimeMillis() + runTimeSecs * 1000;
                while (endTime >= System.currentTimeMillis()) {
                    try {
                        Thread.sleep(runTimeSecs * 1000);
                    } catch (InterruptedException e) {
                        if (!blockInterrupt) {
                            throw e;
                        }
                    }
                }

                testTaskCounter.decrementAndGet();
                testTaskSuccessfulCounter.incrementAndGet();
            } catch (InterruptedException e) {
                testTaskInterruptedCounter.incrementAndGet();
            }
        }
    }
}
