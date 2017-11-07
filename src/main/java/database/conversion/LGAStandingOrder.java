package database.conversion;

import org.joda.time.DateTime;

/**
 * Created by Thomas on 06.09.2015.
 */
public class LGAStandingOrder extends LGADatabaseItem {
    public static final DateTime Unlimited = new DateTime(3000, 12, 31, 23, 59);
    private int number;
    private String who;
    private float costs;
    private String name;
    private String category;
    private String user;
    private DateTime firstDate;
    private LGAFrequency LGAFrequency;
    private DateTime lastDate;

    public LGAStandingOrder(int id, String who, float costs, String name, String category, String user, DateTime firstDate, DateTime lastDate, LGAFrequency LGAFrequency, int number, String myId) {
        super(id, myId, myId);
        this.who = who;
        this.costs = costs;
        this.name = name;
        this.category = category;
        this.user = user;
        this.firstDate = firstDate;
        this.lastDate = lastDate;
        this.LGAFrequency = LGAFrequency;
        this.number = number;
    }

    public LGAStandingOrder(int id, String who, float costs, String name, String category, String user, DateTime firstDate, DateTime lastDate, LGAFrequency LGAFrequency, int number, DateTime insertDate, DateTime lastModified, String myId) {
        super(id, insertDate, lastModified, myId, myId);
        this.who = who;
        this.costs = costs;
        this.name = name;
        this.category = category;
        this.user = user;
        this.firstDate = firstDate;
        this.lastDate = lastDate;
        this.LGAFrequency = LGAFrequency;
        this.number = number;
    }

    public LGAStandingOrder(int id, String who, float costs, String name, String category, String user, DateTime firstDate, DateTime lastDate, LGAFrequency LGAFrequency, int number, DateTime insertDate, DateTime lastModified, boolean deleted, String myId) {
        super(id, insertDate, lastModified, deleted, myId, myId);
        this.who = who;
        this.costs = costs;
        this.name = name;
        this.category = category;
        this.user = user;
        this.firstDate = firstDate;
        this.lastDate = lastDate;
        this.LGAFrequency = LGAFrequency;
        this.number = number;
    }

    public LGAStandingOrder(int id, String who, float costs, String name, String category, String user, DateTime firstDate, DateTime lastDate, LGAFrequency LGAFrequency, int number, DateTime insertDate, DateTime lastModified, boolean deleted, String createdFrom, String lastChangedFrom) {
        super(id, insertDate, lastModified, deleted, createdFrom, lastChangedFrom);
        this.who = who;
        this.costs = costs;
        this.name = name;
        this.category = category;
        this.user = user;
        this.firstDate = firstDate;
        this.lastDate = lastDate;
        this.LGAFrequency = LGAFrequency;
        this.number = number;
    }

    public LGAStandingOrder(String who, float costs, String name, String category, String user, DateTime firstDate, DateTime lastDate, LGAFrequency LGAFrequency, int number, String myId) {
        super(myId, myId);
        this.who = who;
        this.costs = costs;
        this.name = name;
        this.category = category;
        this.user = user;
        this.firstDate = firstDate;
        this.lastDate = lastDate;
        this.LGAFrequency = LGAFrequency;
        this.number = number;
    }

    public LGAStandingOrder(String myId) {
        super(myId, myId);
    }

    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    public LGAFrequency getLGAFrequency() {
        return LGAFrequency;
    }

    public void setLGAFrequency(LGAFrequency LGAFrequency) {
        this.LGAFrequency = LGAFrequency;
    }

    public DateTime getLastDate() {
        return lastDate;
    }

    public void setLastDate(DateTime lastDate) {
        this.lastDate = lastDate;
    }

    @Override
    public String toString() {
        return "LGAStandingOrder{" +
                "id=" + id +
                ", number=" + number +
                ", who='" + who + '\'' +
                ", costs=" + costs +
                ", name='" + name + '\'' +
                ", category=" + category +
                ", user='" + user + '\'' +
                ", firstDate=" + firstDate.toString("yy-MM-dd HH:mm") +
                ", LGAFrequency=" + LGAFrequency +
                ", lastDate=" + lastDate.toString("yy-MM-dd HH:mm") +
                ", deleted=" + deleted +
                ", modifiedDate=" + lastModifiedDate.toString("yy-MM-dd HH:mm") +
                ", insertDate=" + insertDate.toString("yy-MM-dd HH:mm") +
                '}';
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getWho() {
        return who;
    }

    public void setWho(String who) {
        this.who = who;
    }

    public float getCosts() {
        return costs;
    }

    public void setCosts(float costs) {
        this.costs = costs;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public DateTime getFirstDate() {
        return firstDate;
    }

    public void setFirstDate(DateTime firstDate) {
        this.firstDate = firstDate;
    }

}
