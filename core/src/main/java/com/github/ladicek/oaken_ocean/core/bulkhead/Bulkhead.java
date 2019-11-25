package com.github.ladicek.oaken_ocean.core.bulkhead;

import org.eclipse.microprofile.faulttolerance.exceptions.BulkheadException;

import java.util.concurrent.Callable;
import java.util.concurrent.Semaphore;

/**
 * this class is a one big TODO :)
 *
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 */
public class Bulkhead {

    private final Semaphore bulkheadSemaphore;
    private final MetricsRecorder recorder;

    public Bulkhead(int size,
                    int queueSize,
                    MetricsRecorder recorder) {
        this.bulkheadSemaphore = new Semaphore(size);
        this.recorder = recorder == null ? MetricsRecorder.NOOP : recorder;
    }

    public <V> Callable<V> callable(Callable<V> delegate) {
        if (false) { //mstodo if async exec
            recorder.bulkheadQueueEntered();
        }
        recorder.bulkheadQueueEntered(); // mstodo needed or not?
        if (bulkheadSemaphore.tryAcquire()) {
            recorder.bulkheadEntered(0L); // mstodo do it only for async execution
            long startTime = System.nanoTime();
            return new Callable<V>() {
                @Override
                public V call() throws Exception {
                    try {
                        return delegate.call();
                    } finally {
                        bulkheadSemaphore.release();
                        recorder.bulkheadLeft(System.nanoTime() - startTime);
                    }
                }
            };
        } else {
            recorder.bulkheadRejected();
            throw new BulkheadException(); // mstodo
        }
    }

    public interface MetricsRecorder {
        void bulkheadQueueEntered();
        void bulkheadEntered(long timeInQueue);
        void bulkheadRejected();
        void bulkheadLeft(long processingTime);

        MetricsRecorder NOOP = new MetricsRecorder() {
            @Override
            public void bulkheadQueueEntered() {
            }

            @Override
            public void bulkheadEntered(long timeInQueue) {
            }

            @Override
            public void bulkheadRejected() {
            }

            @Override
            public void bulkheadLeft(long processingTime) {
            }
        };
    }
}
