package database.conversion;


import org.joda.time.DateTime;

/**
 * Created by Thomas on 06.09.2015.
 */
public class LGAExpenses extends LGADatabaseItem {
    private String who;
    private float costs;
    private String name;
    private String category;
    private String user;
    private DateTime date;
    private boolean standingOrder;

    public LGAExpenses(int id, String who, float costs, String name, String category, String user, DateTime date, boolean standingOrder, String myId) {
        super(id, myId, myId);
        this.who = who;
        this.costs = costs;
        this.name = name;
        this.category = category;
        this.user = user;
        this.date = date;
        this.standingOrder = standingOrder;
    }

    public LGAExpenses(int id, String who, float costs, String name, String category, String user, DateTime date, boolean standingOrder, DateTime insertDate, DateTime lastModified, String myId) {
        super(id, insertDate, lastModified, myId, myId);
        this.who = who;
        this.costs = costs;
        this.name = name;
        this.category = category;
        this.user = user;
        this.date = date;
        this.standingOrder = standingOrder;
    }

    public LGAExpenses(int id, String who, float costs, String name, String category, String user, DateTime date, boolean standingOrder, DateTime insertDate, DateTime lastModified, boolean deleted, String myId) {
        super(id, insertDate, lastModified, deleted, myId, myId);
        this.who = who;
        this.costs = costs;
        this.name = name;
        this.category = category;
        this.user = user;
        this.date = date;
        this.standingOrder = standingOrder;
    }

    public LGAExpenses(int id, String who, float costs, String name, String category, String user, DateTime date, boolean standingOrder, DateTime insertDate, DateTime lastModified, boolean deleted, String createdFrom, String lastChangedFrom) {
        super(id, insertDate, lastModified, deleted, createdFrom, lastChangedFrom);
        this.who = who;
        this.costs = costs;
        this.name = name;
        this.category = category;
        this.user = user;
        this.date = date;
        this.standingOrder = standingOrder;
    }

    public LGAExpenses(String who, float costs, String name, String category, String user, DateTime date, boolean standingOrder, String myId) {
        super(myId, myId);
        this.who = who;
        this.costs = costs;
        this.name = name;
        this.category = category;
        this.user = user;
        this.date = date;
        this.standingOrder = standingOrder;

    }

    public LGAExpenses(String who, float costs, String name, String category, String user, DateTime date, boolean standingOrder, String createdFrom, String lastChangedFrom) {
        this(0, who, costs, name, category, user, date, standingOrder, DateTime.now(), DateTime.now(), false, createdFrom, lastChangedFrom);
    }

    public LGAExpenses(String myId) {
        super(myId, myId);
    }

    @Override
    public String toString() {
        return "LGAExpenses{" +
                "id=" + id +
                ", who='" + who + '\'' +
                ", costs=" + costs +
                ", name='" + name + '\'' +
                ", category=" + category +
                ", user='" + user + '\'' +
                ", date=" + date +
                ", standingOrder=" + standingOrder +
                '}' + super.toString();
    }

    public boolean isStandingOrder() {
        return standingOrder;
    }

    public void setStandingOrder(boolean standingOrder) {
        this.standingOrder = standingOrder;
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

    public DateTime getDate() {
        return date;
    }

    public void setDate(DateTime date) {
        this.date = date;
    }

    public String toReadableString() {
        return "LGAExpenses{" +
                "who='" + who + '\'' +
                ", costs=" + costs +
                ", name='" + name + '\'' +
                ", category=" + category +
                ", user='" + user + '\'' +
                ", date=" + date.toString("yy-MM-dd HH:mm") +
                ", standingOrder=" + standingOrder +
                ", deleted=" + deleted +
                '}';
    }
}
