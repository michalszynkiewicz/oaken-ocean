package com.github.ladicek.oaken_ocean.core.fallback;

import com.github.ladicek.oaken_ocean.core.FaultToleranceStrategy;

import java.util.concurrent.Callable;

public class Fallback<V> implements FaultToleranceStrategy<V> {
    final FaultToleranceStrategy<V> delegate;
    final String description;

    final FallbackFunction<V> fallback;
    final MetricsRecorder metricsRecorder;

    public Fallback(FaultToleranceStrategy<V> delegate, String description, FallbackFunction<V> fallback,
                    MetricsRecorder metricsRecorder) {
        this.delegate = delegate;
        this.description = description;
        this.fallback = fallback;
        this.metricsRecorder = metricsRecorder == null ? MetricsRecorder.NO_OP : metricsRecorder;
    }

    @Override
    public V apply(Callable<V> target) throws Exception {
        Throwable failure;
        try {
            return delegate.apply(target);
        } catch (Exception e) {
            failure = e;
        }

        if (failure instanceof InterruptedException || Thread.interrupted()) {
            throw new InterruptedException();
        }

        metricsRecorder.fallbackCalled();
        return fallback.call(failure);
    }

    public interface MetricsRecorder {
        void fallbackCalled();

        MetricsRecorder NO_OP = () -> {
        };
    }
}
