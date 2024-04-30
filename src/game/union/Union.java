package game.union;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

public class Union {
    long unionId;
    // 帮会成员列表
    public Map<Long, Member> members = new ConcurrentHashMap<>();
    // 加入帮会的申请列表
    public Map<Long, JoinRequest> joinRequests = new ConcurrentHashMap<>();

    public ReentrantLock lock = new ReentrantLock();

    /**
     * 创建帮会
     * */
    public Union() {
        // 初始化
    }
}