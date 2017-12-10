package database.finance;

import database.TransactionTable;

import java.sql.Connection;

public class FinanceTransactionTable extends TransactionTable {
    public final String NAME = "FinanceTransactionTable";

    public FinanceTransactionTable(Connection connection) {
        super(connection);
    }

    @Override
    public String getTableName() {
        return NAME;
    }
}
