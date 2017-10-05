package database;

import org.joda.time.DateTime;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public abstract class Table<T> {
    public static final String COLUMN_INSERT_DATE = "insert_date";
    public static final String COLUMN_MODIFIED_DATE = "modified_date";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_INSERT_ID = "insert_id";
    public static final String COLUMN_MODIFIED_ID = "modified_id";
    public static final String COLUMN_DELETED = "deleted";
    public static final int FALSE = 0;
    public static final int TRUE = 1;
    protected Connection connection;

    public Table(Connection connection) {
        this.connection = connection;
    }

    public static boolean getBool(ResultSet rs, String name) throws SQLException {
        return rs.getInt(name) != 0;
    }

    public static DateTime getDateTime(ResultSet rs, String name) throws SQLException {
        return new DateTime(rs.getLong(name));
    }

    public static String getParameter(int number) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < number; i++) {
            builder.append("?,");
        }

        // delete last separator
        builder.deleteCharAt(builder.length() - 1);
        return builder.toString();
    }

    public abstract String getTableName();

    public boolean isExisting() throws SQLException {
        DatabaseMetaData dbm = connection.getMetaData();
        ResultSet tables = dbm.getTables(null, null, getTableName(), null);
        // Table exists
// Table does not exist
        return tables.next();
    }

    public void create() throws SQLException {
        Statement statement = connection.createStatement();
        statement.executeUpdate(buildCreateStatement());
        statement.close();
    }

    public List<DatabaseItem<T>> getAll() throws SQLException {
        ArrayList<DatabaseItem<T>> items = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + getTableName() + " WHERE " + COLUMN_DELETED + " = :" + COLUMN_DELETED;

        NamedParameterStatement statement = new NamedParameterStatement(connection, selectQuery);
        statement.setInt(COLUMN_DELETED, FALSE);
        ResultSet rs = statement.executeQuery();
        while (rs.next()) {
            items.add(resultToItem(rs));
        }

        return items;
    }

    public List<T> ToItemList(List<DatabaseItem<T>> list) {
        List<T> users = new ArrayList<>(list.size());
        for (DatabaseItem<T> user : list) {
            users.add(user.item);
        }

        return users;
    }

    private DatabaseItem<T> resultToItem(ResultSet rs) throws SQLException {
        int id = rs.getInt(COLUMN_ID);
        boolean deleted = getBool(rs, COLUMN_DELETED);
        DateTime insertDate = getDateTime(rs, COLUMN_INSERT_DATE);
        DateTime lastModifiedDate = getDateTime(rs, COLUMN_MODIFIED_DATE);
        String insertId = rs.getString(COLUMN_INSERT_ID);
        String modifiedId = rs.getString(COLUMN_MODIFIED_ID);
        return new DatabaseItem<T>(getItem(rs), id, insertDate, lastModifiedDate, deleted, insertId, modifiedId);
    }

    protected abstract T getItem(ResultSet rs) throws SQLException;

    private String buildCreateStatement() {
        return "CREATE TABLE " + getTableName() + " (" +
                COLUMN_ID + " STRING PRIMARY KEY, " +
                COLUMN_DELETED + " INT, " +
                COLUMN_INSERT_DATE + " LONG, " +
                COLUMN_INSERT_ID + " TEXT, " +
                COLUMN_MODIFIED_DATE + " LONG, " +
                COLUMN_MODIFIED_ID + " TEXT" +
                getTableSpecificCreateStatement() +
                ");";
    }

    /**
     * Shall return the create statement for the specific tables field, i.e for field name and birthday:
     * name TEXT, birthday LONG
     *
     * @return
     */
    protected abstract String getTableSpecificCreateStatement();

    public void add(DatabaseItem<T> databaseItem) throws SQLException {
        NamedParameterStatement ps = new NamedParameterStatement(connection, "insert into " + getTableName() +
                "(" + COLUMN_ID + ", " + COLUMN_DELETED + ", " + COLUMN_INSERT_DATE + ", " + COLUMN_INSERT_ID + ", " + COLUMN_MODIFIED_DATE + ", " + COLUMN_MODIFIED_ID + getNamesOfSpecificParameter() + ") " +
                " values(:" + COLUMN_ID + ", :" + COLUMN_DELETED + ", :" + COLUMN_INSERT_DATE + ", :" + COLUMN_INSERT_ID + ", :" + COLUMN_MODIFIED_DATE + ", :" + COLUMN_MODIFIED_ID +
                getNamesOfSpecificParameterWithColon() + ")");
        setDateTime(ps, COLUMN_MODIFIED_DATE, databaseItem.lastModifiedDate);
        setDateTime(ps, COLUMN_INSERT_DATE, databaseItem.insertDate);
        ps.setString(COLUMN_INSERT_ID, databaseItem.insertId);
        ps.setString(COLUMN_MODIFIED_ID, databaseItem.lastModifiedId);
        ps.setLong(COLUMN_ID, databaseItem.getId());
        setBool(ps, COLUMN_DELETED, databaseItem.isDeleted());
        setItem(ps, databaseItem.item);
        ps.executeUpdate();
    }

    protected void setDateTime(NamedParameterStatement ps, String columnName, DateTime date) throws SQLException {
        ps.setLong(columnName, date.getMillis());
    }

    protected abstract void setItem(NamedParameterStatement ps, T item) throws SQLException;

    protected void setBool(NamedParameterStatement statement, String columnName, boolean b) throws SQLException {
        statement.setInt(columnName, b ? 1 : 0);
    }

    protected abstract String getNamesOfSpecificParameterWithColon();

    protected abstract String getNamesOfSpecificParameter();
}
