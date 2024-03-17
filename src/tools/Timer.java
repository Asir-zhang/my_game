package tools;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Timer {
    private final Runnable task;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(10);

    public Timer(Runnable task) {
        this.task = task;
    }

    // 方法以指定间隔运行任务
    public Timer runWithFixedDelay(long delay, TimeUnit unit) {
        scheduler.scheduleWithFixedDelay(task, 0, delay, unit);
        return this;
    }
}


