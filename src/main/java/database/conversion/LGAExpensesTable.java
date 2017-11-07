package database.conversion;


import database.NamedParameterStatement;
import org.joda.time.DateTime;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

/**
 * Created by Thomas on 06.09.2015.
 */
public class LGAExpensesTable {
    private static final String KEY_ID = "id";
    private static final String KEY_NAME = "name";
    private static final String KEY_DATE = "date";
    private static final String KEY_INSERT_DATE = "insert_date";
    private static final String KEY_MODIFIED_DATE = "modified_date";
    private static final String KEY_INSERT_ID = "insert_id";
    private static final String KEY_MODIFIED_ID = "modified_id";
    private static final String KEY_CATEGORY = "category";
    private static final String KEY_USER = "user";
    private static final String KEY_COSTS = "costs";
    private static final String KEY_WHO = "who";
    private static final String KEY_STANDING_ORDER = "standing_order";
    private static final String KEY_DELETED = "deleted";
    private static final String TABLE_NAME = "expenses";
    private static String LogKey = "LGAExpensesTable";
    private final Connection connection;

    public LGAExpensesTable(Connection connection) {
        this.connection = connection;
    }

    protected static String getCreateTableString() {
        return "CREATE TABLE " + TABLE_NAME + "("
                + KEY_ID + " INTEGER PRIMARY KEY,"
                + KEY_NAME + " TEXT,"
                + KEY_DATE + " LONG,"
                + KEY_CATEGORY + " TEXT,"
                + KEY_USER + " TEXT,"
                + KEY_COSTS + " INT,"
                + KEY_WHO + " TEXT,"
                + KEY_STANDING_ORDER + " INT,"
                + KEY_INSERT_DATE + " LONG,"
                + KEY_MODIFIED_DATE + " LONG,"
                + KEY_DELETED + " INT,"
                + KEY_INSERT_ID + " TEXT,"
                + KEY_MODIFIED_ID + " TEXT"
                + ");";
    }

    private static LGAExpenses resultToItem(ResultSet rs) throws SQLException {
        return new LGAExpenses(rs.getInt(1), rs.getString(7), rs.getInt(6) / 100f, rs.getString(2), rs.getString(4), rs.getString(5), new DateTime(rs.getLong(3)), rs.getInt(8) != 0, new DateTime(rs.getLong(9)), new DateTime(rs.getLong(10)), rs.getInt(11) != 0, rs.getString(12), rs.getString(13));
    }

    public ArrayList<LGAExpenses> getAll() throws SQLException {
        ArrayList<LGAExpenses> expenses = new ArrayList<>();

        String selectQuery = "SELECT * FROM " + TABLE_NAME;

        NamedParameterStatement statement = new NamedParameterStatement(connection, selectQuery);

        ResultSet rs = statement.executeQuery();
        while (rs.next()) {
            expenses.add(resultToItem(rs));
        }
        return expenses;
    }


}
