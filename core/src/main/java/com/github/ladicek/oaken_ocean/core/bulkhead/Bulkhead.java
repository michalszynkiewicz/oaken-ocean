package com.github.ladicek.oaken_ocean.core.bulkhead;

import org.eclipse.microprofile.faulttolerance.exceptions.BulkheadException;

import java.util.concurrent.Callable;
import java.util.concurrent.Semaphore;

/**
 * this class is a one big TODO :)
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 */
public class Bulkhead {

    private final Semaphore bulkheadSemaphore;

    public Bulkhead(Integer size) {
        this.bulkheadSemaphore = new Semaphore(size);
    }

    public <V> Callable<V> callable(Callable<V> delegate) {
        if (bulkheadSemaphore.tryAcquire()) {
            return delegate;
        } else {
            throw new BulkheadException(); // mstodo
        }
    }
}
