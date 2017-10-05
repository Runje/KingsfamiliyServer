package database;

import org.joda.time.DateTime;

/**
 * Created by Thomas on 24.11.2015.
 */
public class DatabaseItem<T> {
    protected T item;
    protected boolean deleted;
    protected long id;
    protected DateTime insertDate;
    protected DateTime lastModifiedDate;
    protected String lastModifiedId;
    protected String insertId;

    public DatabaseItem(T item, long id, DateTime insertDate, DateTime lastModified, boolean deleted, String insertId, String lastModifiedId) {
        this.item = item;
        this.id = id;
        this.insertDate = insertDate;
        this.lastModifiedDate = lastModified;
        this.deleted = deleted;
        this.insertId = insertId;
        this.lastModifiedId = lastModifiedId;
    }

    public DatabaseItem(T item, long id, String insertId, String lastModifiedId) {
        this(item, id, DateTime.now(), DateTime.now(), insertId, lastModifiedId);
    }

    public DatabaseItem(T item, String insertId, String lastModifiedId) {
        this(item, 0, DateTime.now(), DateTime.now(), insertId, lastModifiedId);
    }

    public DatabaseItem(T item, long id, DateTime insertDate, DateTime lastModifiedDate, String insertId, String lastModifiedId) {
        this(item, id, insertDate, lastModifiedDate, false, insertId, lastModifiedId);
    }

    public DatabaseItem(T user, String id) {
        this(user, id, id);
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    @Override
    public String toString() {
        return "DatabaseItem{" +
                "deleted=" + deleted +
                ", id=" + id +
                ", insertDate=" + insertDate +
                ", lastModifiedDate=" + lastModifiedDate +
                ", lastModifiedId='" + lastModifiedId + '\'' +
                ", insertId='" + insertId + '\'' +
                '}';
    }

    public long getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public DateTime getInsertDate() {
        return insertDate;
    }

    public void setInsertDate(DateTime insertDate) {
        this.insertDate = insertDate;
    }

    public DateTime getLastModifiedDate() {
        return lastModifiedDate;
    }

    public void setLastModifiedDate(DateTime lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }

    public String getInsertId() {
        return insertId;
    }

    public void setInsertId(String insertId) {
        this.insertId = insertId;
    }

    public String getLastModifiedId() {
        return lastModifiedId;
    }

    public void setLastModifiedId(String lastModifiedId) {
        this.lastModifiedId = lastModifiedId;
    }

}
