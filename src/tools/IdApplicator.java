package tools;

import java.util.concurrent.atomic.AtomicLong;

public class IdApplicator {
    static AtomicLong curBaseId = new AtomicLong(1000L);

    public static long applyId() {
        return curBaseId.incrementAndGet();
    }
}
