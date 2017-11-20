package database.finance;

import com.koenig.commonModel.Category;
import database.DatabaseItem;
import database.NamedParameterStatement;
import database.Table;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class CategoryTable extends Table<Category> {
    public static final String NAME = "category_table";
    private static final String MAIN = "main";
    private static final String SUBS = "subs";

    public CategoryTable(Connection connection) {
        super(connection);
    }

    @Override
    public String getTableName() {
        return NAME;
    }

    @Override
    protected Category getItem(ResultSet rs) throws SQLException {
        String main = rs.getString(MAIN);
        List<String> subs = getStringList(rs.getString(SUBS));
        return new Category(main, subs);
    }

    @Override
    protected String getTableSpecificCreateStatement() {
        return ", " + MAIN + " TEXT, " + SUBS + " TEXT";
    }

    @Override
    protected void setItem(NamedParameterStatement ps, Category item) throws SQLException {
        ps.setString(MAIN, item.getMain());
        setStringList(ps, SUBS, item.getSubs());
    }

    @Override
    protected List<String> getColumnNames() {
        ArrayList<String> columnNames = new ArrayList<>();
        columnNames.add(MAIN);
        columnNames.add(SUBS);
        return columnNames;
    }

    public void addIfNew(Category category, String userId) throws SQLException {
        runInLock(() -> {
            DatabaseItem<Category> dbCategory = getDatabaseItemFromName(category.getMain());
            if (dbCategory == null) {
                addFrom(category, userId);
            } else {
                boolean changed = false;
                // add subcategories if new

                for (String sub : category.getSubs()) {
                    boolean isNew = true;
                    for (String dbSub : dbCategory.getItem().getSubs()) {
                        if (sub.equals(dbSub)) {
                            isNew = false;
                            break;
                        }
                    }

                    if (isNew) {
                        // add subcategory
                        dbCategory.getItem().addSub(sub);
                        changed = true;
                    }
                }

                if (changed) {
                    updateFrom(dbCategory.getItem(), userId);
                }
            }
        });
    }

    public Category getFromName(String mainCategory) throws SQLException {
        DatabaseItem<Category> databaseItemFromName = runInLock(() -> getDatabaseItemFromName(mainCategory));

        return databaseItemFromName == null ? null : databaseItemFromName.getItem();
    }

    private DatabaseItem<Category> getDatabaseItemFromName(String mainCategory) throws SQLException {
        final DatabaseItem<Category>[] category = new DatabaseItem[1];
        runInLock(() -> {

                    String selectQuery = "SELECT * FROM " + getTableName() + " WHERE " + COLUMN_DELETED + " = :" + COLUMN_DELETED + " AND " + getNamedParameter(MAIN);

                    NamedParameterStatement statement = new NamedParameterStatement(connection, selectQuery);
                    statement.setInt(COLUMN_DELETED, FALSE);
                    statement.setString(MAIN, mainCategory);
                    ResultSet rs = statement.executeQuery();
                    while (rs.next()) {
                        category[0] = resultToItem(rs);
                    }
                }
        );

        return category[0];
    }
}
