package com.github.ladicek.oaken_ocean.core.circuit.breaker;

import com.github.ladicek.oaken_ocean.core.stopwatch.TestStopwatch;
import com.github.ladicek.oaken_ocean.core.util.SetOfThrowables;
import com.github.ladicek.oaken_ocean.core.util.TestException;
import org.eclipse.microprofile.faulttolerance.exceptions.CircuitBreakerOpenException;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class CircuitBreakerTest {
    private static final SetOfThrowables exception = SetOfThrowables.create(Collections.singletonList(Exception.class));
    private static final SetOfThrowables testException = SetOfThrowables.create(Collections.singletonList(TestException.class));

    private TestStopwatch stopwatch;

    @Before
    public void setUp() {
        stopwatch = new TestStopwatch();
    }

    @Test
    public void test1() throws Exception {
        TestAction<String> action = TestAction.create(
                () -> "foobar1",
                () -> "foobar2",
                () -> { throw new RuntimeException(); },
                () -> "foobar3",
                () -> "foobar4",
                () -> "foobar5",
                TestException::doThrow,
                () -> "foobar6",
                TestException::doThrow,
                () -> "foobar7",
                TestException::doThrow,
                () -> "foobar8",
                () -> "foobar9",
                () -> "foobar10"
        );
        CircuitBreaker cb = new CircuitBreaker("test action", testException,
                1000, 4, 0.5, 2, stopwatch, null);

        // circuit breaker is closed
        assertThat(cb.callable(action).call()).isEqualTo("foobar1");
        assertThat(cb.callable(action).call()).isEqualTo("foobar2");
        assertThatThrownBy(() -> cb.callable(action).call()).isExactlyInstanceOf(RuntimeException.class); // treated as success
        assertThat(cb.callable(action).call()).isEqualTo("foobar3");
        assertThat(cb.callable(action).call()).isEqualTo("foobar4");
        assertThat(cb.callable(action).call()).isEqualTo("foobar5");
        assertThatThrownBy(() -> cb.callable(action).call()).isExactlyInstanceOf(TestException.class);
        assertThat(cb.callable(action).call()).isEqualTo("foobar6");
        assertThatThrownBy(() -> cb.callable(action).call()).isExactlyInstanceOf(TestException.class);
        // circuit breaker is open
        assertThatThrownBy(() -> cb.callable(action).call()).isExactlyInstanceOf(CircuitBreakerOpenException.class);
        assertThatThrownBy(() -> cb.callable(action).call()).isExactlyInstanceOf(CircuitBreakerOpenException.class);
        assertThatThrownBy(() -> cb.callable(action).call()).isExactlyInstanceOf(CircuitBreakerOpenException.class);
        assertThatThrownBy(() -> cb.callable(action).call()).isExactlyInstanceOf(CircuitBreakerOpenException.class);
        assertThatThrownBy(() -> cb.callable(action).call()).isExactlyInstanceOf(CircuitBreakerOpenException.class);
        stopwatch.setCurrentValue(1500);
        assertThat(cb.callable(action).call()).isEqualTo("foobar7");
        // circuit breaker is half-open
        assertThatThrownBy(() -> cb.callable(action).call()).isExactlyInstanceOf(TestException.class);
        // circuit breaker is open
        stopwatch.setCurrentValue(0);
        assertThatThrownBy(() -> cb.callable(action).call()).isExactlyInstanceOf(CircuitBreakerOpenException.class);
        assertThatThrownBy(() -> cb.callable(action).call()).isExactlyInstanceOf(CircuitBreakerOpenException.class);
        assertThatThrownBy(() -> cb.callable(action).call()).isExactlyInstanceOf(CircuitBreakerOpenException.class);
        stopwatch.setCurrentValue(1500);
        assertThat(cb.callable(action).call()).isEqualTo("foobar8");
        // circuit breaker is half-open
        assertThat(cb.callable(action).call()).isEqualTo("foobar9");
        // circuit breaker is closed
        assertThat(cb.callable(action).call()).isEqualTo("foobar10");
    }

    @Test
    public void test2() throws Exception {
        TestAction<String> action = TestAction.create(
                () -> "foobar1",
                TestException::doThrow,
                TestException::doThrow,
                () -> "foobar2",
                // open
                () -> "foobar3",
                () -> "foobar4",
                () -> "foobar5"
        );
        CircuitBreaker cb = new CircuitBreaker("test action", testException,
                1000, 4, 0.5, 2, stopwatch, null);


        // circuit breaker is closed
        assertThat(cb.callable(action).call()).isEqualTo("foobar1");
        assertThatThrownBy(() -> cb.callable(action).call()).isExactlyInstanceOf(TestException.class);
        assertThatThrownBy(() -> cb.callable(action).call()).isExactlyInstanceOf(TestException.class);
        assertThat(cb.callable(action).call()).isEqualTo("foobar2");
        // circuit breaker is open
        assertThatThrownBy(() -> cb.callable(action).call()).isExactlyInstanceOf(CircuitBreakerOpenException.class);
        assertThatThrownBy(() -> cb.callable(action).call()).isExactlyInstanceOf(CircuitBreakerOpenException.class);
        assertThatThrownBy(() -> cb.callable(action).call()).isExactlyInstanceOf(CircuitBreakerOpenException.class);
        assertThatThrownBy(() -> cb.callable(action).call()).isExactlyInstanceOf(CircuitBreakerOpenException.class);
        assertThatThrownBy(() -> cb.callable(action).call()).isExactlyInstanceOf(CircuitBreakerOpenException.class);
        stopwatch.setCurrentValue(1500);
        assertThat(cb.callable(action).call()).isEqualTo("foobar3");
        // circuit breaker is half-open
        assertThat(cb.callable(action).call()).isEqualTo("foobar4");
        // circuit breaker is closed
        assertThat(cb.callable(action).call()).isEqualTo("foobar5");
    }
}
