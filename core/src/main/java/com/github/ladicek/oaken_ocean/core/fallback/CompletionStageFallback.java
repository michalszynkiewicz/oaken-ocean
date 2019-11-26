package com.github.ladicek.oaken_ocean.core.fallback;

import com.github.ladicek.oaken_ocean.core.FaultToleranceStrategy;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;

public class CompletionStageFallback<V> extends Fallback<CompletionStage<V>> {
    private final Executor executor;

    public CompletionStageFallback(FaultToleranceStrategy<CompletionStage<V>> delegate, String description,
                                   FallbackFunction<CompletionStage<V>> fallback, Executor executor,
                                   MetricsRecorder metricsRecorder) {
        super(delegate, description, fallback, metricsRecorder);
        this.executor = executor;
    }

    @Override
    public CompletionStage<V> apply(Callable<CompletionStage<V>> target) throws Exception {
        CompletableFuture<V> result = new CompletableFuture<>();

        executor.execute(() -> {
            CompletionStage<V> originalResult;
            try {
                originalResult = delegate.apply(target);
            } catch (Exception e) {
                CompletableFuture<V> failure = new CompletableFuture<>();
                failure.completeExceptionally(e);
                originalResult = failure;
            }

            originalResult.whenComplete((value, exception) -> {
                if (value != null) {
                    result.complete(value);
                    return;
                }

                if (exception instanceof InterruptedException || Thread.interrupted()) {
                    result.completeExceptionally(new InterruptedException());
                    return;
                }

                try {
                    fallback.call(exception).whenComplete((fallbackValue, fallbackException) -> {
                        if (fallbackValue != null) {
                            result.complete(fallbackValue);
                        } else {
                            result.completeExceptionally(fallbackException);
                        }
                    });
                } catch (Exception e) {
                    result.completeExceptionally(e);
                }
            });
        });

        return result;
    }
}
