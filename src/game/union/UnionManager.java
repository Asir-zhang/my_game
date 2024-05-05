package game.union;

import game.auction.HumanObject;
import tools.ReasonResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;


/**
 * 一个极其简单的帮会实现
 * <p>只写了最核心的申请、审批功能</p>
 * */
public class UnionManager {
    // 所有帮会
    Map<Long, Union> uniomMap = new ConcurrentHashMap<>();

    /**
     * 获取帮会
     * */
    public Union getUnion(HumanObject humanObj) {
        return uniomMap.get(humanObj.unionId);
    }

    /**
     * 申请加入帮会
     * */
    public ReasonResult join(HumanObject humanObj, long unionId) {
        // 自己已经有帮会了
        if (humanObj.hasUnion()) {
            return ReasonResult.failure("玩家当前已有帮会");
        }

        Union union = uniomMap.get(unionId);
        // 帮会不存在
        if (union == null) {
            return ReasonResult.failure("帮会不存在");
        }

        // 已经有同样的申请了
        if (union.joinRequests.containsKey(humanObj.getId())) {
            return ReasonResult.failure("已经申请，请勿重复操作");
        }

        // 否则将请求加入到帮会的请求列表中
        union.joinRequests.put(humanObj.getId(), new JoinRequest(humanObj.getId()));

        return ReasonResult.SUCCESS;
    }

    /**
     * 处理审批
     * @param humanObj 审批人
     * @param playerId 被审批人
     * @param approve 是否通过
     * */
    public ReasonResult handleApprove(HumanObject humanObj, long playerId, boolean approve) {
        // 权限校验
        if (!checkRight(humanObj)) {
            return ReasonResult.failure("权限不足");
        }

        if (!approve) {
            // 直接拒绝
            return ReasonResult.failure("申请被拒绝");
        }

        Union union = getUnion(humanObj);

        // 更多校验

        // 通过remove原子操作移除申请，保证同一时刻只有一个线程能够审批，不会出现重复审批情况
        JoinRequest joinRequest = union.joinRequests.remove(playerId);
        if (joinRequest == null) {
            return ReasonResult.failure("不存在该申请或者审批已经完成");
        }

        // 通过playerId拿到对应的玩家实体
        HumanObject player = new HumanObject();

        // 玩家已经有了帮会
        if (player.hasUnion()) {
            return ReasonResult.failure("玩家已有帮会");
        }

        // 锁住玩家，保证只能加入到一个公会
        player.lock.lock();
        try {
            if (player.hasUnion()) {
                return ReasonResult.failure("玩家已有帮会");
            }

            // 默认就是Member
            union.members.putIfAbsent(playerId, new Member(joinRequest.player, MemberRole.MEMBER));
            // 设置玩家所属帮会
            player.setUnion(union.unionId);
            // 发送审批结果给对应玩家
        } finally {
            player.lock.unlock();
        }

        return ReasonResult.SUCCESS;
    }

    /**
     * 批量审批
     * <p>只能同时失败或者同时成功</p>
     *@param humanObj 审批人
     *@param playerIds 被审批人
     *@param approve 是否通过
     * */
    public ReasonResult handleApprove(HumanObject humanObj, List<Long> playerIds, boolean approve) {
        // 权限校验
        if (!checkRight(humanObj)) {
            return ReasonResult.failure("权限不足");
        }

        if (!approve) {
            // 直接拒绝
            return ReasonResult.failure("申请被拒绝");
        }

        // 模拟玩家集合
        List<HumanObject> players = playerIds.stream()
                .sorted()
                .map(_ -> new HumanObject())
                .toList();

        Union union = getUnion(humanObj);

        // pre判断，尽量避免没有必要的加锁
        ReasonResult preCheck = approvePreCheck(players);
        if (!preCheck.result) {
            return preCheck;
        }

        List<JoinRequest> requests = new ArrayList<>();
        // 先获取union锁，因为需要批量拿取joinRequests
        union.lock.lock();
        try {
            // 如果不存在某个申请直接返回失败(当然也可以忽略掉,不过也需要同步忽略掉players中的玩家)
            for (long playerId : playerIds) {
                JoinRequest joinRequest = union.joinRequests.remove(playerId);
                if (joinRequest == null) {
                    return ReasonResult.failure("存在无效的申请");
                }
                requests.add(joinRequest);
            }
        } finally {
            union.lock.unlock();
        }

        // 一次性按序锁住所有玩家
        try {
            for (int i = 0;i < players.size(); i++) {
                HumanObject player = players.get(i);
                // 尝试获取5s的锁，如果失败，则有可能遇到死锁
                boolean b = player.lock.tryLock(5, TimeUnit.SECONDS);
                if (!b) {
                    // 先主动把之前的释放出来
                    for (int j = 0; j < i; j++) {
                        players.get(j).lock.unlock();
                    }
                    // 然后i=-1。，从头开始获取锁
                    i = -1;
                    // 等待一会再试
                    Thread.sleep(1000);
                }
            }
        } catch (InterruptedException e) {
            // 表示被中断，那么joinRequest处理失败，需要把之前的请求加回去
            // 当然也有个问题是上面的remove和这里的put是存在时间窗口的
            // union.joinRequests中可能已经重复请求了，但在正常的时间逻辑中可以被直接覆盖掉
            for (JoinRequest joinRequest : requests) {
                union.joinRequests.put(joinRequest.player, joinRequest);
            }
            return ReasonResult.failure("操作失败");
        }

        // 执行审批逻辑
        try {
            // 再次检查
            preCheck = approvePreCheck(players);
            if (!preCheck.result) {
                return preCheck;
            }

            // 一次性加入
            for (int i = 0; i < players.size(); i++) {
                // 简化获取
                HumanObject player = players.get(i);
                JoinRequest joinRequest = requests.get(i);
                long playerId = playerIds.get(i);

                // 默认就是Member
                union.members.putIfAbsent(playerId, new Member(joinRequest.player, MemberRole.MEMBER));
                // 设置玩家所属帮会
                player.setUnion(union.unionId);
                // 发送审批结果给对应玩家
            }
        } finally {
            for (HumanObject player : players) {
                player.unionLock.unlock();
            }
        }

        return ReasonResult.SUCCESS;
    }

    private ReasonResult approvePreCheck(List<HumanObject> players) {
        // pre判断，尽量避免没有必要的加锁
        for (HumanObject player : players) {
            if (player.hasUnion()) {
                return ReasonResult.failure("组队申请中有玩家已有帮会");
            }
        }
        return ReasonResult.SUCCESS;
    }

    /**
     * 踢出玩家
     * */
    public void kickPlayer(HumanObject humanObj, long playerId) {
        // 权限检验
        if (!checkRight(humanObj)) {
            return;
        }

        Union union = getUnion(humanObj);

        Member member = union.members.get(playerId);
        if (member == null) {
            return;
        }

        // 锁住Member, 确保踢出的时候这个成员不再进行其他活动
        member.lock.lock();

        HumanObject player = new HumanObject();
        player.lock.lock();
        try {
            // 从帮会中移除
            union.members.remove(playerId);
            // 可能出现类似ABA的问题，想想概率奇低
            if (player.hasUnion()) {
                player.setUnion(0);
            }
        } finally {
            member.lock.unlock();
        }
    }

    /**
     * 权限检验
     * */
    public boolean checkRight(HumanObject humanObj) {
        return humanObj.hasUnion();
    }
}
