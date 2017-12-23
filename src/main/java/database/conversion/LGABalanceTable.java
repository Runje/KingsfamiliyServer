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
public class LGABalanceTable {
    private static final String KEY_ID = "id";
    private static final String KEY_NAME = "name";
    private static final String KEY_DATE = "date";
    private static final String KEY_INSERT_DATE = "insert_date";
    private static final String KEY_MODIFIED_DATE = "modified_date";
    private static final String KEY_INSERT_ID = "insert_id";
    private static final String KEY_MODIFIED_ID = "modified_id";
    private static final String KEY_BANK = "bank";
    private static final String KEY_BALANCE = "balance";
    private static final String KEY_DELETED = "deleted";
    private static final String TABLE_NAME = "BalanceTable";
    private static String LogKey = "ExpensesTable";
    private final Connection connection;


    public LGABalanceTable(Connection connection) {
        this.connection = connection;
    }

    protected static String getCreateTableString() {
        return "CREATE TABLE " + TABLE_NAME + "("
                + KEY_ID + " INTEGER PRIMARY KEY,"
                + KEY_NAME + " TEXT,"
                + KEY_DATE + " LONG,"
                + KEY_BANK + " TEXT,"
                + KEY_BALANCE + " INT,"
                + KEY_INSERT_DATE + " LONG,"
                + KEY_MODIFIED_DATE + " LONG,"
                + KEY_DELETED + " INT,"
                + KEY_INSERT_ID + " TEXT,"
                + KEY_MODIFIED_ID + " TEXT"
                + ");";
    }

    private static LGABalance resultToItem(ResultSet cursor) throws SQLException {
        return new LGABalance(cursor.getInt(1), cursor.getInt(5) / 100f, new DateTime(cursor.getLong(3)), cursor.getString(4), cursor.getString(2), new DateTime(cursor.getLong(6)), new DateTime(cursor.getLong(7)), cursor.getInt(8) != 0, cursor.getString(9), cursor.getString(10));
    }

    public ArrayList<LGABalance> getAll() throws SQLException {
        ArrayList<LGABalance> LGABalances = new ArrayList<>();

        String selectQuery = "SELECT * FROM " + TABLE_NAME;

        NamedParameterStatement statement = new NamedParameterStatement(connection, selectQuery);

        ResultSet rs = statement.executeQuery();
        while (rs.next()) {
            LGABalances.add(resultToItem(rs));
        }

        return LGABalances;
    }


}
