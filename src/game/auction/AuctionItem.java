package game.auction;

import tools.TimeTools;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 竞拍品
 */
public class AuctionItem {
    // id
    private long id;
    // 创建者
    private long creatorId;
    // 初始价格
    private double initialPrice;
    // 是否可以一口价
    private boolean canFixed;
    // 一口价
    private double fixedPrice;
    // 当前竞价
    private double currentPrice;
    // 创建时间
    private long createTime;
    // 最新一次竞价时间
    private long lastBidTime;
    // 最新的竞价者
    private long lastBidderId;
    // 竞拍品内容
    private String item = "通用竞拍奖励";
    // 竞拍是否完成
    private boolean isFinished = false;
    // 竞拍是否已经结算
    private boolean isClosed = false;
    // 历史参与的竞拍者，改为线程安全队列，因为可能会多线程操作、读去，导致抛出异常
    public ConcurrentLinkedQueue<Long> bidderHistory = new ConcurrentLinkedQueue<>();   // 这里改为立即归还比较好

    public Lock lock = new ReentrantLock();


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
        return new AuctionItem(id, creatorId, initialPrice, canFixed, fixedPrice, initialPrice,
                TimeTools.getCurTime(), -1L, -1L, item);
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


    public boolean isFinished() {
        return isFinished;
    }

    public void setFinished(boolean finished) {
        isFinished = finished;
    }

    public boolean isClosed() {
        return isClosed;
    }

    public void setClosed(boolean closed) {
        isClosed = closed;
    }
}
