package game.auction;

import tools.IdApplicator;
import tools.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 前提：
 * <p>1.涉及到玩家自身的信息都是单线程的</p>
 */

public class AuctionManager {
    // 存放所有竞拍品
    Map<Long, AuctionItem> auctionMap = new ConcurrentHashMap<>();

    /**
     * 创建竞拍品
     */
    public void createAuction(HumanObject humanObj, double initialPrice, boolean canFixed, double fixedPrice, String item) {
        if (!canCreateAuction(humanObj, initialPrice, canFixed, fixedPrice, item)) {
            return;
        }

        AuctionItem auctionItem = AuctionItem.createAuctionItem(IdApplicator.applyId(),
                humanObj.getId(),
                initialPrice,
                canFixed,
                fixedPrice,
                item);

        auctionMap.put(auctionItem.getId(), auctionItem);
    }

    /**
     * 能否创建
     */
    public boolean canCreateAuction(HumanObject humanObj, double initialPrice, boolean canFixed, double fixedPrice, String item) {
        // 基础信息检验，不涉及玩家信息的，无需考虑并发问题
        if (canFixed && fixedPrice <= initialPrice) {
            Log.error("初始价格大于等于了一口价价格,初始价格={}", initialPrice);
            return false;
        }
        // ... ...

        // 物品校验
        if (!humanObj.hasItem(item, 1)) {
            Log.error("玩家未拥有物品，无法创建竞拍，item={}", item);
            return false;
        }

        return true;
    }

    /**
     * 玩家竞价
     */
    public void bid() {

    }

    /**
     * 能否竞价
     */
    public boolean canBid() {
        
    }

    /**
     * 竞拍结算
     */
    private void settlementAuction(long auctionId) {

    }

    /**
     * 竞拍品过期
     */
    public boolean isExpired(long auctionId) {

    }

    /**
     * 获取所有竞拍品
     */
    public List<AuctionItem> getAllAuction() {
        return new ArrayList<>();
    }

    /**
     * 刷新列表
     */
    public void refresh(HumanObject humanObj) {
        List<AuctionItem> allAuction = getAllAuction();

        // 发送给客户端
        humanObj.sendMsg(allAuction);
    }
}
