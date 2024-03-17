package game.auction;

public class HumanObject {
    private long id;
    private String name;

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

    public HumanObject(long id, String name) {
        this.id = id;
        this.name = name;
    }

    /**
     * 拥有指定数量的物品
     */
    public boolean hasItem(String item, int num) {
        return true;
    }

    public void sendMsg(Object msg) {

    }
}
