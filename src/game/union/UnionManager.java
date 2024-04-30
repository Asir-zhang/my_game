package game.union;

import game.auction.HumanObject;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class UnionManager {
    // 所有帮会
    Map<Long, Union> uniomMap = new ConcurrentHashMap<>();

    /**
     * 申请加入帮会
     * */
    public void join(HumanObject humanObj, long unionId) {
        Union union = uniomMap.get(unionId);
        // 帮会不存在
        if (union == null) {
            return;
        }

        // 已经有同样的申请了
        if (union.joinRequests.containsKey(humanObj.getId())) {
            return;
        }

        // 已经是帮会成员了
        if (union.members.containsKey(humanObj.getId())) {
            return;
        }

        union.joinRequests.put(humanObj.getId(), new JoinRequest(humanObj.getId()));
    }

    /**
     * 处理审批
     * */
    public void handleApprove(HumanObject humanObject, MemberRole role, Union union, long playerId, boolean approve) {
        // 权限校验
        if (!checkRight(humanObject)) {
            return;
        }

        // 更多校验

        JoinRequest joinRequest = union.joinRequests.get(playerId);
        if (joinRequest == null) {
            humanObject.sendMsg("审批已处理完成");
            return;
        }

        // 通过playerId拿到对应的玩家实体
        HumanObject player = new HumanObject();

        // 玩家已经有了公会
        if (player.hasUnion()) {
            return;
        }

        // 锁住公会，保证同一个公会内部不会重复审批
        union.lock.lock();
        // 锁住玩家，保证只能加入到一个公会
        player.lock.lock();
        try {
            if (!union.joinRequests.containsKey(playerId)) {
                humanObject.sendMsg("审批已处理完成");
                return;
            }

            if (player.hasUnion()) {
                return;
            }

            if (approve) {
                union.members.put(playerId, new Member(joinRequest.player, role));
                union.joinRequests.remove(playerId);
                // 设置玩家所属帮会
                player.setUnion(union.unionId);
                // 发送审批结果给对应玩家
            } else {
                // 直接拒绝
                union.joinRequests.remove(playerId);
                // 发送审批结果给对应玩家
            }
        } finally {
            player.lock.unlock();
            union.lock.unlock();
        }
    }

    /**
     * 踢出玩家
     * */
    public void kickPlayer(HumanObject humanObj, long playerId, Union union) {
        // HumanObj权限检验
        if (!checkRight(humanObj)) {
            return;
        }

        if (!union.members.containsKey(playerId)) {
            return;
        }

        union.members.remove(playerId);
    }

    public boolean checkRight(HumanObject humanObj) {
        return false;
    }
}
