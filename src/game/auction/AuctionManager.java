package game.auction;

import game.money.MoneyManager;
import tools.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class AuctionManager {
    // 2min过期
    public static final int EXPIRE_TIME = 2 * 60 * 1000;

    // 这里应该是注入器注入
    MoneyManager moneyManager = new MoneyManager();

    // 存放所有竞拍品
    Map<Long, AuctionItem> auctionMap = new ConcurrentHashMap<>();

    private final Timer timer = new Timer(this::AuctionTimerTask);

    public void init() {
        timer.runWithFixedDelay(2, TimeUnit.SECONDS);   //  定时任务执行
    }

    public void AuctionTimerTask() {
        System.out.println("开始检查");
        // 遍历检查所有竞拍品是否可以开启结算
        for (AuctionItem item : auctionMap.values()) {
            if (isExpired(item)) {
                // 过期了
                item.lock.lock();
                try {
                    // 设置为已经完成
                    item.setFinished(true);
                } finally {
                    item.lock.unlock();
                }
                // 开启结算
                settlementAuction(item);
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
            humanObj.sendMsg(ReasonResult.failure("不存在此竞拍品, auctionId={}", auctionId));
            return;
        }

        auctionItem.lock.lock();
        ReasonResult result = ReasonResult.failure("");
        try {
            // 校验
            if (price <= auctionItem.getCurrentPrice()) {
                Log.error("出价过低, price={}", price);
                result = ReasonResult.failure("出价过低, price={}", price);
            }

            if (humanObj.getId() == auctionItem.getCreatorId()) {
                Log.error("自己不能参与竞拍");
                result = ReasonResult.failure("自己不能参与竞拍");
            }

            long now = TimeTools.getCurTime();
            if (auctionItem.isFinished() || isExpired(auctionItem)) {
                Log.error("该商品竞拍已经结束");
                result = ReasonResult.failure("该商品竞拍已经结束");
            }

            if (fixed && price < auctionItem.getFixedPrice()) {
                Log.error("一口价价格过低");
                result = ReasonResult.failure("一口价价格过低");
            }

            // 这一步放在最后，因为下面不仅会检查钱还会扣钱
            if (!humanObj.hasMoneyAndReduce(price)) {
                Log.error("玩家货币不足,curMoney={}", humanObj.getMoney());
                result = ReasonResult.failure("玩家货币不足,curMoney={}", humanObj.getMoney());
            }

            if (!result.result) {
                humanObj.sendMsg(result.getReason());
                return;
            }

            result = ReasonResult.SUCCESS;
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
            }
            // 日志输出
            Log.info("玩家：{}参与竞拍，竞拍物品={}，竞拍价格={}", humanObj.getId(), auctionId, price);
        } catch (Exception e) {
            Log.error("竞拍失败");
        } finally {
            auctionItem.lock.unlock();
        }

        if (auctionItem.isFinished()) {
            // 开启结算
            settlementAuction(auctionItem);
        }

        if (result.result) {
            // 竞价成功，发送给客户端
            humanObj.sendMsg(auctionItem.toString());
        }
    }

    /**
     * 竞拍结算
     */
    private void settlementAuction(AuctionItem auctionItem) {
        if (auctionItem == null) {
            return;
        }

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


        // 移除竞拍成功者货币暂存区的记录
        moneyManager.removeMoneyCache(auctionItem.getLastBidderId(), MoneyManager.ReduceType.Auction, auctionItem.getId());

        // 归还失败者，包括成功者之前的暂存区的钱
        moneyManager.repayMoneyCache(auctionItem.historyBidder, MoneyManager.ReduceType.Auction, auctionItem.getId());

        // 移除竞拍品缓存
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
