package database.conversion;

import org.joda.time.DateTime;

/**
 * Created by Thomas on 24.11.2015.
 */
public abstract class LGADatabaseItem {
    protected boolean deleted;
    protected int id;
    protected DateTime insertDate;
    protected DateTime lastModifiedDate;
    protected String lastChangeFrom;
    protected String createdFrom;

    public LGADatabaseItem(int id, DateTime insertDate, DateTime lastModified, boolean deleted, String createdFrom, String lastChangeFrom) {
        this.id = id;
        this.insertDate = insertDate;
        this.lastModifiedDate = lastModified;
        this.deleted = deleted;
        this.createdFrom = createdFrom;
        this.lastChangeFrom = lastChangeFrom;
    }

    public LGADatabaseItem(int id, String createdFrom, String lastChangeFrom) {
        this(id, DateTime.now(), DateTime.now(), createdFrom, lastChangeFrom);
    }

    public LGADatabaseItem(String createdFrom, String lastChangeFrom) {
        this(0, DateTime.now(), DateTime.now(), createdFrom, lastChangeFrom);
    }

    public LGADatabaseItem(int id, DateTime insertDate, DateTime lastModifiedDate, String createdFrom, String lastChangeFrom) {
        this(id, insertDate, lastModifiedDate, false, createdFrom, lastChangeFrom);
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    @Override
    public String toString() {
        return "LGADatabaseItem{" +
                "deleted=" + deleted +
                ", id=" + id +
                ", insertDate=" + insertDate +
                ", lastModifiedDate=" + lastModifiedDate +
                ", lastChangeFrom='" + lastChangeFrom + '\'' +
                ", createdFrom='" + createdFrom + '\'' +
                '}';
    }

    public int getId() {
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

    public String getCreatedFrom() {
        return createdFrom;
    }

    public void setCreatedFrom(String createdFrom) {
        this.createdFrom = createdFrom;
    }

    public String getLastChangeFrom() {
        return lastChangeFrom;
    }

    public void setLastChangeFrom(String lastChangeFrom) {
        this.lastChangeFrom = lastChangeFrom;
    }

}
