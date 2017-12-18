package database.finance;

import com.koenig.commonModel.Frequency;
import com.koenig.commonModel.database.DatabaseTable;
import com.koenig.commonModel.finance.BookkeepingEntry;
import com.koenig.commonModel.finance.StandingOrder;
import database.NamedParameterStatement;
import org.joda.time.DateTime;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class StandingOrderTable extends BookkeepingTable<StandingOrder> {

    public static final String NAME = "standing_order_table";
    private static final String FIRST_DATE = "first_date";
    private static final String END_DATE = "end_date";
    private static final String FREQUENCY = "frequency";
    private static final String FREQUENCY_FACTOR = "frequency_factor";
    private static final String EXECUTED_EXPENSES = "executed_expenses";

    public StandingOrderTable(Connection connection) {
        super(connection);
    }

    @Override
    protected StandingOrder getBookkeepingItem(BookkeepingEntry entry, ResultSet rs) throws SQLException {
        DateTime firstDate = getDateTime(rs, FIRST_DATE);
        DateTime endDate = getDateTime(rs, END_DATE);
        Frequency frequency = Frequency.valueOf(rs.getString(FREQUENCY));
        int frequencyFactor = rs.getInt(FREQUENCY_FACTOR);
        List<String> executedExpenses = DatabaseTable.getStringList(EXECUTED_EXPENSES);
        return new StandingOrder(entry, firstDate, endDate, frequency, frequencyFactor, executedExpenses);
    }


    @Override
    protected String getBookkeepingTableSpecificCreateStatement() {
        return "," + FIRST_DATE + " LONG, "
                + END_DATE + " LONG, "
                + FREQUENCY + " TEXT,"
                + FREQUENCY_FACTOR + " INT, "
                + EXECUTED_EXPENSES + " TEXT ";
    }

    @Override
    protected void setBookkeepingItem(NamedParameterStatement ps, StandingOrder item) throws SQLException {
        setDateTime(ps, FIRST_DATE, item.getFirstDate());
        setDateTime(ps, END_DATE, item.getEndDate());
        ps.setString(FREQUENCY, item.getFrequency().name());
        ps.setInt(FREQUENCY_FACTOR, item.getFrequencyFactor());
        setStringList(ps, EXECUTED_EXPENSES, item.getExecutedExpenses());
    }

    @Override
    protected Collection<? extends String> getBookkeepingColumnNames() {
        return Arrays.asList(FIRST_DATE, END_DATE, FREQUENCY, FREQUENCY_FACTOR, EXECUTED_EXPENSES);
    }

    @Override
    public String getTableName() {
        return NAME;
    }

    public void addExpensesToStandingOrders(String standingOrderId, String expensesId) throws SQLException {
        StandingOrder standingOrder = getFromId(standingOrderId);
        standingOrder.addExpenses(expensesId);
        update(standingOrderId, EXECUTED_EXPENSES, (ps -> setStringList(ps, EXECUTED_EXPENSES, standingOrder.getExecutedExpenses())));
    }


}
