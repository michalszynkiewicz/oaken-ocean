package com.github.ladicek.oaken_ocean.core.timeout;

import com.github.ladicek.oaken_ocean.core.stopwatch.RunningStopwatch;
import com.github.ladicek.oaken_ocean.core.stopwatch.Stopwatch;
import com.github.ladicek.oaken_ocean.core.stopwatch.SystemStopwatch;
import com.github.ladicek.oaken_ocean.core.util.TestException;
import org.assertj.core.data.Percentage;
import org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.github.ladicek.oaken_ocean.core.util.CompletionStages.completedStage;
import static com.github.ladicek.oaken_ocean.core.util.CompletionStages.failedStage;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.data.Percentage.withPercentage;

public class RealWorldCompletionStageTimeoutTest {
    private static final Percentage tolerance = withPercentage(10);

    private ScheduledExecutorService executor;
    private ScheduledExecutorTimeoutWatcher watcher;
    private ExecutorService taskExecutor;

    private Stopwatch stopwatch = new SystemStopwatch();

    @Before
    public void setUp() {
        executor = Executors.newSingleThreadScheduledExecutor();
        watcher = new ScheduledExecutorTimeoutWatcher(executor);

        taskExecutor = Executors.newSingleThreadExecutor();
    }

    @After
    public void tearDown() throws InterruptedException {
        executor.shutdownNow();
        taskExecutor.shutdownNow();

        executor.awaitTermination(1, TimeUnit.SECONDS);
        taskExecutor.awaitTermination(1, TimeUnit.SECONDS);
    }

    @Test
    public void shouldReturnRightAway() throws Exception {
        RunningStopwatch runningStopwatch = stopwatch.start();

        Callable<CompletionStage<String>> timeout = new CompletionStageTimeout<>(() -> {
            Thread.sleep(100);
            return completedStage("foobar");
        }, "completion stage timeout", 1000, watcher, taskExecutor);

        assertThat(timeout.call().toCompletableFuture().get()).isEqualTo("foobar");
        assertThat(runningStopwatch.elapsedTimeInMillis()).isCloseTo(100, tolerance);
    }

    @Test
    public void shouldPropagateMethodError() throws Exception {
        RunningStopwatch runningStopwatch = stopwatch.start();

        Callable<CompletionStage<String>> timeout = new CompletionStageTimeout<>(() -> {
            Thread.sleep(100);
            throw new TestException();
        }, "completion stage timeout", 1000, watcher, taskExecutor);

        assertThatThrownBy(timeout.call().toCompletableFuture()::get)
                .isExactlyInstanceOf(ExecutionException.class)
                .hasCauseExactlyInstanceOf(TestException.class);
        assertThat(runningStopwatch.elapsedTimeInMillis()).isCloseTo(100, tolerance);
    }

    @Test
    public void shouldPropagateCompletionStageError() throws Exception {
        RunningStopwatch runningStopwatch = stopwatch.start();

        Callable<CompletionStage<String>> timeout = new CompletionStageTimeout<>(() -> {
            Thread.sleep(100);
            return failedStage(new TestException());
        }, "completion stage timeout", 1000, watcher, taskExecutor);

        assertThatThrownBy(timeout.call().toCompletableFuture()::get)
                .isExactlyInstanceOf(ExecutionException.class)
                .hasCauseExactlyInstanceOf(TestException.class);
        assertThat(runningStopwatch.elapsedTimeInMillis()).isCloseTo(100, tolerance);
    }

    @Test
    public void shouldTimeOut() throws Exception {
        RunningStopwatch runningStopwatch = stopwatch.start();

        Callable<CompletionStage<String>> timeout = new CompletionStageTimeout<>(() -> {
            Thread.sleep(200);
            return completedStage("foobar");
        }, "completion stage timeout", 100, watcher, taskExecutor);

        assertThatThrownBy(timeout.call().toCompletableFuture()::get)
                .isExactlyInstanceOf(ExecutionException.class)
                .hasCauseExactlyInstanceOf(TimeoutException.class);
        assertThat(runningStopwatch.elapsedTimeInMillis()).isCloseTo(100, tolerance);
    }
}
