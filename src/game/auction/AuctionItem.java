package game.auction;

import tools.Log;
import tools.TimeTools;

/**
 * 竞拍品
 */
public class AuctionItem {
    // id
    long id;
    // 创建者
    long creatorId;
    // 初始价格
    double initialPrice;
    // 是否可以一口价
    boolean canFixed;
    // 一口价
    double fixedPrice;
    // 当前竞价
    double currentPrice;
    // 创建时间
    long createTime;
    // 最新一次竞价时间
    long lastBidTime;
    // 最新的竞价者
    long lastBidderId;
    // 竞拍品内容
    String item = "通用竞拍奖励";


    private AuctionItem(long id, long creatorId, double initialPrice, boolean canFixed,
                        double fixedPrice, double currentPrice, long createTime, long lastBidTime, long lastBidderId, String item) {
        this.id = id;
        this.creatorId = creatorId;
        this.initialPrice = initialPrice;
        this.canFixed = canFixed;
        this.fixedPrice = fixedPrice;
        this.currentPrice = currentPrice;
        this.createTime = createTime;
        this.lastBidTime = lastBidTime;
        this.lastBidderId = lastBidderId;
        this.item = item;
    }

    /**
     * 创建竞拍品
     */
    public static AuctionItem createAuctionItem(long id, long creatorId, double initialPrice, boolean canFixed, double fixedPrice, String item) {
        if (canFixed && fixedPrice <= initialPrice) {
            Log.error("初始价格大于等于了一口价价格,初始价格={}", initialPrice);
            return null;
        }

        return new AuctionItem(id, creatorId, initialPrice, canFixed, fixedPrice, initialPrice,
                TimeTools.getCurTime(), -1L, -1L, item);
    }

    /**
     * 可以创建竞拍品
     */
    public static boolean canCreateAucItem(long playerId, long creatorId, double initialPrice, boolean canFixed, double fixedPrice, String item) {
        if (canFixed && fixedPrice <= initialPrice) {
            Log.error("初始价格大于等于了一口价价格,初始价格={}", initialPrice);
            return false;
        }
        return true;
    }


    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getCreatorId() {
        return creatorId;
    }

    public void setCreatorId(long creatorId) {
        this.creatorId = creatorId;
    }

    public double getInitialPrice() {
        return initialPrice;
    }

    public void setInitialPrice(double initialPrice) {
        this.initialPrice = initialPrice;
    }

    public double getFixedPrice() {
        return fixedPrice;
    }

    public void setFixedPrice(double fixedPrice) {
        this.fixedPrice = fixedPrice;
    }

    public double getCurrentPrice() {
        return currentPrice;
    }

    public void setCurrentPrice(double currentPrice) {
        this.currentPrice = currentPrice;
    }

    public long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }

    public long getLastBidTime() {
        return lastBidTime;
    }

    public void setLastBidTime(long lastBidTime) {
        this.lastBidTime = lastBidTime;
    }

    public long getLastBidderId() {
        return lastBidderId;
    }

    public void setLastBidderId(long lastBidderId) {
        this.lastBidderId = lastBidderId;
    }

    public String getItem() {
        return item;
    }

    public void setItem(String item) {
        this.item = item;
    }

    public boolean getCanFixed() {
        return canFixed;
    }

    public void setCanFixed(boolean tf) {
        this.canFixed = tf;
    }

}
