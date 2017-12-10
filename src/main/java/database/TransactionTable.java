package database;

import java.sql.Connection;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public abstract class TransactionTable extends Table<TransactionID> {

    public TransactionTable(Connection connection) {
        super(connection);
    }

    @Override
    protected TransactionID getItem(ResultSet rs) {
        return new TransactionID("");
    }


    @Override
    protected List<String> getColumnNames() {
        return new ArrayList<>();
    }

    @Override
    protected String getTableSpecificCreateStatement() {
        return "";
    }

    @Override
    protected void setItem(NamedParameterStatement ps, TransactionID item) {

    }
}
