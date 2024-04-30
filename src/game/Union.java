package game;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.ReentrantLock;
import java.util.*;

class Union {
    private String unionName; // 帮会的名称
    private Map<String, Member> members = new ConcurrentHashMap<>(); // 帮会成员列表
    private ConcurrentLinkedQueue<JoinRequest> joinRequests = new ConcurrentLinkedQueue<>(); // 加入帮会的申请列表
    private ReentrantLock lock = new ReentrantLock(); // 用于控制并发的锁

    // 帮主创建帮会
    public boolean createUnion(String founderName) {
        lock.lock();
        try {
            // 如果已存在成员，则不允许重新创建帮会
            if (!members.isEmpty()) {
                return false;
            }
            Member founder = new Member(founderName, MemberRole.ADMIN); // 创建帮会创始人，赋予管理员权限
            members.put(founderName, founder);
            return true;
        } finally {
            lock.unlock(); // 释放锁
        }
    }

    // 添加成员的方法，指定玩家名称和角色
    public void addMember(String playerName, String role) {
        lock.lock(); // 加锁
        try {
            members.put(playerName, new Member(playerName, MemberRole.valueOf(role))); // 将新成员添加到成员列表中
        } finally {
            lock.unlock(); // 释放锁
        }
    }

    // 提交加入帮会的申请
    public void submitJoinRequest(String playerName) {
        lock.lock(); // 加锁
        try {
            joinRequests.add(new JoinRequest(playerName)); // 在申请列表中添加新的加入申请
        } finally {
            lock.unlock(); // 释放锁
        }
    }

    // 处理申请，需要管理员权限
    public boolean processJoinRequest(String adminName, String playerName, boolean approve) {
        lock.lock(); // 加锁
        try {
            if (!members.containsKey(adminName) || members.get(adminName).role != MemberRole.ADMIN) {
                return false;  // 如果操作者不是管理员，返回失败
            }

            Optional<JoinRequest> request = joinRequests.stream()
                    .filter(r -> r.playerName.equals(playerName))
                    .findFirst(); // 查找对应玩家的申请

            if (request.isPresent() && approve) {
                members.put(playerName, new Member(playerName, MemberRole.MEMBER)); // 如果批准申请，将玩家添加为成员
                joinRequests.remove(request.get()); // 从申请列表中移除这条申请
                return true;
            }
            return false;
        } finally {
            lock.unlock(); // 释放锁
        }
    }

    // 帮会成员
    private class Member {
        String name; // 成员名
        MemberRole role; // 成员角色

        Member(String name, MemberRole role) {
            this.name = name;
            this.role = role;
        }
    }

    // 加入帮会的申请
    private class JoinRequest {
        String playerName; // 申请加入的玩家名

        JoinRequest(String playerName) {
            this.playerName = playerName;
        }
    }

    // 成员角色
    private enum MemberRole {
        MEMBER, ADMIN
    }
}
