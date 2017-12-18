package database;

import com.koenig.commonModel.Item;
import com.koenig.commonModel.database.DatabaseItem;
import com.koenig.commonModel.database.DatabaseTable;
import org.joda.time.DateTime;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public abstract class Table<T extends Item> extends DatabaseTable<T> {

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

        // deleteFrom last separator
        builder.deleteCharAt(builder.length() - 1);
        return builder.toString();
    }



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



    protected abstract T getItem(ResultSet rs) throws SQLException;

    protected DatabaseItem<T> resultToItem(ResultSet rs) throws SQLException {
        String id = rs.getString(COLUMN_ID);
        boolean deleted = getBool(rs, COLUMN_DELETED);
        DateTime insertDate = getDateTime(rs, COLUMN_INSERT_DATE);
        DateTime lastModifiedDate = getDateTime(rs, COLUMN_MODIFIED_DATE);
        String insertId = rs.getString(COLUMN_INSERT_ID);
        String modifiedId = rs.getString(COLUMN_MODIFIED_ID);
        String name = rs.getString(COLUMN_NAME);
        T item = getItem(rs);
        item.setId(id);
        item.setName(name);
        return new DatabaseItem<T>(item, insertDate, lastModifiedDate, deleted, insertId, modifiedId);
    }

    /**
     * Shall return the create statement for the specific tables field, i.e for field name and birthday:
     * name TEXT, birthday LONG
     *
     * @return
     */
    protected abstract String getTableSpecificCreateStatement();



    protected void setDateTime(NamedParameterStatement ps, String columnName, DateTime date) throws SQLException {
        ps.setLong(columnName, date.getMillis());
    }

    protected abstract void setItem(NamedParameterStatement ps, T item) throws SQLException;

    protected void setBool(NamedParameterStatement statement, String columnName, boolean b) throws SQLException {
        // TODO: use Short
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



    protected String getNamesOfSpecificParameter() {
        List<String> columnNames = getColumnNames();
        StringBuilder builder = new StringBuilder(", ");
        for (String columnName : columnNames) {
            builder.append(columnName + ", ");
        }

        return builder.substring(0, builder.length() - 2);
    }



    public void add(DatabaseItem<T> databaseItem) throws SQLException {
        runInLock(() -> {

            NamedParameterStatement ps = new NamedParameterStatement(connection, "insert into " + getTableName() +
                    "(" + COLUMN_ID + ", " + COLUMN_DELETED + ", " + COLUMN_INSERT_DATE + ", " + COLUMN_INSERT_ID + ", " + COLUMN_MODIFIED_DATE + ", " + COLUMN_MODIFIED_ID + ", " + COLUMN_NAME + getNamesOfSpecificParameter() + ") " +
                    " values(:" + COLUMN_ID + ", :" + COLUMN_DELETED + ", :" + COLUMN_INSERT_DATE + ", :" + COLUMN_INSERT_ID + ", :" + COLUMN_MODIFIED_DATE + ", :" + COLUMN_MODIFIED_ID + ", :" + COLUMN_NAME +
                    getNamesOfSpecificParameterWithColon() + ")");
            setDateTime(ps, COLUMN_MODIFIED_DATE, databaseItem.getLastModifiedDate());
            setDateTime(ps, COLUMN_INSERT_DATE, databaseItem.getInsertDate());
            ps.setString(COLUMN_INSERT_ID, databaseItem.getInsertId());
            ps.setString(COLUMN_MODIFIED_ID, databaseItem.getLastModifiedId());
            ps.setString(COLUMN_ID, databaseItem.getId());
            setBool(ps, COLUMN_DELETED, databaseItem.isDeleted());
            ps.setString(COLUMN_NAME, databaseItem.getName());
            setItem(ps, databaseItem.getItem());
            ps.executeUpdate();
        });
    }

    protected void setStringList(NamedParameterStatement ps, String name, List<String> list) throws SQLException {
        ps.setString(name, DatabaseTable.buildStringList(list));
    }



    public DatabaseItem<T> getDatabaseItemFromId(String id) throws SQLException {
        lock.lock();
        try {

            DatabaseItem<T> item = null;
            String selectQuery = "SELECT * FROM " + getTableName() + " WHERE " + COLUMN_DELETED + " = :" + COLUMN_DELETED + " AND " + COLUMN_ID + " = :" + COLUMN_ID;

            NamedParameterStatement statement = new NamedParameterStatement(connection, selectQuery);
            // TODO: only undeleted?
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

            return databaseItemFromId.getItem();
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



    public void deleteFrom(String itemId, String userId) throws SQLException {
        update(itemId, new String[]{COLUMN_DELETED, COLUMN_MODIFIED_ID, COLUMN_MODIFIED_DATE}, (ps -> {
            setBool(ps, COLUMN_DELETED, true);
            ps.setString(COLUMN_MODIFIED_ID, userId);
            setDateTime(ps, COLUMN_MODIFIED_DATE, DateTime.now());
        }));
    }

    public void updateFrom(T item, String userId) throws SQLException {
        List<String> columns = new ArrayList<>();
        columns.add(COLUMN_MODIFIED_ID);
        columns.add(COLUMN_MODIFIED_DATE);
        columns.add(COLUMN_NAME);
        columns.addAll(getColumnNames());
        update(item.getId(), columns.toArray(new String[]{}), (ps -> {
            ps.setString(COLUMN_MODIFIED_ID, userId);
            setDateTime(ps, COLUMN_MODIFIED_DATE, DateTime.now());
            ps.setString(COLUMN_NAME, item.getName());
            setItem(ps, item);
        }));


    }

    public List<DatabaseItem<T>> getChangesSinceDatabaseItems(DateTime lastSyncDate) throws SQLException {
        String query = "SELECT * FROM " + getTableName() + " WHERE " + COLUMN_MODIFIED_DATE + " > " + ":" + COLUMN_MODIFIED_DATE;
        NamedParameterStatement statement = new NamedParameterStatement(connection, query);
        setDateTime(statement, COLUMN_MODIFIED_DATE, lastSyncDate);

        return createListFromStatement(statement);
    }

    public List<T> getChangesSince(DateTime lastSyncDate) throws SQLException {
        return toItemList(getChangesSinceDatabaseItems(lastSyncDate));
    }

    private List<DatabaseItem<T>> createListFromStatement(NamedParameterStatement statement) throws SQLException {
        ResultSet rs = statement.executeQuery();
        List<DatabaseItem<T>> result = new ArrayList<>();
        while (rs.next()) {
            result.add(resultToItem(rs));
        }

        return result;
    }

    public boolean doesItemExist(String id) throws SQLException {
        String query = "SELECT COUNT(*) FROM " + getTableName() + " WHERE " + getNamedParameter(COLUMN_ID) + " LIMIT 1";
        NamedParameterStatement statement = new NamedParameterStatement(connection, query);
        statement.setString(COLUMN_ID, id);
        ResultSet rs = statement.executeQuery();
        if (rs.next()) {

            return rs.getInt(1) != 0;
        }

        throw new SQLException("Couldn't check existence");
    }

    public List<DatabaseItem<T>> getDatabaseItemsFromName(String name) throws SQLException {
        lock.lock();
        try {

            String selectQuery = "SELECT * FROM " + getTableName() + " WHERE " + COLUMN_DELETED + " = :" + COLUMN_DELETED + " AND " + COLUMN_NAME + " = :" + COLUMN_NAME;

            NamedParameterStatement statement = new NamedParameterStatement(connection, selectQuery);
            statement.setInt(COLUMN_DELETED, FALSE);
            statement.setString(COLUMN_NAME, name);
            return createListFromStatement(statement);
        } finally {
            lock.unlock();
        }
    }

    public List<T> getFromName(String name) throws SQLException {
        return toItemList(getDatabaseItemsFromName(name));
    }

    protected interface ParameterSetter {
        void set(NamedParameterStatement ps) throws SQLException;
    }
}
