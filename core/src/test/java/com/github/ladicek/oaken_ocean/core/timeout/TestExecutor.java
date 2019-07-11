package com.github.ladicek.oaken_ocean.core.timeout;

import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Can only be used once; subsequent usages will throw an exception during {@code execute}.
 */
public class TestExecutor implements Executor {
    private final AtomicBoolean alreadyUsed = new AtomicBoolean(false);

    private volatile Thread executingThread;

    @Override
    public void execute(Runnable command) {
        if (alreadyUsed.compareAndSet(false, true)) {
            executingThread = new Thread(command, "TestExecutor thread");
            executingThread.start();
        } else {
            throw new IllegalStateException("TestExecutor cannot be reused");
        }
    }

    public void interruptExecutingThread() {
        executingThread.interrupt();
    }
}
