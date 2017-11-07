package database;

import com.koenig.commonModel.Item;
import org.joda.time.DateTime;

import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

public abstract class Table<T extends Item> {
    public static final String COLUMN_INSERT_DATE = "insert_date";
    public static final String COLUMN_MODIFIED_DATE = "modified_date";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_INSERT_ID = "insert_id";
    public static final String COLUMN_MODIFIED_ID = "modified_id";
    public static final String COLUMN_DELETED = "deleted";
    public static final int FALSE = 0;
    public static final int TRUE = 1;
    private static final String STRING_LIST_SEPARATOR = ";";
    protected Connection connection;
    protected ReentrantLock lock = new ReentrantLock();

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

        // deleteFrom last separator
        builder.deleteCharAt(builder.length() - 1);
        return builder.toString();
    }

    public abstract String getTableName();

    public static String getNamedParameters(String[] parameters) {
        StringBuilder builder = new StringBuilder();
        for (String parameter : parameters) {
            builder.append(getNamedParameter(parameter) + ", ");
        }

        return builder.substring(0, builder.length() - 2);
    }

    public boolean isExisting() throws SQLException {
        lock.lock();
        try {
            DatabaseMetaData dbm = connection.getMetaData();
            ResultSet tables = dbm.getTables(null, null, getTableName(), null);
            return tables.next();
        } finally {
            lock.unlock();
        }
    }

    public void create() throws SQLException {
        lock.lock();
        try {
            Statement statement = connection.createStatement();
            statement.executeUpdate(buildCreateStatement());
            statement.close();
        } finally {
            lock.unlock();
        }
    }

    public List<DatabaseItem<T>> getAll() throws SQLException {
        lock.lock();
        try {

            ArrayList<DatabaseItem<T>> items = new ArrayList<>();
            String selectQuery = "SELECT * FROM " + getTableName() + " WHERE " + COLUMN_DELETED + " = :" + COLUMN_DELETED;

            NamedParameterStatement statement = new NamedParameterStatement(connection, selectQuery);
            statement.setInt(COLUMN_DELETED, FALSE);
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                items.add(resultToItem(rs));
            }

            return items;
        } finally {
            lock.unlock();
        }
    }

    public static String getNamedParameter(String parameter) {
        return parameter + " = :" + parameter;
    }

    public List<T> toItemList(List<DatabaseItem<T>> list) {
        List<T> users = new ArrayList<>(list.size());
        for (DatabaseItem<T> user : list) {
            users.add(user.item);
        }

        return users;
    }

    protected abstract T getItem(ResultSet rs) throws SQLException;

    protected DatabaseItem<T> resultToItem(ResultSet rs) throws SQLException {
        String id = rs.getString(COLUMN_ID);
        boolean deleted = getBool(rs, COLUMN_DELETED);
        DateTime insertDate = getDateTime(rs, COLUMN_INSERT_DATE);
        DateTime lastModifiedDate = getDateTime(rs, COLUMN_MODIFIED_DATE);
        String insertId = rs.getString(COLUMN_INSERT_ID);
        String modifiedId = rs.getString(COLUMN_MODIFIED_ID);
        T item = getItem(rs);
        item.setId(id);
        return new DatabaseItem<T>(item, insertDate, lastModifiedDate, deleted, insertId, modifiedId);
    }

    /**
     * Shall return the create statement for the specific tables field, i.e for field name and birthday:
     * name TEXT, birthday LONG
     *
     * @return
     */
    protected abstract String getTableSpecificCreateStatement();

    private String buildCreateStatement() {
        return "CREATE TABLE " + getTableName() + " (" +
                COLUMN_ID + " TEXT PRIMARY KEY, " +
                COLUMN_DELETED + " INT, " +
                COLUMN_INSERT_DATE + " LONG, " +
                COLUMN_INSERT_ID + " TEXT, " +
                COLUMN_MODIFIED_DATE + " LONG, " +
                COLUMN_MODIFIED_ID + " TEXT" +
                getTableSpecificCreateStatement() +
                ");";
    }

    protected void setDateTime(NamedParameterStatement ps, String columnName, DateTime date) throws SQLException {
        ps.setLong(columnName, date.getMillis());
    }

    protected abstract void setItem(NamedParameterStatement ps, T item) throws SQLException;

    protected void setBool(NamedParameterStatement statement, String columnName, boolean b) throws SQLException {
        statement.setInt(columnName, b ? 1 : 0);
    }

    protected String getNamesOfSpecificParameterWithColon() {
        List<String> columnNames = getColumnNames();
        StringBuilder builder = new StringBuilder(", :");
        for (String columnName : columnNames) {
            builder.append(columnName + ", :");
        }

        return builder.substring(0, builder.length() - 3);
    }

    protected abstract List<String> getColumnNames();

    protected String getNamesOfSpecificParameter() {
        List<String> columnNames = getColumnNames();
        StringBuilder builder = new StringBuilder(", ");
        for (String columnName : columnNames) {
            builder.append(columnName + ", ");
        }

        return builder.substring(0, builder.length() - 2);
    }

    public void addFrom(T item, String userId) throws SQLException {
        runInLock(() -> {
            DateTime now = DateTime.now();
            DatabaseItem<T> databaseItem = new DatabaseItem<>(item, now, now, false, userId, userId);
            add(databaseItem);
        });
    }

    public void add(DatabaseItem<T> databaseItem) throws SQLException {
        runInLock(() -> {

            NamedParameterStatement ps = new NamedParameterStatement(connection, "insert into " + getTableName() +
                    "(" + COLUMN_ID + ", " + COLUMN_DELETED + ", " + COLUMN_INSERT_DATE + ", " + COLUMN_INSERT_ID + ", " + COLUMN_MODIFIED_DATE + ", " + COLUMN_MODIFIED_ID + getNamesOfSpecificParameter() + ") " +
                    " values(:" + COLUMN_ID + ", :" + COLUMN_DELETED + ", :" + COLUMN_INSERT_DATE + ", :" + COLUMN_INSERT_ID + ", :" + COLUMN_MODIFIED_DATE + ", :" + COLUMN_MODIFIED_ID +
                    getNamesOfSpecificParameterWithColon() + ")");
            setDateTime(ps, COLUMN_MODIFIED_DATE, databaseItem.lastModifiedDate);
            setDateTime(ps, COLUMN_INSERT_DATE, databaseItem.insertDate);
            ps.setString(COLUMN_INSERT_ID, databaseItem.insertId);
            ps.setString(COLUMN_MODIFIED_ID, databaseItem.lastModifiedId);
            ps.setString(COLUMN_ID, databaseItem.getId());
            setBool(ps, COLUMN_DELETED, databaseItem.isDeleted());
            setItem(ps, databaseItem.item);
            ps.executeUpdate();
        });
    }

    protected List<String> getStringList(String executedExpenses) {
        String[] items = executedExpenses.split(STRING_LIST_SEPARATOR);
        // need to make a copy to allow List.add method! (Alternative create list  manually)
        return new ArrayList<>(Arrays.asList(items));
    }

    protected void setStringList(NamedParameterStatement ps, String name, List<String> list) throws SQLException {
        if (list.size() > 0) {
            StringBuilder builder = new StringBuilder();
            for (String s : list) {
                builder.append(s + STRING_LIST_SEPARATOR);
            }

            ps.setString(name, builder.substring(0, builder.length() - 1));
        } else {
            ps.setString(name, "");
        }
    }

    public DatabaseItem<T> getDatabaseItemFromId(String id) throws SQLException {
        lock.lock();
        try {

            DatabaseItem<T> item = null;
            String selectQuery = "SELECT * FROM " + getTableName() + " WHERE " + COLUMN_DELETED + " = :" + COLUMN_DELETED + " AND " + COLUMN_ID + " = :" + COLUMN_ID;

            NamedParameterStatement statement = new NamedParameterStatement(connection, selectQuery);
            statement.setInt(COLUMN_DELETED, FALSE);
            statement.setString(COLUMN_ID, id);
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                item = resultToItem(rs);
            }

            return item;
        } finally {
            lock.unlock();
        }
    }

    public T getFromId(String id) throws SQLException {
        lock.lock();
        try {

            DatabaseItem<T> databaseItemFromId = getDatabaseItemFromId(id);
            if (databaseItemFromId == null) {
                return null;
            }

            return databaseItemFromId.item;
        } finally {
            lock.unlock();
        }
    }

    public void deleteAllEntrys() throws SQLException {
        lock.lock();
        try {

            String query = "DELETE FROM " + getTableName();
            Statement statement = connection.createStatement();
            statement.execute(query);
        } finally {
            lock.unlock();
        }
    }

    public void update(String itemId, String updateParameter, ParameterSetter setter) throws SQLException {
        update(itemId, new String[]{updateParameter}, setter);
    }

    public void update(String itemId, String[] updateParameters, ParameterSetter setter) throws SQLException {
        runInLock(() -> {
            String selectQuery = "UPDATE " + getTableName() + " SET " + getNamedParameters(updateParameters) + " WHERE " + getNamedParameter(COLUMN_ID);

            NamedParameterStatement statement = new NamedParameterStatement(connection, selectQuery);
            statement.setString(COLUMN_ID, itemId);
            setter.set(statement);
            statement.executeUpdate();
        });
    }

    protected void runInLock(Database.Transaction runnable) throws SQLException {
        lock.lock();
        try {
            runnable.run();
        } finally {
            lock.unlock();
        }
    }

    public void deleteFrom(String itemId, String userId) throws SQLException {
        update(itemId, new String[]{COLUMN_DELETED, COLUMN_MODIFIED_ID, COLUMN_MODIFIED_DATE}, (ps -> {
            setBool(ps, COLUMN_DELETED, true);
            ps.setString(COLUMN_MODIFIED_ID, userId);
            setDateTime(ps, COLUMN_MODIFIED_DATE, DateTime.now());
        }));
    }

    public void updateFrom(T item, String userId) throws SQLException {
        List<String> columns = new ArrayList<>();
        columns.add(COLUMN_DELETED);
        columns.add(COLUMN_MODIFIED_ID);
        columns.add(COLUMN_MODIFIED_DATE);
        columns.addAll(getColumnNames());
        update(item.getId(), columns.toArray(new String[]{}), (ps -> {
            setBool(ps, COLUMN_DELETED, true);
            ps.setString(COLUMN_MODIFIED_ID, userId);
            setDateTime(ps, COLUMN_MODIFIED_DATE, DateTime.now());
            setItem(ps, item);
        }));


    }

    protected interface ParameterSetter {
        void set(NamedParameterStatement ps) throws SQLException;
    }
}
