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
public class LGABankAccountTable {
    private static final String KEY_ID = "id";
    private static final String KEY_NAME = "name";
    private static final String KEY_DATE = "date";
    private static final String KEY_INSERT_DATE = "insert_date";
    private static final String KEY_MODIFIED_DATE = "modified_date";
    private static final String KEY_INSERT_ID = "insert_id";
    private static final String KEY_MODIFIED_ID = "modified_id";
    private static final String KEY_BANK = "bank";
    private static final String KEY_OWNER = "owner";
    private static final String KEY_MONTHLY_COSTS = "monthly_costs";
    private static final String KEY_INTEREST = "interest";
    private static final String KEY_BALANCE = "balance";
    private static final String KEY_DELETED = "deleted";
    private static final String TABLE_NAME = "BankAccountTable";
    private static String LogKey = "ExpensesTable";
    private final Connection connection;

    public LGABankAccountTable(Connection connection) {
        this.connection = connection;
    }

    protected static String getCreateTableString() {
        return "CREATE TABLE " + TABLE_NAME + "("
                + KEY_ID + " INTEGER PRIMARY KEY,"
                + KEY_NAME + " TEXT,"
                + KEY_DATE + " LONG,"
                + KEY_BANK + " TEXT,"
                + KEY_OWNER + " TEXT,"
                + KEY_MONTHLY_COSTS + " INT,"
                + KEY_INTEREST + " INT,"
                + KEY_BALANCE + " INT,"
                + KEY_INSERT_DATE + " LONG,"
                + KEY_MODIFIED_DATE + " LONG,"
                + KEY_DELETED + " INT,"
                + KEY_INSERT_ID + " TEXT,"
                + KEY_MODIFIED_ID + " TEXT"
                + ");";
    }

    private static LGABankAccount resultToItem(ResultSet rs) throws SQLException {
        return new LGABankAccount(rs.getInt(1), rs.getString(2), rs.getString(4), rs.getInt(7) / 100f, rs.getInt(6) / 100f, rs.getString(5), new DateTime(rs.getLong(3)), rs.getInt(8) / 100f, new DateTime(rs.getLong(9)), new DateTime(rs.getLong(10)), rs.getInt(11) != 0, rs.getString(12), rs.getString(13));
    }

    public ArrayList<LGABankAccount> getAll() throws SQLException {
        ArrayList<LGABankAccount> lgaBankAccounts = new ArrayList<>();

        String selectQuery = "SELECT * FROM " + TABLE_NAME;
        NamedParameterStatement statement = new NamedParameterStatement(connection, selectQuery);

        ResultSet rs = statement.executeQuery();
        while (rs.next()) {
            lgaBankAccounts.add(resultToItem(rs));
        }
        return lgaBankAccounts;
    }

}
