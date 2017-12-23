package database.conversion;


import org.joda.time.DateTime;

/**
 * Created by Thomas on 21.10.2015.
 */
public class LGABankAccount extends LGADatabaseItem {
    private String owner;
    private String name;
    private String bank;
    private float interest;
    private float balance;
    private DateTime date;
    private float monthly_costs;

    public LGABankAccount(String myId) {
        super(myId, myId);
    }

    public LGABankAccount(int id, String name, String bank, float interest, float monthly_costs, String owner, DateTime date, float balance, String myId) {
        super(id, myId, myId);
        this.owner = owner;
        this.date = date;
        this.name = name;
        this.bank = bank;
        this.interest = interest;
        this.monthly_costs = monthly_costs;
        this.balance = balance;
    }

    public LGABankAccount(int id, String name, String bank, float interest, float monthly_costs, String owner, DateTime date, float balance, DateTime insertDate, DateTime lastModified, String myId) {
        super(id, insertDate, lastModified, myId, myId);
        this.owner = owner;
        this.date = date;

        this.name = name;
        this.bank = bank;
        this.interest = interest;
        this.monthly_costs = monthly_costs;
        this.balance = balance;
    }

    public LGABankAccount(int id, String name, String bank, float interest, float monthly_costs, String owner, DateTime date, float balance, DateTime insertDate, DateTime lastModified, boolean deleted, String myId) {
        super(id, insertDate, lastModified, deleted, myId, myId);
        this.owner = owner;
        this.date = date;

        this.name = name;
        this.bank = bank;
        this.interest = interest;
        this.monthly_costs = monthly_costs;
        this.balance = balance;
    }

    public LGABankAccount(int id, String name, String bank, float interest, float monthly_costs, String owner, DateTime date, float balance, DateTime insertDate, DateTime lastModified, boolean deleted, String createdFrom, String lastChangeFrom) {
        super(id, insertDate, lastModified, deleted, createdFrom, lastChangeFrom);
        this.owner = owner;
        this.date = date;

        this.name = name;
        this.bank = bank;
        this.interest = interest;
        this.monthly_costs = monthly_costs;
        this.balance = balance;
    }

    @Override
    public String toString() {
        return "LGABankAccount{" +
                "owner='" + owner + '\'' +
                ", name='" + name + '\'' +
                ", bank='" + bank + '\'' +
                ", interest=" + interest +
                ", balance=" + balance +
                ", date=" + date.toString("yy-MM-dd HH:mm") +
                ", monthly_costs=" + monthly_costs +
                ", deleted=" + deleted +
                ", modifiedDate=" + lastModifiedDate.toString("yy-MM-dd HH:mm") +
                ", iinsertDate=" + insertDate.toString("yy-MM-dd HH:mm") +
                '}';
    }

    public int getId() {
        return id;
    }

    public DateTime getDate() {
        return date;
    }

    public void setDate(DateTime date) {
        this.date = date;
    }

    public float getBalance() {
        return balance;
    }

    public void setBalance(float balance) {
        this.balance = balance;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getName() {

        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBank() {
        return bank;
    }

    public void setBank(String bank) {
        this.bank = bank;
    }

    public float getInterest() {
        return interest;
    }

    public void setInterest(float interest) {
        this.interest = interest;
    }

    public float getMonthly_costs() {
        return monthly_costs;
    }

    public void setMonthly_costs(float monthly_costs) {
        this.monthly_costs = monthly_costs;
    }

    public String toUserString() {
        return getBank() + " - " + getName();
    }
}
