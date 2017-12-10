package database.finance;

import com.koenig.commonModel.Category;
import com.koenig.commonModel.database.DatabaseItem;
import database.NamedParameterStatement;
import database.Table;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class CategoryTable extends Table<Category> {
    public static final String NAME = "category_table";
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
        String main = rs.getString(COLUMN_NAME);
        List<String> subs = getStringList(rs.getString(SUBS));
        return new Category(main, subs);
    }

    @Override
    protected String getTableSpecificCreateStatement() {
        return ", " + SUBS + " TEXT";
    }

    @Override
    protected void setItem(NamedParameterStatement ps, Category item) throws SQLException {
        setStringList(ps, SUBS, item.getSubs());
    }

    @Override
    protected List<String> getColumnNames() {
        ArrayList<String> columnNames = new ArrayList<>();
        columnNames.add(SUBS);
        return columnNames;
    }

    public void addIfNew(Category category, String userId) throws SQLException {
        runInLock(() -> {
            List<DatabaseItem<Category>> dbCategorys = getDatabaseItemsFromName(category.getName());
            if (dbCategorys.size() == 0) {
                addFrom(category, userId);
            } else {
                DatabaseItem<Category> dbCategory = dbCategorys.get(0);
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


}
