package database.finance;

import com.koenig.commonModel.finance.Expenses;
import database.Database;
import database.conversion.Converter;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class FinanceDatabase extends Database {

    ExpensesTable expensesTable;
    StandingOrderTable standingOrderTable;

    public FinanceDatabase(Connection connection) throws SQLException {
        super(connection);
        expensesTable = new ExpensesTable(connection);
        standingOrderTable = new StandingOrderTable(connection);
        tables.add(expensesTable);
        tables.add(standingOrderTable);
        createAllTables();
    }

    public void start() throws SQLException {

        //convert();

    }

    private void convert() throws SQLException {
        logger.info("Starting conversion...");
        String thomasId = "c572d4e7-da4b-41d8-9c1f-7e9a97657155";
        String milenaId = "c6540de0-46bb-42cd-939b-ce52677fa19d";
        Converter converter = new Converter(expensesTable, standingOrderTable, milenaId, thomasId);
        converter.convert("D:\\Bibliotheken\\Dokumente\\finances_db_backup_2017.10.11.sqlite");
    }

    public List<Expenses> getAllExpenses() throws SQLException {
        return expensesTable.toItemList(expensesTable.getAll());
    }

    public void addExpenses(Expenses expenses, String userId) throws SQLException {
        expensesTable.addFrom(expenses, userId);
    }

    public void updateExpenses(Expenses expenses, String userId) throws SQLException {
        expensesTable.updateFrom(expenses, userId);
    }

    public void deleteExpenses(Expenses ex, String userId) throws SQLException {
        expensesTable.deleteFrom(ex.getId(), userId);
    }
}
