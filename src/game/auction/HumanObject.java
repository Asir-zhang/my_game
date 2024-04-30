package game.auction;

import java.util.concurrent.locks.ReentrantLock;

public class HumanObject {
    private long id;
    private String name;
    // 所处的公会id
    public long unionId;

    public ReentrantLock lock = new ReentrantLock();

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public HumanObject() {

    }

    public HumanObject(long id, String name) {
        this.id = id;
        this.name = name;
    }

    public boolean hasUnion() {
        return unionId > 0;
    }

    public void setUnion(long unionId) {

    }

    /**
     * 拥有指定数量的物品
     */
    public boolean hasItem(String item, int num) {
        return true;
    }

    public void sendMsg(Object msg) {

    }

    /**
     * 拥有指定的货币并且减去
     * @return true：拥有并且已经成功减去；false：未拥有或者未成功减去
     * */
    public boolean hasMoneyAndReduce(double price) {
        return true;
    }

    public long getMoney() {
        return 1000L;
    }
}
