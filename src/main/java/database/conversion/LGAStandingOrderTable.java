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
public class LGAStandingOrderTable {
    private static final String KEY_ID = "id";
    private static final String KEY_NAME = "name";
    private static final String KEY_FIRST_DATE = "first_date";
    private static final String KEY_LAST_DATE = "last_date";
    private static final String KEY_CATEGORY = "category";
    private static final String KEY_USER = "user";
    private static final String KEY_COSTS = "costs";
    private static final String KEY_WHO = "who";
    private static final String KEY_FREQUENCY = "frequency";
    private static final String KEY_NUMBER = "number";
    private static final String TABLE_NAME = "standing_order";
    private static final String KEY_INSERT_DATE = "insert_date";
    private static final String KEY_MODIFIED_DATE = "modified_date";
    private static final String KEY_INSERT_ID = "insert_id";
    private static final String KEY_MODIFIED_ID = "modified_id";
    private static final String KEY_DELETED = "deleted";
    private final Connection connection;

    public LGAStandingOrderTable(Connection connection) {
        this.connection = connection;
    }

    protected static String getCreateTableString() {
        return "CREATE TABLE " + TABLE_NAME + "("
                + KEY_ID + " INTEGER PRIMARY KEY,"
                + KEY_NAME + " TEXT,"
                + KEY_FIRST_DATE + " LONG,"
                + KEY_LAST_DATE + " LONG,"
                + KEY_CATEGORY + " TEXT,"
                + KEY_USER + " TEXT,"
                + KEY_COSTS + " INT,"
                + KEY_WHO + " TEXT,"
                + KEY_FREQUENCY + " TEXT,"
                + KEY_NUMBER + " INT,"
                + KEY_INSERT_DATE + " LONG,"
                + KEY_MODIFIED_DATE + " LONG,"
                + KEY_DELETED + " INT,"
                + KEY_INSERT_ID + " TEXT,"
                + KEY_MODIFIED_ID + " TEXT"
                + ");";
    }

    private static LGAStandingOrder createStandingOrderFromCursor(ResultSet cursor) throws SQLException {
        return new LGAStandingOrder(cursor.getInt(1), cursor.getString(8), cursor.getInt(7) / 100f, cursor.getString(2), String.valueOf(cursor.getString(5)), cursor.getString(6), new DateTime(cursor.getLong(3)), new DateTime(cursor.getLong(4)), LGAFrequency.valueOf(cursor.getString(9)), cursor.getInt(10), new DateTime(cursor.getLong(11)), new DateTime(cursor.getLong(12)), cursor.getInt(13) != 0, cursor.getString(14), cursor.getString(15));
    }

    public ArrayList<LGAStandingOrder> getAll() throws SQLException {
        ArrayList<LGAStandingOrder> standingOrders = new ArrayList<>();

        String selectQuery = "SELECT * FROM " + TABLE_NAME;

        NamedParameterStatement statement = new NamedParameterStatement(connection, selectQuery);

        ResultSet rs = statement.executeQuery();
        while (rs.next()) {
            standingOrders.add(createStandingOrderFromCursor(rs));
        }
        return standingOrders;
    }


}
