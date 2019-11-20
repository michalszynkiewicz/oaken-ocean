package com.github.ladicek.oaken_ocean.core.integration;

import com.github.ladicek.oaken_ocean.core.circuit.breaker.CircuitBreaker;
import com.github.ladicek.oaken_ocean.core.circuit.breaker.CircuitBreakerListener;
import com.github.ladicek.oaken_ocean.core.fallback.Fallback;
import com.github.ladicek.oaken_ocean.core.retry.Delay;
import com.github.ladicek.oaken_ocean.core.retry.Retry;
import com.github.ladicek.oaken_ocean.core.stopwatch.TestStopwatch;
import com.github.ladicek.oaken_ocean.core.util.SetOfThrowables;

import java.util.Collections;
import java.util.concurrent.Callable;

/**
 * Factory methods for fault tolerance strategies that are easier to use than their constructors.
 * This is to be used for testing strategies integration, where one doesn't have to verify all possible
 * behaviors of the strategy (they are covered by unit tests of individual strategies).
 */
final class Strategies {
    static Fallback<String> fallback(Callable<String> delegate) {
        return new Fallback<>(delegate, "fallback", e -> "fallback after [" + e.getMessage() + "]");
    }

    static <V> Retry<V> retry(Callable<V> delegate) {
        return new Retry<>(delegate, "retry",
                SetOfThrowables.withoutCustomThrowables(Collections.singletonList(Exception.class)),
                SetOfThrowables.EMPTY, 10, 0, Delay.NONE, new TestStopwatch());
    }

    static CircuitBreaker circuitBreaker(CircuitBreakerListener listener) {
        return circuitBreaker(0, listener);
    }

    static CircuitBreaker circuitBreaker(int delayInMillis, CircuitBreakerListener listener) {
        CircuitBreaker result = new CircuitBreaker("circuit breaker", SetOfThrowables.ALL,
              delayInMillis, 5, 0.2, 3, new TestStopwatch());
        result.addListener(listener);
        return result;
    }
}
