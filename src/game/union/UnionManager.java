package game.union;

import game.auction.HumanObject;
import tools.ReasonResult;

import java.util.ArrayList;
import java.util.Comparator;
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

        Union union = getUnion(humanObj);
        // 所有的申请
        List<JoinRequest> applications = new ArrayList<>();

        // 对Union加锁，保证能正确同时取出所有的申请
        union.lock.lock();
        try {
            for (long playerId : playerIds) {
                JoinRequest joinRequest = union.joinRequests.get(playerId);
                if (joinRequest == null || joinRequest.getApproveStatus() == JoinRequest.ApproveStatus.HANDLED) {
                    return ReasonResult.failure("存在过时的申请，请刷新后重试");
                }
                if (joinRequest.getApproveStatus() == JoinRequest.ApproveStatus.HANDLING) {
                    return ReasonResult.failure("请刷新后重试");
                }
            }

            // 获取所有请求，同时设置为“审批中”
            for (long playerId : playerIds) {
                JoinRequest joinRequest = union.joinRequests.get(playerId);
                joinRequest.setApproveStatus(JoinRequest.ApproveStatus.HANDLING);
                applications.add(joinRequest);
            }

            //--------------------------------- 拒绝审批 ---------------------------------
            if (!approve) {
                // 直接拒绝
                // 那么移除所有的申请，由于当前为正在审批状态，所以不会有别的线程修改，直接移除即可
                for (long playerId : playerIds) {
                    union.joinRequests.remove(playerId);
                }

                // 向玩家发送拒绝消息，不在线就不处理了 TODO

                return ReasonResult.SUCCESS;
            }


            //--------------------------------- 通过审批 ---------------------------------
            // 模拟玩家集合，先排个序，保证取锁顺序
            List<HumanObject> players = playerIds.stream()
                    .sorted()
                    .map(_ -> new HumanObject())
                    .toList();

            // pre判断，这里简单只判断玩家当前有无帮会
            for (HumanObject player : players) {
                if (player.hasUnion()) {
                    // 先将所有的申请设置为未处理
                    for (JoinRequest application : applications) {
                        application.setApproveStatus(JoinRequest.ApproveStatus.UNHANDLED);
                    }
                    // 将此申请移除
                    union.joinRequests.remove(player.getId());
                    // 返回失败
                    return ReasonResult.failure("组队申请中有玩家已有帮会");
                }
            }

            // 一次性按序锁住所有玩家
            for (HumanObject player : players) {
                player.unionLock.lock();
            }

            // 执行审批逻辑
            try {
                // 再次检查
                for (HumanObject player : players) {
                    if (player.hasUnion()) {
                        // 先将所有的申请设置为未处理
                        for (JoinRequest application : applications) {
                            application.setApproveStatus(JoinRequest.ApproveStatus.UNHANDLED);
                        }
                        // 将此申请移除
                        union.joinRequests.remove(player.getId());
                        // 返回失败
                        return ReasonResult.failure("组队申请中有玩家已有帮会");
                    }
                }

                // 一次性加入
                for (int i = 0; i < players.size(); i++) {
                    // 简化获取
                    HumanObject player = players.get(i);
                    JoinRequest joinRequest = applications.get(i);
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
        } catch (Exception e) {
            // 有异常，那么就把所有申请设置为未处理
            for (JoinRequest application : applications) {
                application.setApproveStatus(JoinRequest.ApproveStatus.UNHANDLED);
            }

            // 返回操作失败
            return ReasonResult.failure("未知原因，请重试");
        } finally {
            // 帮会释放锁
            union.lock.unlock();


        }
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
