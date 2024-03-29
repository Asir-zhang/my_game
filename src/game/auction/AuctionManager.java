package game.auction;

import game.money.MoneyManager;
import tools.IdApplicator;
import tools.Log;
import tools.TimeTools;
import tools.Timer;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 前提：
 * <p>1.涉及到玩家自身的信息都是单线程的</p>
 */

public class AuctionManager {
    // 2min过期
    public static final int EXPIRE_TIME = 2 * 60 * 1000;

    // 这里应该是注入器注入
    MoneyManager moneyManager = new MoneyManager();

    // 存放所有竞拍品
    Map<Long, AuctionItem> auctionMap = new ConcurrentHashMap<>();

    private final Timer timer = new Timer(this::AuctionTimerTask);

    public void init() {
        timer.runWithFixedDelay(2, TimeUnit.MINUTES);   //  定时任务执行
    }

    public void AuctionTimerTask() {
        // 遍历检查所有竞拍品是否可以开启结算
        for (AuctionItem item : auctionMap.values()) {
            if (isExpired(item)) {
                // 过期了
                item.lock.lock();
                try {
                    // 设置为已经完成
                    item.setFinished(true);
                    // 开启结算
                    settlementAuction(item);
                } finally {
                    item.lock.unlock();
                }
            }
        }

    }

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

        // 日志输出
        Log.info("玩家{}创建竞拍品={},初始价为{}", humanObj.getId(), item, initialPrice);
    }

    /**
     * 能否创建
     */
    public boolean canCreateAuction(HumanObject humanObj, double initialPrice, boolean canFixed, double fixedPrice, String item) {
        // 基础信息检验
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
    public void bid(HumanObject humanObj, long auctionId, boolean fixed, double price) {
        AuctionItem auctionItem = auctionMap.get(auctionId);
        if (auctionItem == null) {
            Log.error("不存在此竞拍品, auctionId={}", auctionId);
            return;
        }

        auctionItem.lock.lock();
        try {
            // 校验
            if (price <= auctionItem.getCurrentPrice()) {
                Log.error("出价过低, price={}", price);
                return;
            }

            if (humanObj.getId() == auctionItem.getCreatorId()) {
                Log.error("自己不能参与竞拍");
                return;
            }

            long now = TimeTools.getCurTime();
            if (auctionItem.isFinished() || isExpired(auctionItem)) {
                Log.error("该商品竞拍已经结束");
                return;
            }

            if (fixed && price < auctionItem.getFixedPrice()) {
                Log.error("一口价价格过低");
                return;
            }

            if (!humanObj.hasMoney(price)) {
                Log.error("玩家货币不足,curMoney={}", humanObj.getMoney());
                return;
            }

            // 检验通过，玩家金额前往暂存区，使用auctionId当作cacheId
            moneyManager.addMoneyCache(humanObj, price, MoneyManager.ReduceType.Auction, auctionId);

            // 检验通过，开始修改属性
            auctionItem.setLastBidTime(now);
            auctionItem.setCurrentPrice(price);
            // 添加历史竞拍者。同一个玩家可以重复竞拍，但是金额是每次都扣
            if (auctionItem.getLastBidderId() > 0) {
                auctionItem.historyBidder.add(auctionItem.getLastBidderId());
            }
            auctionItem.setLastBidderId(humanObj.getId());

            if (fixed) {
                // 一口价，那么直接结束竞拍
                auctionItem.setFinished(true);
                // 直接开启结算
                settlementAuction(auctionItem);
            }
            // 日志输出
            Log.info("玩家：{}参与竞拍，竞拍物品={}，竞拍价格={}", humanObj.getId(), auctionId, price);
        } catch (Exception e) {
            Log.error("竞拍失败");
        } finally {
            auctionItem.lock.unlock();
        }
    }

    /**
     * 竞拍结算
     */
    private void settlementAuction(AuctionItem auctionItem) {
        if (!auctionItem.isFinished()) {
            return;
        }

        if (auctionItem.getLastBidderId() <= 0) {
            // 表示无人竞拍，归还物品给竞拍创建者

            return;
        }

        long playerId = auctionItem.getLastBidderId();
        String item = auctionItem.getItem();

        // 调用其他模块发放奖励


        // 移除暂存区的钱
        moneyManager.removeMoneyCache(auctionItem.getLastBidderId(), MoneyManager.ReduceType.Auction, auctionItem.getId());

        // 归还暂存区的钱
        moneyManager.repayMoneyCache(auctionItem.historyBidder, MoneyManager.ReduceType.Auction, auctionItem.getId());

        // 移除缓存
        auctionMap.remove(auctionItem.getId());

        // 日志输出
        Log.info("玩家{}竞拍成功，获得物品：{}", auctionItem.getId(), auctionItem.getItem());
    }

    /**
     * 竞拍品过期
     */
    public boolean isExpired(AuctionItem auctionItem) {
        return auctionItem.getCreateTime() + EXPIRE_TIME < TimeTools.getCurTime();
    }

    /**
     * 获取所有竞拍品
     */
    public List<AuctionItem> getAllAuction() {
        return auctionMap.values().stream()
                .toList();
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
