package game.money;

public class MoneyCacheItem {
    private long playerId;
    private double price;
    private long cacheId;

    public MoneyCacheItem(long playerId, double price, long cacheId) {
        this.playerId = playerId;
        this.price = price;
        this.cacheId = cacheId;
    }

    public long getPlayerId() {
        return playerId;
    }

    public void setPlayerId(long playerId) {
        this.playerId = playerId;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(long price) {
        this.price = price;
    }

    public long getCacheId() {
        return cacheId;
    }

    public void setCacheId(long cacheId) {
        this.cacheId = cacheId;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof MoneyCacheItem item)) {
            return false;
        }

        return item.getCacheId() == cacheId &&
                item.getPlayerId() == playerId;
    }
}
