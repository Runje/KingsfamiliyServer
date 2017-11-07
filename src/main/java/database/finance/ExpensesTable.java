package database.finance;

import com.koenig.commonModel.finance.BookkeepingEntry;
import com.koenig.commonModel.finance.Expenses;
import database.NamedParameterStatement;
import org.joda.time.DateTime;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;

public class ExpensesTable extends BookkeepingTable<Expenses> {

    public static final String NAME = "expenses_table";
    private static final String DATE = "date";
    private static final String STANDING_ORDER = "standing_order";

    public ExpensesTable(Connection connection) {
        super(connection);
    }

    @Override
    protected Expenses getBookkeepingItem(BookkeepingEntry entry, ResultSet rs) throws SQLException {
        DateTime date = getDateTime(rs, DATE);
        String standingOrder = rs.getString(STANDING_ORDER);
        return new Expenses(entry, date, standingOrder);
    }

    @Override
    protected String getBookkeepingTableSpecificCreateStatement() {
        return "," + DATE + " TEXT, " + STANDING_ORDER + " LONG";
    }

    @Override
    protected void setBookkeepingItem(NamedParameterStatement ps, Expenses item) throws SQLException {
        setDateTime(ps, DATE, item.getDate());
        ps.setString(STANDING_ORDER, item.getStandingOrder());
    }

    @Override
    protected Collection<? extends String> getBookkeepingColumnNames() {
        return Arrays.asList(DATE, STANDING_ORDER);
    }

    @Override
    public String getTableName() {
        return NAME;
    }
}
