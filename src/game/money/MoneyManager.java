package game.money;

import game.auction.HumanObject;
import tools.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class MoneyManager {
    // 货币暂存区缓存,初始化时从数据库中读取
    static final Map<Long, List<MoneyCacheItem>> auctionMap = new ConcurrentHashMap<>();

    public void reduceMoney(HumanObject humanObj, double price) {

    }

    public void addMoney(HumanObject humanObj, double price) {

    }

    /**
     * 添加暂存区缓存
     */
    public void addMoneyCache(HumanObject humanObj, double price, ReduceType type, long caCheId) {
        // 先减少金额
        reduceMoney(humanObj, price);

        MoneyCacheItem cacheItem = new MoneyCacheItem(humanObj.getId(), price, caCheId);
        switch (type) {
            case Auction -> {
                // DB入库

                // 写入缓存
                List<MoneyCacheItem> list = auctionMap.get(humanObj.getId());
                if (list == null) {
                    list = new ArrayList<>();
                }
                list.add(cacheItem);
                auctionMap.put(humanObj.getId(), list);
            }
            // ... ...
        }
    }

    /**
     * 归还暂存区钱
     */
    public void repayMoneyCache(List<Long> ids, ReduceType type, long cacheId) {
        for (Long id : ids) {
            // 通过邮件等归还钱


            // 移除暂存区的钱
            removeMoneyCache(id, type, cacheId);

            // 日志输出
            Log.info("[货币暂存区]:归还玩家货币，id={}", id);
        }
    }

    /**
     * 移除暂存区的钱
     */
    public void removeMoneyCache(long playerId, ReduceType type, long cacheId) {
        switch (type) {
            case Auction -> {
                List<MoneyCacheItem> list = auctionMap.get(playerId);
                Optional<MoneyCacheItem> op = list.stream()
                        .filter(e -> e.getPlayerId() == playerId && e.getCacheId() == cacheId)
                        .findFirst();

                if (op.isPresent()) {
                    MoneyCacheItem cacheItem = op.get();
                    // 缓存删除
                    list.remove(cacheItem);
                    // DB删除
                    // ... ...
                }
            }
        }
    }

    /**
     * 货币暂存来源
     */
    public enum ReduceType {
        Auction,        // 竞拍
        Other
    }
}
