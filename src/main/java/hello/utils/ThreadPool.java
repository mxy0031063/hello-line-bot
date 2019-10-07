package hello.utils;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class ThreadPool {

    private static ThreadPoolExecutor poolExecutor = null;

    private static void init() {
        poolExecutor = new ThreadPoolExecutor(5,
                10,
                60,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(10),
                (thread) -> {
                    AtomicInteger count = new AtomicInteger(0);
                    Thread t = new Thread(thread);
                    t.setName(ThreadPool.class.getSimpleName()+count.addAndGet(1));
                    return t;
                });
    }

    public void destory() {
        if (poolExecutor != null) {
            poolExecutor.shutdownNow();
        }
    }

    public static ExecutorService getCustomThreadPoolExecutor() {
        if (poolExecutor == null) {
            init();
        }
        return poolExecutor;
    }

    private class CustomRejectedExecutionHandler implements RejectedExecutionHandler {
        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            System.out.println("Error Message");
        }
    }


}
