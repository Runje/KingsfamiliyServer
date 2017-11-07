package database.finance;

import com.koenig.commonModel.finance.BookkeepingEntry;
import com.koenig.commonModel.finance.CostDistribution;
import database.DatabaseItem;
import database.NamedParameterStatement;
import database.Table;

import java.nio.ByteBuffer;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public abstract class BookkeepingTable<T extends BookkeepingEntry> extends Table<T> {
    private static final String NAME = "name";
    private static final String CATEGORY = "category";
    private static final String SUBCATEGORY = "sub_category";
    private static final String COSTS = "costs";
    private static final String COSTDISTRIBUTION = "cost_distribution";


    public BookkeepingTable(Connection connection) {
        super(connection);
    }

    @Override
    protected T getItem(ResultSet rs) throws SQLException {
        String name = rs.getString(NAME);
        String category = rs.getString(CATEGORY);
        String subcategory = rs.getString(SUBCATEGORY);
        int costs = rs.getInt(COSTS);
        CostDistribution costDistribution = getCostDistribution(rs, COSTDISTRIBUTION);
        BookkeepingEntry entry = new BookkeepingEntry(name, category, subcategory, costs, costDistribution);

        return getBookkeepingItem(entry, rs);
    }

    protected abstract T getBookkeepingItem(BookkeepingEntry entry, ResultSet rs) throws SQLException;

    private CostDistribution getCostDistribution(ResultSet rs, String costDistribution) throws SQLException {
        ByteBuffer buffer = ByteBuffer.wrap(rs.getBytes(costDistribution));
        return new CostDistribution(buffer);
    }

    @Override
    protected String getTableSpecificCreateStatement() {
        return ", "
                + NAME + " TEXT,"
                + CATEGORY + " TEXT,"
                + SUBCATEGORY + " TEXT,"
                + COSTS + " INT,"
                + COSTDISTRIBUTION + " BLOB"
                + getBookkeepingTableSpecificCreateStatement();
    }

    protected abstract String getBookkeepingTableSpecificCreateStatement();

    @Override
    protected void setItem(NamedParameterStatement ps, BookkeepingEntry item) throws SQLException {
        ps.setString(NAME, item.getName());
        ps.setString(CATEGORY, item.getCategory());
        ps.setString(SUBCATEGORY, item.getSubCategory());
        ps.setInt(COSTS, item.getCosts());
        ps.setBytes(COSTDISTRIBUTION, item.getCostDistribution().getBytes());
        setBookkeepingItem(ps, (T) item);
    }

    protected abstract void setBookkeepingItem(NamedParameterStatement ps, T item) throws SQLException;

    @Override
    protected List<String> getColumnNames() {
        ArrayList<String> columnNames = new ArrayList<>();
        columnNames.add(NAME);
        columnNames.add(CATEGORY);
        columnNames.add(SUBCATEGORY);
        columnNames.add(COSTS);
        columnNames.add(COSTDISTRIBUTION);
        columnNames.addAll(getBookkeepingColumnNames());
        return columnNames;
    }

    protected abstract Collection<? extends String> getBookkeepingColumnNames();

    public List<DatabaseItem<T>> getDatabaseItemsFromName(String name) throws SQLException {
        lock.lock();
        try {
            List<DatabaseItem<T>> result = new ArrayList<>();
            DatabaseItem<T> item = null;
            String selectQuery = "SELECT * FROM " + getTableName() + " WHERE " + COLUMN_DELETED + " = :" + COLUMN_DELETED + " AND " + NAME + " = :" + NAME;

            NamedParameterStatement statement = new NamedParameterStatement(connection, selectQuery);
            statement.setInt(COLUMN_DELETED, FALSE);
            statement.setString(NAME, name);
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                item = resultToItem(rs);
                result.add(item);
            }

            return result;
        } finally {
            lock.unlock();
        }
    }

    public List<T> getFromName(String name) throws SQLException {
        return toItemList(getDatabaseItemsFromName(name));
    }
}
