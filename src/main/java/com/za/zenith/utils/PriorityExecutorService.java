package com.za.zenith.utils;

import java.util.concurrent.*;
import java.util.Comparator;

public class PriorityExecutorService extends ThreadPoolExecutor {
    public PriorityExecutorService(int nThreads, ThreadFactory threadFactory) {
        super(nThreads, nThreads, 0L, TimeUnit.MILLISECONDS,
              new PriorityBlockingQueue<>(11, new PriorityTaskComparator()),
              threadFactory);
    }

    private static class PriorityTaskComparator implements Comparator<Runnable> {
        @Override
        public int compare(Runnable r1, Runnable r2) {
            if (r1 instanceof PriorityFutureTask && r2 instanceof PriorityFutureTask) {
                int p1 = ((PriorityFutureTask<?>) r1).getPriority();
                int p2 = ((PriorityFutureTask<?>) r2).getPriority();
                return Integer.compare(p1, p2); // Lower number = Higher priority
            }
            return 0;
        }
    }

    @Override
    protected <T> RunnableFuture<T> newTaskFor(Callable<T> callable) {
        if (callable instanceof PrioritizedCallable) {
            return new PriorityFutureTask<>(callable, ((PrioritizedCallable<?>) callable).getPriority());
        }
        return super.newTaskFor(callable);
    }

    @Override
    protected <T> RunnableFuture<T> newTaskFor(Runnable runnable, T value) {
        if (runnable instanceof PrioritizedRunnable) {
            return new PriorityFutureTask<>(runnable, value, ((PrioritizedRunnable) runnable).getPriority());
        }
        return super.newTaskFor(runnable, value);
    }

    public static class PriorityFutureTask<V> extends FutureTask<V> {
        private final int priority;

        public PriorityFutureTask(Callable<V> callable, int priority) {
            super(callable);
            this.priority = priority;
        }

        public PriorityFutureTask(Runnable runnable, V result, int priority) {
            super(runnable, result);
            this.priority = priority;
        }

        public int getPriority() {
            return priority;
        }
    }

    public interface PrioritizedCallable<V> extends Callable<V> {
        int getPriority();
    }

    public interface PrioritizedRunnable extends Runnable {
        int getPriority();
    }
}
